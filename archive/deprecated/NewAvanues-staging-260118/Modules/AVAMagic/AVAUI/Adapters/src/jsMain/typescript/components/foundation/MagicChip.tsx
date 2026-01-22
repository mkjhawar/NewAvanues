import React from 'react';
import { Chip, ChipProps } from '@mui/material';

/**
 * MagicChip - React/Material-UI Chip Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export enum ChipVariant {
  DEFAULT = 'filled',
  OUTLINED = 'outlined',
  FILLED = 'filled'
}

export interface MagicChipProps {
  label: string;
  variant?: ChipVariant;
  icon?: React.ReactNode;
  deletable?: boolean;
  onClick?: () => void;
  onDelete?: () => void;
  className?: string;
}

export const MagicChip: React.FC<MagicChipProps> = ({
  label,
  variant = ChipVariant.DEFAULT,
  icon,
  deletable = false,
  onClick,
  onDelete,
  className
}) => {
  const chipProps: ChipProps = {
    label,
    variant: variant as any,
    icon: icon as any,
    onClick,
    onDelete: deletable ? onDelete : undefined,
    className
  };

  return <Chip {...chipProps} />;
};

export default MagicChip;
