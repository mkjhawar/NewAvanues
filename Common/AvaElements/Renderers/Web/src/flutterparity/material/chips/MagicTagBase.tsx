/**
 * RawChip Component - Flutter Parity Material Design
 *
 * Low-level chip component with full customization.
 * Base component for other chip variants.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { MagicTag as MuiChip, Avatar } from '@mui/material';
import type { MagicTagBaseProps } from './types';

export const MagicTagBase: React.FC<MagicTagBaseProps> = ({
  label,
  avatar,
  icon,
  deleteIcon,
  onPressed,
  onSelected,
  onDeleted,
  selected = false,
  enabled = true,
  variant = 'filled',
  backgroundColor,
  selectedColor,
  disabledColor,
  labelPadding,
  padding,
  elevation = 0,
  accessibilityLabel,
  ...rest
}) => {
  const handleClick = () => {
    if (!enabled) return;
    onPressed?.();
    if (onSelected) {
      onSelected(!selected);
    }
  };

  const handleDelete = onDeleted && enabled ? onDeleted : undefined;

  return (
    <MuiChip
      label={label}
      onClick={handleClick}
      onDelete={handleDelete}
      disabled={!enabled}
      variant={variant === 'outlined' ? 'outlined' : 'filled'}
      avatar={avatar ? <Avatar src={avatar} /> : undefined}
      icon={icon}
      deleteIcon={deleteIcon}
      aria-label={accessibilityLabel || label}
      aria-pressed={onSelected ? selected : undefined}
      sx={{
        backgroundColor: !enabled
          ? disabledColor || 'action.disabledBackground'
          : selected
          ? selectedColor || 'primary.light'
          : backgroundColor || 'default',
        padding: padding ? `${padding}px` : undefined,
        '& .MuiChip-label': {
          padding: labelPadding ? `0 ${labelPadding}px` : undefined,
        },
        opacity: enabled ? 1 : 0.5,
        boxShadow: elevation > 0 ? elevation : undefined,
        ...rest.sx,
      }}
      {...rest}
    />
  );
};

export default RawChip;
