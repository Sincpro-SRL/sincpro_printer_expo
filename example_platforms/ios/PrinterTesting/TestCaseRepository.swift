import Foundation

private let unreachableMac = "00:11:22:33:44:55"

struct TestFailure: LocalizedError {
  let errorDescription: String?
  init(_ message: String) { errorDescription = message }
}

/// Same catalogue as the Android testing app (TestCaseRepository.kt).
final class TestCaseRepository {
  private let service: PrinterService
  private let selectedAddress: () -> String?

  private lazy var logoBase64: String = resourceBase64("sincpro_simbolo", "png")
  private lazy var receiptPdfBase64: String = resourceBase64("comprobante_1767994755165", "pdf")

  init(service: PrinterService, selectedAddress: @escaping () -> String?) {
    self.service = service
    self.selectedAddress = selectedAddress
  }

  private func resourceBase64(_ name: String, _ ext: String) -> String {
    guard let url = Bundle.main.url(forResource: name, withExtension: ext), let data = try? Data(contentsOf: url) else {
      return ""
    }
    return data.base64EncodedString()
  }

  private func print(_ job: PrintJob) async throws -> String {
    try await service.call { try $0.print(job) }
    return "✓ Sent \(job.commands.count) command(s)"
  }

  private func expectError(_ code: PrinterErrorCode, _ task: () async throws -> Void) async throws -> String {
    do {
      try await task()
    } catch let error as PrinterError {
      guard error.code == code else { throw TestFailure("Expected \(code.rawValue), got \(error.code.rawValue): \(error.message)") }
      return "✓ \(error.code.rawValue): \(error.message)"
    }
    throw TestFailure("Expected \(code.rawValue) but the call succeeded")
  }

  private func requireAddress() throws -> String {
    guard let address = selectedAddress() else { throw TestFailure("Select a printer with Connect first") }
    return address
  }

  func allTestCases() -> [TestCase] {
    connectivityTests() + printTextTests() + printQrTests() + printBarcodeTests() + printBinaryTests()
      + printReceiptTests() + errorHandlingTests() + configurationTests()
  }

  private func connectivityTests() -> [TestCase] {
    [
      TestCase(id: "conn_01", name: "Get Paired Devices", description: "MFi printers with an active classic link", category: .connectivity, requiresConnection: false) {
        let devices = self.service.pairedDevices()
        return "Found \(devices.count) devices:\n" + devices.map { "• \($0.name) (\($0.address), \($0.transport.rawValue))" }.joined(separator: "\n")
      },
      TestCase(id: "conn_02", name: "Discover Printers", description: "BLE + classic discovery for 6 s", category: .connectivity, requiresConnection: false) {
        let devices = try await self.service.call { $0.discover(timeoutMs: 6000) }
        return "Found \(devices.count):\n" + devices.map { "• \($0.name) (\($0.address), \($0.transport.rawValue))" }.joined(separator: "\n")
      },
      TestCase(id: "conn_03", name: "Check Connection Status", description: "isConnected() and connection state", category: .connectivity, requiresConnection: false) {
        "isConnected=\(self.service.isConnected()) · \(self.service.connectionState())"
      },
      TestCase(id: "conn_04", name: "Get Printer Status", description: "Paper, cover, overheat, cutter, ribbon, media", category: .connectivity) {
        String(describing: try await self.service.call { $0.status() })
      },
      TestCase(id: "conn_05", name: "Get Printer Info", description: "Model, firmware, serial and dpi", category: .connectivity) {
        String(describing: try await self.service.call { try $0.info() })
      },
      TestCase(id: "conn_06", name: "Reconnect Same Printer", description: "Connecting again must keep the session", category: .connectivity) {
        let address = try self.requireAddress()
        try await self.service.call { try $0.connectBluetooth(address: address, timeoutMs: 10000) }
        guard self.service.isConnected() else { throw TestFailure("Session was lost on reconnect") }
        return "✓ Still connected: \(self.service.connectionState())"
      },
      TestCase(id: "conn_07", name: "Double Connect (regression)", description: "Two connects at once must end CONNECTED", category: .connectivity, requiresConnection: false) {
        let address = try self.requireAddress()
        try await self.service.call { $0.disconnect() }
        async let first: Void = self.service.call { try $0.connectBluetooth(address: address, timeoutMs: 10000) }
        async let second: Void = self.service.call { try $0.connectBluetooth(address: address, timeoutMs: 10000) }
        _ = try? await (first, second)
        guard self.service.isConnected() else { throw TestFailure("Not connected: \(self.service.connectionState())") }
        _ = try await self.print(TestJob().text("Double connect OK").build())
        return "✓ \(self.service.connectionState())"
      },
      TestCase(id: "conn_08", name: "Link Lost (manual)", description: "Run it, then TURN OFF the printer within 15 s", category: .connectivity) {
        for _ in 0..<30 {
          let connection = self.service.connectionState()
          if connection.state == .disconnected {
            guard connection.reason == .linkLost else { throw TestFailure("Unexpected reason: \(connection)") }
            return "✓ Detected: \(connection)"
          }
          try await Task.sleep(nanoseconds: 500_000_000)
        }
        throw TestFailure("Printer still reported as connected after 15 s")
      },
      TestCase(id: "conn_09", name: "Connect Timeout", description: "Unreachable MAC must fail (drops the current session)", category: .connectivity, requiresConnection: false) {
        let startedAt = Date()
        do {
          try await self.service.call { try $0.connectBluetooth(address: unreachableMac, timeoutMs: 4000) }
        } catch let error as PrinterError {
          return "✓ \(error.code.rawValue) after \(Int(Date().timeIntervalSince(startedAt) * 1000))ms"
        }
        throw TestFailure("Connected to an unreachable MAC")
      },
    ]
  }

