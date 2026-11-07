import type { EventSubscription } from 'expo';
import { useSyncExternalStore } from 'react';

import NativePrinter, { type NativeBluetoothDevice } from '../adapters/native_module';
import { DEFAULT_DPI, resolveMedia, type MediaConfig } from '../domain/media';
import type { NativeMedia, PrintCommand } from '../domain/print_job';
import type {
  PrintBarcodeOptions,
  PrintImageOptions,
  PrintKeyValueOptions,
  PrintPdfOptions,
  PrintQROptions,
  PrintReceiptOptions,
  PrintTextOptions,
  PrintTextsOptions,
} from '../domain/print_options';
import {
  createPrinterError,
  isLikelyPrinter,
  type BluetoothDevice,
  type ConnectionChangeEvent,
  type ConnectionState,
  type PairedPrinter,
  type PrinterConfig,
  type PrinterInfo,
  type PrinterStatus,
  type ScanOptions,
} from '../domain/printer';
import {
  layoutLines,
  layoutPdfPage,
  layoutReceipt,
  type Receipt,
  type ReceiptLine,
} from '../domain/receipt';

const DEFAULT_CONNECT_TIMEOUT_MS = 10000;
const DEFAULT_SCAN_TIMEOUT_MS = 6000;
const DEFAULT_WIFI_PORT = 9100;

function toBluetoothDevice(device: NativeBluetoothDevice): BluetoothDevice {
  return { ...device, isPrinter: isLikelyPrinter(device.name) };
}

function resolvePrintableMedia(config: MediaConfig | undefined): NativeMedia {
  const dpi = NativePrinter.getDpi();
  const media = resolveMedia(config, dpi > 0 ? dpi : DEFAULT_DPI);
  if (media.type !== 'continuous' && media.heightDots <= 0) {
    throw createPrinterError('INVALID_ARGUMENT', `Media type "${media.type}" requires a height`);
  }
  return media;
}

async function submit(
  media: NativeMedia,
  commands: PrintCommand[],
  copies: number = 1
): Promise<void> {
  await NativePrinter.print({ media, copies, commands });
}

async function printLines(lines: ReceiptLine[], mediaConfig?: MediaConfig): Promise<void> {
  const media = resolvePrintableMedia(mediaConfig);
  await submit(media, layoutLines(lines, media.widthDots));
}

export const bluetooth = {
  getPairedDevices: (): BluetoothDevice[] =>
    NativePrinter.getPairedDevices().map(toBluetoothDevice),

  getPairedPrinters: (): PairedPrinter[] =>
    bluetooth.getPairedDevices().filter((device) => device.isPrinter),

  /**
   * Context: iOS only lists Bluetooth printers through discovery (classic MFi + BLE); on
   * Android this returns the paired devices, since pairing happens in system settings.
   */
  scan: async (options?: ScanOptions): Promise<BluetoothDevice[]> => {
    const devices = await NativePrinter.scan(options?.timeoutMs ?? DEFAULT_SCAN_TIMEOUT_MS);
    return devices.map(toBluetoothDevice);
  },
};

export const connection = {
  connectBluetooth: (address: string, timeoutMs: number = DEFAULT_CONNECT_TIMEOUT_MS) =>
    NativePrinter.connectBluetooth(address, timeoutMs),

  connectWifi: (
    ip: string,
    port: number = DEFAULT_WIFI_PORT,
    timeoutMs: number = DEFAULT_CONNECT_TIMEOUT_MS
  ) => NativePrinter.connectWifi(ip, port, timeoutMs),

  /** Android only; rejects with UNSUPPORTED_PLATFORM on iOS. */
  connectUsb: () => NativePrinter.connectUsb(),

  disconnect: () => NativePrinter.disconnect(),

  isConnected: (): boolean => NativePrinter.isConnected(),

  getState: (): ConnectionState => NativePrinter.getConnection().state,

  getConnection: (): ConnectionChangeEvent => NativePrinter.getConnection(),

  addListener: (listener: (event: ConnectionChangeEvent) => void): EventSubscription =>
    NativePrinter.addListener('onConnectionChange', listener),

  getStatus: (): Promise<PrinterStatus> => NativePrinter.getStatus(),

  getInfo: (): Promise<PrinterInfo> => NativePrinter.getInfo(),

  getDpi: (): number => NativePrinter.getDpi(),
};

export const config = {
  /** Stores the configuration and applies it now if a printer is connected. */
  set: (printerConfig: PrinterConfig): Promise<void> => NativePrinter.setConfig(printerConfig),

  get: (): PrinterConfig => NativePrinter.getConfig(),
};

export const print = {
  text: (text: string, options?: PrintTextOptions) =>
    printLines(
      [
        {
          type: 'text',
          content: text,
          fontSize: options?.fontSize,
          bold: options?.bold,
          alignment: options?.alignment,
        },
      ],
      options?.media
    ),

  texts: (texts: string[], options?: PrintTextsOptions) =>
    printLines(
      texts.map((content) => ({ type: 'text', content, fontSize: options?.fontSize })),
      options?.media
    ),

  qr: (data: string, options?: PrintQROptions) =>
    printLines(
      [{ type: 'qr', data, size: options?.size, alignment: options?.alignment }],
      options?.media
    ),

  barcode: (data: string, options?: PrintBarcodeOptions) =>
    printLines(
      [
        {
          type: 'barcode',
          data,
          barcodeType: options?.type,
          height: options?.height,
          alignment: options?.alignment,
        },
      ],
      options?.media
    ),

  imageBase64: (base64: string, options?: PrintImageOptions) =>
    printLines([{ type: 'image', base64, alignment: options?.alignment }], options?.media),

  pdfBase64: async (base64: string, options?: PrintPdfOptions): Promise<void> => {
    const media = resolvePrintableMedia(options?.media);
    await submit(media, layoutPdfPage(base64, options?.page ?? 1, media.widthDots));
  },

  getPdfPageCount: (base64: string): number => NativePrinter.getPdfPageCount(base64),

  keyValue: (key: string, value: string, options?: PrintKeyValueOptions) =>
    printLines(
      [{ type: 'keyValue', key, value, fontSize: options?.fontSize, bold: options?.bold }],
      options?.media
    ),

  receipt: async (receipt: Receipt, options?: PrintReceiptOptions): Promise<void> => {
    const media = resolvePrintableMedia(options?.media);
    await submit(media, layoutReceipt(receipt, media.widthDots), options?.copies ?? 1);
  },
};

let connectionSnapshot: ConnectionChangeEvent | null = null;

function sameConnection(a: ConnectionChangeEvent, b: ConnectionChangeEvent): boolean {
  return a.state === b.state && a.address === b.address && a.reason === b.reason;
}

function readConnection(): ConnectionChangeEvent {
  const latest = NativePrinter.getConnection();
  if (!connectionSnapshot || !sameConnection(connectionSnapshot, latest)) {
    connectionSnapshot = latest;
  }
  return connectionSnapshot;
}

/**
 * Context: re-reads the native state on subscribe, so a change fired while nobody was
 * listening is never lost; the snapshot keeps its identity while the state is unchanged.
 */
function subscribeToConnection(onChange: () => void): () => void {
  const subscription = NativePrinter.addListener('onConnectionChange', (event) => {
    connectionSnapshot = event;
    onChange();
  });
  readConnection();
  return () => subscription.remove();
}

export function useConnectionState(): ConnectionChangeEvent {
  return useSyncExternalStore(subscribeToConnection, () => connectionSnapshot ?? readConnection());
}
