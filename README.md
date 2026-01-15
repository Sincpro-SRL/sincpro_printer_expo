# @sincpro/printer-expo

[![npm version](https://badge.fury.io/js/@sincpro/printer-expo.svg)](https://badge.fury.io/js/@sincpro/printer-expo)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Expo](https://img.shields.io/badge/Expo-000000.svg?style=flat&logo=expo&logoColor=white)](https://expo.dev)

A powerful React Native module for controlling thermal printers in Expo applications. Built with **Clean Architecture** and **Hexagonal Architecture** (Ports & Adapters) for maximum flexibility and maintainability.

## ✨ Features

- 🔗 **Multiple Connectivity**: Bluetooth, WiFi, and USB support
- 🖨️ **Advanced Printing**: Text, QR codes, barcodes, images, and PDFs
- 📋 **Structured Receipts**: Header/body/footer with flexible line types
- ⚙️ **Configurable**: Margins, density, speed, orientation, auto-cutter
- 🏗️ **Clean Architecture**: SOLID principles, testable, swappable adapters
- 📚 **Official SDK**: Integration with Bixolon SDK (extensible to other brands)
- 📝 **TypeScript**: 100% type-safe API with comprehensive definitions
- 🛠️ **Easy Setup**: Simple installation and minimal configuration
- 📱 **Android Focus**: Optimized for Android thermal printers

## 📦 Installation

```bash
npm install @sincpro/printer-expo
# or
yarn add @sincpro/printer-expo
```

### Requirements

- **Expo SDK**: `>=52.0.0`
- **React Native**: Compatible with Expo SDK
- **Platform**: Android (iOS not currently supported)

### Post-Installation

```bash
# Rebuild native modules
npx expo prebuild --clean

# Run on device
npx expo run:android
```

## ⚙️ Configuration

### Android Permissions

The module requires Bluetooth permissions. Add to your `app.json`:

```json
{
  "expo": {
    "android": {
      "permissions": [
        "android.permission.BLUETOOTH",
        "android.permission.BLUETOOTH_ADMIN",
        "android.permission.BLUETOOTH_SCAN",
        "android.permission.BLUETOOTH_CONNECT",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION"
      ]
    }
  }
}
```

**Note**: These permissions are required for Bluetooth device discovery and connection on Android 12+.

## 🚀 Quick Start

### Basic Example

```typescript
import { bluetooth, connection, config, print } from '@sincpro/printer-expo';

// 1. Get paired devices
const devices = bluetooth.getPairedDevices();
const printers = devices.filter(d => d.isPrinter);

// 2. Connect to printer via Bluetooth
await connection.connectBluetooth(printers[0].address, 30000); // 30s timeout

// 3. Optional: Configure printer
await config.set({
  marginLeft: 10,
  marginTop: 5,
  density: 'dark',
  speed: 'medium',
  orientation: 'top_to_bottom',
  autoCutter: { enabled: true, fullCut: true }
});

// 4. Print a simple text
await print.text('Hello World!', {
  fontSize: 'large',
  alignment: 'center',
  bold: true,
  media: { preset: 'continuous80mm' }
});

// 5. Print a complete receipt
await print.receipt({
  header: [
    { type: 'text', content: 'MY STORE', fontSize: 'large', alignment: 'center', bold: true },
    { type: 'text', content: '123 Main Street', alignment: 'center' },
    { type: 'separator' },
  ],
  body: [
    { type: 'keyValue', key: 'Product 1', value: '$10.00' },
    { type: 'keyValue', key: 'Product 2', value: '$15.00' },
    { type: 'separator' },
    { type: 'keyValue', key: 'TOTAL', value: '$25.00', bold: true },
  ],
  footer: [
    { type: 'qr', data: 'https://mystore.com/receipt/123', alignment: 'center' },
    { type: 'text', content: 'Thank you!', alignment: 'center' },
    { type: 'space', lines: 2 },
  ]
}, { media: { preset: 'continuous80mm' } });

// 6. Disconnect
await connection.disconnect();
```

### Complete React Component

```typescript
import React, { useState } from 'react';
import { View, Button, FlatList, Text, Alert } from 'react-native';
import { bluetooth, connection, print } from '@sincpro/printer-expo';
import type { BluetoothDevice } from '@sincpro/printer-expo';

export default function PrinterScreen() {
  const [devices, setDevices] = useState<BluetoothDevice[]>([]);
  const [connected, setConnected] = useState(false);

  const scanDevices = async () => {
    try {
      const foundDevices = bluetooth.getPairedDevices();
      setDevices(foundDevices.filter((d) => d.isPrinter));
    } catch (error) {
      Alert.alert('Error', error.message);
    }
  };

  const connectDevice = async (device: BluetoothDevice) => {
    try {
      await connection.connectBluetooth(device.address, 30000);
      setConnected(true);
      Alert.alert('Success', `Connected to ${device.name}`);
    } catch (error) {
      Alert.alert('Error', 'Connection failed');
    }
  };

  const printTest = async () => {
    try {
      await print.receipt({
        header: [
          { type: 'text', content: 'Test Receipt', fontSize: 'large', alignment: 'center' },
          { type: 'separator' },
        ],
        body: [{ type: 'text', content: 'This is a test print' }],
        footer: [{ type: 'space', lines: 2 }],
      });
      Alert.alert('Success', 'Receipt printed');
    } catch (error) {
      Alert.alert('Error', error.message);
    }
  };

  return (
    <View style={{ padding: 20 }}>
      <Button title="Scan Devices" onPress={scanDevices} />
      <Button title="Print Test" onPress={printTest} disabled={!connected} />

      <FlatList
        data={devices}
        keyExtractor={(item) => item.address}
        renderItem={({ item }) => (
          <Button title={`${item.name} (${item.address})`} onPress={() => connectDevice(item)} />
        )}
      />
    </View>
  );
}
```

---

## 📚 API Reference

### Connectivity API

#### `bluetooth.getPairedDevices(): BluetoothDevice[]`

Get all paired/bonded Bluetooth devices (synchronous).

```typescript
import { bluetooth } from '@sincpro/printer-expo';

const devices = bluetooth.getPairedDevices();
const printers = devices.filter(d => d.isPrinter);
```

#### `bluetooth.getPairedPrinters(): PairedPrinter[]`

Get only paired devices that are printers (synchronous).

```typescript
const printers = bluetooth.getPairedPrinters();
console.log(printers); // [{ name: 'SPP-R200III', address: '00:11:22:AA:BB:CC' }]
```

---

#### `connection.connectBluetooth(address: string, timeoutMs?: number): Promise<void>`

Connect to a printer via Bluetooth.

**Parameters:**
- `address`: MAC address of the printer (e.g., `"00:11:22:AA:BB:CC"`)
- `timeoutMs`: Connection timeout in milliseconds (default: `10000`)

```typescript
import { connection } from '@sincpro/printer-expo';

await connection.connectBluetooth('00:11:22:AA:BB:CC', 30000);
```

#### `connection.connectWifi(ip: string, port?: number, timeoutMs?: number): Promise<void>`

Connect to a printer via WiFi.

**Parameters:**
- `ip`: IP address (e.g., `"192.168.1.100"`)
- `port`: TCP port (default: `9100`)
- `timeoutMs`: Connection timeout in milliseconds (default: `10000`)

```typescript
await connection.connectWifi('192.168.1.100', 9100, 30000);
```

#### `connection.connectUsb(): Promise<void>`

Connect to a printer via USB.

```typescript
await connection.connectUsb();
```

#### `connection.disconnect(): Promise<void>`

Disconnect from the current printer.

```typescript
await connection.disconnect();
```

#### `connection.isConnected(): boolean`

Check if currently connected (synchronous).

```typescript
const connected = connection.isConnected();
```

#### `connection.getStatus(): Promise<PrinterStatus>`

Get current printer status (paper, cover, errors).

```typescript
const status = await connection.getStatus();
console.log('Connection:', status.connectionState); // 'CONNECTED' | 'CONNECTING' | 'DISCONNECTED' | 'ERROR'
console.log('Has paper:', status.hasPaper);
console.log('Cover open:', status.isCoverOpen);
console.log('Error:', status.errorMessage);
```

#### `connection.getInfo(): Promise<PrinterInfo>`

Get printer information (model, firmware, serial).

```typescript
const info = await connection.getInfo();
console.log('Model:', info.model);
console.log('Firmware:', info.firmware);
console.log('Serial:', info.serial);
console.log('DPI:', info.dpi);
```

#### `connection.getDpi(): number`

Get printer DPI (synchronous).

```typescript
const dpi = connection.getDpi(); // e.g., 203 or 300
```

---

### Configuration API

#### `config.set(printerConfig: PrinterConfig): Promise<void>`

Set printer configuration (margins, density, speed, orientation, cutter). This sets the default config and applies it immediately if connected.

```typescript
import { config } from '@sincpro/printer-expo';

await config.set({
  marginLeft: 10,       // Left margin in dots
  marginTop: 5,         // Top margin in dots
  density: 'dark',      // 'light' | 'medium' | 'dark' | 'extra_dark'
  speed: 'medium',      // 'slow' | 'medium' | 'fast' | 'extra_fast'
  orientation: 'top_to_bottom', // 'top_to_bottom' | 'bottom_to_top'
  autoCutter: {
    enabled: true,
    fullCut: true       // true = full cut, false = partial cut
  }
});
```

#### `config.get(): PrinterConfig`

Get current printer configuration (synchronous).

```typescript
const currentConfig = config.get();
console.log('Density:', currentConfig.density);
console.log('Speed:', currentConfig.speed);
```

---

### Print API

#### `print.text(text: string, options?: PrintTextOptions): Promise<void>`

Print a single line of text.

**Options:**
- `fontSize`: `'small'` | `'medium'` | `'large'` | `'xlarge'`
- `alignment`: `'left'` | `'center'` | `'right'`
- `bold`: `boolean`
- `media`: `MediaConfig`

```typescript
import { print } from '@sincpro/printer-expo';

await print.text('Hello World!', {
  fontSize: 'large',
  alignment: 'center',
  bold: true,
  media: { preset: 'continuous80mm' }
});
```

#### `print.texts(texts: string[], options?: PrintTextsOptions): Promise<void>`

Print multiple lines of text.

```typescript
await print.texts(
  ['Line 1', 'Line 2', 'Line 3'],
  { fontSize: 'medium', media: { preset: 'continuous80mm' } }
);
```

#### `print.qr(data: string, options?: PrintQROptions): Promise<void>`

Print a QR code.

**Options:**
- `size`: QR size 1-10 (default: `5`)
- `alignment`: `'left'` | `'center'` | `'right'`
- `media`: `MediaConfig`

```typescript
await print.qr('https://sincpro.com', {
  size: 8,
  alignment: 'center',
  media: { preset: 'continuous80mm' }
});
```

#### `print.barcode(data: string, options?: PrintBarcodeOptions): Promise<void>`

Print a barcode.

**Options:**
- `type`: `'CODE128'` | `'CODE39'` | `'EAN13'` | `'EAN8'` | `'UPCA'` | `'UPCE'` | `'CODE93'` | `'CODABAR'`
- `height`: Barcode height in dots
- `alignment`: `'left'` | `'center'` | `'right'`
- `media`: `MediaConfig`

```typescript
await print.barcode('123456789012', {
  type: 'CODE128',
  height: 80,
  alignment: 'center',
  media: { preset: 'continuous80mm' }
});
```

#### `print.imageBase64(base64Data: string, options?: PrintImageOptions): Promise<void>`

Print an image from base64 data.

**Options:**
- `alignment`: `'left'` | `'center'` | `'right'`
- `media`: `MediaConfig`

```typescript
await print.imageBase64(base64ImageData, {
  alignment: 'center',
  media: { preset: 'continuous80mm' }
});
```

#### `print.pdfBase64(base64Data: string, options?: PrintPdfOptions): Promise<void>`

Print a PDF page from base64 data.

**Options:**
- `page`: Page number to print (default: `0`)
- `alignment`: `'left'` | `'center'` | `'right'`
- `media`: `MediaConfig`

```typescript
await print.pdfBase64(base64PdfData, {
  page: 0,
  alignment: 'center',
  media: { preset: 'continuous80mm' }
});
```

#### `print.getPdfPageCount(base64Data: string): number`

Get page count from a PDF (synchronous).

```typescript
const pageCount = print.getPdfPageCount(base64PdfData);
```

#### `print.keyValue(key: string, value: string, options?: PrintKeyValueOptions): Promise<void>`

Print a key-value pair (two columns).

```typescript
await print.keyValue('Total', '$25.00', {
  fontSize: 'large',
  bold: true,
  media: { preset: 'continuous80mm' }
});
```

#### `print.receipt(receipt: Receipt, options?: PrintReceiptOptions): Promise<void>`

Print a complete structured receipt with header, body, and footer sections.

**Options:**
- `media`: `MediaConfig`
- `copies`: Number of copies to print (default: `1`)

```typescript
await print.receipt({
  header: [
    { type: 'text', content: 'MY STORE', fontSize: 'large', alignment: 'center', bold: true },
    { type: 'text', content: '123 Main Street', alignment: 'center' },
    { type: 'separator' },
  ],
  body: [
    { type: 'keyValue', key: 'Product 1', value: '$10.00' },
    { type: 'keyValue', key: 'Product 2', value: '$15.00' },
    { type: 'separator' },
    { type: 'keyValue', key: 'Tax', value: '$2.50' },
  ],
  footer: [
    { type: 'separator' },
    { type: 'keyValue', key: 'TOTAL', value: '$27.50', bold: true, fontSize: 'large' },
    { type: 'qr', data: 'https://mystore.com/receipt/123', alignment: 'center', size: 6 },
    { type: 'text', content: 'Thank you for your purchase!', alignment: 'center' },
    { type: 'space', lines: 2 },
  ]
}, { media: { preset: 'continuous80mm' }, copies: 1 });
```

---

### Receipt Line Types

Receipt lines are the building blocks of structured receipts. Each line type has specific properties.

#### `TextLine`

Print formatted text with customizable style.

```typescript
{
  type: 'text',
  content: string,
  fontSize?: 'small' | 'medium' | 'large' | 'xlarge',
  bold?: boolean,
  alignment?: 'left' | 'center' | 'right'
}
```

**Example:**
```typescript
{ type: 'text', content: 'INVOICE', fontSize: 'xlarge', alignment: 'center', bold: true }
```

#### `KeyValueLine`

Print key-value pairs in two columns (common in receipts).

```typescript
{
  type: 'keyValue',
  key: string,
  value: string,
  fontSize?: 'small' | 'medium' | 'large' | 'xlarge',
  bold?: boolean
}
```

**Example:**
```typescript
{ type: 'keyValue', key: 'Subtotal', value: '$25.00' }
{ type: 'keyValue', key: 'TOTAL', value: '$27.50', bold: true, fontSize: 'large' }
```

#### `QRLine`

Embed QR codes in receipts.

```typescript
{
  type: 'qr',
  data: string,
  size?: number,           // 1-10, default: 5
  alignment?: 'left' | 'center' | 'right'
}
```

**Example:**
```typescript
{ type: 'qr', data: 'https://store.com/receipt/12345', size: 6, alignment: 'center' }
```

#### `BarcodeLine`

Embed barcodes in receipts.

```typescript
{
  type: 'barcode',
  data: string,
  barcodeType?: 'CODE128' | 'CODE39' | 'EAN13' | 'EAN8' | 'UPCA' | 'UPCE' | 'CODE93' | 'CODABAR',
  height?: number,
  alignment?: 'left' | 'center' | 'right'
}
```

**Example:**
```typescript
{ type: 'barcode', data: '123456789012', barcodeType: 'EAN13', height: 80, alignment: 'center' }
```

#### `ImageLine`

Embed images (base64) in receipts.

```typescript
{
  type: 'image',
  base64: string,
  alignment?: 'left' | 'center' | 'right'
}
```

**Example:**
```typescript
{ type: 'image', base64: 'iVBORw0KGgoAAAANS...', alignment: 'center' }
```

#### `SeparatorLine`

Print horizontal separator lines.

```typescript
{
  type: 'separator',
  char?: string,       // Character to repeat, default: '-'
  length?: number      // Line length in characters, default: 48
}
```

**Examples:**
```typescript
{ type: 'separator' }
{ type: 'separator', char: '=', length: 32 }
```

#### `SpaceLine`

Add blank lines for spacing.

```typescript
{
  type: 'space',
  lines?: number       // Number of blank lines, default: 1
}
```

**Example:**
```typescript
{ type: 'space', lines: 2 }
```

#### `ColumnsLine`

Print multiple columns in one row.

```typescript
{
  type: 'columns',
  columns: Array<{
    text: string,
    widthRatio?: number,
    alignment?: 'left' | 'center' | 'right'
  }>,
  fontSize?: 'small' | 'medium' | 'large' | 'xlarge',
  bold?: boolean
}
```

**Example:**
```typescript
{
  type: 'columns',
  columns: [
    { text: 'Item', widthRatio: 2, alignment: 'left' },
    { text: 'Qty', widthRatio: 1, alignment: 'center' },
    { text: 'Price', widthRatio: 1, alignment: 'right' }
  ],
  bold: true
}
```

---

### MediaConfig - Paper Configuration

Configure paper/label dimensions for printing. You can use presets or custom configurations.

#### Using Presets

```typescript
// Continuous paper presets
{ preset: 'continuous58mm' }  // 58mm continuous paper
{ preset: 'continuous72mm' }  // 72mm continuous paper
{ preset: 'continuous80mm' }  // 80mm continuous paper (most common)
```

#### Custom Configuration (Millimeters)

```typescript
// Custom continuous paper
{
  widthMm: 72,
  type: 'continuous'
}

// Labels with gap
{
  widthMm: 50,
  heightMm: 30,
  gapMm: 3,
  type: 'gap'
}

// Labels with black mark
{
  widthMm: 60,
  heightMm: 40,
  type: 'black_mark'
}
```

#### Custom Configuration (Dots)

If you need precise control, you can specify dimensions in dots (based on printer DPI).

```typescript
{
  widthDots: 576,   // 72mm at 203 DPI
  heightDots: 240,  // 30mm at 203 DPI
  gapDots: 24,      // 3mm at 203 DPI
  type: 'gap'
}
```

**Note:** The module automatically converts millimeters to dots based on the printer's DPI. Using millimeters is recommended for easier configuration.

---

## 📦 TypeScript Types

### BluetoothDevice

```typescript
interface BluetoothDevice {
  name: string;            // Device name
  address: string;         // MAC address (e.g., "00:11:22:AA:BB:CC")
  isPrinter: boolean;      // True if device is identified as a printer
}
```

### PairedPrinter

```typescript
interface PairedPrinter {
  name: string;            // Printer name
  address: string;         // MAC address
}
```

### PrinterStatus

```typescript
interface PrinterStatus {
  connectionState: 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'ERROR';
  hasPaper: boolean;       // True if paper is available
  isCoverOpen: boolean;    // True if printer cover is open
  isOverheated: boolean;   // True if printer is overheated
  hasError: boolean;       // True if printer has an error
  errorMessage: string | null; // Error message if hasError is true
}
```

### PrinterInfo

```typescript
interface PrinterInfo {
  model: string;           // Printer model (e.g., "SPP-R200III")
  firmware: string;        // Firmware version
  serial: string;          // Serial number
  dpi: number;             // Printer DPI (e.g., 203 or 300)
}
```

### PrinterConfig

```typescript
interface PrinterConfig {
  marginLeft?: number;     // Left margin in dots
  marginTop?: number;      // Top margin in dots
  density?: 'light' | 'medium' | 'dark' | 'extra_dark';
  speed?: 'slow' | 'medium' | 'fast' | 'extra_fast';
  orientation?: 'top_to_bottom' | 'bottom_to_top';
  autoCutter?: {
    enabled: boolean;      // Enable auto cutter
    fullCut?: boolean;     // Full cut (true) or partial cut (false)
  };
}
```

### MediaConfig

```typescript
interface MediaConfig {
  // Use preset for common paper sizes
  preset?: 'continuous58mm' | 'continuous72mm' | 'continuous80mm';
  
  // Or custom configuration in millimeters
  widthMm?: number;        // Paper width in mm
  heightMm?: number;       // Label height in mm (for labels)
  gapMm?: number;          // Gap size in mm (for labels)
  
  // Or custom configuration in dots
  widthDots?: number;      // Paper width in dots
  heightDots?: number;     // Label height in dots
  gapDots?: number;        // Gap size in dots
  
  // Media type
  type?: 'continuous' | 'gap' | 'label' | 'black_mark';
}
```

### Receipt

```typescript
interface Receipt {
  header?: ReceiptLine[];  // Header section (logo, store info)
  body?: ReceiptLine[];    // Body section (items, details)
  footer?: ReceiptLine[];  // Footer section (totals, QR, thanks)
}
```

### ReceiptLine Types

```typescript
type ReceiptLine =
  | TextLine
  | KeyValueLine
  | QRLine
  | BarcodeLine
  | ImageLine
  | SeparatorLine
  | SpaceLine
  | ColumnsLine;
```

---

## 🎯 Usage Examples

### Example 1: Basic Connection and Text Printing

```typescript
import { bluetooth, connection, print } from '@sincpro/printer-expo';

async function basicPrint() {
  try {
    // 1. Get paired printers
    const printers = bluetooth.getPairedPrinters();
    if (printers.length === 0) {
      throw new Error('No paired printers found');
    }

    // 2. Connect to first printer
    await connection.connectBluetooth(printers[0].address, 30000);
    console.log('Connected to', printers[0].name);

    // 3. Print simple text
    await print.text('Hello from Expo!', {
      fontSize: 'large',
      alignment: 'center',
      bold: true,
      media: { preset: 'continuous80mm' }
    });

    // 4. Disconnect
    await connection.disconnect();
    console.log('Print complete!');
  } catch (error) {
    console.error('Print failed:', error);
  }
}
```

### Example 2: Sales Receipt

```typescript
import { connection, print } from '@sincpro/printer-expo';

async function printSalesReceipt(items: Array<{ name: string; qty: number; price: number }>) {
  const subtotal = items.reduce((sum, item) => sum + (item.qty * item.price), 0);
  const tax = subtotal * 0.08; // 8% tax
  const total = subtotal + tax;

  await print.receipt({
    header: [
      { type: 'text', content: '🏪 MY RETAIL STORE', fontSize: 'xlarge', alignment: 'center', bold: true },
      { type: 'text', content: '123 Commerce Street', alignment: 'center' },
      { type: 'text', content: 'Phone: (555) 123-4567', alignment: 'center' },
      { type: 'separator', char: '=' },
      { type: 'text', content: 'SALES RECEIPT', fontSize: 'large', alignment: 'center' },
      { type: 'separator', char: '=' },
      { type: 'space' },
      { type: 'keyValue', key: 'Date', value: new Date().toLocaleDateString() },
      { type: 'keyValue', key: 'Receipt #', value: 'RCP-' + Date.now() },
      { type: 'space' },
      { type: 'separator' },
    ],
    body: [
      { type: 'text', content: 'ITEMS', bold: true },
      { type: 'separator' },
      ...items.map(item => ({
        type: 'keyValue' as const,
        key: `${item.name} (x${item.qty})`,
        value: `$${(item.qty * item.price).toFixed(2)}`
      })),
      { type: 'space' },
      { type: 'separator' },
      { type: 'keyValue', key: 'Subtotal', value: `$${subtotal.toFixed(2)}` },
      { type: 'keyValue', key: 'Tax (8%)', value: `$${tax.toFixed(2)}` },
    ],
    footer: [
      { type: 'separator', char: '=' },
      { type: 'keyValue', key: 'TOTAL', value: `$${total.toFixed(2)}`, fontSize: 'large', bold: true },
      { type: 'separator', char: '=' },
      { type: 'space', lines: 2 },
      { type: 'text', content: 'Thank you for your purchase!', alignment: 'center' },
      { type: 'text', content: 'Visit us at www.mystore.com', alignment: 'center' },
      { type: 'space' },
      { type: 'qr', data: `https://mystore.com/receipt/${Date.now()}`, size: 6, alignment: 'center' },
      { type: 'space', lines: 3 },
    ]
  }, { media: { preset: 'continuous80mm' } });
}

