export type ConnectionState = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED';

export type ConnectionChangeReason =
  'connected' | 'user_disconnect' | 'link_lost' | 'connect_failed';

export interface ConnectionChangeEvent {
  state: ConnectionState;
  address: string | null;
  reason?: ConnectionChangeReason;
}

export type BluetoothTransport = 'classic' | 'ble' | 'unknown';

export interface BluetoothDevice {
  name: string;
  address: string;
  isPrinter: boolean;
  transport: BluetoothTransport;
}

/** Kept for 1.x compatibility: a paired printer is a `BluetoothDevice` flagged as printer. */
export type PairedPrinter = BluetoothDevice;

export interface ScanOptions {
  timeoutMs?: number;
}

export interface PrinterStatus {
  connectionState: ConnectionState;
  hasPaper: boolean;
  isCoverOpen: boolean;
  isOverheated: boolean;
  isCutterJammed: boolean;
  isRibbonEnd: boolean;
  /** Gap / black-mark sensing failure (also reported on paper jam). */
  hasMediaError: boolean;
  hasError: boolean;
  errorMessage: string | null;
}

export interface PrinterInfo {
  model: string;
  firmware: string;
  serial: string;
  dpi: number;
}

export type PrintDensity = 'light' | 'medium' | 'dark' | 'extra_dark';

export type PrintSpeed = 'slow' | 'medium' | 'fast' | 'extra_fast';

export type PrintOrientation = 'top_to_bottom' | 'bottom_to_top';

export interface CutterConfig {
  enabled: boolean;
  /** Android only. iOS has no full/partial selector and always uses the printer default. */
  fullCut?: boolean;
}

export interface PrinterConfig {
  marginLeft?: number;
  marginTop?: number;
  density?: PrintDensity;
  speed?: PrintSpeed;
  orientation?: PrintOrientation;
  autoCutter?: CutterConfig;
}

export type PrinterErrorCode =
  | 'NOT_CONNECTED'
  | 'CONNECT_TIMEOUT'
  | 'CONNECT_FAILED'
  | 'UNSUPPORTED_MODEL'
  | 'BLUETOOTH_UNAVAILABLE'
  | 'PRINT_FAILED'
  | 'STATUS_FAILED'
  | 'INVALID_ARGUMENT'
  | 'UNSUPPORTED_PLATFORM';

export interface PrinterError extends Error {
  code: PrinterErrorCode;
}

const PRINTER_ERROR_CODES: ReadonlySet<string> = new Set<PrinterErrorCode>([
  'NOT_CONNECTED',
  'CONNECT_TIMEOUT',
  'CONNECT_FAILED',
  'UNSUPPORTED_MODEL',
  'BLUETOOTH_UNAVAILABLE',
  'PRINT_FAILED',
  'STATUS_FAILED',
  'INVALID_ARGUMENT',
  'UNSUPPORTED_PLATFORM',
]);

export function createPrinterError(code: PrinterErrorCode, message: string): PrinterError {
  return Object.assign(new Error(message), { code });
}

export function isPrinterError(error: unknown): error is PrinterError {
  if (!(error instanceof Error)) return false;
  const code: unknown = (error as Error & { code?: unknown }).code;
  return typeof code === 'string' && PRINTER_ERROR_CODES.has(code);
}

const PRINTER_NAME_HINTS = ['printer', 'bixolon', 'spp', 'sppr', 'zebra', 'tsc', 'honeywell'];

export function isLikelyPrinter(deviceName: string): boolean {
  const name = deviceName.toLowerCase();
  return PRINTER_NAME_HINTS.some((hint) => name.includes(hint));
}
