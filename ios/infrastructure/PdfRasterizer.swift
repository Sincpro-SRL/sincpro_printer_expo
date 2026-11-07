import PDFKit
import UIKit

enum PdfRasterizer {
  static func pageCount(_ base64: String) -> Int {
    guard let data = ImageDecoder.decodeData(base64), let document = PDFDocument(data: data) else {
      return 0
    }
    return document.pageCount
  }

  /// Renders a 1-based page scaled to `targetWidth` dots on a white background.
  static func render(_ base64: String, page: Int, targetWidth: Int) -> UIImage? {
    guard let data = ImageDecoder.decodeData(base64),
      let document = PDFDocument(data: data),
      let pdfPage = document.page(at: page - 1)
    else { return nil }

    let bounds = pdfPage.bounds(for: .mediaBox)
    let scale = CGFloat(targetWidth) / bounds.width
    let size = CGSize(width: CGFloat(targetWidth), height: (bounds.height * scale).rounded())
    let format = UIGraphicsImageRendererFormat()
    format.scale = 1
    format.opaque = true

    return UIGraphicsImageRenderer(size: size, format: format).image { context in
      UIColor.white.setFill()
      context.fill(CGRect(origin: .zero, size: size))
      let cgContext = context.cgContext
      cgContext.translateBy(x: 0, y: size.height)
      cgContext.scaleBy(x: scale, y: -scale)
      cgContext.translateBy(x: -bounds.minX, y: -bounds.minY)
      pdfPage.draw(with: .mediaBox, to: cgContext)
    }
  }
}
