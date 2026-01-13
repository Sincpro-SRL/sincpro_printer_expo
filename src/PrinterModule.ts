import { requireNativeModule, EventEmitter, EventSubscription } from 'expo-modules-core';
import type {
  BluetoothDevice,
  PermissionStatus,
  ConnectionInfo,
  Receipt,
  ReceiptLine,
  PrinterEvents,
  EventListener,
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
 * Event emitter for printer events
 */
const printerEmitter = new EventEmitter<{
  onDeviceDiscovered: (event: PrinterEvents['onDeviceDiscovered']) => void;
  onConnectionChanged: (event: PrinterEvents['onConnectionChanged']) => void;
  onPrintProgress: (event: PrinterEvents['onPrintProgress']) => void;
  onPrintCompleted: (event: PrinterEvents['onPrintCompleted']) => void;
  onPrintError: (event: PrinterEvents['onPrintError']) => void;
}>(NativeModule as any);

/**
 * Event listener API
 */
export const events = {
  /**
   * Add event listener for device discovered events
   */
  addDeviceDiscoveredListener: (
    listener: EventListener<PrinterEvents['onDeviceDiscovered']>
  ): EventSubscription => printerEmitter.addListener('onDeviceDiscovered', listener),

  /**
   * Add event listener for connection status changes
   */
  addConnectionChangedListener: (
    listener: EventListener<PrinterEvents['onConnectionChanged']>
  ): EventSubscription => printerEmitter.addListener('onConnectionChanged', listener),

  /**
   * Add event listener for print progress updates
   */
  addPrintProgressListener: (
    listener: EventListener<PrinterEvents['onPrintProgress']>
  ): EventSubscription => printerEmitter.addListener('onPrintProgress', listener),

  /**
   * Add event listener for print completion
   */
  addPrintCompletedListener: (
    listener: EventListener<PrinterEvents['onPrintCompleted']>
  ): EventSubscription => printerEmitter.addListener('onPrintCompleted', listener),

  /**
   * Add event listener for print errors
   */
  addPrintErrorListener: (
    listener: EventListener<PrinterEvents['onPrintError']>
  ): EventSubscription => printerEmitter.addListener('onPrintError', listener),
};

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
  events,
};

export default Printer;
