import React, { useState } from 'react';
import { TextField as MuiTextField } from '@mui/material';
import { TextFieldProps } from '../types';

export const TextField: React.FC<TextFieldProps> = ({
  id,
  placeholder = '',
  value: initialValue = '',
  maxLength,
  onChange,
  disabled = false,
  error = false,
  helperText,
  label,
  variant = 'outlined',
  fullWidth = true,
  ...props
}) => {
  const [value, setValue] = useState(initialValue);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value;
    if (!maxLength || newValue.length <= maxLength) {
      setValue(newValue);
      onChange?.(newValue);
    }
  };

  return (
    <MuiTextField
      id={id}
      value={value}
      onChange={handleChange}
      placeholder={placeholder}
      label={label}
      disabled={disabled}
      error={error}
      helperText={helperText}
      variant={variant}
      fullWidth={fullWidth}
      inputProps={{ maxLength }}
      {...props}
    />
  );
};
