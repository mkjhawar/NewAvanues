/**
 * OutlinedButton - Flutter Parity Material Design
 *
 * A Material Design outlined button with border.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { Button } from '@mui/material';
import type { OutlinedButtonProps } from './types';

export const OutlinedButton: React.FC<OutlinedButtonProps> = ({
  text,
  icon,
  enabled = true,
  onPressed,
  accessibilityLabel,
  ...rest
}) => {
  return (
    <Button
      variant="outlined"
      onClick={enabled ? onPressed : undefined}
      disabled={!enabled}
      startIcon={icon}
      aria-label={accessibilityLabel || text}
      sx={{
        textTransform: 'none',
        minHeight: 40,
        paddingX: 2,
        borderWidth: 1,
        ...rest.sx,
      }}
      {...rest}
    >
      {text}
    </Button>
  );
};

export default OutlinedButton;
