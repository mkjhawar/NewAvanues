import React from 'react';
import { Button } from '@mui/material';

export interface TextButtonProps {
  label: string;
  onClick?: () => void;
  disabled?: boolean;
  startIcon?: React.ReactNode;
  endIcon?: React.ReactNode;
  color?: 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning';
  size?: 'small' | 'medium' | 'large';
}

export const TextButton: React.FC<TextButtonProps> = ({
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
      variant="text"
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

export default TextButton;
