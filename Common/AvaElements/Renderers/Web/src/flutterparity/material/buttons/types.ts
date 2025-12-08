/**
 * Button Component Type Definitions
 *
 * @since 3.0.0-flutter-parity
 */

import type { ReactNode } from 'react';
import type { ButtonProps as MuiButtonProps } from '@mui/material';

export interface BaseButtonProps extends Omit<MuiButtonProps, 'variant'> {
  text?: string;
  icon?: ReactNode;
  enabled?: boolean;
  onPressed?: () => void;
  accessibilityLabel?: string;
}

export interface ElevatedButtonProps extends BaseButtonProps {
  elevation?: number;
}

export interface TextButtonProps extends BaseButtonProps {}

export interface OutlinedButtonProps extends BaseButtonProps {}

export interface IconButtonProps extends Omit<MuiButtonProps, 'onClick'> {
  icon: ReactNode;
  enabled?: boolean;
  onPressed?: () => void;
  iconSize?: number;
  tooltip?: string;
  accessibilityLabel?: string;
}

export interface FloatingActionButtonProps extends Omit<MuiButtonProps, 'onClick'> {
  icon?: ReactNode;
  label?: string;
  extended?: boolean;
  heroTag?: string;
  elevation?: number;
  backgroundColor?: string;
  foregroundColor?: string;
  enabled?: boolean;
  onPressed?: () => void;
  accessibilityLabel?: string;
}

export interface FilledButtonProps extends BaseButtonProps {
  iconPosition?: 'leading' | 'trailing';
}

export interface FilledTonalButtonProps extends BaseButtonProps {
  iconPosition?: 'leading' | 'trailing';
}

export interface SegmentedButtonProps {
  segments: Array<{
    value: string;
    label?: string;
    icon?: ReactNode;
    enabled?: boolean;
  }>;
  selected: string[];
  onSelectionChanged?: (selected: string[]) => void;
  multiSelect?: boolean;
  emptySelectionAllowed?: boolean;
  enabled?: boolean;
}

export interface ButtonBarProps {
  children?: ReactNode;
  alignment?: 'start' | 'end' | 'center' | 'spaceBetween' | 'spaceAround' | 'spaceEvenly';
  mainAxisSize?: 'min' | 'max';
  layoutBehavior?: 'constrained' | 'padded';
}

export interface ButtonThemeData {
  textStyle?: {
    fontSize?: number;
    fontWeight?: string;
    color?: string;
  };
  backgroundColor?: string;
  foregroundColor?: string;
  overlayColor?: string;
  shadowColor?: string;
  elevation?: number;
  padding?: {
    horizontal?: number;
    vertical?: number;
  };
  minimumSize?: {
    width?: number;
    height?: number;
  };
  shape?: {
    borderRadius?: number;
  };
}

export interface ButtonThemeProps {
  data: ButtonThemeData;
  children: ReactNode;
}

export interface SplitButtonProps extends BaseButtonProps {
  menuItems: Array<{
    label: string;
    value: string;
    icon?: ReactNode;
    enabled?: boolean;
    onPressed?: () => void;
  }>;
  onMenuItemPressed?: (value: string) => void;
  menuPosition?: 'bottom' | 'top' | 'left' | 'right';
}

export interface LoadingButtonProps extends BaseButtonProps {
  loading?: boolean;
  loadingPosition?: 'start' | 'end' | 'center';
  loadingIndicator?: ReactNode;
  loadingText?: string;
}

export interface CloseButtonProps extends Omit<MuiButtonProps, 'onClick' | 'variant'> {
  enabled?: boolean;
  onPressed?: () => void;
  size?: 'small' | 'medium' | 'large';
  edge?: 'start' | 'end' | false;
  accessibilityLabel?: string;
}
