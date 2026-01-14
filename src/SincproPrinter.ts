import { requireNativeModule } from 'expo-modules-core';
import type {
  BluetoothDevice,
  PairedPrinter,
  PrinterStatus,
  PrinterInfo,
  PrintTextOptions,
  PrintTextsOptions,
  PrintQROptions,
  PrintBarcodeOptions,
  PrintImageOptions,
  PrintPdfOptions,
  PrintKeyValueOptions,
  PrintReceiptOptions,
  Receipt,
} from './types';

/**
 * Native module interface matching PrinterModule.kt
 */
interface SincproPrinterNativeModule {
  // Bluetooth
  getPairedDevices(): BluetoothDevice[];
  getPairedPrinters(): PairedPrinter[];

  // Connection
  connectBluetooth(address: string, timeoutMs?: number): Promise<void>;
  connectWifi(ip: string, port?: number, timeoutMs?: number): Promise<void>;
  connectUsb(): Promise<void>;
  disconnect(): Promise<void>;
  isConnected(): boolean;
  getStatus(): Promise<PrinterStatus>;
  getInfo(): Promise<PrinterInfo>;
  getDpi(): number;

  // Print - Text
  printText(text: string, options?: PrintTextOptions): Promise<void>;
  printTexts(texts: string[], options?: PrintTextsOptions): Promise<void>;

  // Print - QR & Barcode
  printQR(data: string, options?: PrintQROptions): Promise<void>;
  printBarcode(data: string, options?: PrintBarcodeOptions): Promise<void>;

  // Print - Images & PDF
  printImageBase64(base64Data: string, options?: PrintImageOptions): Promise<void>;
  printPdfBase64(base64Data: string, options?: PrintPdfOptions): Promise<void>;
  getPdfPageCount(base64Data: string): number;

  // Print - Receipt
  printReceipt(receipt: Receipt, options?: PrintReceiptOptions): Promise<void>;

  // Print - Key-Value
  printKeyValue(key: string, value: string, options?: PrintKeyValueOptions): Promise<void>;
}

const NativeModule = requireNativeModule<SincproPrinterNativeModule>('SincproPrinter');

// ============================================================
// BLUETOOTH API
// ============================================================

/**
 * Bluetooth API for device discovery
 */
export const bluetooth = {
  /**
   * Get all paired/bonded Bluetooth devices
   */
  getPairedDevices: (): BluetoothDevice[] => NativeModule.getPairedDevices(),

  /**
   * Get paired devices that are printers
   */
  getPairedPrinters: (): PairedPrinter[] => NativeModule.getPairedPrinters(),
};

// ============================================================
// CONNECTION API
// ============================================================

/**
 * Connection API for printer connectivity
 */
export const connection = {
  /**
   * Connect to printer via Bluetooth
   * @param address MAC address (e.g., "00:11:22:33:44:55")
   * @param timeoutMs Connection timeout in milliseconds (default: 10000)
   */
  connectBluetooth: (address: string, timeoutMs?: number): Promise<void> =>
    NativeModule.connectBluetooth(address, timeoutMs),

  /**
   * Connect to printer via WiFi
   * @param ip IP address (e.g., "192.168.1.100")
   * @param port TCP port (default: 9100)
   * @param timeoutMs Connection timeout in milliseconds (default: 10000)
   */
  connectWifi: (ip: string, port?: number, timeoutMs?: number): Promise<void> =>
    NativeModule.connectWifi(ip, port, timeoutMs),

  /**
   * Connect to printer via USB
   */
  connectUsb: (): Promise<void> => NativeModule.connectUsb(),

  /**
   * Disconnect from current printer
   */
  disconnect: (): Promise<void> => NativeModule.disconnect(),

  /**
   * Check if currently connected
   */
  isConnected: (): boolean => NativeModule.isConnected(),

  /**
   * Get printer status (paper, cover, errors)
   */
  getStatus: (): Promise<PrinterStatus> => NativeModule.getStatus(),

  /**
   * Get printer info (model, firmware, serial)
   */
  getInfo: (): Promise<PrinterInfo> => NativeModule.getInfo(),

  /**
   * Get printer DPI
   */
  getDpi: (): number => NativeModule.getDpi(),
};

// ============================================================
// PRINT API
// ============================================================

/**
 * Print API for all printing operations
 */
export const print = {
  /**
   * Print a single line of text
   */
  text: (text: string, options?: PrintTextOptions): Promise<void> =>
    NativeModule.printText(text, options),

  /**
   * Print multiple lines of text
   */
  texts: (texts: string[], options?: PrintTextsOptions): Promise<void> =>
    NativeModule.printTexts(texts, options),

  /**
   * Print a QR code
   */
  qr: (data: string, options?: PrintQROptions): Promise<void> =>
    NativeModule.printQR(data, options),

  /**
   * Print a barcode
   */
  barcode: (data: string, options?: PrintBarcodeOptions): Promise<void> =>
    NativeModule.printBarcode(data, options),

  /**
   * Print an image from base64
   */
  imageBase64: (base64Data: string, options?: PrintImageOptions): Promise<void> =>
    NativeModule.printImageBase64(base64Data, options),

  /**
   * Print a PDF page from base64
   */
  pdfBase64: (base64Data: string, options?: PrintPdfOptions): Promise<void> =>
    NativeModule.printPdfBase64(base64Data, options),

  /**
   * Get page count from a PDF (base64)
   */
  getPdfPageCount: (base64Data: string): number => NativeModule.getPdfPageCount(base64Data),

  /**
   * Print a complete receipt with header, body, footer
   */
  receipt: (receipt: Receipt, options?: PrintReceiptOptions): Promise<void> =>
    NativeModule.printReceipt(receipt, options),

  /**
   * Print a key-value pair (two columns)
   */
  keyValue: (key: string, value: string, options?: PrintKeyValueOptions): Promise<void> =>
    NativeModule.printKeyValue(key, value, options),
};

// ============================================================
// DEFAULT EXPORT
// ============================================================

/**
 * Sincpro Printer SDK for Expo
 */
const SincproPrinter = {
  bluetooth,
  connection,
  print,
};

export default SincproPrinter;
