/**
 * ChoiceChip Component - Flutter Parity Material Design
 *
 * A chip for single selection from a group (radio-button-like).
 * Selected state uses filled style with primary color.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { MagicTag as MuiChip, Avatar } from '@mui/material';
import CheckIcon from '@mui/icons-material/Check';
import type { MagicChoiceProps } from './types';

export const MagicChoice: React.FC<MagicChoiceProps> = ({
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
      variant="filled"
      icon={
        selected && showCheckmark ? (
          <CheckIcon sx={{ fontSize: 16, color: 'inherit' }} />
        ) : avatar && !selected ? (
          <Avatar src={avatar} sx={{ width: 24, height: 24 }} />
        ) : undefined
      }
      aria-label={accessibilityLabel || `${label}, ${selected ? 'selected' : 'not selected'}`}
      aria-pressed={selected}
      role="radio"
      aria-checked={selected}
      sx={{
        backgroundColor: selected ? 'primary.main' : 'grey.200',
        color: selected ? 'primary.contrastText' : 'text.primary',
        opacity: enabled ? 1 : 0.5,
        '&:hover': {
          backgroundColor: selected ? 'primary.dark' : 'grey.300',
        },
        ...rest.sx,
      }}
      {...rest}
    />
  );
};

export default ChoiceChip;
