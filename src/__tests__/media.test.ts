import { mmToDots, resolveMedia } from '../domain/media';

describe('resolveMedia', () => {
  it('defaults to 80mm continuous paper', () => {
    expect(resolveMedia(undefined)).toEqual({
      widthDots: 640,
      heightDots: 0,
      gapDots: 0,
      type: 'continuous',
    });
  });

  it('uses the preset width in dots', () => {
    expect(resolveMedia({ preset: 'continuous72mm' }).widthDots).toBe(576);
    expect(resolveMedia({ preset: 'continuous58mm' }).widthDots).toBe(464);
  });

  it('converts millimetres with the printer dpi', () => {
    expect(mmToDots(50, 203)).toBe(400);
    expect(mmToDots(50, 300)).toBe(591);
  });

  it('treats "label" as gap media and keeps its height and gap', () => {
    expect(resolveMedia({ widthMm: 50, heightMm: 30, gapMm: 3, type: 'label' }, 203)).toEqual({
      widthDots: 400,
      heightDots: 240,
      gapDots: 24,
      type: 'gap',
    });
  });

  it('ignores height on continuous media so native sizes the page to the content', () => {
    expect(resolveMedia({ widthDots: 500, heightDots: 900 }).heightDots).toBe(0);
  });
});
