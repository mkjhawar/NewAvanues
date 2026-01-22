import React from 'react';
import {
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  FormHelperText,
  SelectChangeEvent,
  SelectProps as MUISelectProps
} from '@mui/material';

/**
 * Dropdown - Material-UI Select Component Wrapper
 *
 * A dropdown/select component for choosing from a list of options.
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface DropdownOption {
  /** The value to store when selected */
  value: string | number;
  /** The text to display */
  label: string;
  /** Whether this option is disabled */
  disabled?: boolean;
  /** Divider after this item */
  divider?: boolean;
}

export enum DropdownVariant {
  OUTLINED = 'outlined',
  FILLED = 'filled',
  STANDARD = 'standard'
}

export enum DropdownSize {
  SMALL = 'small',
  MEDIUM = 'medium'
}

export interface DropdownProps {
  /** Currently selected value */
  value: string | number;
  /** Change handler */
  onChange: (value: string | number) => void;
  /** List of options */
  options: DropdownOption[];
  /** Label text */
  label?: string;
  /** Placeholder text (shown when value is empty) */
  placeholder?: string;
  /** Visual variant */
  variant?: DropdownVariant;
  /** Size */
  size?: DropdownSize;
  /** Whether the field is required */
  required?: boolean;
  /** Whether the field is disabled */
  disabled?: boolean;
  /** Error state */
  error?: boolean;
  /** Helper text */
  helperText?: string;
  /** Whether to take full width */
  fullWidth?: boolean;
  /** Whether to allow multiple selections */
  multiple?: boolean;
  /** Additional CSS class name */
  className?: string;
  /** Custom styles */
  sx?: MUISelectProps['sx'];
}

/**
 * Dropdown component for selecting from options
 *
 * @example
 * ```tsx
 * // Basic dropdown
 * const [country, setCountry] = useState('');
 * <Dropdown
 *   value={country}
 *   onChange={setCountry}
 *   label="Country"
 *   options={[
 *     { value: 'us', label: 'United States' },
 *     { value: 'uk', label: 'United Kingdom' },
 *     { value: 'ca', label: 'Canada' }
 *   ]}
 * />
 *
 * // With placeholder and helper text
 * <Dropdown
 *   value={status}
 *   onChange={setStatus}
 *   label="Status"
 *   placeholder="Select status..."
 *   helperText="Choose the current project status"
 *   options={statusOptions}
 * />
 *
 * // With validation
 * <Dropdown
 *   value={role}
 *   onChange={setRole}
 *   label="Role"
 *   required
 *   error={!role}
 *   helperText={!role ? 'Role is required' : ''}
 *   options={roleOptions}
 * />
 * ```
 */
export const Dropdown: React.FC<DropdownProps> = ({
  value,
  onChange,
  options,
  label,
  placeholder,
  variant = DropdownVariant.OUTLINED,
  size = DropdownSize.MEDIUM,
  required = false,
  disabled = false,
  error = false,
  helperText,
  fullWidth = true,
  multiple = false,
  className,
  sx
}) => {
  const handleChange = (event: SelectChangeEvent<string | number>) => {
    onChange(event.target.value as string | number);
  };

  const labelId = label ? `${label.toLowerCase().replace(/\s+/g, '-')}-label` : undefined;

  return (
    <FormControl
      fullWidth={fullWidth}
      variant={variant as any}
      size={size as any}
      required={required}
      disabled={disabled}
      error={error}
      className={className}
    >
      {label && <InputLabel id={labelId}>{label}</InputLabel>}
      <Select
        labelId={labelId}
        value={value}
        onChange={handleChange}
        label={label}
        displayEmpty={!!placeholder}
        multiple={multiple}
        sx={sx}
      >
        {placeholder && (
          <MenuItem value="" disabled>
            <em>{placeholder}</em>
          </MenuItem>
        )}
        {options.map((option, index) => (
          <MenuItem
            key={`${option.value}-${index}`}
            value={option.value}
            disabled={option.disabled}
            divider={option.divider}
          >
            {option.label}
          </MenuItem>
        ))}
      </Select>
      {helperText && <FormHelperText>{helperText}</FormHelperText>}
    </FormControl>
  );
};

export default Dropdown;
