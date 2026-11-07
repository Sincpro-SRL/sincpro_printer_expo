import Foundation
import UIKit
import frmLabelPrinterSDK

private let interfaceBluetooth = 4
private let interfaceBluetoothNonExclusive = 0xF4
private let interfaceBle = 10
private let continuousTailDots = 100
private let imageLevel: UInt = 50
private let autoCutterEveryLabel = 1
private let statusTimeout: DispatchTimeInterval = .seconds(4)

/// Raw status bitmask (Defines.h `__SDK_STATUS`): each bit is one printer condition.
struct StatusFlags {
  let value: Int
  var paperEmpty: Bool { value & 0x80 != 0 }
  var coverOpen: Bool { value & 0x40 != 0 }
  var cutterJammed: Bool { value & 0x20 != 0 }
  var overheated: Bool { value & 0x10 != 0 }
  var mediaError: Bool { value & 0x08 != 0 }
  var ribbonEnd: Bool { value & 0x04 != 0 }
}

/// Wraps LabelPrinterSDK (v1.1.16). Every SDK call blocks, and delegate callbacks and receivers
/// arrive on the main queue, so callers must use this from one background serial queue
/// (PrinterService guarantees it) and never wait for a receiver on the main thread.
///
/// Verified in the SDK binary: `connectWithTarget:timeout:` takes milliseconds, while
/// `connectWithAddress:port:` connects over TCP for dotted IPs and over BLE for MAC addresses.
final class BixolonClient: NSObject, LabelPrinterSDKDelegate {
  private let sdk = LabelPrinterSDK()
  private let lookupLock = NSLock()
  private var lookupFinished: (([LabelPrinterObject]) -> Void)?

  /// Called on the main queue when the SDK reports a broken link.
  var onLinkBroken: (() -> Void)?

  override init() {
    super.init()
    LabelPrinterSDK.setEnableLog(false, saveFile: false, hexDump: false)
    sdk.delegate = self
    _ = sdk.open()
  }

  var isLinkUp: Bool { sdk.isConnected() }

  private func transport(of printer: LabelPrinterObject) -> BluetoothTransport {
    switch printer.getInterfaceType() {
    case interfaceBluetooth, interfaceBluetoothNonExclusive: return .classic
    case interfaceBle: return .ble
    default: return .unknown
    }
  }

  private func device(of printer: LabelPrinterObject) -> BluetoothDevice? {
    guard let mac = printer.getMacAddress(), !mac.isEmpty else { return nil }
    let name = printer.getBluetoothDeviceName() ?? printer.getModelName() ?? "Bixolon"
    return BluetoothDevice(name: name, address: mac.uppercased(), transport: transport(of: printer))
  }

  private func pairedPrinters() -> [LabelPrinterObject] {
    (sdk.getPairedDevices() as? [LabelPrinterObject]) ?? []
  }

  /// MFi printers whose Bluetooth classic link is up; iOS exposes no other "paired" list.
  func pairedDevices() -> [BluetoothDevice] {
    pairedPrinters().compactMap(device(of:))
  }

  /// Context: discovery scans BLE (and the local network); classic MFi printers come from the
  /// connected accessories. Results are merged by MAC address.
  func discover(timeoutMs: Int) -> [BluetoothDevice] {
    let semaphore = DispatchSemaphore(value: 0)
    var found: [LabelPrinterObject] = []
    lookupLock.lock()
    lookupFinished = { printers in
      found = printers
      semaphore.signal()
    }
    lookupLock.unlock()

    let seconds = max(1, timeoutMs / 1000)
    sdk.lookupPrinter(withCount: CGFloat(seconds), interval: 1.0)
    if semaphore.wait(timeout: .now() + .milliseconds(timeoutMs + 3000)) == .timedOut {
      _ = sdk.cancelToLookupPrinter()
    }

    lookupLock.lock()
    lookupFinished = nil
    lookupLock.unlock()

    var devices: [String: BluetoothDevice] = [:]
    for printer in pairedPrinters() + found {
      guard let device = device(of: printer), device.transport != .unknown else { continue }
      devices[device.address] = devices[device.address] ?? device
    }
    return Array(devices.values)
  }

  private func connectError(target: String, code: Int, startedAt: Date, timeoutMs: Int) -> PrinterError {
    let elapsedMs = Int(Date().timeIntervalSince(startedAt) * 1000)
    if timeoutMs > 0 && elapsedMs >= timeoutMs {
      return PrinterError(code: .connectTimeout, message: "\(target) did not answer within \(timeoutMs)ms")
    }
    return PrinterError(
      code: .connectFailed, message: "Could not connect to \(target) (SDK code 0x\(String(code, radix: 16)))")
  }

