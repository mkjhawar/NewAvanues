import React from 'react';
import { FormControl, FormLabel, RadioGroup as MuiRadioGroup, FormControlLabel, Radio } from '@mui/material';

export interface RadioOption {
  label: string;
  value: string;
  disabled?: boolean;
}

export interface RadioGroupProps {
  label?: string;
  options: RadioOption[];
  value?: string;
  onChange?: (value: string) => void;
  disabled?: boolean;
  row?: boolean;
}

export const RadioGroup: React.FC<RadioGroupProps> = ({
  label,
  options,
  value,
  onChange,
  disabled = false,
  row = false,
}) => {
  return (
    <FormControl disabled={disabled}>
      {label && <FormLabel>{label}</FormLabel>}
      <MuiRadioGroup
        value={value}
        onChange={(e) => onChange?.(e.target.value)}
        row={row}
      >
        {options.map((option) => (
          <FormControlLabel
            key={option.value}
            value={option.value}
            control={<Radio />}
            label={option.label}
            disabled={option.disabled}
          />
        ))}
      </MuiRadioGroup>
    </FormControl>
  );
};

export default RadioGroup;
