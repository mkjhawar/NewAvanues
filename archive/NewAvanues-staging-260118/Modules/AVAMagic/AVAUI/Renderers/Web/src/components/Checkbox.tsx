import React, { useState } from 'react';
import { FormControlLabel, Checkbox as MuiCheckbox } from '@mui/material';
import { CheckboxProps } from '../types';

export const Checkbox: React.FC<CheckboxProps> = ({
  id,
  label,
  checked: initialChecked = false,
  onChange,
  disabled = false,
  color = 'primary',
  ...props
}) => {
  const [checked, setChecked] = useState(initialChecked);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newChecked = e.target.checked;
    setChecked(newChecked);
    onChange?.(newChecked);
  };

  const checkbox = (
    <MuiCheckbox
      id={id}
      checked={checked}
      onChange={handleChange}
      disabled={disabled}
      color={color}
      {...props}
    />
  );

  if (label) {
    return <FormControlLabel control={checkbox} label={label} />;
  }

  return checkbox;
};