// Usage
printSalesReceipt([
  { name: 'Coffee', qty: 2, price: 4.50 },
  { name: 'Croissant', qty: 1, price: 3.25 },
  { name: 'Orange Juice', qty: 1, price: 2.75 }
]);
```

### Example 3: Product Label Printing

```typescript
import { config, print } from '@sincpro/printer-expo';

async function printProductLabel(product: { name: string; sku: string; price: number }) {
  // Configure for label printing
  await config.set({
    marginLeft: 5,
    marginTop: 5,
    density: 'dark',
    speed: 'medium'
  });

  await print.receipt({
    header: [
      { type: 'text', content: product.name, fontSize: 'large', alignment: 'center', bold: true },
      { type: 'space' },
    ],
    body: [
      { type: 'barcode', data: product.sku, barcodeType: 'CODE128', height: 60, alignment: 'center' },
      { type: 'space' },
      { type: 'text', content: `SKU: ${product.sku}`, alignment: 'center' },
    ],
    footer: [
      { type: 'space' },
      { type: 'text', content: `$${product.price.toFixed(2)}`, fontSize: 'xlarge', alignment: 'center', bold: true },
    ]
  }, {
    media: {
      widthMm: 50,
      heightMm: 30,
      gapMm: 3,
      type: 'gap'
    }
  });
}

