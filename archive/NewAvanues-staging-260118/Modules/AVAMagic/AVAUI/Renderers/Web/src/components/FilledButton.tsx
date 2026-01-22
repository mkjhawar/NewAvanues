import React from 'react';
import { Button } from '@mui/material';

export interface FilledButtonProps {
  label: string;
  onClick?: () => void;
  disabled?: boolean;
  startIcon?: React.ReactNode;
  endIcon?: React.ReactNode;
  color?: 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning';
  size?: 'small' | 'medium' | 'large';
}

export const FilledButton: React.FC<FilledButtonProps> = ({
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
      variant="contained"
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

export default FilledButton;
