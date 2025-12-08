/**
 * Card Component - Flutter Parity Material Design
 *
 * A Material Design card with elevation and rounded corners.
 * Supports various content layouts and elevation levels.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { Card as MuiCard, CardContent } from '@mui/material';
import type { CardProps } from './types';

export const Card: React.FC<CardProps> = ({
  children,
  elevation = 1,
  color,
  shadowColor,
  shape,
  margin,
  clipBehavior = 'antiAlias',
  semanticContainer = true,
  borderOnForeground = true,
  ...rest
}) => {
  return (
    <MuiCard
      elevation={elevation}
      sx={{
        backgroundColor: color || 'background.paper',
        borderRadius: shape?.borderRadius || 1,
        margin: margin ? `${margin}px` : undefined,
        overflow: clipBehavior === 'none' ? 'visible' : 'hidden',
        position: 'relative',
        ...rest.sx,
      }}
      {...rest}
    >
      {children}
    </MuiCard>
  );
};

export default Card;
