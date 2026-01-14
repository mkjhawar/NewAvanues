/**
 * ActionChip Component - Flutter Parity Material Design
 *
 * A compact button-like chip for triggering actions.
 * Similar to AssistChip in Material Design 3.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { MagicTag as MuiChip, Avatar } from '@mui/material';
import type { MagicActionProps } from './types';

export const MagicAction: React.FC<MagicActionProps> = ({
  label,
  onPressed,
  avatar,
  enabled = true,
  accessibilityLabel,
  ...rest
}) => {
  const handleClick = () => {
    if (enabled && onPressed) {
      onPressed();
    }
  };

  return (
    <MuiChip
      label={label}
      onClick={handleClick}
      disabled={!enabled}
      variant="outlined"
      avatar={avatar ? <Avatar src={avatar} sx={{ width: 24, height: 24 }} /> : undefined}
      aria-label={accessibilityLabel || label}
      role="button"
      sx={{
        borderColor: 'divider',
        opacity: enabled ? 1 : 0.5,
        '&:hover': {
          backgroundColor: 'action.hover',
          borderColor: 'action.active',
        },
        '&:active': {
          backgroundColor: 'action.selected',
        },
        ...rest.sx,
      }}
      {...rest}
    />
  );
};

export default ActionChip;
