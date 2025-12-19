/**
 * Badge Component - Flutter Parity Material Design
 *
 * A small component typically displayed on top of another component
 * to show a counter or status indicator.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { Badge as MuiBadge } from '@mui/material';
import type { BadgeProps } from './types';

export const Badge: React.FC<BadgeProps> = ({
  children,
  label,
  isLabelVisible = true,
  backgroundColor,
  textColor,
  smallSize,
  largeSize,
  alignment = 'topEnd',
  offset,
  ...rest
}) => {
  const getAnchorOrigin = () => {
    switch (alignment) {
      case 'topStart':
        return { vertical: 'top' as const, horizontal: 'left' as const };
      case 'topEnd':
        return { vertical: 'top' as const, horizontal: 'right' as const };
      case 'bottomStart':
        return { vertical: 'bottom' as const, horizontal: 'left' as const };
      case 'bottomEnd':
        return { vertical: 'bottom' as const, horizontal: 'right' as const };
      default:
        return { vertical: 'top' as const, horizontal: 'right' as const };
    }
  };

  return (
    <MuiBadge
      badgeContent={isLabelVisible ? label : undefined}
      invisible={!isLabelVisible}
      anchorOrigin={getAnchorOrigin()}
      sx={{
        '& .MuiBadge-badge': {
          backgroundColor: backgroundColor || 'error.main',
          color: textColor || 'error.contrastText',
          minWidth: smallSize || 20,
          height: smallSize || 20,
          fontSize: 10,
          ...(offset && {
            transform: `translate(${offset.x || 0}%, ${offset.y || 0}%)`,
          }),
        },
        ...rest.sx,
      }}
      {...rest}
    >
      {children}
    </MuiBadge>
  );
};

export default Badge;
