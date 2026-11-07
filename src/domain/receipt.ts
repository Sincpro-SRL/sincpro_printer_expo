import { readImageSize } from './image_size';
import {
  BARCODE_SYMBOLOGY,
  DEVICE_FONTS,
  barcodeWidthDots,
  qrSideDots,
  type BarcodeType,
  type FontSize,
} from './metrics';
import type { PrintCommand } from './print_job';
import { createPrinterError } from './printer';

export type Alignment = 'left' | 'center' | 'right';

export interface TextLine {
  type: 'text';
  content: string;
  fontSize?: FontSize;
  bold?: boolean;
  alignment?: Alignment;
}

export interface KeyValueLine {
  type: 'keyValue';
  key: string;
  value: string;
  fontSize?: FontSize;
  bold?: boolean;
}

export interface QRLine {
  type: 'qr';
  data: string;
  /** Module size 1..9 (default 5). */
  size?: number;
  alignment?: Alignment;
}

export interface BarcodeLine {
  type: 'barcode';
  data: string;
  barcodeType?: BarcodeType;
  /** Bar height in dots (default 60). */
  height?: number;
  alignment?: Alignment;
}

export interface ImageLine {
  type: 'image';
  base64: string;
  alignment?: Alignment;
}

export interface SeparatorLine {
  type: 'separator';
  char?: string;
  /** Characters; defaults to the full media width. */
  length?: number;
}

export interface SpaceLine {
  type: 'space';
  lines?: number;
}

export interface Column {
  text: string;
  widthRatio?: number;
  alignment?: Alignment;
}

export interface ColumnsLine {
  type: 'columns';
  columns: Column[];
  fontSize?: FontSize;
  bold?: boolean;
}

export type ReceiptLine =
  | TextLine
  | KeyValueLine
  | QRLine
  | BarcodeLine
  | ImageLine
  | SeparatorLine
  | SpaceLine
  | ColumnsLine;

export interface Receipt {
  header?: ReceiptLine[];
  body?: ReceiptLine[];
  footer?: ReceiptLine[];
}

// Spacing values match the 1.x output so printed receipts keep their look; only fonts taller
// than the 1.x fixed 30-dot row (large, xlarge) get their real height, which fixes overlaps.
const SIDE_PADDING_DOTS = 10;
const TOP_PADDING_DOTS = 20;
const MIN_TEXT_ROW_DOTS = 30;
const SEPARATOR_ROW_DOTS = 20;
const SPACE_LINE_DOTS = 20;
const QR_GAP_DOTS = 20;
const IMAGE_GAP_DOTS = 10;
const BARCODE_TEXT_DOTS = 30;
const BARCODE_NARROW_DOTS = 2;
const BARCODE_WIDE_DOTS = 6;

interface Placed {
  commands: PrintCommand[];
  height: number;
}

function alignX(alignment: Alignment, mediaWidth: number, contentWidth: number): number {
  switch (alignment) {
    case 'left':
      return SIDE_PADDING_DOTS;
    case 'center':
      return Math.max(0, Math.floor((mediaWidth - contentWidth) / 2));
    case 'right':
      return Math.max(0, mediaWidth - contentWidth - SIDE_PADDING_DOTS);
  }
}

function charsPerLine(fontSize: FontSize, mediaWidth: number): number {
  const usable = mediaWidth - 2 * SIDE_PADDING_DOTS;
  return Math.max(1, Math.floor(usable / DEVICE_FONTS[fontSize].charWidth));
}

export function wrapText(text: string, maxChars: number): string[] {
  const lines: string[] = [];
  for (const paragraph of text.split('\n')) {
    let current = '';
    for (const word of paragraph.split(' ')) {
      let rest = word;
      while (rest.length > maxChars) {
        if (current) {
          lines.push(current);
          current = '';
        }
        lines.push(rest.slice(0, maxChars));
        rest = rest.slice(maxChars);
      }
      const candidate = current ? `${current} ${rest}` : rest;
      if (candidate.length <= maxChars) {
        current = candidate;
      } else {
        lines.push(current);
        current = rest;
      }
    }
    lines.push(current);
  }
  return lines;
}

function textRows(
  rows: string[],
  fontSize: FontSize,
  bold: boolean,
  alignment: Alignment,
  y: number,
  mediaWidth: number,
  rowHeight: number = Math.max(MIN_TEXT_ROW_DOTS, DEVICE_FONTS[fontSize].charHeight)
): Placed {
  const font = DEVICE_FONTS[fontSize];
  const commands: PrintCommand[] = rows.map((row, index) => ({
    type: 'text',
    x: alignX(alignment, mediaWidth, row.length * font.charWidth),
    y: y + index * rowHeight,
    height: font.charHeight,
    text: row,
    font: font.code,
    bold,
  }));
  return { commands, height: rows.length * rowHeight };
}

function fitColumn(text: string, width: number, alignment: Alignment): string {
  const clipped = text.slice(0, width);
  switch (alignment) {
    case 'left':
      return clipped.padEnd(width);
    case 'right':
      return clipped.padStart(width);
    case 'center':
      return clipped.padStart(Math.floor((width + clipped.length) / 2)).padEnd(width);
  }
}

