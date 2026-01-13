/**
 * Bluetooth types for @sincpro/printer-expo
 */

/**
 * Bluetooth device information
 */
export interface BluetoothDevice {
  name: string;
  address: string;
  type: BluetoothDeviceType;
  isPrinter: boolean;
}

/**
 * Bluetooth device types
 */
export type BluetoothDeviceType = 'CLASSIC' | 'LE' | 'DUAL' | 'UNKNOWN';

/**
 * Permission status
 */
export interface PermissionStatus {
  allGranted: boolean;
  grantedPermissions: string[];
  deniedPermissions: string[];
  androidVersion: number;
}
