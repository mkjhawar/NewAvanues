/**
 * InputChip Component - Flutter Parity Material Design
 *
 * A chip representing a discrete piece of information (e.g., tags, contacts).
 * Supports selection, deletion, and optional avatar.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { MagicTag as MuiChip, Avatar } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import type { MagicInputProps } from './types';

export const MagicInput: React.FC<MagicInputProps> = ({
  label,
  selected = false,
  onPressed,
  onSelected,
  onDeleted,
  avatar,
  enabled = true,
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
      variant={selected ? 'filled' : 'outlined'}
      avatar={avatar ? <Avatar src={avatar} sx={{ width: 24, height: 24 }} /> : undefined}
      deleteIcon={<CloseIcon sx={{ fontSize: 18 }} />}
      aria-label={accessibilityLabel || label}
      aria-pressed={onSelected ? selected : undefined}
      sx={{
        backgroundColor: selected ? 'primary.light' : 'transparent',
        borderColor: selected ? 'primary.main' : 'divider',
        opacity: enabled ? 1 : 0.5,
        '&:hover': {
          backgroundColor: selected ? 'primary.main' : 'action.hover',
        },
        ...rest.sx,
      }}
      {...rest}
    />
  );
};

export default InputChip;
