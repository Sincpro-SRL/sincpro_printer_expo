import ExpoModulesCore

// Transport shapes for the JS bridge; `toDomain()` keeps Expo types out of the inner layers.

struct MediaRecord: Record {
  @Field var widthDots: Int = 640
  @Field var heightDots: Int = 0
  @Field var gapDots: Int = 0
  @Field var type: String = "continuous"

  func toDomain() -> Media {
    Media(
      widthDots: widthDots, heightDots: heightDots, gapDots: gapDots,
      type: MediaType(rawValue: type) ?? .continuous)
  }
}

/// One flat record for every command kind: fields not used by a kind keep their defaults.
struct PrintCommandRecord: Record {
  @Field var type: String = ""
  @Field var x: Int = 0
  @Field var y: Int = 0
  @Field var height: Int = 0
  @Field var text: String = ""
  @Field var font: Int = 51
  @Field var bold: Bool = false
  @Field var data: String = ""
  @Field var size: Int = 5
  @Field var errorCorrection: String = "M"
  @Field var symbology: Int = 1
  @Field var narrowBarWidth: Int = 2
  @Field var wideBarWidth: Int = 6
  @Field var barHeight: Int = 60
  @Field var showText: Bool = true
  @Field var base64: String = ""
  @Field var width: Int = 0
  @Field var page: Int = 1

  func toDomain() throws -> PrintCommand {
    switch type {
    case "text":
      return .text(x: x, y: y, height: height, text: text, font: font, bold: bold)
    case "qr":
      return .qr(
        x: x, y: y, height: height, data: data, size: size,
        errorCorrection: QrErrorCorrection(rawValue: errorCorrection) ?? .m)
    case "barcode":
      return .barcode(
        x: x, y: y, height: height, data: data, symbology: symbology, narrowBarWidth: narrowBarWidth,
        wideBarWidth: wideBarWidth, barHeight: barHeight, showText: showText)
    case "image":
      return .image(x: x, y: y, height: height, base64: base64, width: width)
    case "pdf":
      return .pdfPage(x: x, y: y, height: height, base64: base64, page: page, width: width)
    default:
      throw PrinterError(code: .invalidArgument, message: "Unknown print command \"\(type)\"")
    }
  }
}

struct PrintJobRecord: Record {
  @Field var media: MediaRecord = MediaRecord()
  @Field var copies: Int = 1
  @Field var commands: [PrintCommandRecord] = []

  func toDomain() throws -> PrintJob {
    PrintJob(
      media: media.toDomain(), copies: min(max(copies, 1), 65535),
      commands: try commands.map { try $0.toDomain() })
  }
}

struct CutterRecord: Record {
  @Field var enabled: Bool = false
  @Field var fullCut: Bool = true
}

/// Missing fields fall back to the 1.x defaults, except speed, which stays untouched when unset.
struct PrinterConfigRecord: Record {
  @Field var marginLeft: Int = 0
  @Field var marginTop: Int = 0
  @Field var density: String?
  @Field var speed: String?
  @Field var orientation: String?
  @Field var autoCutter: CutterRecord?

  func toDomain() -> PrinterConfig {
    PrinterConfig(
      marginLeft: marginLeft,
      marginTop: marginTop,
      density: density.flatMap(Density.init(rawValue:)) ?? .medium,
      speed: speed.flatMap(Speed.init(rawValue:)),
      orientation: orientation.flatMap(Orientation.init(rawValue:)) ?? .topToBottom,
      autoCutter: autoCutter?.enabled ?? false,
      fullCut: autoCutter?.fullCut ?? true)
  }
}

extension PrinterConfig {
  func toDictionary() -> [String: Any?] {
    [
      "marginLeft": marginLeft,
      "marginTop": marginTop,
      "density": density.rawValue,
      "speed": speed?.rawValue,
      "orientation": orientation.rawValue,
      "autoCutter": ["enabled": autoCutter, "fullCut": fullCut],
    ]
  }
}
