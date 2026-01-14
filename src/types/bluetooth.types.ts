/**
 * Bluetooth types for @sincpro/printer-expo
 */

/**
 * Bluetooth device information
 */
export interface BluetoothDevice {
  name: string;
  address: string;
  isPrinter: boolean;
}

/**
 * Paired printer (simplified device)
 */
export interface PairedPrinter {
  name: string;
  address: string;
}
