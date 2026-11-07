import { NativeModule, requireNativeModule } from 'expo';

import type { NativePrintJob } from '../domain/print_job';
import type {
  BluetoothTransport,
  ConnectionChangeEvent,
  PrinterConfig,
  PrinterInfo,
  PrinterStatus,
} from '../domain/printer';

export interface NativeBluetoothDevice {
  name: string;
  address: string;
  transport: BluetoothTransport;
}

export type SincproPrinterEvents = {
  onConnectionChange: (event: ConnectionChangeEvent) => void;
};

/** Native contract implemented by PrinterModule.kt and SincproPrinterModule.swift. */
declare class SincproPrinterNativeModule extends NativeModule<SincproPrinterEvents> {
  getPairedDevices(): NativeBluetoothDevice[];
  scan(timeoutMs: number): Promise<NativeBluetoothDevice[]>;

  connectBluetooth(address: string, timeoutMs: number): Promise<void>;
  connectWifi(ip: string, port: number, timeoutMs: number): Promise<void>;
  connectUsb(): Promise<void>;
  disconnect(): Promise<void>;
  isConnected(): boolean;
  getConnection(): ConnectionChangeEvent;

  getStatus(): Promise<PrinterStatus>;
  getInfo(): Promise<PrinterInfo>;
  getDpi(): number;

  setConfig(config: PrinterConfig): Promise<void>;
  getConfig(): PrinterConfig;

  print(job: NativePrintJob): Promise<void>;
  getPdfPageCount(base64: string): number;
}

export default requireNativeModule<SincproPrinterNativeModule>('SincproPrinter');