// Usage
printProductLabel({
  name: 'Premium Coffee Beans',
  sku: '1234567890',
  price: 12.99
});
```

### Example 4: Error Handling Pattern

```typescript
import { connection, print } from '@sincpro/printer-expo';
import type { Receipt } from '@sincpro/printer-expo';

async function safePrint(receipt: Receipt) {
  try {
    // Check connection
    if (!connection.isConnected()) {
      throw new Error('Printer not connected');
    }

    // Check printer status
    const status = await connection.getStatus();
    
    if (status.connectionState !== 'CONNECTED') {
      throw new Error(`Printer is ${status.connectionState.toLowerCase()}`);
    }
    
    if (!status.hasPaper) {
      throw new Error('Printer is out of paper');
    }
    
    if (status.isCoverOpen) {
      throw new Error('Printer cover is open');
    }
    
    if (status.isOverheated) {
      throw new Error('Printer is overheated');
    }
    
    if (status.hasError) {
      throw new Error(`Printer error: ${status.errorMessage}`);
    }

    // Print receipt
    await print.receipt(receipt, { media: { preset: 'continuous80mm' } });
    
    console.log('✅ Print successful');
    return { success: true };
  } catch (error) {
    console.error('❌ Print failed:', error);
    
    // Return user-friendly error
    return {
      success: false,
      error: error instanceof Error ? error.message : 'Unknown error'
    };
  }
}

