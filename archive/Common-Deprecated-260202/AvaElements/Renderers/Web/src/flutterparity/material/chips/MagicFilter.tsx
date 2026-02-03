/**
 * FilterChip Component - Flutter Parity Material Design
 *
 * A chip that can be selected/deselected to filter content.
 * Shows checkmark when selected, matching Flutter Material Design 3.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { MagicTag as MuiChip, Avatar } from '@mui/material';
import CheckIcon from '@mui/icons-material/Check';
import type { MagicFilterProps } from './types';

export const MagicFilter: React.FC<MagicFilterProps> = ({
  label,
  selected = false,
  onSelected,
  avatar,
  showCheckmark = true,
  enabled = true,
  accessibilityLabel,
  ...rest
}) => {
  const handleClick = () => {
    if (enabled && onSelected) {
      onSelected(!selected);
    }
  };

  return (
    <MuiChip
      label={label}
      onClick={handleClick}
      disabled={!enabled}
      variant={selected ? 'filled' : 'outlined'}
      icon={
        selected && showCheckmark ? (
          <CheckIcon sx={{ fontSize: 16 }} />
        ) : avatar && !selected ? (
          <Avatar src={avatar} sx={{ width: 24, height: 24 }} />
        ) : undefined
      }
      aria-label={accessibilityLabel || `${label}, ${selected ? 'selected' : 'not selected'}`}
      aria-pressed={selected}
      role="button"
      sx={{
        backgroundColor: selected ? 'primary.main' : 'transparent',
        color: selected ? 'primary.contrastText' : 'text.primary',
        borderColor: selected ? 'primary.main' : 'divider',
        opacity: enabled ? 1 : 0.5,
        '&:hover': {
          backgroundColor: selected ? 'primary.dark' : 'action.hover',
        },
        ...rest.sx,
      }}
      {...rest}
    />
  );
};

export default FilterChip;
