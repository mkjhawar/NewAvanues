import React from 'react';
import { Typography } from '@mui/material';
import { TextProps } from '../types';

export const Text: React.FC<TextProps> = ({
  text,
  variant = 'body1',
  color,
  align = 'left',
  bold = false,
  italic = false,
  sx,
  ...props
}) => {
  return (
    <Typography
      variant={variant}
      color={color}
      align={align}
      sx={{
        fontWeight: bold ? 'bold' : 'normal',
        fontStyle: italic ? 'italic' : 'normal',
        ...sx
      }}
      {...props}
    >
      {text}
    </Typography>
  );
};
