/**
 * MagicTag Component Type Definitions
 * TypeScript interfaces for all magic tag variants
 *
 * @since 3.0.0-flutter-parity
 */

import type { ReactNode } from 'react';
import type { ChipProps as MuiChipProps } from '@mui/material';

// Base magic tag props
export interface BaseMagicTagProps extends Omit<MuiChipProps, 'variant' | 'color'> {
  label: string;
  enabled?: boolean;
  accessibilityLabel?: string;
}

// MagicTag
export interface MagicTagProps extends BaseMagicTagProps {
  avatar?: string;
  icon?: ReactNode;
  onDelete?: () => void;
  onClick?: () => void;
  selected?: boolean;
  variant?: 'filled' | 'outlined';
  color?: 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning';
  size?: 'small' | 'medium';
}

// MagicFilter
export interface MagicFilterProps extends Omit<BaseMagicTagProps, 'onClick'> {
  selected?: boolean;
  onSelected?: (selected: boolean) => void;
  avatar?: string;
  showCheckmark?: boolean;
}

// MagicChoice
export interface MagicChoiceProps extends Omit<BaseMagicTagProps, 'onClick'> {
  selected?: boolean;
  onSelected?: (selected: boolean) => void;
  avatar?: string;
  showCheckmark?: boolean;
}

// MagicInput
export interface MagicInputProps extends Omit<BaseMagicTagProps, 'onClick' | 'onDelete'> {
  selected?: boolean;
  onPressed?: () => void;
  onSelected?: (selected: boolean) => void;
  onDeleted?: () => void;
  avatar?: string;
}

// MagicAction
export interface MagicActionProps extends Omit<BaseMagicTagProps, 'onClick'> {
  onPressed?: () => void;
  avatar?: string;
}

// MagicTagBase
export interface MagicTagBaseProps extends Omit<BaseMagicTagProps, 'onClick' | 'onDelete'> {
  avatar?: string;
  icon?: ReactNode;
  deleteIcon?: ReactNode;
  onPressed?: () => void;
  onSelected?: (selected: boolean) => void;
  onDeleted?: () => void;
  selected?: boolean;
  variant?: 'filled' | 'outlined';
  backgroundColor?: string;
  selectedColor?: string;
  disabledColor?: string;
  labelPadding?: number;
  padding?: number;
  elevation?: number;
}

// MagicTagTheme
export interface MagicTagStyle {
  backgroundColor?: string;
  selectedColor?: string;
  disabledColor?: string;
  labelStyle?: {
    fontSize?: number;
    fontWeight?: string;
    color?: string;
  };
  padding?: number;
  elevation?: number;
  shape?: {
    borderRadius?: number;
  };
}

export interface MagicTagThemeProps {
  data: MagicTagStyle;
  children: ReactNode;
}
