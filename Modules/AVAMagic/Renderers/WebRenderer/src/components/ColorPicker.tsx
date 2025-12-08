import React, { useState } from 'react';
import { TextField, Box } from '@mui/material';
import { ColorPickerProps } from '../types';

export const ColorPicker: React.FC<ColorPickerProps> = ({
  id,
  value: initialValue = '#000000',
  onChange,
  disabled = false,
  sx,
  ...props
}) => {
  const [value, setValue] = useState(initialValue);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value;
    setValue(newValue);
    onChange?.(newValue);
  };

  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, ...sx }} {...props}>
      <input
        id={id}
        type="color"
        value={value}
        onChange={handleChange}
        disabled={disabled}
        style={{
          width: 56,
          height: 56,
          border: '1px solid #ccc',
          borderRadius: 4,
          cursor: disabled ? 'not-allowed' : 'pointer'
        }}
      />
      <TextField
        value={value}
        onChange={handleChange}
        disabled={disabled}
        label="Color"
        variant="outlined"
        size="small"
        inputProps={{ maxLength: 7, style: { fontFamily: 'monospace' } }}
      />
    </Box>
  );
};
