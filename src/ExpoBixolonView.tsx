import { requireNativeView } from 'expo';
import * as React from 'react';

import { ExpoBixolonViewProps } from './ExpoBixolon.types';

const NativeView: React.ComponentType<ExpoBixolonViewProps> =
  requireNativeView('ExpoBixolon');

export default function ExpoBixolonView(props: ExpoBixolonViewProps) {
  return <NativeView {...props} />;
}
