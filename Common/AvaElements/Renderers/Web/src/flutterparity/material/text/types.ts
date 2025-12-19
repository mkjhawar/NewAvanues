/**
 * Text Component Type Definitions
 *
 * @since 3.0.0-flutter-parity
 */

import type { TypographyProps } from '@mui/material';

export type TextAlign = 'left' | 'right' | 'center' | 'justify' | 'start' | 'end';
export type TextOverflow = 'clip' | 'ellipsis' | 'fade' | 'visible';

export interface TextStyle {
  fontSize?: number;
  fontWeight?: string | number;
  fontStyle?: 'normal' | 'italic' | 'oblique';
  color?: string;
  letterSpacing?: string | number;
  decoration?: string;
  backgroundColor?: string;
}

export interface TextProps extends Omit<TypographyProps, 'align'> {
  content: string;
  style?: TextStyle;
  textAlign?: TextAlign;
  overflow?: TextOverflow;
  maxLines?: number;
  softWrap?: boolean;
  selectable?: boolean;
}

export interface TextSpan {
  text: string;
  style?: TextStyle;
}

export interface RichTextProps extends Omit<TypographyProps, 'align'> {
  spans: TextSpan[];
  textAlign?: TextAlign;
  overflow?: TextOverflow;
  maxLines?: number;
  softWrap?: boolean;
}
