import { bluetooth, config, connection, print } from './services/printer_service';

export {
  bluetooth,
  config,
  connection,
  print,
  useConnectionState,
} from './services/printer_service';

export { isPrinterError } from './domain/printer';
export type {
  BluetoothDevice,
  BluetoothTransport,
  ConnectionChangeEvent,
  ConnectionChangeReason,
  ConnectionState,
  CutterConfig,
  PairedPrinter,
  PrintDensity,
  PrintOrientation,
  PrintSpeed,
  PrinterConfig,
  PrinterError,
  PrinterErrorCode,
  PrinterInfo,
  PrinterStatus,
  ScanOptions,
} from './domain/printer';

export type { MediaConfig, MediaPreset, MediaType } from './domain/media';
export type { BarcodeType, FontSize } from './domain/metrics';
export type {
  Alignment,
  BarcodeLine,
  Column,
  ColumnsLine,
  ImageLine,
  KeyValueLine,
  QRLine,
  Receipt,
  ReceiptLine,
  SeparatorLine,
  SpaceLine,
  TextLine,
} from './domain/receipt';
export type {
  PrintBarcodeOptions,
  PrintImageOptions,
  PrintKeyValueOptions,
  PrintPdfOptions,
  PrintQROptions,
  PrintReceiptOptions,
  PrintTextOptions,
  PrintTextsOptions,
} from './domain/print_options';

const SincproPrinter = { bluetooth, connection, config, print };

export default SincproPrinter;
