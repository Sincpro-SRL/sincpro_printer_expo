/**
 * Receipt types for @sincpro/printer-expo
 */

import { MediaConfig } from './printer.types';

/**
 * Receipt structure with header, details, and footer sections
 */
export interface Receipt {
  header?: ReceiptLine[];
  details?: ReceiptLine[];
  footer?: ReceiptLine[];
  mediaConfig?: MediaConfig;
  copies?: number;
}

/**
 * Receipt line types (discriminated union)
 */
export type ReceiptLine =
  | TextLine
  | KeyValueLine
  | QRCodeLine
  | SeparatorLine
  | SpaceLine;

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
export interface QRCodeLine {
  type: 'qrCode';
  data: string;
  size?: number;
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
 * Font size options
 */
export type FontSize = 'small' | 'medium' | 'large' | 'xlarge';

/**
 * Alignment options
 */
export type Alignment = 'left' | 'center' | 'right';