function placeLine(line: ReceiptLine, y: number, mediaWidth: number): Placed {
  switch (line.type) {
    case 'text': {
      const fontSize = line.fontSize ?? 'medium';
      const rows = wrapText(line.content, charsPerLine(fontSize, mediaWidth));
      return textRows(rows, fontSize, line.bold ?? false, line.alignment ?? 'left', y, mediaWidth);
    }
    case 'keyValue': {
      const fontSize = line.fontSize ?? 'medium';
      const maxChars = charsPerLine(fontSize, mediaWidth);
      const gap = maxChars - line.key.length - line.value.length;
      const rows =
        gap >= 1
          ? [`${line.key}${' '.repeat(gap)}${line.value}`]
          : wrapText(`${line.key} ${line.value}`, maxChars);
      return textRows(rows, fontSize, line.bold ?? false, 'left', y, mediaWidth);
    }
    case 'columns': {
      const fontSize = line.fontSize ?? 'medium';
      const maxChars = charsPerLine(fontSize, mediaWidth);
      const row = line.columns
        .map((column) =>
          fitColumn(
            column.text,
            Math.floor(maxChars * (column.widthRatio ?? 1 / line.columns.length)),
            column.alignment ?? 'left'
          )
        )
        .join('');
      return textRows([row], fontSize, line.bold ?? false, 'left', y, mediaWidth);
    }
    case 'separator': {
      const char = (line.char ?? '-').charAt(0) || '-';
      const length = line.length ?? charsPerLine('small', mediaWidth);
      return textRows(
        [char.repeat(length)],
        'small',
        false,
        'left',
        y,
        mediaWidth,
        SEPARATOR_ROW_DOTS
      );
    }
    case 'space':
      return { commands: [], height: (line.lines ?? 1) * SPACE_LINE_DOTS };
    case 'qr': {
      const size = Math.min(9, Math.max(1, line.size ?? 5));
      const side = qrSideDots(line.data, size);
      return {
        commands: [
          {
            type: 'qr',
            x: alignX(line.alignment ?? 'center', mediaWidth, side),
            y,
            height: side,
            data: line.data,
            size,
            errorCorrection: 'M',
          },
        ],
        height: side + QR_GAP_DOTS,
      };
    }
    case 'barcode': {
      const barcodeType = line.barcodeType ?? 'CODE128';
      const barHeight = line.height ?? 60;
      const width = barcodeWidthDots(
        barcodeType,
        line.data,
        BARCODE_NARROW_DOTS,
        BARCODE_WIDE_DOTS
      );
      return {
        commands: [
          {
            type: 'barcode',
            x: alignX(line.alignment ?? 'center', mediaWidth, width),
            y,
            height: barHeight + BARCODE_TEXT_DOTS,
            data: line.data,
            symbology: BARCODE_SYMBOLOGY[barcodeType],
            narrowBarWidth: BARCODE_NARROW_DOTS,
            wideBarWidth: BARCODE_WIDE_DOTS,
            barHeight,
            showText: true,
          },
        ],
        height: barHeight + BARCODE_TEXT_DOTS,
      };
    }
    case 'image': {
      const size = readImageSize(line.base64);
      const width = size ? Math.min(size.width, mediaWidth) : mediaWidth;
      const height = size ? Math.round((size.height * width) / size.width) : 0;
      return {
        commands: [
          {
            type: 'image',
            x: size ? alignX(line.alignment ?? 'center', mediaWidth, width) : 0,
            y,
            height,
            base64: line.base64,
            width,
          },
        ],
        height: height + IMAGE_GAP_DOTS,
      };
    }
  }
}

/**
 * Context: lays out receipt lines top to bottom in printer dots. An image whose size can't be
 * read (not PNG/JPEG) is only allowed as the last line, since nothing can be placed below it.
 */
export function layoutLines(lines: ReceiptLine[], mediaWidth: number): PrintCommand[] {
  const commands: PrintCommand[] = [];
  let y = TOP_PADDING_DOTS;
  lines.forEach((line, index) => {
    const placed = placeLine(line, y, mediaWidth);
    const unknownImage = placed.commands.some((c) => c.type === 'image' && c.height === 0);
    if (unknownImage && index < lines.length - 1) {
      throw createPrinterError(
        'INVALID_ARGUMENT',
        'Receipt images must be PNG or JPEG when more lines follow them'
      );
    }
    commands.push(...placed.commands);
    y += placed.height;
  });
  return commands;
}

export function layoutReceipt(receipt: Receipt, mediaWidth: number): PrintCommand[] {
  return layoutLines(
    [...(receipt.header ?? []), ...(receipt.body ?? []), ...(receipt.footer ?? [])],
    mediaWidth
  );
}

export function layoutPdfPage(base64: string, page: number, mediaWidth: number): PrintCommand[] {
  return [{ type: 'pdf', x: 0, y: TOP_PADDING_DOTS, height: 0, base64, page, width: mediaWidth }];
}
