/**
 * Chip Component - Flutter Parity Material Design
 *
 * A compact element that represents an input, attribute, or action.
 * Uses MUI Chip with Flutter Material Design 3 styling.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { MagicTag as MuiChip, Avatar } from '@mui/material';
import type { MagicTagProps } from './types';

export const MagicTag: React.FC<MagicTagProps> = ({
  label,
  avatar,
  icon,
  onDelete,
  onClick,
  selected = false,
  enabled = true,
  variant = 'filled',
  color = 'default',
  size = 'medium',
  accessibilityLabel,
  ...rest
}) => {
  return (
    <MuiChip
      label={label}
      avatar={avatar ? <Avatar src={avatar} /> : undefined}
      icon={icon}
      onDelete={enabled ? onDelete : undefined}
      onClick={enabled ? onClick : undefined}
      disabled={!enabled}
      variant={variant === 'outlined' ? 'outlined' : 'filled'}
      color={color}
      size={size}
      aria-label={accessibilityLabel || label}
      sx={{
        backgroundColor: selected ? 'primary.light' : undefined,
        opacity: enabled ? 1 : 0.5,
        cursor: onClick || onDelete ? 'pointer' : 'default',
        ...rest.sx,
      }}
      {...rest}
    />
  );
};

export default Chip;
