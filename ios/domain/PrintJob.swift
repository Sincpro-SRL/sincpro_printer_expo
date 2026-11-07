import Foundation

// Mirrors src/domain/print_job.ts: positions are already laid out in printer dots by TypeScript.

enum MediaType: String {
  case continuous
  case gap
  case blackMark = "black_mark"
}

struct Media {
  let widthDots: Int
  let heightDots: Int
  let gapDots: Int
  let type: MediaType
}

enum QrErrorCorrection: String {
  case l = "L"
  case m = "M"
  case q = "Q"
  case h = "H"
}

enum PrintCommand {
  case text(x: Int, y: Int, height: Int, text: String, font: Int, bold: Bool)
  case qr(x: Int, y: Int, height: Int, data: String, size: Int, errorCorrection: QrErrorCorrection)
  case barcode(
    x: Int, y: Int, height: Int, data: String, symbology: Int, narrowBarWidth: Int,
    wideBarWidth: Int, barHeight: Int, showText: Bool)
  case image(x: Int, y: Int, height: Int, base64: String, width: Int)
  case pdfPage(x: Int, y: Int, height: Int, base64: String, page: Int, width: Int)

  var y: Int {
    switch self {
    case .text(_, let y, _, _, _, _), .qr(_, let y, _, _, _, _),
      .barcode(_, let y, _, _, _, _, _, _, _), .image(_, let y, _, _, _),
      .pdfPage(_, let y, _, _, _, _):
      return y
    }
  }

  var height: Int {
    switch self {
    case .text(_, _, let height, _, _, _), .qr(_, _, let height, _, _, _),
      .barcode(_, _, let height, _, _, _, _, _, _), .image(_, _, let height, _, _),
      .pdfPage(_, _, let height, _, _, _):
      return height
    }
  }
}

struct PrintJob {
  let media: Media
  let copies: Int
  let commands: [PrintCommand]
}
