import { requireNativeModule } from 'expo-modules-core';
import type {
  BluetoothDevice,
  PermissionStatus,
  ConnectionInfo,
  Receipt,
  ReceiptLine,
} from './types';

/**
 * Native module interface
 */
interface PrinterNativeModule {
  // Bluetooth
  isBluetoothEnabled(): Promise<boolean>;
  isBluetoothSupported(): Promise<boolean>;
  getPairedDevices(): Promise<BluetoothDevice[]>;
  startBluetoothDiscovery(): Promise<boolean>;
  stopBluetoothDiscovery(): Promise<boolean>;

  // Permissions
  hasBluetoothPermissions(): boolean;
  getRequiredPermissions(): string[];
  getMissingPermissions(): string[];
  getPermissionStatus(): PermissionStatus;

  // Connection
  connect(address: string, port: number): Promise<boolean>;
  disconnect(): Promise<boolean>;
  getConnectionStatus(): Promise<ConnectionInfo>;
  isConnected(): boolean;

  // Print
  printReceipt(receipt: Receipt): Promise<void>;
  printLines(lines: ReceiptLine[]): Promise<void>;
  printQRCode(data: string, size: number): Promise<void>;
}

const NativeModule = requireNativeModule<PrinterNativeModule>('SincproPrinter');

/**
 * Bluetooth API
 */
export const bluetooth = {
  /**
   * Check if Bluetooth is enabled
   */
  isEnabled: (): Promise<boolean> => NativeModule.isBluetoothEnabled(),

  /**
   * Check if Bluetooth is supported on this device
   */
  isSupported: (): Promise<boolean> => NativeModule.isBluetoothSupported(),

  /**
   * Get list of paired/bonded Bluetooth devices
   */
  getPairedDevices: (): Promise<BluetoothDevice[]> => NativeModule.getPairedDevices(),

  /**
   * Start Bluetooth discovery (finds nearby devices)
   */
  startDiscovery: (): Promise<boolean> => NativeModule.startBluetoothDiscovery(),

  /**
   * Stop Bluetooth discovery
   */
  stopDiscovery: (): Promise<boolean> => NativeModule.stopBluetoothDiscovery(),
};

/**
 * Permission API
 */
export const permission = {
  /**
   * Check if all required Bluetooth permissions are granted
   */
  hasBluetoothPermissions: (): boolean => NativeModule.hasBluetoothPermissions(),

  /**
   * Get list of required permissions for current Android version
   */
  getRequiredPermissions: (): string[] => NativeModule.getRequiredPermissions(),

  /**
   * Get list of missing/denied permissions
   */
  getMissingPermissions: (): string[] => NativeModule.getMissingPermissions(),

  /**
   * Get detailed permission status
   */
  getStatus: (): PermissionStatus => NativeModule.getPermissionStatus(),
};

/**
 * Connection API
 */
export const connection = {
  /**
   * Connect to a printer
   */
  connect: (address: string, port: number = 9100): Promise<boolean> =>
    NativeModule.connect(address, port),

  /**
   * Disconnect from current printer
   */
  disconnect: (): Promise<boolean> => NativeModule.disconnect(),

  /**
   * Get current connection status
   */
  getStatus: (): Promise<ConnectionInfo> => NativeModule.getConnectionStatus(),

  /**
   * Check if currently connected
   */
  isConnected: (): boolean => NativeModule.isConnected(),
};

/**
 * Print API
 */
export const print = {
  /**
   * Print a complete receipt
   */
  receipt: (receipt: Receipt): Promise<void> => NativeModule.printReceipt(receipt),

  /**
   * Print a list of lines
   */
  lines: (lines: ReceiptLine[]): Promise<void> => NativeModule.printLines(lines),

  /**
   * Print a QR code
   */
  qrCode: (data: string, size: number = 5): Promise<void> =>
    NativeModule.printQRCode(data, size),
};

/**
 * Default export with all APIs
 */
const Printer = {
  bluetooth,
  permission,
  connection,
  print,
};

export default Printer;
