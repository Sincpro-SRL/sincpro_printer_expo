/**
 * Event types for @sincpro/printer-expo
 */

/**
 * Device discovered event payload
 */
export interface DeviceDiscoveredEvent {
  name: string;
  address: string;
}

/**
 * Connection status change event payload
 */
export interface ConnectionChangedEvent {
  status: 'disconnected' | 'connecting' | 'connected' | 'error';
  address: string;
  error?: string;
}

/**
 * Print progress event payload
 */
export interface PrintProgressEvent {
  jobId: string;
  progress: number;
}

/**
 * Print completed event payload
 */
export interface PrintCompletedEvent {
  jobId: string;
}

/**
 * Print error event payload
 */
export interface PrintErrorEvent {
  jobId: string;
  error: string;
}

/**
 * All printer events
 */
export interface PrinterEvents {
  /**
   * Fired when a new Bluetooth device is discovered during scanning
   */
  onDeviceDiscovered: DeviceDiscoveredEvent;

  /**
   * Fired when printer connection status changes
   */
  onConnectionChanged: ConnectionChangedEvent;

  /**
   * Fired when print job progress updates
   */
  onPrintProgress: PrintProgressEvent;

  /**
   * Fired when print job completes successfully
   */
  onPrintCompleted: PrintCompletedEvent;

  /**
   * Fired when print job fails with an error
   */
  onPrintError: PrintErrorEvent;
}

/**
 * Event listener function type
 */
export type EventListener<T> = (event: T) => void;

/**
 * Event subscription
 */
export interface EventSubscription {
  /**
   * Remove this event listener
   */
  remove(): void;
}
