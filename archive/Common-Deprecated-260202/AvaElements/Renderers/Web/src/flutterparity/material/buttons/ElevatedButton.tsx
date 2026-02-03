/**
 * ElevatedButton - Flutter Parity Material Design
 *
 * A Material Design elevated button with shadow elevation.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { Button } from '@mui/material';
import type { ElevatedButtonProps } from './types';

export const ElevatedButton: React.FC<ElevatedButtonProps> = ({
  text,
  icon,
  enabled = true,
  onPressed,
  elevation = 2,
  accessibilityLabel,
  ...rest
}) => {
  return (
    <Button
      variant="contained"
      onClick={enabled ? onPressed : undefined}
      disabled={!enabled}
      startIcon={icon}
      aria-label={accessibilityLabel || text}
      sx={{
        boxShadow: enabled ? elevation : 0,
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

export default ElevatedButton;
