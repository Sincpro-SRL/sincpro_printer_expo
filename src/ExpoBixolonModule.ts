import { NativeModule, requireNativeModule } from 'expo';

import { ExpoBixolonModuleEvents } from './ExpoBixolon.types';

declare class ExpoBixolonModule extends NativeModule<ExpoBixolonModuleEvents> {
  PI: number;
  hello(): string;
  setValueAsync(value: string): Promise<void>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<ExpoBixolonModule>('ExpoBixolon');