// Usage in React component
function PrintButton() {
  const handlePrint = async () => {
    const result = await safePrint(myReceipt);
    
    if (result.success) {
      Alert.alert('Success', 'Receipt printed successfully');
    } else {
      Alert.alert('Print Error', result.error);
    }
  };

  return <Button title="Print" onPress={handlePrint} />;
}
```

### Example 5: Custom Hook for Printer Management

```typescript
import { useState, useEffect } from 'react';
import { bluetooth, connection, print } from '@sincpro/printer-expo';
import type { BluetoothDevice, PrinterStatus, Receipt } from '@sincpro/printer-expo';

export function usePrinter() {
  const [devices, setDevices] = useState<BluetoothDevice[]>([]);
  const [connected, setConnected] = useState(false);
  const [status, setStatus] = useState<PrinterStatus | null>(null);

  useEffect(() => {
    checkConnection();
  }, []);

  const checkConnection = () => {
    setConnected(connection.isConnected());
  };

  const scanDevices = () => {
    try {
      const foundDevices = bluetooth.getPairedDevices();
      setDevices(foundDevices.filter(d => d.isPrinter));
      return foundDevices;
    } catch (error) {
      console.error('Scan failed:', error);
      throw error;
    }
  };

  const connect = async (address: string, timeoutMs = 30000) => {
    try {
      await connection.connectBluetooth(address, timeoutMs);
      setConnected(true);
      
      const printerStatus = await connection.getStatus();
      setStatus(printerStatus);
      
      return true;
    } catch (error) {
      console.error('Connection failed:', error);
      throw error;
    }
  };

  const disconnect = async () => {
    try {
      await connection.disconnect();
      setConnected(false);
      setStatus(null);
      return true;
    } catch (error) {
      console.error('Disconnection failed:', error);
      throw error;
    }
  };

  const printReceipt = async (receipt: Receipt) => {
    if (!connected) {
      throw new Error('Not connected to printer');
    }

    await print.receipt(receipt, { media: { preset: 'continuous80mm' } });
  };

  const refreshStatus = async () => {
    if (connected) {
      const printerStatus = await connection.getStatus();
      setStatus(printerStatus);
      return printerStatus;
    }
    return null;
  };

  return {
    devices,
    connected,
    status,
    scanDevices,
    connect,
    disconnect,
    printReceipt,
    refreshStatus,
  };
}

