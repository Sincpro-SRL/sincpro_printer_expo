/**
 * @sincpro/printer-expo
 *
 * Expo module for Bluetooth/WiFi/USB thermal printers.
 * Supports Bixolon label printers.
 *
 * @packageDocumentation
 */

// Main module export
export { default } from './SincproPrinter';
export { bluetooth, connection, print } from './SincproPrinter';

// Type exports
export * from './types';
