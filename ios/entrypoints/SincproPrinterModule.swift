import ExpoModulesCore

private let connectionEvent = "onConnectionChange"

private func printerException(_ code: PrinterErrorCode, _ message: String) -> Exception {
  Exception(name: "PrinterError", description: message, code: code.rawValue)
}

private func toException(_ error: Error) -> Exception {
  if let error = error as? PrinterError { return printerException(error.code, error.message) }
  if let error = error as? Exception { return error }
  return printerException(.printFailed, error.localizedDescription)
}

private func toDictionary(_ connection: Connection) -> [String: Any?] {
  ["state": connection.state.rawValue, "address": connection.address, "reason": connection.reason?.rawValue]
}

private func toDictionary(_ device: BluetoothDevice) -> [String: Any] {
  ["name": device.name, "address": device.address, "transport": device.transport.rawValue]
}

private func toDictionary(_ status: PrinterStatus) -> [String: Any?] {
  [
    "connectionState": status.connectionState.rawValue,
    "hasPaper": status.hasPaper,
    "isCoverOpen": status.isCoverOpen,
    "isOverheated": status.isOverheated,
    "isCutterJammed": status.isCutterJammed,
    "isRibbonEnd": status.isRibbonEnd,
    "hasMediaError": status.hasMediaError,
    "hasError": status.hasError,
    "errorMessage": status.errorMessage,
  ]
}

private func toDictionary(_ info: PrinterInfo) -> [String: Any] {
  ["model": info.model, "firmware": info.firmware, "serial": info.serial, "dpi": info.dpi]
}

/// Exposure layer only: maps JS calls to `PrinterService` and its results back to JS values.
public final class SincproPrinterModule: Module {
  private var printerService: PrinterService?

  private func runOnSdk<T>(
    _ promise: Promise, _ task: @escaping (PrinterService) throws -> T,
    toJs: @escaping (T) -> Any? = { _ in nil }
  ) {
    guard let service = printerService else {
      promise.reject(printerException(.bluetoothUnavailable, "Printer module not ready"))
      return
    }
    service.enqueue(task) { result in
      switch result {
      case .success(let value): promise.resolve(toJs(value))
      case .failure(let error): promise.reject(toException(error))
      }
    }
  }

  public func definition() -> ModuleDefinition {
    Name("SincproPrinter")

    Events(connectionEvent)

    OnCreate {
      self.printerService = PrinterService { [weak self] connection in
        self?.sendEvent(connectionEvent, toDictionary(connection))
      }
    }

    OnDestroy {
      self.printerService?.destroy()
      self.printerService = nil
    }

    Function("getPairedDevices") { () -> [[String: Any]] in
      (self.printerService?.pairedDevices() ?? []).map(toDictionary)
    }

    AsyncFunction("scan") { (timeoutMs: Int, promise: Promise) in
      self.runOnSdk(promise, { $0.discover(timeoutMs: timeoutMs) }) { $0.map(toDictionary) }
    }

    AsyncFunction("connectBluetooth") { (address: String, timeoutMs: Int, promise: Promise) in
      self.runOnSdk(promise, { try $0.connectBluetooth(address: address, timeoutMs: timeoutMs) })
    }

    AsyncFunction("connectWifi") { (ip: String, port: Int, timeoutMs: Int, promise: Promise) in
      self.runOnSdk(promise, { try $0.connectWifi(ip: ip, port: port, timeoutMs: timeoutMs) })
    }

    AsyncFunction("connectUsb") { (promise: Promise) in
      promise.reject(printerException(.unsupportedPlatform, "USB printers are not supported on iOS"))
    }

    AsyncFunction("disconnect") { (promise: Promise) in
      self.runOnSdk(promise, { $0.disconnect() })
    }

    Function("isConnected") { () -> Bool in
      self.printerService?.isConnected() ?? false
    }

    Function("getConnection") { () -> [String: Any?] in
      toDictionary(self.printerService?.connectionState() ?? .idle)
    }

    AsyncFunction("getStatus") { (promise: Promise) in
      self.runOnSdk(promise, { $0.status() }) { toDictionary($0) }
    }

    AsyncFunction("getInfo") { (promise: Promise) in
      self.runOnSdk(promise, { try $0.info() }) { toDictionary($0) }
    }

    Function("getDpi") { () -> Int in
      self.printerService?.dpi() ?? 203
    }

    AsyncFunction("setConfig") { (config: PrinterConfigRecord, promise: Promise) in
      let next = config.toDomain()
      self.runOnSdk(promise, { $0.setConfig(next) })
    }

    Function("getConfig") { () -> [String: Any?] in
      (self.printerService?.config ?? PrinterConfig()).toDictionary()
    }

    AsyncFunction("print") { (job: PrintJobRecord, promise: Promise) in
      do {
        let printJob = try job.toDomain()
        self.runOnSdk(promise, { try $0.print(printJob) })
      } catch {
        promise.reject(toException(error))
      }
    }

    Function("getPdfPageCount") { (base64: String) -> Int in
      PdfRasterizer.pageCount(base64)
    }
  }
}