  /// Classic MFi printers connect through the accessory found by MAC; otherwise the MAC is
  /// treated as a BLE printer, which the SDK scans for while connecting.
  func connectBluetooth(address: String, timeoutMs: Int) throws {
    let mac = address.uppercased()
    let startedAt = Date()
    let code: Int
    if let accessory = pairedPrinters().first(where: { $0.getMacAddress()?.uppercased() == mac }) {
      code = sdk.connect(withTarget: accessory, timeout: timeoutMs).rawValue
    } else {
      code = sdk.connect(withAddress: mac, port: "").rawValue
    }
    guard code == 0, sdk.isConnected() else {
      throw connectError(target: "Bluetooth \(mac)", code: code, startedAt: startedAt, timeoutMs: timeoutMs)
    }
  }

  func connectWifi(ip: String, port: Int, timeoutMs: Int) throws {
    let startedAt = Date()
    sdk.setKeepNetworkConnection(1)
    let code = sdk.connect(withAddress: ip, port: String(port)).rawValue
    guard code == 0, sdk.isConnected() else {
      throw connectError(target: "WiFi \(ip):\(port)", code: code, startedAt: startedAt, timeoutMs: timeoutMs)
    }
  }

  func disconnect() {
    if sdk.isConnected() { _ = sdk.disconnect() }
  }

  func release() {
    disconnect()
    _ = sdk.close()
  }

  /// The receiver fires on the main queue after the printer answers; a missing answer times out.
  func readStatus() -> StatusFlags? {
    let semaphore = DispatchSemaphore(value: 0)
    var status: Int?
    let code = sdk.checkPrinterStatus { value in
      status = value
      semaphore.signal()
    }
    guard code.rawValue == 0, semaphore.wait(timeout: .now() + statusTimeout) == .success,
      let value = status, value >= 0
    else { return nil }
    return StatusFlags(value: value)
  }

  func dpi() -> Int {
    let value = sdk.getDpi()
    return value > 0 ? value : 203
  }

  func readInfo() -> PrinterInfo {
    PrinterInfo(
      model: sdk.getModelName() ?? "",
      firmware: sdk.getFirmwareVersion() ?? "",
      serial: sdk.getSerialNumber() ?? "",
      dpi: dpi())
  }

  /// Same firmware codes as Android (`SPEED_25IPS` = 0 … `SPEED_80IPS` = 6).
  private func speedCode(_ speed: Speed) -> Int {
    switch speed {
    case .slow: return 0
    case .medium: return 3
    case .fast: return 5
    case .extraFast: return 6
    }
  }

  /// Best effort: a setting the model rejects is logged and the rest still apply.
  func applyConfig(_ config: PrinterConfig) {
    var results: [(String, Int)] = [
      ("setMargin", sdk.setMargin(config.marginLeft, verticalMargin: config.marginTop).rawValue),
      ("setDensity", sdk.setDensity(config.density.level).rawValue),
      ("setOrientation", sdk.setOrientation(config.orientation == .topToBottom ? 0 : 1).rawValue),
      (
        "setAutoCutter",
        sdk.setAutoCutter(config.autoCutter, cuttingPeriod: autoCutterEveryLabel).rawValue
      ),
    ]
    if let speed = config.speed {
      results.append(("setSpeed", sdk.setSpeed(speedCode(speed)).rawValue))
    }
    for (operation, code) in results where code != 0 {
      NSLog("[SincproPrinter] %@ rejected by the printer (code %ld)", operation, code)
    }
  }

  private func ensureOk(_ operation: String, _ code: Int) throws {
    guard code == 0 else {
      throw PrinterError(
        code: .printFailed, message: "\(operation) failed (SDK code 0x\(String(code, radix: 16)))")
    }
  }

  private func eccLevel(_ level: QrErrorCorrection) -> Int {
    switch level {
    case .l: return 0
    case .m: return 1
    case .q: return 2
    case .h: return 3
    }
  }

  private func mediaCode(_ type: MediaType) -> CChar {
    switch type {
    case .continuous: return CChar(UInt8(ascii: "C"))
    case .gap: return CChar(UInt8(ascii: "G"))
    case .blackMark: return CChar(UInt8(ascii: "B"))
    }
  }

  /// The SDK scales images to the command width keeping the aspect ratio; PDF pages arrive pre-scaled.
  private func drawnHeight(_ command: PrintCommand, image: UIImage?) -> Int {
    guard let image else { return command.height }
    if case .image(_, _, _, _, let width) = command, image.size.width > 0 {
      return Int(image.size.height * CGFloat(width) / image.size.width)
    }
    return Int(image.size.height)
  }

