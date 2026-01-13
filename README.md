# Expo Bixolon Module

[![npm version](https://badge.fury.io/js/expo-bixolon.svg)](https://badge.fury.io/js/expo-bixolon)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Expo](https://img.shields.io/badge/Expo-000000.svg?style=flat&logo=expo&logoColor=white)](https://expo.dev)

A powerful React Native module for controlling Bixolon thermal printers in Expo applications. This module provides comprehensive Bluetooth connectivity, advanced printing functionality, and seamless integration with Bixolon's official SDK.

## ✨ Features

- 🔗 **Bluetooth Connectivity**: Full Bluetooth device discovery and connection management
- 📱 **Permission Management**: Automatic handling of Bluetooth and location permissions
- 🖨️ **Advanced Printing**: Text, invoices, and formatted document printing
- 📋 **QR Code Printing**: Multiple QR code formats (URL, Contact, WiFi, Payment)
- 🔌 **Multiple Interfaces**: Support for Bluetooth, WiFi, and USB connections
- 📚 **Official SDK**: Integration with BixolonLabelPrinter library
- 📝 **TypeScript**: Complete TypeScript definitions and type safety
- 🔄 **Event-Driven**: Real-time events for device discovery and connection status
- 🛠️ **Easy Setup**: Simple installation and configuration
- 📱 **Cross-Platform**: Works on both Android and iOS (Android focus)

## 📦 Installation

### Method 1: Install from NPM (when published)

```bash
npm install expo-bixolon
# or
yarn add expo-bixolon
```

### Method 2: Install from Local Tarball

If you have the module locally or want to use a specific version:

```bash
# First, create a tarball from the module directory
cd /path/to/expo-bixolon
npm pack

# Then install in your app
cd /path/to/your-app
npm install /path/to/expo-bixolon-0.1.0.tgz
```

### Method 3: Install as Local Development Dependency

For development or testing purposes:

```bash
# In your app directory
npm install /absolute/path/to/expo-bixolon
```

### Post-Installation Setup

After installation, run these commands in your app directory:

```bash
# Install native dependencies
npx expo install

# Generate native code
npx expo prebuild

# For development builds
npx expo run:android
# or
npx expo run:ios
```

## ⚙️ Configuration

### 1. Add Plugin to app.json

```json
{
  "expo": {
    "plugins": [
      [
        "expo-bixolon",
        {
          "bluetoothPermissions": [
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.BLUETOOTH_SCAN",
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION"
          ]
        }
      ]
    ]
  }
}
```

### 2. Android Permissions (app.json)

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

## 🚀 Quick Start

### Basic Setup

```typescript
import BixolonPrinter, { BluetoothDevice } from 'expo-bixolon';

// Initialize the printer
const initializePrinter = async () => {
  try {
    const success = await BixolonPrinter.initializePrinter();
    if (success) {
      console.log('✅ Printer initialized successfully');
    }
  } catch (error) {
    console.error('❌ Error initializing printer:', error);
  }
};
```

### Complete Example

```typescript
import React, { useState, useEffect } from 'react';
import { View, Button, Alert } from 'react-native';
import BixolonPrinter, { BluetoothDevice } from 'expo-bixolon';

export default function PrinterApp() {
  const [devices, setDevices] = useState<BluetoothDevice[]>([]);
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    initializePrinter();
    checkPermissions();
  }, []);

  const initializePrinter = async () => {
    try {
      await BixolonPrinter.initializePrinter();
      console.log('Printer initialized');
    } catch (error) {
      console.error('Initialization failed:', error);
    }
  };

  const checkPermissions = async () => {
    const permissions = await BixolonPrinter.checkBluetoothPermissions();
    console.log('Permissions:', permissions);
  };

  const discoverDevices = async () => {
    try {
      const foundDevices = await BixolonPrinter.discoverBluetoothDevices();
      setDevices(foundDevices);
    } catch (error) {
      Alert.alert('Error', 'Failed to discover devices');
    }
  };

  const connectToDevice = async (device: BluetoothDevice) => {
    try {
      const success = await BixolonPrinter.connectPrinter('BLUETOOTH', device.address, 1);
      if (success) {
        setIsConnected(true);
        Alert.alert('Success', 'Connected to printer');
      }
    } catch (error) {
      Alert.alert('Error', 'Connection failed');
    }
  };

  const printTest = async () => {
    try {
      const success = await BixolonPrinter.testPlainText('Hello from Expo!');
      if (success) {
        Alert.alert('Success', 'Text printed successfully');
      }
    } catch (error) {
      Alert.alert('Error', 'Print failed');
    }
  };

  return (
    <View style={{ padding: 20 }}>
      <Button title="Discover Devices" onPress={discoverDevices} />
      <Button title="Print Test" onPress={printTest} disabled={!isConnected} />
      {/* Render device list */}
    </View>
  );
}
```

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
```

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

### Core Methods

#### `initializePrinter(): Promise<boolean>`

Initializes the printer module. Must be called before any other operations.

**Returns:** `Promise<boolean>` - `true` if initialization successful

**Example:**

```typescript
const success = await BixolonPrinter.initializePrinter();
```

#### `connectPrinter(interfaceType: string, address: string, port: number): Promise<boolean>`

Connects to a printer using the specified interface type, address, and port.

**Parameters:**

- `interfaceType`: `'BLUETOOTH' | 'WIFI' | 'USB'`
- `address`: Device MAC address (Bluetooth) or IP address (WiFi)
- `port`: Port number (usually 1 for Bluetooth, 9100 for WiFi)

**Returns:** `Promise<boolean>` - `true` if connection successful

**Example:**

```typescript
const success = await BixolonPrinter.connectPrinter('BLUETOOTH', '00:11:22:33:44:55', 1);
```

#### `disconnectPrinter(): Promise<boolean>`

Disconnects from the currently connected printer.

**Returns:** `Promise<boolean>` - `true` if disconnection successful

#### `executeCommand(command: string): Promise<boolean>`

Executes a raw command on the printer.

**Parameters:**

- `command`: Raw command string to send to printer

### Printing Methods

#### `testPlainText(text: string): Promise<boolean>`

Prints plain text to the connected printer.

**Parameters:**

- `text`: Text content to print

#### `printFormattedText(text: string, fontSize?: number): Promise<boolean>`

Prints formatted text with optional font size.

**Parameters:**

- `text`: Formatted text content
- `fontSize`: Optional font size (default: 10)

#### `printTextSimple(text: string): Promise<boolean>`

Prints text using simple formatting (line-by-line).

#### `printTextInPages(text: string): Promise<boolean>`

Prints long text split into multiple pages.

#### `printInvoice(invoiceText: string): Promise<boolean>`

Prints a complete invoice from formatted text.

**Parameters:**

- `invoiceText`: Complete invoice text with formatting

### Bluetooth Methods

#### `requestBluetoothPermissions(): Promise<boolean>`

Requests necessary Bluetooth and location permissions.

**Returns:** `Promise<boolean>` - `true` if permissions granted

#### `checkBluetoothPermissions(): Promise<BluetoothPermissions>`

Returns the current status of Bluetooth and location permissions.

**Returns:** `Promise<BluetoothPermissions>` - Object with permission status

#### `discoverBluetoothDevices(): Promise<BluetoothDevice[]>`

Discovers available Bluetooth devices.

**Returns:** `Promise<BluetoothDevice[]>` - Array of discovered devices

#### `startBluetoothDiscovery(): Promise<boolean>`

Starts the Bluetooth device discovery process.

#### `stopBluetoothDiscovery(): Promise<boolean>`

Stops the Bluetooth device discovery process.

#### `isBluetoothEnabled(): Promise<boolean>`

Checks if Bluetooth is enabled on the device.

## Types

### BluetoothDevice

```typescript
interface BluetoothDevice {
  name: string;
  address: string;
  type: 'CLASSIC' | 'LE' | 'DUAL' | 'UNKNOWN';
  isPrinter: boolean;
}
```

### BluetoothPermissions

```typescript
interface BluetoothPermissions {
  ACCESS_FINE_LOCATION: boolean;
  ACCESS_COARSE_LOCATION: boolean;
  BLUETOOTH_SCAN?: boolean;
  BLUETOOTH_CONNECT?: boolean;
}
```

### InvoiceItem

```typescript
interface InvoiceItem {
  description: string;
  quantity: number;
  price: number;
}
```

## QR Code Printing

The module now includes comprehensive QR code printing functionality using the official Bixolon libraries.

### Basic QR Code Printing

```typescript
import QrCodePrinter from 'expo-bixolon/src/QrCodePrinter';

// Get the QR printer instance
const qrPrinter = QrCodePrinter.getInstance();

// Print a simple QR code
await qrPrinter.printSimpleQRCode('Hello World!', 9);
```

### Advanced QR Code Options

```typescript
import { QRCodeOptions } from 'expo-bixolon/src/QrCodePrinter';

const options: QRCodeOptions = {
  data: 'https://www.example.com',
  horizontalPosition: 200,
  verticalPosition: 100,
  model: 'MODEL2',
  eccLevel: 'ECC_LEVEL_15',
  size: 9,
  rotation: 'NONE',
};

await qrPrinter.printQRCode(options);
```

### Specialized QR Code Types

#### URL QR Code

```typescript
await qrPrinter.printURLQRCode('www.example.com', 9);
```

#### Contact QR Code (vCard)

```typescript
const contact = {
  name: 'John Doe',
  phone: '+1234567890',
  email: 'john.doe@example.com',
  company: 'Example Corp',
  title: 'Software Engineer',
};

await qrPrinter.printContactQRCode(contact, 9);
```

#### WiFi QR Code

```typescript
await qrPrinter.printWiFiQRCode('MyWiFiNetwork', 'password123', 'WPA', 9);
```

#### Payment QR Code

```typescript
const paymentData = {
  amount: 99.99,
  currency: 'USD',
  description: 'Product purchase',
  merchantName: 'Example Store',
};

await qrPrinter.printPaymentQRCode(paymentData, 9);
```

### QR Code Configuration

- **Models**: MODEL1 (original) or MODEL2 (enhanced, recommended)
- **Error Correction**: ECC_LEVEL_7, ECC_LEVEL_15, ECC_LEVEL_25, ECC_LEVEL_30
- **Size**: 1-10 (recommended: 7-9)
- **Rotation**: NONE, ROTATION_90_DEGREES, ROTATION_180_DEGREES, ROTATION_270_DEGREES

For detailed QR code printing documentation, see [QR_PRINTING_README.md](./QR_PRINTING_README.md).

## Events

The module emits the following events:

- `onBluetoothDeviceDiscovered`: Fired when a new Bluetooth device is discovered
- `onBluetoothDiscoveryStarted`: Fired when Bluetooth discovery starts
- `onBluetoothDiscoveryStopped`: Fired when Bluetooth discovery stops
- `onPrinterConnected`: Fired when successfully connected to a printer
- `onPrinterDisconnected`: Fired when disconnected from a printer
- `onPrintComplete`: Fired when a print operation completes

## Example

See the `example` directory for a complete working example that demonstrates:

- Bluetooth device discovery
- Permission management
- Printer connection
- Text and invoice printing
- Event handling

## 🔧 Requirements

- **React Native**: 0.79+
- **Expo SDK**: 53+
- **Android**: API level 21+ (Android 5.0+)
- **iOS**: 13+ (limited support)
- **Node.js**: 18+
- **TypeScript**: 4.9+ (optional but recommended)

## 🖨️ Supported Printers

### Bixolon Thermal Printers

- **SPP-L310** ✅ (Primary target)
- **SPP-R200III** ✅
- **SPP-R300** ✅
- **SPP-R400** ✅
- **SPP-L410** ✅
- **SPP-L420** ✅

### Connection Types

- **Bluetooth** ✅ (Primary)
- **WiFi** ✅ (Limited support)
- **USB** ⚠️ (Experimental)

## 🐛 Troubleshooting

### Common Issues

#### 1. Bluetooth Permissions Not Granted

```typescript
// Always check permissions first
const permissions = await BixolonPrinter.checkBluetoothPermissions();
console.log('Permissions:', permissions);

// Request if needed
if (!permissions.ACCESS_FINE_LOCATION) {
  await BixolonPrinter.requestBluetoothPermissions();
}
```

#### 2. Device Not Found

- Ensure Bluetooth is enabled on both devices
- Make sure the printer is in pairing mode
- Check if the device is already paired in system settings
- Try restarting Bluetooth discovery

#### 3. Connection Fails

- Verify the device MAC address is correct
- Ensure the printer is compatible with the module
- Check if another app is connected to the printer
- Try disconnecting and reconnecting

#### 4. Print Jobs Not Completing

- Check if the printer has paper
- Verify the printer is not in error state
- Try printing a simple test first
- Check printer logs for error messages

#### 5. Module Not Found Error

```bash
# Make sure to run prebuild after installation
npx expo prebuild

# Clear cache if needed
npx expo start --clear
```

### Debug Mode

Enable debug logging for detailed information:

```bash
# Set environment variable
export EXPO_DEBUG=true

# Or in your app
process.env.EXPO_DEBUG = 'true';
```

### Performance Tips

1. **Initialize once**: Call `initializePrinter()` only once per app session
2. **Reuse connections**: Keep connections alive when possible
3. **Batch operations**: Group multiple print operations together
4. **Error handling**: Always wrap print operations in try-catch blocks

### Getting Help

If you encounter issues:

1. Check the [example app](./example) for working code
2. Enable debug mode and check logs
3. Verify your printer model is supported
4. Test with a simple print operation first

## 🤝 Contributing

We welcome contributions! Here's how you can help:

### Development Setup

```bash
# Clone the repository
git clone https://github.com/your-username/expo-bixolon.git
cd expo-bixolon

# Install dependencies
npm install

# Build the module
npm run build

# Run the example app
cd example
npm install
npx expo run:android
```

### Making Changes

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Make your changes** and test thoroughly
4. **Add tests** if applicable
5. **Update documentation** as needed
6. **Submit a pull request**

### Publishing to NPM

El paquete se publica automáticamente a NPM cuando se crea un release en GitHub:

1. **Update version**: `make update-version VERSION=x.y.z`
2. **Commit**: `git add . && git commit -m "chore: bump version to x.y.z"`
3. **Push**: `git push origin main`
4. **Create GitHub Release**: El CI/CD publicará automáticamente a NPM
   - Requiere secret `NPM_TOKEN` configurado en GitHub

**Comandos de desarrollo**:

- `make build` - Construye el módulo
- `make test` - Ejecuta tests
- `make format` - Formatea código
- `make verify-format` - Verifica formato (usado en CI)
- `make publish-dry-run` - Simula publicación
- `make publish` - Publica manualmente (requiere `npm login`)

### Code Style

- Use TypeScript for all new code
- Follow existing code patterns
- Add proper error handling
- Include JSDoc comments for public APIs
- Test on real Bixolon printers when possible

## 📄 License

MIT License - see [LICENSE](./LICENSE) file for details.

## 🆘 Support

### Getting Help

- 📖 **Documentation**: Check this README and the example app
- 🐛 **Bug Reports**: Open an issue with detailed information
- 💡 **Feature Requests**: Open an issue with use case description
- 💬 **Discussions**: Use GitHub Discussions for questions

### Issue Template

When reporting issues, please include:

```markdown
**Printer Model**: SPP-L310
**Android Version**: 12
**Expo SDK Version**: 53
**Steps to Reproduce**:

1. Initialize printer
2. Connect to device
3. Print text
   **Expected Behavior**: Text should print
   **Actual Behavior**: Connection fails
   **Logs**: [Include relevant logs]
```

## 🎯 Roadmap

### Planned Features

- [ ] **Enhanced WiFi Support**: Full WiFi printing capabilities
- [ ] **Image Printing**: Support for printing images and logos
- [ ] **Barcode Printing**: Various barcode formats
- [ ] **iOS Support**: Full iOS implementation
- [ ] **Web Support**: Web-based printing interface
- [ ] **Cloud Printing**: Remote printing capabilities

### Recent Updates

- ✅ **v0.1.0**: Initial release with Bluetooth support
- ✅ **QR Code Printing**: Advanced QR code functionality
- ✅ **TypeScript Support**: Complete type definitions
- ✅ **Local Installation**: Support for local package distribution

---

**Made with ❤️ for the Expo and Bixolon communities**
