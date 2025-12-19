/**
 * FilledTonalButton - Flutter Parity Material Design
 *
 * A Material Design 3 filled tonal button (lower emphasis than filled).
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { Button } from '@mui/material';
import type { FilledTonalButtonProps } from './types';

export const FilledTonalButton: React.FC<FilledTonalButtonProps> = ({
  text,
  icon,
  iconPosition = 'leading',
  enabled = true,
  onPressed,
  accessibilityLabel,
  ...rest
}) => {
  return (
    <Button
      variant="contained"
      onClick={enabled ? onPressed : undefined}
      disabled={!enabled}
      startIcon={iconPosition === 'leading' ? icon : undefined}
      endIcon={iconPosition === 'trailing' ? icon : undefined}
      aria-label={accessibilityLabel || text}
      sx={{
        textTransform: 'none',
        minHeight: 40,
        paddingX: 3,
        fontWeight: 600,
        backgroundColor: 'primary.light',
        color: 'primary.dark',
        '&:hover': {
          backgroundColor: 'primary.main',
        },
        ...rest.sx,
      }}
      {...rest}
    >
      {text}
    </Button>
  );
};

export default FilledTonalButton;
