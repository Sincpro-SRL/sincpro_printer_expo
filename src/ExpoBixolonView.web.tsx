import * as React from 'react';

import { ExpoBixolonViewProps } from './ExpoBixolon.types';

export default function ExpoBixolonView(props: ExpoBixolonViewProps) {
  return (
    <div>
      <iframe
        style={{ flex: 1 }}
        src={props.url}
        onLoad={() => props.onLoad({ nativeEvent: { url: props.url } })}
      />
    </div>
  );
}
