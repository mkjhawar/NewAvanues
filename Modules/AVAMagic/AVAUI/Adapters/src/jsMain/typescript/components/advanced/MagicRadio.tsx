import React from 'react';
import { Radio, RadioGroup, FormControlLabel, FormControl, FormLabel } from '@mui/material';

/**
 * MagicRadio - React/Material-UI Radio Group Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface RadioOption {
  value: string;
  label: string;
}

export interface MagicRadioProps {
  value: string;
  onChange: (value: string) => void;
  options: RadioOption[];
  label?: string;
  className?: string;
}

export const MagicRadio: React.FC<MagicRadioProps> = ({
  value,
  onChange,
  options,
  label,
  className
}) => {
  return (
    <FormControl component="fieldset" className={className}>
      {label && <FormLabel component="legend">{label}</FormLabel>}
      <RadioGroup value={value} onChange={(e) => onChange(e.target.value)}>
        {options.map((option) => (
          <FormControlLabel
            key={option.value}
            value={option.value}
            control={<Radio />}
            label={option.label}
          />
        ))}
      </RadioGroup>
    </FormControl>
  );
};

export default MagicRadio;
