/**
 * RichText Component - Flutter Parity Material Design
 *
 * A paragraph of rich text with multiple inline styles.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { Typography } from '@mui/material';
import type { RichTextProps, TextSpan } from './types';

export const RichText: React.FC<RichTextProps> = ({
  spans,
  textAlign = 'start',
  overflow = 'clip',
  maxLines,
  softWrap = true,
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

  const renderSpan = (span: TextSpan, index: number) => {
    return (
      <span
        key={index}
        style={{
          fontSize: span.style?.fontSize || 14,
          fontWeight: span.style?.fontWeight || 'normal',
          fontStyle: span.style?.fontStyle || 'normal',
          color: span.style?.color || 'inherit',
          letterSpacing: span.style?.letterSpacing || 'normal',
          textDecoration: span.style?.decoration || 'none',
          backgroundColor: span.style?.backgroundColor,
        }}
      >
        {span.text}
      </span>
    );
  };

  return (
    <Typography
      component="div"
      sx={{
        textAlign: getTextAlign(),
        overflow: overflow !== 'visible' ? 'hidden' : 'visible',
        textOverflow: overflow === 'ellipsis' ? 'ellipsis' : 'clip',
        whiteSpace: !softWrap ? 'nowrap' : 'normal',
        display: '-webkit-box',
        WebkitLineClamp: maxLines || 'unset',
        WebkitBoxOrient: 'vertical',
        ...rest.sx,
      }}
      {...rest}
    >
      {spans.map((span, index) => renderSpan(span, index))}
    </Typography>
  );
};

export default RichText;
