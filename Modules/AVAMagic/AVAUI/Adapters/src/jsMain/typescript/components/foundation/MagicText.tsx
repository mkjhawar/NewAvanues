import React from 'react';
import { Typography, TypographyProps } from '@mui/material';

/**
 * MagicText - React/Material-UI Typography Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export enum TextVariant {
  H1 = 'h1',
  H2 = 'h2',
  H3 = 'h3',
  BODY1 = 'body1',
  BODY2 = 'body2',
  CAPTION = 'caption'
}

export enum TextAlign {
  START = 'left',
  CENTER = 'center',
  END = 'right'
}

export interface MagicTextProps {
  content: string;
  variant?: TextVariant;
  color?: string;
  align?: TextAlign;
  bold?: boolean;
  italic?: boolean;
  underline?: boolean;
  maxLines?: number;
  className?: string;
}

export const MagicText: React.FC<MagicTextProps> = ({
  content,
  variant = TextVariant.BODY1,
  color,
  align = TextAlign.START,
  bold = false,
  italic = false,
  underline = false,
  maxLines,
  className
}) => {
  const typographyProps: TypographyProps = {
    variant: variant as any,
    align: align as any,
    className,
    sx: {
      color,
      fontWeight: bold ? 'bold' : undefined,
      fontStyle: italic ? 'italic' : undefined,
      textDecoration: underline ? 'underline' : undefined,
      overflow: maxLines ? 'hidden' : undefined,
      textOverflow: maxLines ? 'ellipsis' : undefined,
      display: maxLines ? '-webkit-box' : undefined,
      WebkitLineClamp: maxLines,
      WebkitBoxOrient: maxLines ? 'vertical' : undefined
    }
  };

  return (
    <Typography {...typographyProps}>
      {content}
    </Typography>
  );
};

export default MagicText;
