import React from 'react';
import { TextField } from '@mui/material';

/**
 * MagicDatePicker - React Date Picker Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface MagicDatePickerProps {
  value: string;
  onChange: (date: string) => void;
  label?: string;
  className?: string;
}

export const MagicDatePicker: React.FC<MagicDatePickerProps> = ({
  value,
  onChange,
  label,
  className
}) => {
  return (
    <TextField
      type="date"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      label={label}
      InputLabelProps={{ shrink: true }}
      fullWidth
      className={className}
    />
  );
};

export default MagicDatePicker;