// Usage in component
function MyPrinterComponent() {
  const printer = usePrinter();

  return (
    <View>
      <Button title="Scan Printers" onPress={printer.scanDevices} />
      <Button 
        title="Print Receipt" 
        onPress={() => printer.printReceipt(myReceipt)}
        disabled={!printer.connected}
      />
      {printer.status && (
        <Text>Status: {printer.status.connectionState}</Text>
      )}
    </View>
  );
}
```

---

## 🏗️ Architecture

This module follows **Clean Architecture** with **Hexagonal Architecture** (Ports & Adapters) principles for maximum maintainability and extensibility.

### Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│  Dependencies ALWAYS point inward (toward Domain)       │
└─────────────────────────────────────────────────────────┘

TypeScript (React Native)
         ↓
   ENTRYPOINT ← Expo Modules API bridge
         ↓
   SERVICE ← Use cases & orchestration
         ↓
   DOMAIN ← Business entities & rules (interfaces)
         ↑
   ┌─────┴─────┐
ADAPTER    INFRASTRUCTURE
(Vendors)  (Platform APIs)
```

### Layer Responsibilities

| Layer | Purpose | Examples |
|-------|---------|----------|
| **TypeScript** | React Native API | `bluetooth`, `connection`, `config`, `print` |
| **Entrypoint** | Expo ↔ Kotlin bridge | `PrinterModule.kt` |
| **Service** | Business logic | `ConnectivityService`, `PrintService` |
| **Domain** | Contracts & entities | `IPrinter`, `Receipt`, `MediaConfig` |
| **Adapter** | Vendor SDKs | `BixolonPrinterAdapter` |
| **Infrastructure** | Platform utilities | `AndroidBluetoothProvider`, `BinaryConverter` |

