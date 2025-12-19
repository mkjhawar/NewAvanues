import React from 'react';
import { TextField, TextFieldProps } from '@mui/material';

/**
 * DatePicker - Native HTML5 Date Picker Component
 *
 * A date selection component using native browser date input.
 * For advanced date picking, consider using @mui/x-date-pickers.
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface DatePickerProps {
  /** Selected date in YYYY-MM-DD format */
  value: string;
  /** Change handler receiving date string in YYYY-MM-DD format */
  onChange: (date: string) => void;
  /** Label text */
  label?: string;
  /** Minimum selectable date (YYYY-MM-DD) */
  min?: string;
  /** Maximum selectable date (YYYY-MM-DD) */
  max?: string;
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
 * DatePicker component for selecting dates
 *
 * @example
 * ```tsx
 * // Basic date picker
 * const [date, setDate] = useState('2024-01-01');
 * <DatePicker
 *   value={date}
 *   onChange={setDate}
 *   label="Select Date"
 * />
 *
 * // With min/max constraints
 * <DatePicker
 *   value={date}
 *   onChange={setDate}
 *   label="Birth Date"
 *   min="1900-01-01"
 *   max="2024-12-31"
 * />
 *
 * // With validation
 * <DatePicker
 *   value={date}
 *   onChange={setDate}
 *   label="Appointment Date"
 *   required
 *   error={!date}
 *   helperText={!date ? 'Date is required' : ''}
 * />
 * ```
 */
export const DatePicker: React.FC<DatePickerProps> = ({
  value,
  onChange,
  label,
  min,
  max,
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
      type="date"
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
        readOnly
      }}
    />
  );
};

export default DatePicker;
