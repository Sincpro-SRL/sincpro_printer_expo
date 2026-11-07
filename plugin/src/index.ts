import { createRunOncePlugin, withInfoPlist, type ConfigPlugin } from 'expo/config-plugins';

const pkg: { name: string; version: string } = require('../../package.json');

const EXTERNAL_ACCESSORY_PROTOCOL = 'com.bixolon.protocol';

export interface SincproPrinterPluginOptions {
  /** iOS: allow MFi (Bluetooth classic) Bixolon printers through External Accessory. Default true. */
  bluetoothClassic?: boolean;
  bluetoothUsageDescription?: string;
  /** iOS: WiFi printers on the local network. Default false. */
  localNetwork?: boolean;
  localNetworkUsageDescription?: string;
}

/**
 * Context: Android permissions ship in the module manifest; iOS needs Info.plist keys that only
 * the app can declare (Bluetooth usage, the Bixolon External Accessory protocol, local network).
 */
const withSincproPrinter: ConfigPlugin<SincproPrinterPluginOptions | void> = (config, options) => {
  const {
    bluetoothClassic = true,
    bluetoothUsageDescription = 'Bluetooth is used to connect to the printer.',
    localNetwork = false,
    localNetworkUsageDescription = 'The local network is used to connect to WiFi printers.',
  } = options ?? {};

  return withInfoPlist(config, (plistConfig) => {
    const plist = plistConfig.modResults;
    plist.NSBluetoothAlwaysUsageDescription ??= bluetoothUsageDescription;

    if (bluetoothClassic) {
      const protocols = new Set<string>(
        (plist.UISupportedExternalAccessoryProtocols as string[] | undefined) ?? []
      );
      protocols.add(EXTERNAL_ACCESSORY_PROTOCOL);
      plist.UISupportedExternalAccessoryProtocols = [...protocols];
    }

    if (localNetwork) {
      plist.NSLocalNetworkUsageDescription ??= localNetworkUsageDescription;
    }

    return plistConfig;
  });
};

export default createRunOncePlugin(withSincproPrinter, pkg.name, pkg.version);
