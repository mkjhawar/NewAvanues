import React from 'react';
import { Select, MenuItem, FormControl, InputLabel, SelectChangeEvent } from '@mui/material';

/**
 * MagicDropdown - React/Material-UI Select Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface DropdownOption {
  value: string;
  label: string;
}

export interface MagicDropdownProps {
  value: string;
  onChange: (value: string) => void;
  options: DropdownOption[];
  label?: string;
  placeholder?: string;
  className?: string;
}

export const MagicDropdown: React.FC<MagicDropdownProps> = ({
  value,
  onChange,
  options,
  label,
  placeholder,
  className
}) => {
  const handleChange = (event: SelectChangeEvent) => {
    onChange(event.target.value);
  };

  return (
    <FormControl fullWidth className={className}>
      {label && <InputLabel>{label}</InputLabel>}
      <Select
        value={value}
        onChange={handleChange}
        label={label}
        displayEmpty
      >
        {placeholder && (
          <MenuItem value="" disabled>
            {placeholder}
          </MenuItem>
        )}
        {options.map((option) => (
          <MenuItem key={option.value} value={option.value}>
            {option.label}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
};

export default MagicDropdown;
