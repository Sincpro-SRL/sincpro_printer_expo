import Foundation

enum ConnectionState: String {
  case disconnected = "DISCONNECTED"
  case connecting = "CONNECTING"
  case connected = "CONNECTED"
}

enum ConnectionChangeReason: String {
  case connected
  case userDisconnect = "user_disconnect"
  case linkLost = "link_lost"
  case connectFailed = "connect_failed"
}

struct Connection: Equatable {
  let state: ConnectionState
  let address: String?
  let reason: ConnectionChangeReason?

  static let idle = Connection(state: .disconnected, address: nil, reason: nil)
}

enum BluetoothTransport: String {
  case classic
  case ble
  case unknown
}

struct BluetoothDevice {
  let name: String
  let address: String
  let transport: BluetoothTransport
}

struct PrinterStatus {
  let connectionState: ConnectionState
  let hasPaper: Bool
  let isCoverOpen: Bool
  let isOverheated: Bool
  let isCutterJammed: Bool
  let isRibbonEnd: Bool
  let hasMediaError: Bool
  let errorMessage: String?

  var hasError: Bool {
    !hasPaper || isCoverOpen || isOverheated || isCutterJammed || isRibbonEnd || hasMediaError
      || errorMessage != nil
  }

  static func idle(_ state: ConnectionState, error: String? = nil) -> PrinterStatus {
    PrinterStatus(
      connectionState: state, hasPaper: false, isCoverOpen: false, isOverheated: false,
      isCutterJammed: false, isRibbonEnd: false, hasMediaError: false, errorMessage: error)
  }
}

struct PrinterInfo {
  let model: String
  let firmware: String
  let serial: String
  let dpi: Int
}

enum Density: String {
  case light
  case medium
  case dark
  case extraDark = "extra_dark"

  var level: Int {
    switch self {
    case .light: return 5
    case .medium: return 10
    case .dark: return 15
    case .extraDark: return 20
    }
  }
}

enum Speed: String {
  case slow
  case medium
  case fast
  case extraFast = "extra_fast"
}

enum Orientation: String {
  case topToBottom = "top_to_bottom"
  case bottomToTop = "bottom_to_top"
}

/// `speed` stays nil unless the app sets it, so each printer keeps its own configured speed.
struct PrinterConfig {
  var marginLeft = 0
  var marginTop = 0
  var density = Density.medium
  var speed: Speed?
  var orientation = Orientation.topToBottom
  var autoCutter = false
  var fullCut = true
}

enum PrinterErrorCode: String {
  case notConnected = "NOT_CONNECTED"
  case connectTimeout = "CONNECT_TIMEOUT"
  case connectFailed = "CONNECT_FAILED"
  case unsupportedModel = "UNSUPPORTED_MODEL"
  case bluetoothUnavailable = "BLUETOOTH_UNAVAILABLE"
  case printFailed = "PRINT_FAILED"
  case statusFailed = "STATUS_FAILED"
  case invalidArgument = "INVALID_ARGUMENT"
  case unsupportedPlatform = "UNSUPPORTED_PLATFORM"
}

/// The single error type of the printer layer; the code tells JS what happened.
struct PrinterError: Error {
  let code: PrinterErrorCode
  let message: String
}
