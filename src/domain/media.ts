import type { NativeMedia, NativeMediaType } from './print_job';

export type MediaPreset = 'continuous58mm' | 'continuous72mm' | 'continuous80mm';

/** `label` is an alias of `gap` kept for 1.x compatibility. */
export type MediaType = 'continuous' | 'gap' | 'label' | 'black_mark';

export interface MediaConfig {
  preset?: MediaPreset;
  widthMm?: number;
  heightMm?: number;
  gapMm?: number;
  widthDots?: number;
  heightDots?: number;
  gapDots?: number;
  type?: MediaType;
}

export const DEFAULT_DPI = 203;

const PRESET_WIDTH_DOTS: Record<MediaPreset, number> = {
  continuous58mm: 464,
  continuous72mm: 576,
  continuous80mm: 640,
};

const DEFAULT_PRESET: MediaPreset = 'continuous80mm';

export function mmToDots(mm: number, dpi: number = DEFAULT_DPI): number {
  return Math.round((mm * dpi) / 25.4);
}

function toNativeType(type: MediaType | undefined): NativeMediaType {
  if (type === 'gap' || type === 'label') return 'gap';
  if (type === 'black_mark') return 'black_mark';
  return 'continuous';
}

export function resolveMedia(
  config: MediaConfig | undefined,
  dpi: number = DEFAULT_DPI
): NativeMedia {
  if (
    !config ||
    config.preset ||
    (config.widthDots === undefined && config.widthMm === undefined)
  ) {
    const preset = config?.preset ?? DEFAULT_PRESET;
    return { widthDots: PRESET_WIDTH_DOTS[preset], heightDots: 0, gapDots: 0, type: 'continuous' };
  }

  const type = toNativeType(config.type);
  const widthDots = config.widthDots ?? mmToDots(config.widthMm!, dpi);
  const heightDots =
    type === 'continuous'
      ? 0
      : (config.heightDots ?? (config.heightMm !== undefined ? mmToDots(config.heightMm, dpi) : 0));
  const gapDots = config.gapDots ?? (config.gapMm !== undefined ? mmToDots(config.gapMm, dpi) : 0);

  return { widthDots, heightDots, gapDots, type };
}
