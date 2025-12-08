/**
 * FilledButton - Flutter Parity Material Design
 *
 * A Material Design 3 filled button.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { Button } from '@mui/material';
import type { FilledButtonProps } from './types';

export const FilledButton: React.FC<FilledButtonProps> = ({
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
        ...rest.sx,
      }}
      {...rest}
    >
      {text}
    </Button>
  );
};

export default FilledButton;