  private func printTextTests() -> [TestCase] {
    [
      TestCase(id: "text_01", name: "Print Simple Text", description: "Medium font, left", category: .printText) {
        try await self.print(TestJob().text("Hello from Sincpro Printer").build())
      },
      TestCase(id: "text_02", name: "Print Text - Center Aligned", description: "Center alignment", category: .printText) {
        try await self.print(TestJob().text("CENTERED", align: .center).build())
      },
      TestCase(id: "text_03", name: "Print Text - Right Aligned", description: "Right alignment", category: .printText) {
        try await self.print(TestJob().text("RIGHT", align: .right).build())
      },
      TestCase(id: "text_04", name: "Print Text - Font Sizes", description: "Small, medium, large, xlarge", category: .printText) {
        try await self.print(
          TestJob().text("Small", font: .small).text("Medium").text("Large", font: .large).text("XLarge", font: .xlarge).build())
      },
      TestCase(id: "text_05", name: "Print Text - Bold", description: "Bold style", category: .printText) {
        try await self.print(TestJob().text("BOLD TEXT", bold: true).build())
      },
      TestCase(id: "text_06", name: "Print Text - Accents", description: "áéíóú ñ ¿¡", category: .printText) {
        try await self.print(TestJob().text("áéíóú ÁÉÍÓÚ ñÑ ¿¡ °").build())
      },
    ]
  }

  private func printQrTests() -> [TestCase] {
    [
      TestCase(id: "qr_01", name: "Print QR - URL", description: "Size 5, centered", category: .printQr) {
        try await self.print(TestJob().qr("https://sincpro.com.bo").build())
      },
      TestCase(id: "qr_02", name: "Print QR - Small", description: "Size 3", category: .printQr) {
        try await self.print(TestJob().qr("SMALL", size: 3).build())
      },
      TestCase(id: "qr_03", name: "Print QR - Large", description: "Size 8", category: .printQr) {
        try await self.print(TestJob().qr("LARGE", size: 8).build())
      },
      TestCase(id: "qr_04", name: "Print QR - Left Aligned", description: "Left", category: .printQr) {
        try await self.print(TestJob().qr("LEFT", align: .left).build())
      },
    ]
  }

  private func printBarcodeTests() -> [TestCase] {
    [
      TestCase(id: "barcode_01", name: "Print Barcode - CODE128", description: "CODE128 with text", category: .printBarcode) {
        try await self.print(TestJob().barcode("SINCPRO123").build())
      },
      TestCase(id: "barcode_02", name: "Print Barcode - CODE39", description: "CODE39", category: .printBarcode) {
        try await self.print(TestJob().barcode("ABC123", symbology: .code39).build())
      },
      TestCase(id: "barcode_03", name: "Print Barcode - EAN13", description: "EAN13", category: .printBarcode) {
        try await self.print(TestJob().barcode("7501031311309", symbology: .ean13).build())
      },
      TestCase(id: "barcode_04", name: "Print Barcode - Tall", description: "Bar height 120", category: .printBarcode) {
        try await self.print(TestJob().barcode("TALL", barHeight: 120).build())
      },
    ]
  }

