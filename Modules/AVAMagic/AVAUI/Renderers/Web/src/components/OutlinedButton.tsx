import React from 'react';
import { Button } from '@mui/material';

export interface OutlinedButtonProps {
  label: string;
  onClick?: () => void;
  disabled?: boolean;
  startIcon?: React.ReactNode;
  endIcon?: React.ReactNode;
  color?: 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning';
  size?: 'small' | 'medium' | 'large';
}

export const OutlinedButton: React.FC<OutlinedButtonProps> = ({
  label,
  onClick,
  disabled = false,
  startIcon,
  endIcon,
  color = 'primary',
  size = 'medium',
}) => {
  return (
    <Button
      variant="outlined"
      onClick={onClick}
      disabled={disabled}
      startIcon={startIcon}
      endIcon={endIcon}
      color={color}
      size={size}
    >
      {label}
    </Button>
  );
};

export default OutlinedButton;
