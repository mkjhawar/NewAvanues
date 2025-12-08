/**
 * TextButton - Flutter Parity Material Design
 *
 * A Material Design text button (flat button with no elevation).
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { Button } from '@mui/material';
import type { TextButtonProps } from './types';

export const TextButton: React.FC<TextButtonProps> = ({
  text,
  icon,
  enabled = true,
  onPressed,
  accessibilityLabel,
  ...rest
}) => {
  return (
    <Button
      variant="text"
      onClick={enabled ? onPressed : undefined}
      disabled={!enabled}
      startIcon={icon}
      aria-label={accessibilityLabel || text}
      sx={{
        textTransform: 'none',
        minHeight: 40,
        paddingX: 2,
        ...rest.sx,
      }}
      {...rest}
    >
      {text}
    </Button>
  );
};

export default TextButton;