  private func printBinaryTests() -> [TestCase] {
    [
      TestCase(id: "image_01", name: "Print Image (logo)", description: "PNG fixture, centered", category: .printImage) {
        try await self.print(TestJob().image(self.logoBase64).build())
      },
      TestCase(id: "pdf_01", name: "PDF Page Count", description: "Pages of the sample receipt PDF", category: .printPdf, requiresConnection: false) {
        "Pages: \(self.service.pdfPageCount(self.receiptPdfBase64))"
      },
      TestCase(id: "pdf_02", name: "Print PDF Page 1", description: "Sample receipt PDF at 72mm", category: .printPdf) {
        try await self.print(TestJob().pdf(self.receiptPdfBase64).build())
      },
    ]
  }

  private func printReceiptTests() -> [TestCase] {
    [
      TestCase(id: "receipt_01", name: "Print Receipt", description: "Logo, text, separator, QR and barcode", category: .printReceipt) {
        try await self.print(
          TestJob()
            .image(self.logoBase64)
            .text("SINCPRO SRL", font: .large, bold: true, align: .center)
            .text("Comprobante de prueba", align: .center)
            .separator()
            .text("Coca Cola 2L          2   30.00", font: .small)
            .text("Pan                   5   10.00", font: .small)
            .separator()
            .text("TOTAL          Bs 40.00", bold: true)
            .qr("https://sincpro.com.bo/r/123", size: 4)
            .barcode("123456789", barHeight: 50)
            .text("Gracias por su compra", align: .center)
            .space(40)
            .build())
      },
      TestCase(id: "receipt_02", name: "Print Receipt x2 Copies", description: "Same job, two copies", category: .printReceipt) {
        try await self.print(TestJob().text("COPY TEST", font: .large, align: .center).build(copies: 2))
      },
    ]
  }

  private func errorHandlingTests() -> [TestCase] {
    [
      TestCase(id: "error_01", name: "Print While Disconnected", description: "NOT_CONNECTED (disconnects first)", category: .errorHandling, requiresConnection: false) {
        try await self.expectError(.notConnected) {
          try await self.service.call { $0.disconnect() }
          _ = try await self.print(TestJob().text("should not print").build())
        }
      },
      TestCase(id: "error_02", name: "Invalid Image", description: "INVALID_ARGUMENT", category: .errorHandling) {
        try await self.expectError(.invalidArgument) {
          let job = PrintJob(
            media: TestJob().build().media, copies: 1,
            commands: [.image(x: 0, y: 20, height: 0, base64: "bm90LWFuLWltYWdl", width: 200)])
          _ = try await self.print(job)
        }
      },
      TestCase(id: "error_03", name: "PDF Page Out of Range", description: "Page 99 → INVALID_ARGUMENT", category: .errorHandling) {
        try await self.expectError(.invalidArgument) {
          _ = try await self.print(TestJob().pdf(self.receiptPdfBase64, page: 99).build())
        }
      },
    ]
  }

  private func configurationTests() -> [TestCase] {
    [
      TestCase(id: "config_01", name: "Density Extra Dark", description: "Same config the tickets app uses", category: .configuration) {
        try await self.service.call { $0.setConfig(PrinterConfig(density: .extraDark)) }
        _ = try await self.print(TestJob().text("Extra dark density").build())
        return "✓ \(self.service.config)"
      },
      TestCase(id: "config_02", name: "Speed Medium", description: "Speed is applied only when set", category: .configuration) {
        var config = self.service.config
        config.speed = .medium
        try await self.service.call { [config] in $0.setConfig(config) }
        _ = try await self.print(TestJob().text("Speed medium").build())
        return "✓ \(self.service.config)"
      },
      TestCase(id: "config_03", name: "Reset Defaults", description: "Margins 0, density medium", category: .configuration) {
        try await self.service.call { $0.setConfig(PrinterConfig()) }
        return "✓ \(self.service.config)"
      },
    ]
  }
}
