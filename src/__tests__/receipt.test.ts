import type { ImageCommand, QrCommand, TextCommand } from '../domain/print_job';
import { isPrinterError } from '../domain/printer';
import { layoutLines, layoutReceipt, wrapText } from '../domain/receipt';

const WIDTH_72MM = 576;

function pngBase64(width: number, height: number): string {
  const bytes = Buffer.alloc(33);
  Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]).copy(bytes, 0);
  bytes.writeUInt32BE(width, 16);
  bytes.writeUInt32BE(height, 20);
  return bytes.toString('base64');
}

describe('wrapText', () => {
  it('wraps on word boundaries', () => {
    expect(wrapText('Coca Cola 2L retornable', 10)).toEqual(['Coca Cola', '2L', 'retornable']);
  });

  it('splits words longer than the line', () => {
    expect(wrapText('ABCDEFGHIJKL', 5)).toEqual(['ABCDE', 'FGHIJ', 'KL']);
  });

  it('keeps explicit line breaks', () => {
    expect(wrapText('a\nb', 10)).toEqual(['a', 'b']);
  });
});

describe('layoutLines', () => {
  it('keeps the 1.x 30-dot row for small and medium fonts and grows for taller fonts', () => {
    const commands = layoutLines(
      [
        { type: 'text', content: 'TITLE', fontSize: 'xlarge' },
        { type: 'text', content: 'body', fontSize: 'medium' },
      ],
      WIDTH_72MM
    ) as TextCommand[];

    expect(commands[0].y).toBe(20);
    expect(commands[1].y).toBe(20 + 76);
    expect(commands[0].font).toBe(54);
    expect(commands[1].font).toBe(51);
  });

  it('centers and right-aligns text by its character width', () => {
    const [center, right] = layoutLines(
      [
        { type: 'text', content: 'ABCD', alignment: 'center' },
        { type: 'text', content: 'ABCD', alignment: 'right' },
      ],
      WIDTH_72MM
    ) as TextCommand[];

    expect(center.x).toBe(Math.floor((576 - 4 * 19) / 2));
    expect(right.x).toBe(576 - 4 * 19 - 10);
  });

  it('wraps long text into several rows instead of cutting it', () => {
    const commands = layoutLines(
      [{ type: 'text', content: 'x '.repeat(40).trim(), fontSize: 'medium' }],
      WIDTH_72MM
    );
    expect(commands.length).toBeGreaterThan(1);
  });

  it('justifies key and value on one row', () => {
    const [row] = layoutLines(
      [{ type: 'keyValue', key: 'Total', value: 'Bs 50.00' }],
      WIDTH_72MM
    ) as TextCommand[];

    expect(row.text.startsWith('Total')).toBe(true);
    expect(row.text.endsWith('Bs 50.00')).toBe(true);
    expect(row.text.length).toBe(Math.floor((576 - 20) / 19));
  });

  it('pads columns to their width ratio', () => {
    const [row] = layoutLines(
      [
        {
          type: 'columns',
          fontSize: 'small',
          columns: [
            { text: 'Item', widthRatio: 0.5 },
            { text: '9.99', widthRatio: 0.5, alignment: 'right' },
          ],
        },
      ],
      WIDTH_72MM
    ) as TextCommand[];

    const half = Math.floor(Math.floor((576 - 20) / 12) * 0.5);
    expect(row.text).toBe('Item'.padEnd(half) + '9.99'.padStart(half));
  });

  it('fills the separator to the media width by default', () => {
    const [row] = layoutLines([{ type: 'separator' }], WIDTH_72MM) as TextCommand[];
    expect(row.text).toBe('-'.repeat(Math.floor((576 - 20) / 12)));
  });

  it('centers a QR code using its real symbol size', () => {
    const [qr] = layoutLines(
      [{ type: 'qr', data: 'https://sincpro.com.bo', size: 4 }],
      WIDTH_72MM
    ) as QrCommand[];
    // 22 bytes → QR version 2 → 25 modules.
    const side = 25 * 4;
    expect(qr.height).toBe(side);
    expect(qr.x).toBe(Math.floor((576 - side) / 2));
  });

  it('scales wide images down to the media width and places the next line below', () => {
    const commands = layoutLines(
      [
        { type: 'image', base64: pngBase64(1152, 400) },
        { type: 'text', content: 'after' },
      ],
      WIDTH_72MM
    );
    const image = commands[0] as ImageCommand;
    const text = commands[1] as TextCommand;

    expect(image.width).toBe(576);
    expect(image.height).toBe(200);
    expect(image.x).toBe(0);
    expect(text.y).toBe(20 + 200 + 10);
  });

  it('rejects an unreadable image when more lines follow it', () => {
    const gif = Buffer.from('GIF89a......').toString('base64');
    expect.assertions(1);
    try {
      layoutLines(
        [
          { type: 'image', base64: gif },
          { type: 'text', content: 'after' },
        ],
        WIDTH_72MM
      );
    } catch (error) {
      expect(isPrinterError(error) && error.code === 'INVALID_ARGUMENT').toBe(true);
    }
  });
});

describe('layoutReceipt', () => {
  it('prints header, body and footer in order', () => {
    const commands = layoutReceipt(
      {
        header: [{ type: 'text', content: 'H' }],
        body: [{ type: 'text', content: 'B' }],
        footer: [{ type: 'text', content: 'F' }],
      },
      WIDTH_72MM
    ) as TextCommand[];

    expect(commands.map((c) => c.text)).toEqual(['H', 'B', 'F']);
  });
});
