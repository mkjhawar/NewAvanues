import React from 'react';
import { TextField } from '@mui/material';

/**
 * MagicTimePicker - React Time Picker Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface MagicTimePickerProps {
  value: string;
  onChange: (time: string) => void;
  label?: string;
  className?: string;
}

export const MagicTimePicker: React.FC<MagicTimePickerProps> = ({
  value,
  onChange,
  label,
  className
}) => {
  return (
    <TextField
      type="time"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      label={label}
      InputLabelProps={{ shrink: true }}
      fullWidth
      className={className}
    />
  );
};

export default MagicTimePicker;