### Benefits

- ✅ **Testable**: Mock adapters and services independently
- ✅ **Maintainable**: Clear separation of concerns
- ✅ **Extensible**: Easy to add new printer brands (Zebra, Epson, Star, etc.)
- ✅ **Swappable**: Change implementations without affecting business logic
- ✅ **Framework-independent**: Domain layer has no Android/iOS dependencies

### Adding New Printer Brands

The architecture makes it easy to support additional printer brands:

1. **Create Adapter**: Implement `IPrinter` interface for new vendor SDK
2. **Register in SDK**: Add to `SincproPrinterSdk` entry point
3. **No changes needed**: Business logic and API remain unchanged

See [ARCHITECTURE.md](./ARCHITECTURE.md) for detailed architecture documentation.

---

## 🖨️ Supported Printers

### Bixolon Printers

This module currently supports **Bixolon thermal printers** via the official Bixolon SDK.

#### Tested Models

- ✅ **SPP-R200III** - 2" mobile printer (58mm)
- ✅ **SPP-R300** - 3" mobile printer (80mm)
- ✅ **SPP-R400** - 4" mobile printer (112mm)
- ✅ **SRP-275III** - 3" desktop printer (80mm)
- ✅ **SRP-350III** - 3" desktop printer (80mm)
- ✅ **SRP-352III** - 3" desktop printer (80mm)

