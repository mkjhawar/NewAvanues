import React, { useState } from 'react';
import { Slider as MuiSlider, Typography, Box } from '@mui/material';
import { SliderProps } from '../types';

export const Slider: React.FC<SliderProps> = ({
  id,
  label,
  value: initialValue = 50,
  min = 0,
  max = 100,
  step = 1,
  onChange,
  disabled = false,
  showValue = true,
  valueLabelDisplay = 'auto',
  ...props
}) => {
  const [value, setValue] = useState(initialValue);

  const handleChange = (_: Event, newValue: number | number[]) => {
    const val = Array.isArray(newValue) ? newValue[0] : newValue;
    setValue(val);
    onChange?.(val);
  };

  return (
    <Box sx={{ width: '100%' }}>
      {label && (
        <Typography gutterBottom>
          {label} {showValue && `: ${value}`}
        </Typography>
      )}
      <MuiSlider
        id={id}
        value={value}
        onChange={handleChange}
        min={min}
        max={max}
        step={step}
        disabled={disabled}
        valueLabelDisplay={valueLabelDisplay}
        {...props}
      />
    </Box>
  );
};
