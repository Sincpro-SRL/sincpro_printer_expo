/**
 * @sincpro/printer-expo
 *
 * Expo module for Bluetooth thermal printers.
 * Bridge for multiple printer brands (Bixolon, Zebra, Epson, etc.)
 *
 * @packageDocumentation
 */

// Main module export
export { default } from './PrinterModule';
export { bluetooth, permission, connection, print, events } from './PrinterModule';

// Type exports
export * from './types';