#### Compatible Models

The following Bixolon models should work but have not been tested:

- SPP-R210, SPP-R220, SPP-R310, SPP-R410
- SRP-330II, SRP-350plusIII, SRP-380
- XD3-40d, XD5-40d
- XT5-40, XT5-43

#### Connectivity Support

| Connection Type | Status | Notes |
|----------------|--------|-------|
| **Bluetooth** | ✅ Fully supported | Most common for mobile printers |
| **WiFi** | ✅ Supported | For network-connected printers |
| **USB** | ⚠️ Limited | Requires USB OTG on Android |

### Adding Support for Other Brands

The architecture supports adding other printer brands. To add support:

1. Implement the `IPrinter` interface in a new adapter
2. Integrate the vendor's SDK (Zebra, Epson, Star, etc.)
3. Register the adapter in the SDK entry point

See [CONTRIBUTING.md](./CONTRIBUTING.md) for development guidelines.

---

## 🛠️ Troubleshooting

### Connection Issues

**Problem**: Connection fails or times out

**Solutions**:
1. Verify Bluetooth is enabled on the device
2. Check device is paired: `bluetooth.getPairedDevices()`
3. Ensure printer is powered on and in range (< 10 meters)
4. Try increasing timeout: `connectBluetooth(address, 60000)` (60 seconds)
5. Restart printer and retry connection

