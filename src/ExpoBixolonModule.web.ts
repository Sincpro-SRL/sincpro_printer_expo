import { registerWebModule, NativeModule } from 'expo';

import { ExpoBixolonModuleEvents } from './ExpoBixolon.types';

class ExpoBixolonModule extends NativeModule<ExpoBixolonModuleEvents> {
  PI = Math.PI;
  async setValueAsync(value: string): Promise<void> {
    this.emit('onChange', { value });
  }
  hello() {
    return 'Hello world! 👋';
  }
}

export default registerWebModule(ExpoBixolonModule, 'ExpoBixolonModule');
