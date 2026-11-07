import SwiftUI

@MainActor
final class TestRunnerModel: ObservableObject {
  @Published var connectionLabel = "⚪ Disconnected"
  @Published var isConnected = false
  @Published var statuses: [String: TestStatus] = [:]
  @Published var results: [String: String] = [:]
  @Published var logs: [String] = []
  @Published var printers: [BluetoothDevice] = []
  @Published var isSearching = false
  @Published var filter: TestCategory?

  private(set) var selectedAddress: String?
  private var service: PrinterService!
  private(set) var testCases: [TestCase] = []

  init() {
    service = PrinterService { [weak self] connection in
      Task { @MainActor in self?.show(connection) }
    }
    testCases = TestCaseRepository(service: service) { [weak self] in self?.selectedAddress }.allTestCases()
    log("Loaded \(testCases.count) test cases")
  }

  var visibleTests: [TestCase] {
    guard let filter else { return testCases }
    return testCases.filter { $0.category == filter }
  }

  private func show(_ connection: Connection) {
    isConnected = connection.state == .connected
    switch connection.state {
    case .connected: connectionLabel = "🟢 Connected \(connection.address ?? "")"
    case .connecting: connectionLabel = "🔄 Connecting \(connection.address ?? "")"
    case .disconnected: connectionLabel = "⚪ Disconnected \(connection.reason?.rawValue ?? "")"
    }
    log("Connection → \(connection.state.rawValue) \(connection.address ?? "") \(connection.reason?.rawValue ?? "")")
  }

  func log(_ message: String) {
    let time = Date().formatted(date: .omitted, time: .standard)
    logs.insert("[\(time)] \(message)", at: 0)
  }

  func searchPrinters() {
    isSearching = true
    Task {
      defer { isSearching = false }
      do {
        printers = try await service.call { $0.discover(timeoutMs: 6000) }
        log("Found \(printers.count) printer(s)")
      } catch {
        log("Search failed: \(error.localizedDescription)")
      }
    }
  }

  func connect(_ device: BluetoothDevice) {
    selectedAddress = device.address
    Task {
      do {
        try await service.call { try $0.connectBluetooth(address: device.address, timeoutMs: 10000) }
        log("Connected to \(device.name)")
      } catch let error as PrinterError {
        log("Connection failed: \(error.code.rawValue) \(error.message)")
      } catch {
        log("Connection failed: \(error.localizedDescription)")
      }
    }
  }

  func disconnect() {
    Task {
      try? await service.call { $0.disconnect() }
    }
  }

  func run(_ testCase: TestCase) {
    if testCase.requiresConnection && !service.isConnected() {
      log("Connect to a printer first")
      return
    }
    statuses[testCase.id] = .running
    log("Running: \(testCase.name)")
    Task {
      do {
        let message = try await testCase.action()
        statuses[testCase.id] = .passed
        results[testCase.id] = message
        log("✓ PASSED: \(testCase.name)\n\(message)")
      } catch let error as PrinterError {
        statuses[testCase.id] = .failed
        results[testCase.id] = "\(error.code.rawValue): \(error.message)"
        log("✗ FAILED: \(testCase.name) - \(error.code.rawValue): \(error.message)")
      } catch {
        statuses[testCase.id] = .failed
        results[testCase.id] = error.localizedDescription
        log("✗ FAILED: \(testCase.name) - \(error.localizedDescription)")
      }
    }
  }
}

struct TestRunnerView: View {
  @StateObject private var model = TestRunnerModel()
  @State private var showPrinters = false

  private func statusIcon(_ status: TestStatus?) -> String {
    switch status {
    case .running: return "⏳"
    case .passed: return "✅"
    case .failed: return "❌"
    default: return "▫️"
    }
  }

  var body: some View {
    NavigationStack {
      List {
        Section {
          Text(model.connectionLabel).font(.headline)
          HStack {
            Button("Connect") {
              showPrinters = true
              model.searchPrinters()
            }
            .buttonStyle(.borderedProminent)
            .disabled(model.isConnected)
            Button("Disconnect", action: model.disconnect)
              .buttonStyle(.bordered)
              .disabled(!model.isConnected)
          }
          Picker("Category", selection: $model.filter) {
            Text("All").tag(TestCategory?.none)
            ForEach(TestCategory.allCases) { Text($0.rawValue).tag(TestCategory?.some($0)) }
          }
        }

        Section("Tests: \(model.visibleTests.count)") {
          ForEach(model.visibleTests) { testCase in
            Button {
              model.run(testCase)
            } label: {
              VStack(alignment: .leading, spacing: 4) {
                Text("\(statusIcon(model.statuses[testCase.id])) \(testCase.name)").font(.body.weight(.semibold))
                Text(testCase.description).font(.caption).foregroundStyle(.secondary)
                if let result = model.results[testCase.id] {
                  Text(result).font(.caption.monospaced()).foregroundStyle(.secondary)
                }
              }
            }
            .foregroundStyle(.primary)
          }
        }

        Section("Logs") {
          ForEach(Array(model.logs.enumerated()), id: \.offset) { _, line in
            Text(line).font(.caption.monospaced())
          }
        }
      }
      .navigationTitle("Sincpro Printer SDK")
      .sheet(isPresented: $showPrinters) {
        NavigationStack {
          List(model.printers, id: \.address) { device in
            Button("\(device.name)\n\(device.address) · \(device.transport.rawValue)") {
              model.connect(device)
              showPrinters = false
            }
          }
          .overlay {
            if model.isSearching { ProgressView("Searching…") }
          }
          .navigationTitle("Select Printer")
          .toolbar {
            Button("Close") { showPrinters = false }
          }
        }
      }
    }
  }
}
