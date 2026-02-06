import React from 'react';
import { Slider, SliderProps, Box, Typography } from '@mui/material';

/**
 * MagicSlider - React/Material-UI Slider Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface MagicSliderProps {
  value: number;
  onChange: (value: number) => void;
  min?: number;
  max?: number;
  step?: number;
  label?: string;
  showValue?: boolean;
  className?: string;
}

export const MagicSlider: React.FC<MagicSliderProps> = ({
  value,
  onChange,
  min = 0,
  max = 100,
  step = 1,
  label,
  showValue = false,
  className
}) => {
  const sliderProps: SliderProps = {
    value,
    onChange: (_, newValue) => onChange(newValue as number),
    min,
    max,
    step,
    valueLabelDisplay: 'auto',
    className
  };

  return (
    <Box sx={{ width: '100%' }}>
      {label && <Typography variant="body2" gutterBottom>{label}</Typography>}
      <Slider {...sliderProps} />
      {showValue && <Typography variant="caption">{value}</Typography>}
    </Box>
  );
};

export default MagicSlider;
