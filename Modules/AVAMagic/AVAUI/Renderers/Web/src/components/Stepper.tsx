import React from 'react';
import { Box, IconButton, Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';

export interface StepperProps {
  value: number;
  min?: number;
  max?: number;
  step?: number;
  label?: string;
  orientation?: 'horizontal' | 'vertical';
  disabled?: boolean;
  onChange?: (value: number) => void;
}

export const Stepper: React.FC<StepperProps> = ({
  value,
  min = 0,
  max = 100,
  step = 1,
  label,
  orientation = 'horizontal',
  disabled = false,
  onChange,
}) => {
  const handleIncrement = () => {
    if (value + step <= max) {
      onChange?.(value + step);
    }
  };

  const handleDecrement = () => {
    if (value - step >= min) {
      onChange?.(value - step);
    }
  };

  const isHorizontal = orientation === 'horizontal';

  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: isHorizontal ? 'row' : 'column',
        alignItems: 'center',
        gap: 1,
      }}
    >
      {label && (
        <Typography variant="body2" sx={{ mr: isHorizontal ? 1 : 0 }}>
          {label}
        </Typography>
      )}
      <IconButton
        onClick={isHorizontal ? handleDecrement : handleIncrement}
        disabled={disabled || (isHorizontal ? value <= min : value >= max)}
        size="small"
      >
        {isHorizontal ? <RemoveIcon /> : <AddIcon />}
      </IconButton>
      <Typography variant="body1" sx={{ minWidth: 40, textAlign: 'center' }}>
        {value}
      </Typography>
      <IconButton
        onClick={isHorizontal ? handleIncrement : handleDecrement}
        disabled={disabled || (isHorizontal ? value >= max : value <= min)}
        size="small"
      >
        {isHorizontal ? <AddIcon /> : <RemoveIcon />}
      </IconButton>
    </Box>
  );
};

export default Stepper;
