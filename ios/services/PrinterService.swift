import Foundation
import UIKit

/// Owns the printer session. Every Bixolon SDK call runs on one serial queue (`enqueue`) because
/// the SDK blocks and is not safe to call concurrently; the connection state is owned here and
/// verified against the SDK link.
final class PrinterService {
  private let queue = DispatchQueue(label: "sincpro.printer.sdk", qos: .userInitiated)
  private let lock = NSLock()
  private let client = BixolonClient()
  private let onConnectionChange: (Connection) -> Void

  private var connection = Connection.idle
  private var currentConfig = PrinterConfig()
  private var currentDpi = 203

  init(onConnectionChange: @escaping (Connection) -> Void) {
    self.onConnectionChange = onConnectionChange
    client.onLinkBroken = { [weak self] in self?.handleLinkLost() }
  }

  func enqueue<T>(_ task: @escaping (PrinterService) throws -> T, done: @escaping (Result<T, Error>) -> Void) {
    queue.async { done(Result { try task(self) }) }
  }

  private func locked<T>(_ body: () -> T) -> T {
    lock.lock()
    defer { lock.unlock() }
    return body()
  }

  private func publish(_ next: Connection) {
    let changed = locked { () -> Bool in
      guard next != connection else { return false }
      connection = next
      return true
    }
    if changed { onConnectionChange(next) }
  }

  private func handleLinkLost() {
    let current = locked { connection }
    guard current.state == .connected else { return }
    publish(Connection(state: .disconnected, address: current.address, reason: .linkLost))
    queue.async { self.client.disconnect() }
  }

  private func refresh() -> Connection {
    let current = locked { connection }
    if current.state == .connected && !client.isLinkUp { handleLinkLost() }
    return locked { connection }
  }

  private func requireConnected() throws {
    guard refresh().state == .connected else {
      throw PrinterError(code: .notConnected, message: "No printer is connected")
    }
  }

  /// Runs on the SDK queue.
  ///
  /// 1. Reuse the session when already connected to the same printer.
  /// 2. Drop any previous session so the printer is free.
  /// 3. Connect, read the dpi and apply the stored configuration.
  /// 4. Final: publish CONNECTED, or DISCONNECTED with the error on failure.
  private func connect(address: String, attempt: () throws -> Void) throws {
    let current = refresh()
    if current.state == .connected && current.address?.uppercased() == address.uppercased() { return }

    client.disconnect()
    publish(Connection(state: .connecting, address: address, reason: nil))
    do {
      try attempt()
      let dpi = client.dpi()
      let config = locked { () -> PrinterConfig in
        currentDpi = dpi
        return currentConfig
      }
      client.applyConfig(config)
      publish(Connection(state: .connected, address: address, reason: .connected))
    } catch {
      client.disconnect()
      publish(Connection(state: .disconnected, address: address, reason: .connectFailed))
      throw error
    }
  }

  var config: PrinterConfig { locked { currentConfig } }

  func dpi() -> Int { locked { currentDpi } }

  func pairedDevices() -> [BluetoothDevice] { client.pairedDevices() }

  func connectionState() -> Connection { refresh() }

  func isConnected() -> Bool { refresh().state == .connected }

  func pdfPageCount(_ base64: String) -> Int { PdfRasterizer.pageCount(base64) }

  func discover(timeoutMs: Int) -> [BluetoothDevice] { client.discover(timeoutMs: timeoutMs) }

  func connectBluetooth(address: String, timeoutMs: Int) throws {
    try connect(address: address.uppercased()) {
      try client.connectBluetooth(address: address, timeoutMs: timeoutMs)
    }
  }

  func connectWifi(ip: String, port: Int, timeoutMs: Int) throws {
    try connect(address: "\(ip):\(port)") {
      try client.connectWifi(ip: ip, port: port, timeoutMs: timeoutMs)
    }
  }

  func disconnect() {
    client.disconnect()
    let address = locked { connection.address }
    publish(Connection(state: .disconnected, address: address, reason: .userDisconnect))
  }

  func status() -> PrinterStatus {
    let state = refresh().state
    guard state == .connected else { return .idle(state) }
    guard let flags = client.readStatus() else { return .idle(state, error: "Status read failed") }
    return PrinterStatus(
      connectionState: state,
      hasPaper: !flags.paperEmpty,
      isCoverOpen: flags.coverOpen,
      isOverheated: flags.overheated,
      isCutterJammed: flags.cutterJammed,
      isRibbonEnd: flags.ribbonEnd,
      hasMediaError: flags.mediaError,
      errorMessage: nil)
  }

  func info() throws -> PrinterInfo {
    try requireConnected()
    return client.readInfo()
  }

  func setConfig(_ next: PrinterConfig) {
    locked { currentConfig = next }
    if refresh().state == .connected { client.applyConfig(next) }
  }

  private func decodeImages(_ commands: [PrintCommand]) throws -> [Int: UIImage] {
    var images: [Int: UIImage] = [:]
    for (index, command) in commands.enumerated() {
      switch command {
      case .image(_, _, _, let base64, _):
        guard let image = ImageDecoder.decode(base64) else {
          throw PrinterError(code: .invalidArgument, message: "The image could not be decoded")
        }
        images[index] = image
      case .pdfPage(_, _, _, let base64, let page, let width):
        guard let image = PdfRasterizer.render(base64, page: page, targetWidth: width) else {
          throw PrinterError(code: .invalidArgument, message: "PDF page \(page) could not be rendered")
        }
        images[index] = image
      default:
        continue
      }
    }
    return images
  }

  func print(_ job: PrintJob) throws {
    try requireConnected()
    let images = try decodeImages(job.commands)
    do {
      try client.print(job, images: images)
    } catch {
      _ = refresh()
      throw error
    }
  }

  func destroy() {
    queue.async { self.client.release() }
  }
}
