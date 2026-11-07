import Foundation

enum TestStatus: String {
  case pending = "PENDING"
  case running = "RUNNING"
  case passed = "PASSED"
  case failed = "FAILED"
}

enum TestCategory: String, CaseIterable, Identifiable {
  case connectivity = "Connectivity"
  case printText = "Text"
  case printQr = "QR"
  case printBarcode = "Barcode"
  case printImage = "Image"
  case printPdf = "PDF"
  case printReceipt = "Receipt"
  case errorHandling = "Errors"
  case configuration = "Config"

  var id: String { rawValue }
}

/// Same test catalogue as the Android testing app (TestCase.kt).
struct TestCase: Identifiable {
  let id: String
  let name: String
  let description: String
  let category: TestCategory
  var requiresConnection = true
  let action: () async throws -> String
}
