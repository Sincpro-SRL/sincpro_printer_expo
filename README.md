# @sincpro/printer-expo

Expo module for **Bixolon label printers** (SPP-L310 / L410 and family) on **Android and iOS**:
Bluetooth, WiFi and, on Android, USB. It prints text, QR codes, barcodes, images, PDF pages and
receipts, and reports the live connection state.

- Expo SDK **58+** (React Native 0.88), iOS 16.4+, Android 7+
- Bixolon Label SDK 2.1.2 (Android) and Label Printer SDK 1.1.16 (iOS), bundled

## Install

```bash
npx expo install @sincpro/printer-expo
```

Add the config plugin. It writes the iOS Info.plist keys; Android permissions come with the module.

```json
{
  "expo": {
    "plugins": [
      [
        "@sincpro/printer-expo",
        {
          "bluetoothClassic": true,
          "bluetoothUsageDescription": "Se usa Bluetooth para conectar la impresora.",
          "localNetwork": false
        }
      ]
    ]
  }
}
```

| Option                                          | Default      | Effect (iOS)                                                          |
| ----------------------------------------------- | ------------ | --------------------------------------------------------------------- |
| `bluetoothClassic`                              | `true`       | Declares `com.bixolon.protocol` (MFi printers over Bluetooth classic) |
| `bluetoothUsageDescription`                     | English text | `NSBluetoothAlwaysUsageDescription`                                   |
| `localNetwork` / `localNetworkUsageDescription` | `false`      | `NSLocalNetworkUsageDescription` for WiFi printers                    |

On Android 12+, request `BLUETOOTH_CONNECT` (and `BLUETOOTH_SCAN`) at runtime before listing or
connecting.

## Usage

```ts
import { bluetooth, connection, config, print, useConnectionState } from '@sincpro/printer-expo';

const [printer] = bluetooth.getPairedPrinters(); // iOS: await bluetooth.scan()
await connection.connectBluetooth(printer.address, 10000);
await config.set({ marginLeft: 0, marginTop: 0, density: 'extra_dark' });

await print.receipt(
  {
    header: [{ type: 'text', content: 'MI TIENDA', fontSize: 'large', bold: true, alignment: 'center' }],
    body: [
      { type: 'keyValue', key: 'Producto', value: 'Bs 10.00' },
      { type: 'separator' },
      { type: 'keyValue', key: 'TOTAL', value: 'Bs 10.00', bold: true },
    ],
    footer: [{ type: 'qr', data: 'https://sincpro.com.bo' }],
  },
  { media: { preset: 'continuous72mm' } }
);

function PrinterBadge() {
  const { state, reason } = useConnectionState(); // 'CONNECTED' | 'CONNECTING' | 'DISCONNECTED'
  return <Text>{state === 'DISCONNECTED' && reason === 'link_lost' ? 'Impresora desconectada' : state}</Text>;
}
```

## API

### `bluetooth`

| Method                                             | Description                                                                                              |
| -------------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| `getPairedDevices(): BluetoothDevice[]`            | Paired devices (`name`, `address`, `isPrinter`, `transport`). iOS: only MFi printers with an active link |
| `getPairedPrinters(): BluetoothDevice[]`           | Paired devices that look like printers                                                                   |
| `scan({ timeoutMs? }): Promise<BluetoothDevice[]>` | iOS: discovery of classic + BLE printers. Android: paired devices                                        |

### `connection`

| Method                                                                   | Description                                                            |
| ------------------------------------------------------------------------ | ---------------------------------------------------------------------- |
| `connectBluetooth(address, timeoutMs = 10000)`                           | Connects by MAC address. Reuses the session if already connected to it |
| `connectWifi(ip, port = 9100, timeoutMs = 10000)`                        | TCP connection                                                         |
| `connectUsb()`                                                           | Android only (`UNSUPPORTED_PLATFORM` on iOS)                           |
| `disconnect()`                                                           | Closes the session                                                     |
| `isConnected(): boolean`                                                 | Real link state                                                        |
| `getState(): ConnectionState` / `getConnection(): ConnectionChangeEvent` | Current state, address and last reason                                 |
| `addListener(listener): EventSubscription`                               | `onConnectionChange` events: `{ state, address, reason }`              |
| `getStatus(): Promise<PrinterStatus>`                                    | Paper, cover, overheat, cutter jam, ribbon, media errors               |
| `getInfo(): Promise<PrinterInfo>`                                        | Model, firmware, serial, dpi                                           |
| `getDpi(): number`                                                       | Dpi of the connected printer (203 by default)                          |

`reason` is one of `connected`, `user_disconnect`, `link_lost` (printer off or out of range) and
`connect_failed`. `useConnectionState()` is the React hook over the same event.

### `config`

`set(config)` stores the configuration and applies it now and on every later connect.
`get()` returns it. Fields: `marginLeft`, `marginTop` (dots); `density`
(`light | medium | dark | extra_dark`); `speed` (`slow | medium | fast | extra_fast`, only sent
when set); `orientation`; `autoCutter: { enabled }`.

### `print`

| Method                                                           | Notes                                                                                |
| ---------------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| `text(text, { fontSize, alignment, bold, media })`               | Long text wraps                                                                      |
| `texts(lines, { fontSize, media })`                              | One row per line                                                                     |
| `qr(data, { size 1..9, alignment, media })`                      | Centered by default                                                                  |
| `barcode(data, { type, height, alignment, media })`              | `CODE128`, `CODE39`, `EAN13`, `EAN8`, `UPCA`, `UPCE`, `CODE93`, `CODABAR`            |
| `imageBase64(base64, { alignment, media })`                      | PNG/JPEG; scaled down to the paper width                                             |
| `pdfBase64(base64, { page, media })` / `getPdfPageCount(base64)` | Page rendered to the paper width                                                     |
| `keyValue(key, value, { fontSize, bold, media })`                | Key left, value right                                                                |
| `receipt({ header, body, footer }, { media, copies })`           | Lines: `text`, `keyValue`, `columns`, `separator`, `space`, `qr`, `barcode`, `image` |

`media` takes a preset (`continuous58mm`, `continuous72mm`, `continuous80mm`, the default) or a
custom size: `widthMm` / `widthDots`, plus `heightMm` and `gapMm` for `gap` (`label`) or
`black_mark` media.

### Errors

Every rejection carries `error.code`:

```ts
import { isPrinterError } from '@sincpro/printer-expo';

try {
  await print.text('Hola');
} catch (error) {
  if (isPrinterError(error) && error.code === 'NOT_CONNECTED') {
    // reconnect
  }
}
```

The codes are `NOT_CONNECTED`, `CONNECT_TIMEOUT`, `CONNECT_FAILED`, `UNSUPPORTED_MODEL`,
`BLUETOOTH_UNAVAILABLE`, `PRINT_FAILED`, `STATUS_FAILED`, `INVALID_ARGUMENT` and
`UNSUPPORTED_PLATFORM`.

## iOS notes

- Bluetooth classic works only with **MFi** Bixolon models. To publish to the App Store with
  `bluetoothClassic`, Bixolon must register your bundle ID in Apple's MFi program. BLE printers need
  no registration; set `bluetoothClassic: false` if you only use BLE.
- Building Expo SDK 58 apps requires **Xcode 26.4+**.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Testing and hardware checklist](docs/TESTING.md)
- [Migrating from 1.x](docs/MIGRATION.md)
- [Development](docs/DEVELOPMENT.md)
- [Bixolon SDK notes](docs/BIXOLON_NOTES.md) and the [manuals](docs/manuals/)

## License

MIT
