/**
 * Printer types for @sincpro/printer-expo
 */

/**
 * Printer status from getStatus()
 */
export interface PrinterStatus {
  connectionState: ConnectionState;
  hasPaper: boolean;
  isCoverOpen: boolean;
  isOverheated: boolean;
  hasError: boolean;
  errorMessage: string | null;
}

/**
 * Connection state
 */
export type ConnectionState = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'ERROR';

/**
 * Printer info from getInfo()
 */
export interface PrinterInfo {
  model: string;
  firmware: string;
  serial: string;
  dpi: number;
}

/**
 * Media configuration
 */
export interface MediaConfig {
  /** Use preset instead of manual widthDots/heightDots */
  preset?: MediaPreset;
  /** Width in dots (ignored if preset is set) */
  widthDots?: number;
  /** Height in dots (ignored if preset is set) */
  heightDots?: number;
}

/**
 * Media presets
 */
export type MediaPreset = 'continuous58mm' | 'continuous80mm';

/**
 * Font size options
 */
export type FontSize = 'small' | 'medium' | 'large' | 'xlarge';

/**
 * Alignment options
 */
export type Alignment = 'left' | 'center' | 'right';

/**
 * Barcode types
 */
export type BarcodeType =
  | 'CODE128'
  | 'CODE39'
  | 'EAN13'
  | 'EAN8'
  | 'UPCA'
  | 'UPCE'
  | 'CODE93'
  | 'CODABAR';

/**
 * Print text options
 */
export interface PrintTextOptions {
  fontSize?: FontSize;
  alignment?: Alignment;
  bold?: boolean;
  media?: MediaConfig;
}

/**
 * Print texts options (multiple lines)
 */
export interface PrintTextsOptions {
  fontSize?: FontSize;
  media?: MediaConfig;
}

/**
 * Print QR options
 */
export interface PrintQROptions {
  size?: number;
  alignment?: Alignment;
  media?: MediaConfig;
}

/**
 * Print barcode options
 */
export interface PrintBarcodeOptions {
  type?: BarcodeType;
  height?: number;
  alignment?: Alignment;
  media?: MediaConfig;
}

/**
 * Print image options
 */
export interface PrintImageOptions {
  alignment?: Alignment;
  media?: MediaConfig;
}

/**
 * Print PDF options
 */
export interface PrintPdfOptions {
  page?: number;
  alignment?: Alignment;
  media?: MediaConfig;
}

/**
 * Print key-value options
 */
export interface PrintKeyValueOptions {
  fontSize?: FontSize;
  bold?: boolean;
  media?: MediaConfig;
}

/**
 * Print receipt options
 */
export interface PrintReceiptOptions {
  media?: MediaConfig;
  copies?: number;
}
