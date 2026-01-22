/**
 * Expo Module for Sincpro Printer SDK
 *
 * iOS Stub Implementation:
 * This module provides a compatible API surface for iOS builds,
 * but does not implement actual printer functionality.
 * All functions return safe default values without throwing exceptions.
 *
 * Production use is only supported on Android platform.
 */
import ExpoModulesCore

public class ExpoBixolonModule: Module {
  public func definition() -> ModuleDefinition {
    Name("SincproPrinter")

    // ============================================================
    // BLUETOOTH API
    // ============================================================

    Function("getPairedDevices") {
      return []
    }

    Function("getPairedPrinters") {
      return []
    }

    // ============================================================
    // CONNECTION API
    // ============================================================

    AsyncFunction("connectBluetooth") { (address: String, timeoutMs: Double?) in
      // No-op: iOS stub
    }

    AsyncFunction("connectWifi") { (ip: String, port: Int?, timeoutMs: Double?) in
      // No-op: iOS stub
    }

    AsyncFunction("connectUsb") {
      // No-op: iOS stub
    }

    AsyncFunction("disconnect") {
      // No-op: iOS stub
    }

    Function("isConnected") {
      return false
    }

    AsyncFunction("getStatus") {
      return [
        "connectionState": "DISCONNECTED",
        "hasPaper": false,
        "isCoverOpen": false,
        "isOverheated": false,
        "hasError": false,
        "errorMessage": nil
      ]
    }

    AsyncFunction("getInfo") {
      return [
        "model": "",
        "firmware": "",
        "serial": "",
        "dpi": 0
      ]
    }

    Function("getDpi") {
      return 203
    }

    // ============================================================
    // CONFIGURATION API
    // ============================================================

    AsyncFunction("setConfig") { (config: [String: Any]) in
      // No-op: iOS stub
    }

    Function("getConfig") {
      return [:]
    }

    // ============================================================
    // PRINT API - TEXT
    // ============================================================

    AsyncFunction("printText") { (text: String, options: [String: Any]?) in
      // No-op: iOS stub
    }

    AsyncFunction("printTexts") { (texts: [String], options: [String: Any]?) in
      // No-op: iOS stub
    }

    // ============================================================
    // PRINT API - QR & BARCODE
    // ============================================================

    AsyncFunction("printQR") { (data: String, options: [String: Any]?) in
      // No-op: iOS stub
    }

    AsyncFunction("printBarcode") { (data: String, options: [String: Any]?) in
      // No-op: iOS stub
    }

    // ============================================================
    // PRINT API - IMAGES & PDF
    // ============================================================

    AsyncFunction("printImageBase64") { (base64Data: String, options: [String: Any]?) in
      // No-op: iOS stub
    }

    AsyncFunction("printPdfBase64") { (base64Data: String, options: [String: Any]?) in
      // No-op: iOS stub
    }

    Function("getPdfPageCount") { (base64Data: String) in
      return 0
    }

    // ============================================================
    // PRINT API - RECEIPT & KEY-VALUE
    // ============================================================

    AsyncFunction("printReceipt") { (receiptData: [String: Any], options: [String: Any]?) in
      // No-op: iOS stub
    }

    AsyncFunction("printKeyValue") { (key: String, value: String, options: [String: Any]?) in
      // No-op: iOS stub
    }
  }
}
