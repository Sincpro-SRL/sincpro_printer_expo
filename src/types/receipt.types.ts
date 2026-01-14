/**
 * Receipt types for @sincpro/printer-expo
 */

import type { FontSize, Alignment, BarcodeType } from './printer.types';

/**
 * Receipt structure with header, body, and footer sections
 */
export interface Receipt {
  header?: ReceiptLine[];
  body?: ReceiptLine[];
  footer?: ReceiptLine[];
}

/**
 * Receipt line types (discriminated union)
 */
export type ReceiptLine =
  | TextLine
  | KeyValueLine
  | QRLine
  | BarcodeLine
  | ImageLine
  | SeparatorLine
  | SpaceLine
  | ColumnsLine;

/**
 * Text line
 */
export interface TextLine {
  type: 'text';
  content: string;
  fontSize?: FontSize;
  bold?: boolean;
  alignment?: Alignment;
}

/**
 * Key-value pair line
 */
export interface KeyValueLine {
  type: 'keyValue';
  key: string;
  value: string;
  fontSize?: FontSize;
  bold?: boolean;
}

/**
 * QR code line
 */
export interface QRLine {
  type: 'qr';
  data: string;
  size?: number;
  alignment?: Alignment;
}

/**
 * Barcode line
 */
export interface BarcodeLine {
  type: 'barcode';
  data: string;
  barcodeType?: BarcodeType;
  height?: number;
  alignment?: Alignment;
}

/**
 * Image line (base64)
 */
export interface ImageLine {
  type: 'image';
  base64: string;
  alignment?: Alignment;
}

/**
 * Separator line
 */
export interface SeparatorLine {
  type: 'separator';
  char?: string;
  length?: number;
}

/**
 * Space/blank line
 */
export interface SpaceLine {
  type: 'space';
  lines?: number;
}

/**
 * Column definition
 */
export interface Column {
  text: string;
  widthRatio?: number;
  alignment?: Alignment;
}

/**
 * Columns line (multiple columns in one row)
 */
export interface ColumnsLine {
  type: 'columns';
  columns: Column[];
  fontSize?: FontSize;
  bold?: boolean;
}
