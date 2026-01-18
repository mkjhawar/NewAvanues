import React from 'react';
import { Divider as MUIDivider, DividerProps as MUIDividerProps } from '@mui/material';

/**
 * Divider - Material-UI Divider Component Wrapper
 *
 * A thin line that groups content in lists and layouts.
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export enum DividerOrientation {
  HORIZONTAL = 'horizontal',
  VERTICAL = 'vertical'
}

export enum DividerVariant {
  FULLWIDTH = 'fullWidth',
  INSET = 'inset',
  MIDDLE = 'middle'
}

export interface DividerProps {
  /** Orientation of the divider */
  orientation?: DividerOrientation;
  /** Visual variant */
  variant?: DividerVariant;
  /** Whether to render as flexbox item */
  flexItem?: boolean;
  /** Thickness in pixels */
  thickness?: number;
  /** Custom color */
  color?: string;
  /** Text content to display in divider */
  textAlign?: 'left' | 'center' | 'right';
  /** Children (text) to display in divider */
  children?: React.ReactNode;
  /** Additional CSS class name */
  className?: string;
  /** Custom styles */
  sx?: MUIDividerProps['sx'];
}

/**
 * Divider component for separating content
 *
 * @example
 * ```tsx
 * // Simple horizontal divider
 * <Divider />
 *
 * // Vertical divider
 * <Divider orientation="vertical" flexItem />
 *
 * // With text
 * <Divider textAlign="center">OR</Divider>
 *
 * // Custom thickness and color
 * <Divider thickness={2} color="#1976d2" />
 * ```
 */
export const Divider: React.FC<DividerProps> = ({
  orientation = DividerOrientation.HORIZONTAL,
  variant = DividerVariant.FULLWIDTH,
  flexItem = false,
  thickness = 1,
  color,
  textAlign = 'center',
  children,
  className,
  sx
}) => {
  const customSx: MUIDividerProps['sx'] = {
    ...(orientation === DividerOrientation.HORIZONTAL && {
      borderBottomWidth: thickness
    }),
    ...(orientation === DividerOrientation.VERTICAL && {
      borderRightWidth: thickness
    }),
    ...(color && {
      borderColor: color
    }),
    ...sx
  };

  return (
    <MUIDivider
      orientation={orientation as MUIDividerProps['orientation']}
      variant={variant as MUIDividerProps['variant']}
      flexItem={flexItem}
      textAlign={textAlign}
      className={className}
      sx={customSx}
    >
      {children}
    </MUIDivider>
  );
};

export default Divider;
