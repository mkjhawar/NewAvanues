/**
 * Badge Component Type Definitions
 *
 * @since 3.0.0-flutter-parity
 */

import type { ReactNode } from 'react';
import type { BadgeProps as MuiBadgeProps } from '@mui/material';

export type BadgeAlignment = 'topStart' | 'topEnd' | 'bottomStart' | 'bottomEnd';

export interface BadgeProps extends Omit<MuiBadgeProps, 'anchorOrigin' | 'badgeContent'> {
  children: ReactNode;
  label?: string | number;
  isLabelVisible?: boolean;
  backgroundColor?: string;
  textColor?: string;
  smallSize?: number;
  largeSize?: number;
  alignment?: BadgeAlignment;
  offset?: { x?: number; y?: number };
}

export interface BadgeThemeData {
  backgroundColor?: string;
  textColor?: string;
  smallSize?: number;
  largeSize?: number;
  alignment?: BadgeAlignment;
}

export interface BadgeThemeProps {
  data: BadgeThemeData;
  children: ReactNode;
}
