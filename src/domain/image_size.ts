export interface ImageSize {
  width: number;
  height: number;
}

const BASE64_ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
const BASE64_INDEX = new Map(Array.from(BASE64_ALPHABET).map((char, index) => [char, index]));

// JPEG SOF markers can sit after a large EXIF block, so read up to 256 KB.
const MAX_HEADER_BYTES = 256 * 1024;

export function stripDataUri(base64: string): string {
  const comma = base64.indexOf(',');
  return base64.startsWith('data:') && comma !== -1 ? base64.slice(comma + 1) : base64;
}

function decodeBase64Prefix(base64: string, maxBytes: number): Uint8Array {
  const clean = stripDataUri(base64).replace(/[^A-Za-z0-9+/]/g, '');
  const chars = Math.min(clean.length, Math.ceil(maxBytes / 3) * 4);
  const bytes = new Uint8Array(Math.floor((chars * 3) / 4));
  let byteIndex = 0;
  for (let i = 0; i + 1 < chars; i += 4) {
    const a = BASE64_INDEX.get(clean[i]) ?? 0;
    const b = BASE64_INDEX.get(clean[i + 1]) ?? 0;
    const c = BASE64_INDEX.get(clean[i + 2]);
    const d = BASE64_INDEX.get(clean[i + 3]);
    bytes[byteIndex++] = (a << 2) | (b >> 4);
    if (c !== undefined) bytes[byteIndex++] = ((b & 15) << 4) | (c >> 2);
    if (c !== undefined && d !== undefined) bytes[byteIndex++] = ((c & 3) << 6) | d;
  }
  return bytes.subarray(0, byteIndex);
}

function readUint16(bytes: Uint8Array, offset: number): number {
  return (bytes[offset] << 8) | bytes[offset + 1];
}

function readUint32(bytes: Uint8Array, offset: number): number {
  return readUint16(bytes, offset) * 65536 + readUint16(bytes, offset + 2);
}

function pngSize(bytes: Uint8Array): ImageSize | null {
  const signature = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a];
  if (bytes.length < 24 || signature.some((value, i) => bytes[i] !== value)) return null;
  return { width: readUint32(bytes, 16), height: readUint32(bytes, 20) };
}

function isStartOfFrame(marker: number): boolean {
  return marker >= 0xc0 && marker <= 0xcf && marker !== 0xc4 && marker !== 0xc8 && marker !== 0xcc;
}

function jpegSize(bytes: Uint8Array): ImageSize | null {
  if (bytes.length < 4 || bytes[0] !== 0xff || bytes[1] !== 0xd8) return null;
  let offset = 2;
  while (offset + 9 < bytes.length) {
    if (bytes[offset] !== 0xff) return null;
    const marker = bytes[offset + 1];
    if (isStartOfFrame(marker)) {
      return { height: readUint16(bytes, offset + 5), width: readUint16(bytes, offset + 7) };
    }
    offset += 2 + readUint16(bytes, offset + 2);
  }
  return null;
}

/** Reads the pixel size from a PNG or JPEG header; null for other formats. */
export function readImageSize(base64: string): ImageSize | null {
  const bytes = decodeBase64Prefix(base64, MAX_HEADER_BYTES);
  return pngSize(bytes) ?? jpegSize(bytes);
}
