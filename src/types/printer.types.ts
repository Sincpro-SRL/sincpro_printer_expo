/**
 * Printer types for @sincpro/printer-expo
 */

/**
 * Printer connection status
 */
export interface PrinterStatus {
  isConnected: boolean;
  isPaperPresent: boolean;
  isError: boolean;
  errorMessage?: string;
}

/**
 * Connection information
 */
export interface ConnectionInfo {
  address: string;
  port: number;
  type: ConnectionType;
  status: ConnectionStatus;
}

/**
 * Connection types
 */
export type ConnectionType = 'BLUETOOTH' | 'WIFI' | 'USB' | 'UNKNOWN';

/**
 * Connection status
 */
export type ConnectionStatus = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'ERROR';

/**
 * Media configuration presets
 */
export type MediaPreset = 
  | 'continuous104mm' 
  | 'continuous80mm' 
  | 'label80x50mm' 
  | 'label100x60mm';

/**
 * Media type
 */
export type MediaType = 'continuous' | 'labelGap' | 'labelBlackMark';

/**
 * Media configuration
 */
export interface MediaConfig {
  preset?: MediaPreset;
  widthDots?: number;
  heightDots?: number;
  mediaType?: MediaType;
  gapDots?: number;
}