  private func draw(_ command: PrintCommand, image: UIImage?) throws {
    switch command {
    case .text(let x, let y, _, let text, let font, let bold):
      try ensureOk(
        "drawText",
        sdk.drawTextDeviceFont(
          text, xPosition: x, yPosition: y, fontSelection: CChar(font), fontWidth: 1, fontHeight: 1,
          rightSideCharacterSpacing: 0, fontRotation: 0, reverse: false, bold: bold, textAlignment: 0
        ).rawValue)
    case .qr(let x, let y, _, let data, let size, let errorCorrection):
      try ensureOk(
        "drawQr",
        sdk.drawBarcodeQRCode(
          data, xPosition: x, yPosition: y, barcodeSize: size, model: 2,
          errorColectionLevel: eccLevel(errorCorrection), rotation: 0
        ).rawValue)
    case .barcode(let x, let y, _, let data, let symbology, let narrow, let wide, let barHeight, let showText):
      try ensureOk(
        "drawBarcode",
        sdk.drawBarcode1D(
          data, xPosition: x, yPosition: y, barcodeType: symbology, widthNarrow: narrow,
          widthWide: wide, height: barHeight, hri: showText ? 1 : 0, quietZoneWidth: 0, rotation: 0
        ).rawValue)
    case .image(let x, let y, _, _, let width):
      guard let image else { throw PrinterError(code: .invalidArgument, message: "Missing image") }
      try ensureOk(
        "drawImage",
        sdk.draw(image, startPosX: x, startPosY: y, width: width, level: imageLevel, useDithering: true)
          .rawValue)
    case .pdfPage(let x, let y, _, _, _, _):
      guard let image else { throw PrinterError(code: .invalidArgument, message: "Missing PDF page") }
      try ensureOk(
        "drawPdfPage",
        sdk.draw(
          image, startPosX: x, startPosY: y, width: Int(image.size.width), level: imageLevel,
          useDithering: true
        ).rawValue)
    }
  }

  /// Context: mirrors the Android sequence; the iOS SDK has no transaction mode and sends every
  /// call to the printer immediately.
  ///
  /// 1. Set width and page length (continuous pages fit the drawn content).
  /// 2. Draw every command, failing on the first rejected one.
  /// 3. Final: print the copies.
  func print(_ job: PrintJob, images: [Int: UIImage]) throws {
    let contentBottom =
      job.commands.enumerated().map { index, command in
        command.y + drawnHeight(command, image: images[index])
      }.max() ?? 0
    let length =
      job.media.type == .continuous ? contentBottom + continuousTailDots : job.media.heightDots
    let gap = job.media.type == .continuous ? 0 : job.media.gapDots

    try ensureOk("setWidth", sdk.setWidth(job.media.widthDots).rawValue)
    try ensureOk(
      "setLength",
      sdk.setLength(length, gapLength: gap, mediaType: mediaCode(job.media.type), offsetLength: 0).rawValue)
    for (index, command) in job.commands.enumerated() {
      try draw(command, image: images[index])
    }
    try ensureOk("doPrint", sdk.doPrint(job.copies).rawValue)
  }

  // MARK: - LabelPrinterSDKDelegate (callbacks arrive on the main queue)

  func willConnect(_ controller: LabelPrinterSDK!, printer: LabelPrinterObject!) {}

  func didConnect(_ controller: LabelPrinterSDK!, printer: LabelPrinterObject!) {}

  func didNotConnect(_ controller: LabelPrinterSDK!, printer: LabelPrinterObject!, withError error: Error!) {}

  func didDisconnect(_ controller: LabelPrinterSDK!, printer: LabelPrinterObject!) {}

  func didBeBrokenConnection(
    _ controller: LabelPrinterSDK!, printer: LabelPrinterObject!, withError error: Error!
  ) {
    onLinkBroken?()
  }

  func willLookupPrinter(_ controller: LabelPrinterSDK!) {}

  func didFindPrinter(_ controller: LabelPrinterSDK!, printer: LabelPrinterObject!) {}

  private func finishLookup(_ printers: [LabelPrinterObject]) {
    lookupLock.lock()
    let finished = lookupFinished
    lookupFinished = nil
    lookupLock.unlock()
    finished?(printers)
  }

  func didLookupPrinters(_ controller: LabelPrinterSDK!, printerList: [Any]!) {
    finishLookup((printerList as? [LabelPrinterObject]) ?? [])
  }

  func canNotFoundPrinter() {
    finishLookup([])
  }
}
