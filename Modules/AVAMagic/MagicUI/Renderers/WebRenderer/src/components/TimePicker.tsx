import React from 'react';
import { TextField, TextFieldProps } from '@mui/material';

/**
 * TimePicker - Native HTML5 Time Picker Component
 *
 * A time selection component using native browser time input.
 * For advanced time picking, consider using @mui/x-date-pickers.
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface TimePickerProps {
  /** Selected time in HH:mm format (24-hour) */
  value: string;
  /** Change handler receiving time string in HH:mm format */
  onChange: (time: string) => void;
  /** Label text */
  label?: string;
  /** Minimum selectable time (HH:mm) */
  min?: string;
  /** Maximum selectable time (HH:mm) */
  max?: string;
  /** Step interval in seconds (default: 60) */
  step?: number;
  /** Whether the field is required */
  required?: boolean;
  /** Whether the field is disabled */
  disabled?: boolean;
  /** Whether the field is read-only */
  readOnly?: boolean;
  /** Error state */
  error?: boolean;
  /** Helper text */
  helperText?: string;
  /** Whether to take full width */
  fullWidth?: boolean;
  /** Size of the field */
  size?: 'small' | 'medium';
  /** Additional CSS class name */
  className?: string;
  /** Custom styles */
  sx?: TextFieldProps['sx'];
}

/**
 * TimePicker component for selecting times
 *
 * @example
 * ```tsx
 * // Basic time picker
 * const [time, setTime] = useState('09:00');
 * <TimePicker
 *   value={time}
 *   onChange={setTime}
 *   label="Select Time"
 * />
 *
 * // With 30-minute intervals
 * <TimePicker
 *   value={time}
 *   onChange={setTime}
 *   label="Appointment Time"
 *   step={1800} // 30 minutes in seconds
 * />
 *
 * // Business hours only
 * <TimePicker
 *   value={time}
 *   onChange={setTime}
 *   label="Meeting Time"
 *   min="09:00"
 *   max="17:00"
 * />
 * ```
 */
export const TimePicker: React.FC<TimePickerProps> = ({
  value,
  onChange,
  label,
  min,
  max,
  step = 60,
  required = false,
  disabled = false,
  readOnly = false,
  error = false,
  helperText,
  fullWidth = true,
  size = 'medium',
  className,
  sx
}) => {
  return (
    <TextField
      type="time"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      label={label}
      required={required}
      disabled={disabled}
      error={error}
      helperText={helperText}
      fullWidth={fullWidth}
      size={size}
      className={className}
      sx={sx}
      InputLabelProps={{
        shrink: true
      }}
      inputProps={{
        min,
        max,
        step,
        readOnly
      }}
    />
  );
};

export default TimePicker;
