import { NativeEventEmitter } from 'react-native';
import ExpoBixolonModule from './ExpoBixolonModule';
import type { BixolonPrinterInterface } from './ExpoBixolon.types';

const eventEmitter = new NativeEventEmitter(ExpoBixolonModule as any);

if (!eventEmitter.addListener) {
  (eventEmitter as any).addListener = () => ({ remove: () => {} });
}
if (!eventEmitter.removeAllListeners) {
  (eventEmitter as any).removeAllListeners = () => {};
}

export const BixolonPrinter: BixolonPrinterInterface = ExpoBixolonModule;

export default BixolonPrinter;
