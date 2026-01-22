/**
 * MagicUI Button component for React
 */

import React from 'react';
import { Button as MuiButton } from '@mui/material';
import type { ButtonProps, ButtonVariant } from '../types';

/**
 * Button component wrapper
 */
export const Button: React.FC<ButtonProps> = ({
  text,
  variant = 'Filled',
  disabled = false,
  fullWidth = false,
  startIcon,
  endIcon,
  onClick,
  className,
  style,
  id
}) => {
  // Convert MagicUI variant to Material-UI variant
  const muiVariant = convertVariant(variant);

  return (
    <MuiButton
      id={id}
      variant={muiVariant}
      disabled={disabled}
      fullWidth={fullWidth}
      startIcon={startIcon}
      endIcon={endIcon}
      onClick={onClick}
      className={className}
      style={style}
    >
      {text}
    </MuiButton>
  );
};

/**
 * Convert MagicUI button variant to Material-UI variant
 */
function convertVariant(variant: ButtonVariant | string): 'contained' | 'outlined' | 'text' {
  switch (variant) {
    case 'Filled':
    case 'Elevated':
      return 'contained';
    case 'Outlined':
      return 'outlined';
    case 'Text':
      return 'text';
    default:
      return 'contained';
  }
}

export default Button;
