import type { MediaConfig } from './media';
import type { BarcodeType, FontSize } from './metrics';
import type { Alignment } from './receipt';

export interface PrintTextOptions {
  fontSize?: FontSize;
  alignment?: Alignment;
  bold?: boolean;
  media?: MediaConfig;
}

export interface PrintTextsOptions {
  fontSize?: FontSize;
  media?: MediaConfig;
}

export interface PrintQROptions {
  size?: number;
  alignment?: Alignment;
  media?: MediaConfig;
}

export interface PrintBarcodeOptions {
  type?: BarcodeType;
  height?: number;
  alignment?: Alignment;
  media?: MediaConfig;
}

export interface PrintImageOptions {
  alignment?: Alignment;
  media?: MediaConfig;
}

export interface PrintPdfOptions {
  /** 1-based page number (default 1). */
  page?: number;
  media?: MediaConfig;
}

export interface PrintKeyValueOptions {
  fontSize?: FontSize;
  bold?: boolean;
  media?: MediaConfig;
}

export interface PrintReceiptOptions {
  media?: MediaConfig;
  copies?: number;
}
