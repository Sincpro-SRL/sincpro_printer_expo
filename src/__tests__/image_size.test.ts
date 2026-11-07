import { readImageSize } from '../domain/image_size';

function pngHeader(width: number, height: number): string {
  const bytes = Buffer.alloc(33);
  Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]).copy(bytes, 0);
  bytes.writeUInt32BE(13, 8);
  bytes.write('IHDR', 12, 'ascii');
  bytes.writeUInt32BE(width, 16);
  bytes.writeUInt32BE(height, 20);
  return bytes.toString('base64');
}

function jpegHeader(width: number, height: number): string {
  const app0 = [0xff, 0xe0, 0x00, 0x10, ...new Array(14).fill(0)];
  const sof0 = [0xff, 0xc0, 0x00, 0x11, 0x08, height >> 8, height & 255, width >> 8, width & 255];
  return Buffer.from([0xff, 0xd8, ...app0, ...sof0, ...new Array(12).fill(0)]).toString('base64');
}

describe('readImageSize', () => {
  it('reads PNG dimensions', () => {
    expect(readImageSize(pngHeader(576, 1200))).toEqual({ width: 576, height: 1200 });
  });

  it('accepts a data URI prefix', () => {
    expect(readImageSize(`data:image/png;base64,${pngHeader(300, 100)}`)).toEqual({
      width: 300,
      height: 100,
    });
  });

  it('reads JPEG dimensions after other segments', () => {
    expect(readImageSize(jpegHeader(640, 480))).toEqual({ width: 640, height: 480 });
  });

  it('returns null for other formats', () => {
    expect(readImageSize(Buffer.from('GIF89a......').toString('base64'))).toBeNull();
  });
});
