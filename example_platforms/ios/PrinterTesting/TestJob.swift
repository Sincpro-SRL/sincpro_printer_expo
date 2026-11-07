import UIKit

extension PrinterService {
  /// Runs a task on the service's SDK queue, exactly like the Expo module does.
  func call<T>(_ task: @escaping (PrinterService) throws -> T) async throws -> T {
    try await withCheckedThrowingContinuation { continuation in
      enqueue(task) { continuation.resume(with: $0) }
    }
  }
}

enum Font: Int {
  case small = 49
  case medium = 51
  case large = 53
  case xlarge = 54

  var charWidth: Int { [Font.small: 12, .medium: 19, .large: 32, .xlarge: 48][self]! }
  var charHeight: Int { [Font.small: 20, .medium: 30, .large: 40, .xlarge: 76][self]! }
}

enum Align { case left, center, right }

enum Symbology: Int {
  case code39 = 0
  case code128 = 1
  case ean13 = 7
}

/// Minimal top-to-bottom layout for test jobs (mirrors TestJob.kt). The production layout lives in
/// TypeScript (src/domain/receipt.ts); this only places primitives to exercise the native driver.
final class TestJob {
  private let widthDots: Int
  private var commands: [PrintCommand] = []
  private var y = 20

  init(widthDots: Int = 576) { self.widthDots = widthDots }

  private func x(_ align: Align, _ contentWidth: Int) -> Int {
    switch align {
    case .left: return 10
    case .center: return max(0, (widthDots - contentWidth) / 2)
    case .right: return max(0, widthDots - contentWidth - 10)
    }
  }

  @discardableResult
  func text(_ text: String, font: Font = .medium, bold: Bool = false, align: Align = .left) -> TestJob {
    commands.append(
      .text(x: x(align, text.count * font.charWidth), y: y, height: font.charHeight, text: text, font: font.rawValue, bold: bold))
    y += max(30, font.charHeight)
    return self
  }

  @discardableResult
  func qr(_ data: String, size: Int = 5, align: Align = .center) -> TestJob {
    let side = 25 * size
    commands.append(.qr(x: x(align, side), y: y, height: side, data: data, size: size, errorCorrection: .m))
    y += side + 20
    return self
  }

  @discardableResult
  func barcode(_ data: String, symbology: Symbology = .code128, barHeight: Int = 60, align: Align = .center) -> TestJob {
    let width = (11 * data.count + 35) * 2
    commands.append(
      .barcode(
        x: x(align, width), y: y, height: barHeight + 30, data: data, symbology: symbology.rawValue,
        narrowBarWidth: 2, wideBarWidth: 6, barHeight: barHeight, showText: true))
    y += barHeight + 30
    return self
  }

  @discardableResult
  func image(_ base64: String, align: Align = .center) -> TestJob {
    let image = Data(base64Encoded: base64).flatMap(UIImage.init(data:))
    let pixelWidth = Int((image?.size.width ?? 0) * (image?.scale ?? 1))
    let pixelHeight = Int((image?.size.height ?? 0) * (image?.scale ?? 1))
    let width = min(max(pixelWidth, 1), widthDots)
    let height = pixelHeight * width / max(1, pixelWidth)
    commands.append(.image(x: x(align, width), y: y, height: height, base64: base64, width: width))
    y += height + 10
    return self
  }

  @discardableResult
  func pdf(_ base64: String, page: Int = 1) -> TestJob {
    commands.append(.pdfPage(x: 0, y: y, height: 0, base64: base64, page: page, width: widthDots))
    return self
  }

  @discardableResult
  func separator() -> TestJob {
    text(String(repeating: "-", count: (widthDots - 20) / Font.small.charWidth), font: .small)
  }

  @discardableResult
  func space(_ dots: Int = 20) -> TestJob {
    y += dots
    return self
  }

  func build(copies: Int = 1) -> PrintJob {
    PrintJob(media: Media(widthDots: widthDots, heightDots: 0, gapDots: 0, type: .continuous), copies: copies, commands: commands)
  }
}
