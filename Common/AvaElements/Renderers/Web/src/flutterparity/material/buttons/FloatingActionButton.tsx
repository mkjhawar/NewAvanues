/**
 * FloatingActionButton - Flutter Parity Material Design
 *
 * A circular Material Design floating action button (FAB).
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { Fab } from '@mui/material';
import type { FloatingActionButtonProps } from './types';

export const FloatingActionButton: React.FC<FloatingActionButtonProps> = ({
  icon,
  label,
  extended = false,
  elevation = 6,
  backgroundColor,
  foregroundColor,
  enabled = true,
  onPressed,
  accessibilityLabel,
  ...rest
}) => {
  return (
    <Fab
      variant={extended ? 'extended' : 'circular'}
      onClick={enabled ? onPressed : undefined}
      disabled={!enabled}
      aria-label={accessibilityLabel || label || 'Floating action button'}
      sx={{
        backgroundColor: backgroundColor || 'primary.main',
        color: foregroundColor || 'primary.contrastText',
        boxShadow: enabled ? elevation : 0,
        '&:hover': {
          backgroundColor: backgroundColor || 'primary.dark',
        },
        ...rest.sx,
      }}
      {...rest}
    >
      {icon}
      {extended && label && <span style={{ marginLeft: 8 }}>{label}</span>}
    </Fab>
  );
};

export default FloatingActionButton;
