/**
 * Printer firmware metrics used by the layout. Device fonts and symbologies are rendered by
 * the printer firmware, so the same numbers apply on Android and iOS (Bixolon Label SDK
 * manuals: Android §2-2-14, iOS §3-15).
 */

export type FontSize = 'small' | 'medium' | 'large' | 'xlarge';

export type BarcodeType =
  'CODE128' | 'CODE39' | 'EAN13' | 'EAN8' | 'UPCA' | 'UPCE' | 'CODE93' | 'CODABAR';

export interface DeviceFont {
  code: number;
  charWidth: number;
  charHeight: number;
}

export const DEVICE_FONTS: Record<FontSize, DeviceFont> = {
  small: { code: 49, charWidth: 12, charHeight: 20 },
  medium: { code: 51, charWidth: 19, charHeight: 30 },
  large: { code: 53, charWidth: 32, charHeight: 40 },
  xlarge: { code: 54, charWidth: 48, charHeight: 76 },
};

export const BARCODE_SYMBOLOGY: Record<BarcodeType, number> = {
  CODE39: 0,
  CODE128: 1,
  CODABAR: 3,
  CODE93: 4,
  UPCA: 5,
  UPCE: 6,
  EAN13: 7,
  EAN8: 8,
};

/** QR model 2, ECC level M, byte mode: maximum payload per version 1..40. */
const QR_BYTE_CAPACITY_M = [
  14, 26, 42, 62, 84, 106, 122, 152, 180, 213, 251, 287, 331, 362, 412, 450, 504, 560, 624, 666,
  711, 779, 857, 911, 997, 1059, 1125, 1190, 1264, 1370, 1452, 1538, 1628, 1722, 1809, 1911, 1989,
  2099, 2213, 2331,
];

/** Side of the QR symbol in dots. Upper bound: the firmware may pick a denser mode. */
export function qrSideDots(data: string, moduleSize: number): number {
  // The printer encodes text with a single-byte code page, so one character is one byte.
  const bytes = Array.from(data).length;
  const index = QR_BYTE_CAPACITY_M.findIndex((capacity) => bytes <= capacity);
  const version = index === -1 ? QR_BYTE_CAPACITY_M.length : index + 1;
  const modules = 21 + 4 * (version - 1);
  return modules * moduleSize;
}

/** Approximate width in dots of a 1D barcode; used only to align it horizontally. */
export function barcodeWidthDots(
  type: BarcodeType,
  data: string,
  narrow: number,
  wide: number
): number {
  const chars = data.length;
  switch (type) {
    case 'CODE128':
      return (11 * chars + 35) * narrow;
    case 'CODE93':
      return (9 * chars + 37) * narrow;
    case 'CODE39':
      return (chars + 2) * (3 * wide + 7 * narrow) - narrow;
    case 'CODABAR':
      return (chars + 2) * (2 * wide + 6 * narrow);
    case 'EAN13':
    case 'UPCA':
      return 95 * narrow;
    case 'EAN8':
      return 67 * narrow;
    case 'UPCE':
      return 51 * narrow;
  }
}
