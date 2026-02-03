/**
 * CloseButton - Material Design Close Button
 *
 * A standardized close/dismiss button for dialogs, drawers, and alerts.
 * Follows Material Design 3 patterns with consistent styling.
 *
 * @since 3.1.0-phase3
 */

import React from 'react';
import { IconButton } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import type { CloseButtonProps } from './types';

export const CloseButton: React.FC<CloseButtonProps> = ({
  enabled = true,
  onPressed,
  size = 'medium',
  edge = false,
  accessibilityLabel = 'Close',
  ...rest
}) => {
  const getSizeInPixels = () => {
    switch (size) {
      case 'small':
        return 18;
      case 'large':
        return 32;
      case 'medium':
      default:
        return 24;
    }
  };

  return (
    <IconButton
      onClick={enabled ? onPressed : undefined}
      disabled={!enabled}
      edge={edge}
      size={size}
      aria-label={accessibilityLabel}
      sx={{
        color: 'text.secondary',
        '&:hover': {
          backgroundColor: 'action.hover',
          color: 'text.primary',
        },
        transition: 'all 0.2s ease-in-out',
        ...rest.sx,
      }}
      {...rest}
    >
      <CloseIcon sx={{ fontSize: getSizeInPixels() }} />
    </IconButton>
  );
};

export default CloseButton;
