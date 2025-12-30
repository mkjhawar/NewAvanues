import React from 'react';
import { Button, ButtonProps } from '@mui/material';

/**
 * MagicButton - React/Material-UI Button Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export enum ButtonVariant {
  PRIMARY = 'contained',
  SECONDARY = 'outlined',
  OUTLINED = 'outlined',
  TEXT = 'text'
}

export enum ButtonSize {
  SMALL = 'small',
  MEDIUM = 'medium',
  LARGE = 'large'
}

export enum IconPosition {
  START = 'start',
  END = 'end'
}

export interface MagicButtonProps {
  text: string;
  onClick: () => void;
  variant?: ButtonVariant;
  size?: ButtonSize;
  enabled?: boolean;
  fullWidth?: boolean;
  icon?: React.ReactNode;
  iconPosition?: IconPosition;
  className?: string;
}

export const MagicButton: React.FC<MagicButtonProps> = ({
  text,
  onClick,
  variant = ButtonVariant.PRIMARY,
  size = ButtonSize.MEDIUM,
  enabled = true,
  fullWidth = false,
  icon,
  iconPosition = IconPosition.START,
  className
}) => {
  const buttonProps: ButtonProps = {
    variant: variant as any,
    size: size as any,
    disabled: !enabled,
    fullWidth,
    onClick,
    className,
    startIcon: iconPosition === IconPosition.START ? icon : undefined,
    endIcon: iconPosition === IconPosition.END ? icon : undefined
  };

  return (
    <Button {...buttonProps}>
      {text}
    </Button>
  );
};

export default MagicButton;
