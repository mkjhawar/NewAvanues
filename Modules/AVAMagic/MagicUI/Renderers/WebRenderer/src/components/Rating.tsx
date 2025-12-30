import React from 'react';
import { Rating as MuiRating, Box, Typography } from '@mui/material';

export interface RatingProps {
  value?: number;
  max?: number;
  precision?: number;
  size?: 'small' | 'medium' | 'large';
  readOnly?: boolean;
  disabled?: boolean;
  onChange?: (value: number | null) => void;
  label?: string;
}

export const Rating: React.FC<RatingProps> = ({
  value = 0,
  max = 5,
  precision = 1,
  size = 'medium',
  readOnly = false,
  disabled = false,
  onChange,
  label,
}) => {
  const [rating, setRating] = React.useState<number | null>(value);

  const handleChange = (_: React.SyntheticEvent, newValue: number | null) => {
    setRating(newValue);
    onChange?.(newValue);
  };

  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
      <MuiRating
        value={rating}
        max={max}
        precision={precision}
        size={size}
        readOnly={readOnly}
        disabled={disabled}
        onChange={handleChange}
      />
      {label && (
        <Typography variant="body2" color="text.secondary">
          {label}
        </Typography>
      )}
    </Box>
  );
};

export default Rating;