**Example:**
```typescript
try {
  await connection.connectBluetooth(address, 60000);
} catch (error) {
  console.error('Connection failed:', error);
  // Try restarting printer
}
```

### Print Failures

**Problem**: Print command succeeds but nothing prints

**Solutions**:
1. Check printer status:
   ```typescript
   const status = await connection.getStatus();
   if (!status.hasPaper) console.error('No paper!');
   if (status.isCoverOpen) console.error('Cover open!');
   ```
2. Verify connection: `connection.isConnected()`
3. Check media configuration matches paper type
4. Ensure printer is not in an error state

### Paper Not Feeding

**Problem**: Paper doesn't feed after printing

**Solutions**:
1. Add space lines at the end of receipt:
   ```typescript
   footer: [
     // ... other lines
     { type: 'space', lines: 3 }  // Add extra space
   ]
   ```
2. Configure auto-cutter:
   ```typescript
   await config.set({
     autoCutter: { enabled: true, fullCut: true }
   });
   ```

### Bluetooth Permissions Denied

**Problem**: "Permission denied" errors

**Solution**: Ensure permissions are declared in `app.json` and granted at runtime. Android 12+ requires runtime permission grants.

```json
{
  "expo": {
    "android": {
      "permissions": [
        "android.permission.BLUETOOTH",
        "android.permission.BLUETOOTH_ADMIN",
        "android.permission.BLUETOOTH_SCAN",
        "android.permission.BLUETOOTH_CONNECT"
      ]
    }
  }
}
```

### Image/QR Not Printing

**Problem**: Images or QR codes don't appear

**Solutions**:
1. Check base64 data is valid
2. Ensure alignment is correct
3. Try different QR size (1-10)
4. Verify printer supports graphics

### Module Not Found Error

**Problem**: `Module "SincproPrinter" not found`

**Solutions**:
1. Rebuild native modules:
   ```bash
   npx expo prebuild --clean
   npx expo run:android
   ```
2. Clear cache:
   ```bash
   npm start -- --clear
   ```

---

## 📖 Resources

- **Package**: [NPM Package](https://www.npmjs.com/package/@sincpro/printer-expo)
- **Repository**: [GitHub](https://github.com/Sincpro-SRL/sincpro_printer_expo)
- **Architecture**: [ARCHITECTURE.md](./ARCHITECTURE.md) - Detailed architecture guide
- **Contributing**: [CONTRIBUTING.md](./CONTRIBUTING.md) - Development guidelines
- **Expo Modules**: [Official Documentation](https://docs.expo.dev/modules/overview/)
- **Bixolon**: [Official Website](https://www.bixolon.com/)

---

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](./CONTRIBUTING.md) for:

- Development setup and environment
- Code standards (ktlint, Prettier, ESLint)
- Architecture guidelines and patterns
- Git workflow and branch naming
- Pull request process and review
- Testing requirements

### Quick Start for Contributors

```bash
# Clone the repository
git clone https://github.com/Sincpro-SRL/sincpro_printer_expo.git
cd sincpro_printer_expo

# Install dependencies
npm install

# Build TypeScript
npm run build

# Format code
npm run format
npm run format:kotlin

# Lint code
npm run lint
npm run lint:kotlin
```

---

## 📄 License

MIT License - see [LICENSE](./LICENSE) file for details.

Copyright (c) 2024 Sincpro SRL

---

## 🙏 Acknowledgments

- **Bixolon** for the official printer SDK
- **Expo team** for the Modules API
- **Contributors** and testers who helped improve this module
- **Open source community** for inspiration and support

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/Sincpro-SRL/sincpro_printer_expo/issues)
- **Discussions**: [GitHub Discussions](https://github.com/Sincpro-SRL/sincpro_printer_expo/discussions)
- **Email**: support@sincpro.com

---

**Made with ❤️ by Sincpro SRL**
