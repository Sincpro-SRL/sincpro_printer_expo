/**
 * Contract between the TypeScript layout and the native drivers.
 *
 * Context: layout (positions, line heights, alignment) is computed here once for both
 * platforms; native code only draws primitives at the given dot coordinates inside one
 * print transaction. Font and barcode codes are the Bixolon firmware codes, identical in
 * the Android and iOS SDKs.
 */

export type NativeMediaType = 'continuous' | 'gap' | 'black_mark';

export interface NativeMedia {
  widthDots: number;
  /** 0 on continuous media: native sizes the page to the drawn content. */
  heightDots: number;
  gapDots: number;
  type: NativeMediaType;
}

export type QrErrorCorrection = 'L' | 'M' | 'Q' | 'H';

export interface TextCommand {
  type: 'text';
  x: number;
  y: number;
  height: number;
  text: string;
  /** Device font code: ASCII '0'..'9' (48..57). */
  font: number;
  bold: boolean;
}

export interface QrCommand {
  type: 'qr';
  x: number;
  y: number;
  height: number;
  data: string;
  /** Module size 1..9. */
  size: number;
  errorCorrection: QrErrorCorrection;
}

export interface BarcodeCommand {
  type: 'barcode';
  x: number;
  y: number;
  height: number;
  data: string;
  /** Bixolon 1D symbology code (CODE39=0, CODE128=1, ...). */
  symbology: number;
  narrowBarWidth: number;
  wideBarWidth: number;
  barHeight: number;
  showText: boolean;
}

export interface ImageCommand {
  type: 'image';
  x: number;
  y: number;
  /** 0 when unknown: native measures the decoded image. */
  height: number;
  base64: string;
  /** Target width in dots; the image is scaled keeping its aspect ratio. */
  width: number;
}

export interface PdfPageCommand {
  type: 'pdf';
  x: number;
  y: number;
  /** Always 0: native measures the rasterized page. */
  height: number;
  base64: string;
  /** 1-based page number. */
  page: number;
  width: number;
}

export type PrintCommand = TextCommand | QrCommand | BarcodeCommand | ImageCommand | PdfPageCommand;

export interface NativePrintJob {
  media: NativeMedia;
  copies: number;
  commands: PrintCommand[];
}
