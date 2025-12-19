/**
 * IconButton - Flutter Parity Material Design
 *
 * A Material Design icon button.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { IconButton as MuiIconButton, Tooltip } from '@mui/material';
import type { IconButtonProps } from './types';

export const IconButton: React.FC<IconButtonProps> = ({
  icon,
  enabled = true,
  onPressed,
  iconSize = 24,
  tooltip,
  accessibilityLabel,
  ...rest
}) => {
  const button = (
    <MuiIconButton
      onClick={enabled ? onPressed : undefined}
      disabled={!enabled}
      aria-label={accessibilityLabel || tooltip}
      sx={{
        fontSize: iconSize,
        ...rest.sx,
      }}
      {...rest}
    >
      {icon}
    </MuiIconButton>
  );

  if (tooltip && enabled) {
    return <Tooltip title={tooltip}>{button}</Tooltip>;
  }

  return button;
};

export default IconButton;
