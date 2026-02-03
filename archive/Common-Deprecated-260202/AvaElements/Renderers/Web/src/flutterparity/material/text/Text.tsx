/**
 * Text Component - Flutter Parity Material Design
 *
 * A run of text with a single style.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { Typography } from '@mui/material';
import type { TextProps } from './types';

export const Text: React.FC<TextProps> = ({
  content,
  style,
  textAlign = 'start',
  overflow = 'clip',
  maxLines,
  softWrap = true,
  selectable = false,
  ...rest
}) => {
  const getTextAlign = () => {
    switch (textAlign) {
      case 'left':
        return 'left';
      case 'right':
        return 'right';
      case 'center':
        return 'center';
      case 'justify':
        return 'justify';
      case 'start':
        return 'start';
      case 'end':
        return 'end';
      default:
        return 'left';
    }
  };

  const getTextOverflow = () => {
    if (overflow === 'ellipsis') return 'ellipsis';
    if (overflow === 'fade') return 'ellipsis'; // Fallback
    return 'clip';
  };

  return (
    <Typography
      sx={{
        fontSize: style?.fontSize || 14,
        fontWeight: style?.fontWeight || 'normal',
        fontStyle: style?.fontStyle || 'normal',
        color: style?.color || 'text.primary',
        letterSpacing: style?.letterSpacing || 'normal',
        textAlign: getTextAlign(),
        overflow: overflow !== 'visible' ? 'hidden' : 'visible',
        textOverflow: getTextOverflow(),
        whiteSpace: !softWrap ? 'nowrap' : 'normal',
        display: '-webkit-box',
        WebkitLineClamp: maxLines || 'unset',
        WebkitBoxOrient: 'vertical',
        userSelect: selectable ? 'text' : 'none',
        ...rest.sx,
      }}
      {...rest}
    >
      {content}
    </Typography>
  );
};

export default Text;
