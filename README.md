# @sincpro/printer-expo

[![npm version](https://badge.fury.io/js/@sincpro/printer-expo.svg)](https://badge.fury.io/js/@sincpro/printer-expo)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Expo](https://img.shields.io/badge/Expo-000000.svg?style=flat&logo=expo&logoColor=white)](https://expo.dev)

A powerful React Native module for controlling thermal printers in Expo applications. Built with **Clean Architecture** and **Hexagonal Architecture** (Ports & Adapters) for maximum flexibility and maintainability.

## ✨ Features

- 🔗 **Bluetooth Connectivity**: Full device discovery and connection management
- 📱 **Permission Management**: Smart handling of Android Bluetooth permissions
- 🖨️ **Advanced Printing**: Receipt printing with flexible line types
- 📋 **QR Code Support**: Print QR codes with customizable sizes
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

**Note**: The module automatically handles runtime permission requests on Android 12+.

````

## 🚀 Quick Start

### Basic Example

```typescript
import Printer, { BluetoothDevice } from '@sincpro/printer-expo';

// 1. Check Bluetooth status
const isEnabled = await Printer.bluetooth.isEnabled();
const isSupported = await Printer.bluetooth.isSupported();

// 2. Check permissions
const permStatus = Printer.permission.getStatus();
if (!permStatus.allGranted) {
  console.log('Missing permissions:', permStatus.deniedPermissions);
}

// 3. Get paired devices
const devices = await Printer.bluetooth.getPairedDevices();
const printers = devices.filter(d => d.isPrinter);

// 4. Connect to printer
await Printer.connection.connect(printers[0].address);

// 5. Print receipt
await Printer.print.receipt({
  header: [
    { type: 'text', content: 'MY STORE', fontSize: 'large', alignment: 'center', bold: true },
    { type: 'separator' },
  ],
  details: [
    { type: 'keyValue', key: 'Product', value: '$10.00' },
    { type: 'keyValue', key: 'Tax', value: '$1.00' },
  ],
  footer: [
    { type: 'separator' },
    { type: 'keyValue', key: 'TOTAL', value: '$11.00', bold: true },
    { type: 'space', lines: 2 },
  ],
});
````

### Complete React Component

```typescript
import React, { useState, useEffect } from 'react';
import { View, Button, FlatList, Text, Alert } from 'react-native';
import Printer, { BluetoothDevice } from '@sincpro/printer-expo';

export default function PrinterScreen() {
  const [devices, setDevices] = useState<BluetoothDevice[]>([]);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    checkPermissions();
  }, []);

  const checkPermissions = () => {
    const status = Printer.permission.getStatus();

    if (!status.allGranted) {
      Alert.alert('Permissions Required', `Missing: ${status.deniedPermissions.join(', ')}`);
    }
  };

  const scanDevices = async () => {
    try {
      const isEnabled = await Printer.bluetooth.isEnabled();
      if (!isEnabled) {
        Alert.alert('Error', 'Please enable Bluetooth');
        return;
      }

      const foundDevices = await Printer.bluetooth.getPairedDevices();
      setDevices(foundDevices.filter((d) => d.isPrinter));
    } catch (error) {
      Alert.alert('Error', error.message);
    }
  };

  const connectDevice = async (device: BluetoothDevice) => {
    try {
      await Printer.connection.connect(device.address);
      setConnected(true);
      Alert.alert('Success', `Connected to ${device.name}`);
    } catch (error) {
      Alert.alert('Error', 'Connection failed');
    }
  };

  const printTest = async () => {
    try {
      await Printer.print.receipt({
        header: [
          { type: 'text', content: 'Test Receipt', fontSize: 'large', alignment: 'center' },
          { type: 'separator' },
        ],
        details: [{ type: 'text', content: 'This is a test print' }],
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

````

## 📱 Detailed Usage

### Bluetooth Device Discovery

```typescript
// Check Bluetooth permissions
const checkPermissions = async () => {
  const permissions = await BixolonPrinter.checkBluetoothPermissions();
  console.log('Permissions:', permissions);

  // Check if all required permissions are granted
  const allGranted = Object.values(permissions).every((granted) => granted);
  if (!allGranted) {
    await requestPermissions();
  }
};

// Request permissions if needed
const requestPermissions = async () => {
  try {
    const granted = await BixolonPrinter.requestBluetoothPermissions();
    if (granted) {
      console.log('✅ All permissions granted');
    } else {
      console.log('❌ Some permissions were denied');
    }
  } catch (error) {
    console.error('Permission request failed:', error);
  }
};

// Discover Bluetooth devices
const discoverDevices = async () => {
  try {
    // Check if Bluetooth is enabled
    const isEnabled = await BixolonPrinter.isBluetoothEnabled();
    if (!isEnabled) {
      Alert.alert('Error', 'Please enable Bluetooth first');
      return;
    }

    // Start discovery
    await BixolonPrinter.startBluetoothDiscovery();

    // Get paired devices
    const devices = await BixolonPrinter.discoverBluetoothDevices();
    console.log('Found devices:', devices);

    // Filter for printer devices
    const printers = devices.filter((device) => device.isPrinter);
    console.log('Printer devices:', printers);
  } catch (error) {
    console.error('Device discovery failed:', error);
  } finally {
    // Stop discovery
    await BixolonPrinter.stopBluetoothDiscovery();
  }
};
````

### Connecting to a Printer

```typescript
// Connect to a Bluetooth device
const connectToDevice = async (device: BluetoothDevice) => {
  try {
    console.log(`Connecting to ${device.name} (${device.address})...`);

    const success = await BixolonPrinter.connectPrinter(
      'BLUETOOTH', // Interface type
      device.address, // Device MAC address
      1 // Port (usually 1 for Bluetooth)
    );

    if (success) {
      console.log('✅ Connected to printer successfully');
      setIsConnected(true);
    } else {
      console.log('❌ Connection failed');
    }
  } catch (error) {
    console.error('Connection error:', error);
    Alert.alert('Connection Failed', error.message);
  }
};

// Connect via WiFi (if supported)
const connectViaWiFi = async (ipAddress: string, port: number = 9100) => {
  try {
    const success = await BixolonPrinter.connectPrinter('WIFI', ipAddress, port);

    if (success) {
      console.log('✅ Connected via WiFi');
    }
  } catch (error) {
    console.error('WiFi connection failed:', error);
  }
};
```

### Advanced Printing

```typescript
// Print formatted text with custom formatting
const printFormattedText = async () => {
  try {
    const text = `COMPANY NAME
123 Main Street
City, State 12345

Invoice #: INV-001
Date: ${new Date().toLocaleDateString()}
Customer: John Doe

Items:
- Product A    $10.00
- Product B    $15.00
- Product C    $25.00

Total: $50.00

Thank you for your business!`;

    const success = await BixolonPrinter.printFormattedText(text, 10);
    if (success) {
      console.log('✅ Formatted text printed');
    }
  } catch (error) {
    console.error('Print error:', error);
  }
};

// Print text in pages (for long content)
const printLongText = async () => {
  try {
    const longText = `This is a very long text that will be split into multiple pages for better printing on thermal printers...`;

    const success = await BixolonPrinter.printTextInPages(longText);
    if (success) {
      console.log('✅ Long text printed in pages');
    }
  } catch (error) {
    console.error('Page printing error:', error);
  }
};

// Print a complete invoice
const printInvoice = async () => {
  try {
    const items = [
      { description: 'Premium Coffee', quantity: 2, price: 4.5 },
      { description: 'Chocolate Croissant', quantity: 1, price: 3.25 },
      { description: 'Fresh Orange Juice', quantity: 1, price: 2.75 },
    ];

    const success = await BixolonPrinter.printInvoice(
      'Jane Smith', // Customer name
      items, // Items array
      15.0 // Total amount
    );

    if (success) {
      console.log('✅ Invoice printed successfully');
    }
  } catch (error) {
    console.error('Invoice printing error:', error);
  }
};
```

### Disconnecting and Cleanup

```typescript
// Disconnect from printer
const disconnect = async () => {
  try {
    const success = await BixolonPrinter.disconnectPrinter();
    if (success) {
      console.log('✅ Disconnected from printer');
      setIsConnected(false);
    }
  } catch (error) {
    console.error('Disconnection error:', error);
  }
};

// Cleanup on component unmount
useEffect(() => {
  return () => {
    if (isConnected) {
      disconnect();
    }
  };
}, [isConnected]);
```

## 📋 Local Installation Guide

### Creating a Local Package

If you want to distribute the module locally without publishing to NPM:

```bash
# 1. Build the module
cd /path/to/expo-bixolon
npm run build

# 2. Create a tarball
npm pack

# This creates: expo-bixolon-0.1.0.tgz
```

### Installing in Another Project

```bash
# Method 1: Direct tarball installation
cd /path/to/your-app
npm install /path/to/expo-bixolon-0.1.0.tgz

# Method 2: Using file path
npm install /absolute/path/to/expo-bixolon

# Method 3: Using yarn
yarn add /path/to/expo-bixolon-0.1.0.tgz
```

### Post-Installation Steps

```bash
# Install native dependencies
npx expo install

# Generate native code
npx expo prebuild

# Run the app
npx expo run:android
# or
npx expo run:ios
```

## 📚 API Reference

### Namespaces

The API is organized into four main namespaces:

```typescript
import Printer from '@sincpro/printer-expo';

Printer.bluetooth; // Bluetooth operations
Printer.permission; // Permission management
Printer.connection; // Connection control
Printer.print; // Printing operations
```

---

### Bluetooth API

#### `bluetooth.isEnabled(): Promise<boolean>`

Check if Bluetooth is enabled on the device.

```typescript
const enabled = await Printer.bluetooth.isEnabled();
```

#### `bluetooth.isSupported(): Promise<boolean>`

Check if Bluetooth is supported on the device.

```typescript
const supported = await Printer.bluetooth.isSupported();
```

#### `bluetooth.getPairedDevices(): Promise<BluetoothDevice[]>`

Get list of paired/bonded Bluetooth devices.

```typescript
const devices = await Printer.bluetooth.getPairedDevices();
const printers = devices.filter((d) => d.isPrinter);
```

#### `bluetooth.startDiscovery(): Promise<boolean>`

Start Bluetooth discovery to find nearby devices.

```typescript
await Printer.bluetooth.startDiscovery();
```

#### `bluetooth.stopDiscovery(): Promise<boolean>`

Stop Bluetooth discovery.

```typescript
await Printer.bluetooth.stopDiscovery();
```

---

### Permission API

#### `permission.hasBluetoothPermissions(): boolean`

Check if all required Bluetooth permissions are granted (synchronous).

```typescript
const hasAll = Printer.permission.hasBluetoothPermissions();
```

#### `permission.getRequiredPermissions(): string[]`

Get list of required permissions for the current Android version.

```typescript
const required = Printer.permission.getRequiredPermissions();
// Returns: ['android.permission.BLUETOOTH_SCAN', 'android.permission.BLUETOOTH_CONNECT', ...]
```

#### `permission.getMissingPermissions(): string[]`

Get list of missing/denied permissions.

```typescript
const missing = Printer.permission.getMissingPermissions();
console.log('Need to request:', missing);
```

#### `permission.getStatus(): PermissionStatus`

Get detailed permission status.

```typescript
const status = Printer.permission.getStatus();
console.log('All granted:', status.allGranted);
console.log('Granted:', status.grantedPermissions);
console.log('Denied:', status.deniedPermissions);
console.log('Android version:', status.androidVersion);
```

---

### Connection API

#### `connection.connect(address: string, port?: number): Promise<boolean>`

Connect to a printer via Bluetooth.

**Parameters:**

- `address`: MAC address of the printer (e.g., `"00:11:22:AA:BB:CC"`)
- `port`: TCP port (default: `9100`)

```typescript
await Printer.connection.connect('00:11:22:AA:BB:CC');
```

#### `connection.disconnect(): Promise<boolean>`

Disconnect from the current printer.

```typescript
await Printer.connection.disconnect();
```

#### `connection.getStatus(): Promise<ConnectionInfo>`

Get current connection status and details.

```typescript
const info = await Printer.connection.getStatus();
console.log('Address:', info.address);
console.log('Type:', info.type); // 'BLUETOOTH' | 'WIFI' | 'USB'
console.log('Status:', info.status); // 'CONNECTED' | 'CONNECTING' | 'DISCONNECTED' | 'ERROR'
```

#### `connection.isConnected(): boolean`

Check if currently connected (synchronous).

```typescript
const connected = Printer.connection.isConnected();
```

---

### Print API

#### `print.receipt(receipt: Receipt): Promise<void>`

Print a complete receipt with header, details, and footer sections.

**Parameters:**

- `receipt`: Receipt object with sections and configuration

```typescript
await Printer.print.receipt({
  header: [
    { type: 'text', content: 'STORE NAME', fontSize: 'large', alignment: 'center', bold: true },
    { type: 'text', content: '123 Main St', alignment: 'center' },
    { type: 'separator' },
  ],
  details: [
    { type: 'keyValue', key: 'Product A', value: '$10.00' },
    { type: 'keyValue', key: 'Product B', value: '$15.00' },
    { type: 'keyValue', key: 'Tax (10%)', value: '$2.50' },
  ],
  footer: [
    { type: 'separator' },
    { type: 'keyValue', key: 'TOTAL', value: '$27.50', bold: true, fontSize: 'large' },
    { type: 'qrCode', data: 'https://mystore.com/receipt/12345', size: 5, alignment: 'center' },
    { type: 'space', lines: 2 },
  ],
  copies: 1,
});
```

#### `print.lines(lines: ReceiptLine[]): Promise<void>`

Print a list of receipt lines without sections.

```typescript
await Printer.print.lines([
  { type: 'text', content: 'Quick Receipt', alignment: 'center' },
  { type: 'separator' },
  { type: 'keyValue', key: 'Item', value: '$5.00' },
  { type: 'space', lines: 1 },
]);
```

#### `print.qrCode(data: string, size?: number): Promise<void>`

Print a standalone QR code.

**Parameters:**

- `data`: QR code content (URL, text, etc.)
- `size`: QR code size 1-10 (default: `5`)

```typescript
await Printer.print.qrCode('https://example.com', 7);
```

---

## 🎨 Receipt Line Types

### TextLine

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

### KeyValueLine

Print key-value pairs (common in receipts).

```typescript
{
  type: 'keyValue',
  key: string,
  value: string,
  fontSize?: FontSize,
  bold?: boolean
}
```

**Example:**

```typescript
{ type: 'keyValue', key: 'Subtotal', value: '$25.00' }
{ type: 'keyValue', key: 'TOTAL', value: '$27.50', bold: true }
```

### QRCodeLine

Embed QR codes in receipts.

```typescript
{
  type: 'qrCode',
  data: string,
  size?: number,       // 1-10, default: 5
  alignment?: Alignment
}
```

**Example:**

```typescript
{ type: 'qrCode', data: 'https://store.com/receipt/12345', size: 6, alignment: 'center' }
```

### SeparatorLine

Print horizontal separator lines.

```typescript
{
  type: 'separator',
  char?: string,  // Character to repeat, default: '-'
  length?: number // Line length, default: 48
}
```

**Example:**

```typescript
{ type: 'separator' }
{ type: 'separator', char: '=', length: 32 }
```

### SpaceLine

Add blank lines for spacing.

```typescript
{
  type: 'space',
  lines?: number  // Number of blank lines, default: 1
}
```

**Example:**

```typescript
{ type: 'space', lines: 2 }
```

---

## 📦 TypeScript Types

### BluetoothDevice

```typescript
interface BluetoothDevice {
  name: string;
  address: string;
  type: 'CLASSIC' | 'LE' | 'DUAL' | 'UNKNOWN';
  isPrinter: boolean;
}
```

### PermissionStatus

```typescript
interface PermissionStatus {
  allGranted: boolean;
  grantedPermissions: string[];
  deniedPermissions: string[];
  androidVersion: number;
}
```

### ConnectionInfo

```typescript
interface ConnectionInfo {
  address: string;
  port: number;
  type: 'BLUETOOTH' | 'WIFI' | 'USB' | 'UNKNOWN';
  status: 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'ERROR';
}
```

### Receipt

```typescript
interface Receipt {
  header?: ReceiptLine[];
  details?: ReceiptLine[];
  footer?: ReceiptLine[];
  mediaConfig?: MediaConfig;
  copies?: number;
}
```

### MediaConfig

```typescript
interface MediaConfig {
  type: 'continuous' | 'labelGap' | 'labelBlackMark';
  width: number;
  height: number;
  offset?: number;
  gap?: number;
}
```

---

## 🎯 Advanced Examples

### Complete Receipt with All Line Types

```typescript
const fullReceipt: Receipt = {
  header: [
    {
      type: 'text',
      content: '🏪 MY RETAIL STORE',
      fontSize: 'xlarge',
      alignment: 'center',
      bold: true,
    },
    { type: 'text', content: '123 Commerce Street', alignment: 'center' },
    { type: 'text', content: 'Phone: (555) 123-4567', alignment: 'center' },
    { type: 'separator', char: '=' },
    { type: 'text', content: 'SALES RECEIPT', fontSize: 'large', alignment: 'center' },
    { type: 'separator', char: '=' },
    { type: 'space' },
  ],
  details: [
    { type: 'keyValue', key: 'Date', value: new Date().toLocaleDateString() },
    { type: 'keyValue', key: 'Receipt #', value: '00123' },
    { type: 'keyValue', key: 'Cashier', value: 'John Doe' },
    { type: 'space' },
    { type: 'separator' },
    { type: 'text', content: 'ITEMS', bold: true },
    { type: 'separator' },
    { type: 'keyValue', key: 'Coffee (x2)', value: '$9.00' },
    { type: 'keyValue', key: 'Croissant (x1)', value: '$3.50' },
    { type: 'keyValue', key: 'Orange Juice (x1)', value: '$4.00' },
    { type: 'space' },
    { type: 'separator' },
    { type: 'keyValue', key: 'Subtotal', value: '$16.50' },
    { type: 'keyValue', key: 'Tax (8%)', value: '$1.32' },
  ],
  footer: [
    { type: 'separator', char: '=' },
    { type: 'keyValue', key: 'TOTAL', value: '$17.82', fontSize: 'large', bold: true },
    { type: 'separator', char: '=' },
    { type: 'space', lines: 2 },
    { type: 'text', content: 'Thank you for your purchase!', alignment: 'center' },
    { type: 'text', content: 'Visit us at www.mystore.com', alignment: 'center' },
    { type: 'space' },
    { type: 'qrCode', data: 'https://mystore.com/receipt/00123', size: 6, alignment: 'center' },
    { type: 'space', lines: 3 },
  ],
  copies: 1,
};

await Printer.print.receipt(fullReceipt);
```

### Error Handling Pattern

```typescript
const safePrint = async (receipt: Receipt) => {
  try {
    // Check connection
    if (!Printer.connection.isConnected()) {
      throw new Error('Printer not connected');
    }

    // Check status
    const status = await Printer.connection.getStatus();
    if (status.status !== 'CONNECTED') {
      throw new Error(`Connection ${status.status.toLowerCase()}`);
    }

    // Print
    await Printer.print.receipt(receipt);
    console.log('✅ Print successful');
  } catch (error) {
    console.error('❌ Print failed:', error.message);

    // Show user-friendly message
    Alert.alert('Print Error', error.message || 'Could not print receipt', [{ text: 'OK' }]);
  }
};
```

### Custom Hook for Printer Management

```typescript
import { useState, useEffect } from 'react';
import Printer, { BluetoothDevice, ConnectionInfo } from '@sincpro/printer-expo';

export function usePrinter() {
  const [devices, setDevices] = useState<BluetoothDevice[]>([]);
  const [connected, setConnected] = useState(false);
  const [connectionInfo, setConnectionInfo] = useState<ConnectionInfo | null>(null);

  useEffect(() => {
    checkConnection();
  }, []);

  const checkConnection = () => {
    const isConnected = Printer.connection.isConnected();
    setConnected(isConnected);
  };

  const scanDevices = async () => {
    try {
      const isEnabled = await Printer.bluetooth.isEnabled();
      if (!isEnabled) {
        throw new Error('Bluetooth is disabled');
      }

      const foundDevices = await Printer.bluetooth.getPairedDevices();
      setDevices(foundDevices.filter((d) => d.isPrinter));

      return foundDevices;
    } catch (error) {
      console.error('Scan failed:', error);
      throw error;
    }
  };

  const connect = async (address: string) => {
    try {
      await Printer.connection.connect(address);
      setConnected(true);

      const info = await Printer.connection.getStatus();
      setConnectionInfo(info);

      return true;
    } catch (error) {
      console.error('Connection failed:', error);
      throw error;
    }
  };

  const disconnect = async () => {
    try {
      await Printer.connection.disconnect();
      setConnected(false);
      setConnectionInfo(null);
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

    await Printer.print.receipt(receipt);
  };

  return {
    devices,
    connected,
    connectionInfo,
    scanDevices,
    connect,
    disconnect,
    printReceipt,
  };
}

// Usage
function MyComponent() {
  const printer = usePrinter();

  return (
    <View>
      <Button title="Scan" onPress={printer.scanDevices} />
      <Button
        title="Print"
        onPress={() => printer.printReceipt(myReceipt)}
        disabled={!printer.connected}
      />
    </View>
  );
}
```

---

## 🏗️ Architecture

This module follows **Clean Architecture** with **Hexagonal Architecture** (Ports & Adapters) principles:

```
TypeScript (React Native)
         ↓
   ENTRYPOINT - Expo Modules API bridge
         ↓
   SERVICE - Use cases & orchestration
         ↓
   DOMAIN - Business entities & rules
         ↓
   ADAPTER - Printer implementations (Bixolon, Zebra, etc.)
         ↓
INFRASTRUCTURE - Android APIs & SDKs
```

**Benefits:**

- ✅ **Testable**: Mock adapters and services
- ✅ **Maintainable**: Clear separation of concerns
- ✅ **Extensible**: Easy to add new printer brands
- ✅ **Swappable**: Change implementations without affecting business logic

See [ARCHITECTURE.md](ARCHITECTURE.md) for details.

---

## 🛠️ Troubleshooting

### Bluetooth Permissions

**Problem**: "Permission denied" errors

**Solution**: Check permission status and request if needed

```typescript
const status = Printer.permission.getStatus();
if (!status.allGranted) {
  console.log('Missing:', status.deniedPermissions);
  // Guide user to app settings to grant permissions
}
```

### Connection Issues

**Problem**: Connection fails or times out

**Solutions**:

1. Verify Bluetooth is enabled: `await Printer.bluetooth.isEnabled()`
2. Check device is paired: `await Printer.bluetooth.getPairedDevices()`
3. Ensure printer is powered on and in range
4. Try reconnecting: `await Printer.connection.connect(address)`

### Print Failures

**Problem**: Print command succeeds but nothing prints

**Solutions**:

1. Check printer status (paper, errors)
2. Verify connection: `Printer.connection.isConnected()`
3. Try smaller test print first
4. Check printer-specific requirements (media config)

---

## 📖 Resources

- **Architecture**: [ARCHITECTURE.md](ARCHITECTURE.md) - Detailed architecture guide
- **Contributing**: [CONTRIBUTING.md](CONTRIBUTING.md) - Development guidelines
- **Copilot**: [.github/copilot-instructions.md](.github/copilot-instructions.md) - AI coding assistant rules
- **Expo Modules**: [Official Documentation](https://docs.expo.dev/modules/overview/)
- **Bixolon SDK**: [Official Documentation](https://www.bixolon.com/)

---

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for:

- Development setup
- Code standards (ktlint, Prettier)
- Architecture guidelines
- Git workflow
- Pull request process

---

## 📄 License

MIT License - see [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- Bixolon for the official printer SDK
- Expo team for the Modules API
- Contributors and testers

---

**Made with ❤️ by Sincpro SRL**
