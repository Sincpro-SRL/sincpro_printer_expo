import UIKit

enum ImageDecoder {
  static func decodeData(_ base64: String) -> Data? {
    var payload = base64
    if payload.hasPrefix("data:"), let comma = payload.firstIndex(of: ",") {
      payload = String(payload[payload.index(after: comma)...])
    }
    return Data(base64Encoded: payload, options: .ignoreUnknownCharacters)
  }

  /// Transparent pixels print black on thermal heads, so the image is flattened on white at 1x.
  static func decode(_ base64: String) -> UIImage? {
    guard let data = decodeData(base64), let source = UIImage(data: data) else { return nil }
    let size = CGSize(
      width: source.size.width * source.scale, height: source.size.height * source.scale)
    let format = UIGraphicsImageRendererFormat()
    format.scale = 1
    format.opaque = true
    return UIGraphicsImageRenderer(size: size, format: format).image { context in
      UIColor.white.setFill()
      context.fill(CGRect(origin: .zero, size: size))
      source.draw(in: CGRect(origin: .zero, size: size))
    }
  }
}
