import React from 'react';
import { IconButton as MuiIconButton } from '@mui/material';

export interface IconButtonProps {
  icon: React.ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  color?: 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning';
  size?: 'small' | 'medium' | 'large';
  edge?: 'start' | 'end' | false;
}

export const IconButton: React.FC<IconButtonProps> = ({
  icon,
  onClick,
  disabled = false,
  color = 'default',
  size = 'medium',
  edge = false,
}) => {
  return (
    <MuiIconButton
      onClick={onClick}
      disabled={disabled}
      color={color}
      size={size}
      edge={edge}
    >
      {icon}
    </MuiIconButton>
  );
};

export default IconButton;
