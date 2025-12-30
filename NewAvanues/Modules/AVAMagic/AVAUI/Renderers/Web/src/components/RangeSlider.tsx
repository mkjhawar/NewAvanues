import React from 'react';
import { Slider, Box, Typography } from '@mui/material';

export interface RangeSliderProps {
  value?: [number, number];
  min?: number;
  max?: number;
  step?: number;
  onChange?: (value: [number, number]) => void;
  disabled?: boolean;
  marks?: boolean | { value: number; label: string }[];
  valueLabelDisplay?: 'auto' | 'on' | 'off';
  label?: string;
}

export const RangeSlider: React.FC<RangeSliderProps> = ({
  value = [20, 80],
  min = 0,
  max = 100,
  step = 1,
  onChange,
  disabled = false,
  marks = false,
  valueLabelDisplay = 'auto',
  label,
}) => {
  const [range, setRange] = React.useState<[number, number]>(value);

  const handleChange = (_: Event, newValue: number | number[]) => {
    const newRange = newValue as [number, number];
    setRange(newRange);
    onChange?.(newRange);
  };

  return (
    <Box sx={{ width: '100%' }}>
      {label && (
        <Typography gutterBottom>
          {label}
        </Typography>
      )}
      <Slider
        value={range}
        onChange={handleChange}
        min={min}
        max={max}
        step={step}
        disabled={disabled}
        marks={marks}
        valueLabelDisplay={valueLabelDisplay}
        disableSwap
      />
    </Box>
  );
};

export default RangeSlider;
