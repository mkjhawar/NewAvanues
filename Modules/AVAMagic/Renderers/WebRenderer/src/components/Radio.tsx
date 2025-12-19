import React, { useState } from 'react';
import {
  FormControl,
  FormLabel,
  RadioGroup,
  FormControlLabel,
  Radio as MuiRadio
} from '@mui/material';
import { RadioProps, RadioOption } from '../types';

export const Radio: React.FC<RadioProps> = ({
  id,
  label,
  options = [],
  value: initialValue,
  onChange,
  orientation = 'vertical',
  disabled = false,
  ...props
}) => {
  const [value, setValue] = useState(initialValue || options[0]?.value || '');

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value;
    setValue(newValue);
    onChange?.(newValue);
  };

  return (
    <FormControl component="fieldset" disabled={disabled}>
      {label && <FormLabel component="legend">{label}</FormLabel>}
      <RadioGroup
        id={id}
        value={value}
        onChange={handleChange}
        row={orientation === 'horizontal'}
        {...props}
      >
        {options.map((option) => (
          <FormControlLabel
            key={option.value}
            value={option.value}
            control={<MuiRadio />}
            label={option.label}
            disabled={option.disabled}
          />
        ))}
      </RadioGroup>
    </FormControl>
  );
};
