import React from 'react';
import { Chip as MUIChip, ChipProps as MUIChipProps } from '@mui/material';

/**
 * Chip - Material-UI Chip Component Wrapper
 *
 * A compact element that represents an input, attribute, or action.
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export enum ChipVariant {
  FILLED = 'filled',
  OUTLINED = 'outlined'
}

export enum ChipSize {
  SMALL = 'small',
  MEDIUM = 'medium'
}

export enum ChipColor {
  DEFAULT = 'default',
  PRIMARY = 'primary',
  SECONDARY = 'secondary',
  ERROR = 'error',
  INFO = 'info',
  SUCCESS = 'success',
  WARNING = 'warning'
}

export interface ChipProps {
  /** The label text to display */
  label: string;
  /** Visual variant of the chip */
  variant?: ChipVariant;
  /** Size of the chip */
  size?: ChipSize;
  /** Color scheme */
  color?: ChipColor;
  /** Icon to display at the start */
  icon?: React.ReactNode;
  /** Avatar to display at the start */
  avatar?: React.ReactNode;
  /** Whether the chip can be deleted */
  deletable?: boolean;
  /** Whether the chip is disabled */
  disabled?: boolean;
  /** Whether the chip is clickable */
  clickable?: boolean;
  /** Click handler */
  onClick?: () => void;
  /** Delete handler */
  onDelete?: () => void;
  /** Additional CSS class name */
  className?: string;
  /** Custom styles */
  sx?: MUIChipProps['sx'];
}

/**
 * Chip component for displaying compact information or actions
 *
 * @example
 * ```tsx
 * // Basic chip
 * <Chip label="Active" />
 *
 * // With icon and delete
 * <Chip
 *   label="React"
 *   icon={<CodeIcon />}
 *   deletable
 *   onDelete={() => console.log('Deleted')}
 * />
 *
 * // Clickable chip
 * <Chip
 *   label="View Details"
 *   clickable
 *   onClick={() => console.log('Clicked')}
 *   color="primary"
 * />
 * ```
 */
export const Chip: React.FC<ChipProps> = ({
  label,
  variant = ChipVariant.FILLED,
  size = ChipSize.MEDIUM,
  color = ChipColor.DEFAULT,
  icon,
  avatar,
  deletable = false,
  disabled = false,
  clickable = false,
  onClick,
  onDelete,
  className,
  sx
}) => {
  return (
    <MUIChip
      label={label}
      variant={variant as MUIChipProps['variant']}
      size={size as MUIChipProps['size']}
      color={color as MUIChipProps['color']}
      icon={icon as MUIChipProps['icon']}
      avatar={avatar as MUIChipProps['avatar']}
      onDelete={deletable ? onDelete : undefined}
      disabled={disabled}
      clickable={clickable || !!onClick}
      onClick={onClick}
      className={className}
      sx={sx}
    />
  );
};

export default Chip;
