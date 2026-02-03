/**
 * Input Components - Flutter Parity Material Design
 * All input components in one file for efficiency
 *
 * @since 3.0.0-flutter-parity
 */

import React, { useState, useCallback, createContext, useContext, FormEvent } from 'react';
import { TextField as MuiTextField, Autocomplete as MuiAutocomplete, Select, MenuItem, FormControl, InputLabel, FormHelperText } from '@mui/material';
import type { TextFieldProps, TextFormFieldProps, FormProps, FormFieldProps, FormFieldState, AutocompleteProps, DropdownButtonProps } from './types';

// TextField Component
export const TextField: React.FC<TextFieldProps> = ({
  value,
  onChanged,
  decoration,
  maxLength,
  maxLines = 1,
  minLines,
  obscureText = false,
  enabled = true,
  ...rest
}) => {
  return (
    <MuiTextField
      value={value || ''}
      onChange={(e) => onChanged?.(e.target.value)}
      label={decoration?.labelText}
      placeholder={decoration?.hintText}
      helperText={decoration?.helperText || decoration?.errorText}
      error={!!decoration?.errorText}
      disabled={!enabled}
      type={obscureText ? 'password' : 'text'}
      multiline={maxLines > 1}
      rows={maxLines}
      minRows={minLines}
      variant={decoration?.border === 'outline' ? 'outlined' : decoration?.border === 'none' ? 'standard' : 'outlined'}
      InputProps={{
        startAdornment: decoration?.prefixIcon,
        endAdornment: decoration?.suffixIcon,
      }}
      inputProps={{ maxLength }}
      sx={{ width: '100%', ...rest.sx }}
      {...rest}
    />
  );
};

// TextFormField Component
export const TextFormField: React.FC<TextFormFieldProps> = ({
  value,
  onChanged,
  validator,
  onSaved,
  autovalidateMode = 'onUserInteraction',
  ...rest
}) => {
  const [error, setError] = useState<string | null>(null);
  const [touched, setTouched] = useState(false);

  const handleChange = (newValue: string) => {
    onChanged?.(newValue);
    if (autovalidateMode === 'always' || (autovalidateMode === 'onUserInteraction' && touched)) {
      const validationError = validator?.(newValue);
      setError(validationError || null);
    }
  };

  const handleBlur = () => {
    setTouched(true);
    if (validator) {
      const validationError = validator(value);
      setError(validationError || null);
    }
    onSaved?.(value);
  };

  return (
    <TextField
      {...rest}
      value={value}
      onChanged={handleChange}
      decoration={{
        ...rest.decoration,
        errorText: error || rest.decoration?.errorText,
      }}
      onBlur={handleBlur}
    />
  );
};

// Form Context
const FormContext = createContext<{
  autovalidateMode?: 'always' | 'onUserInteraction' | 'disabled';
  onChanged?: () => void;
}>({});

export const useFormContext = () => useContext(FormContext);

// Form Component
export const Form: React.FC<FormProps> = ({ children, onChanged, autovalidateMode = 'disabled' }) => {
  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    onChanged?.();
  };

  return (
    <FormContext.Provider value={{ autovalidateMode, onChanged }}>
      <form onSubmit={handleSubmit} style={{ width: '100%' }}>
        {children}
      </form>
    </FormContext.Provider>
  );
};

// FormField Component
export function FormField<T>({
  initialValue,
  onSaved,
  validator,
  builder,
  enabled = true,
  autovalidateMode = 'disabled',
}: FormFieldProps<T>) {
  const [value, setValue] = useState<T | undefined>(initialValue);
  const [errorText, setErrorText] = useState<string | null>(null);
  const formContext = useFormContext();

  const validate = useCallback((): boolean => {
    if (!validator) return true;
    const error = validator(value);
    setErrorText(error);
    return !error;
  }, [validator, value]);

  const save = useCallback(() => {
    onSaved?.(value);
  }, [onSaved, value]);

  const reset = useCallback(() => {
    setValue(initialValue);
    setErrorText(null);
  }, [initialValue]);

  const didChange = useCallback((newValue?: T) => {
    setValue(newValue);
    const mode = autovalidateMode || formContext.autovalidateMode;
    if (mode === 'always') {
      if (validator) {
        const error = validator(newValue);
        setErrorText(error);
      }
    }
    formContext.onChanged?.();
  }, [autovalidateMode, formContext, validator]);

  const state: FormFieldState<T> = {
    value,
    errorText,
    hasError: !!errorText,
    isValid: !errorText,
    save,
    validate,
    reset,
    didChange,
  };

  return <>{builder(state)}</>;
}

// Autocomplete Component
export function Autocomplete<T>({
  options,
  value,
  onChanged,
  getOptionLabel,
  renderOption,
  label,
  placeholder,
  enabled = true,
}: AutocompleteProps<T>) {
  return (
    <MuiAutocomplete
      options={options}
      value={value || null}
      onChange={(_, newValue) => onChanged?.(newValue)}
      getOptionLabel={getOptionLabel || ((option) => String(option))}
      renderOption={renderOption ? (props, option) => <li {...props}>{renderOption(option)}</li> : undefined}
      disabled={!enabled}
      renderInput={(params) => (
        <MuiTextField {...params} label={label} placeholder={placeholder} variant="outlined" />
      )}
      sx={{ width: '100%' }}
    />
  );
}

// DropdownButton Component
export function DropdownButton<T>({
  value,
  items,
  onChanged,
  hint,
  disabledHint,
  enabled = true,
  icon,
  underline = true,
}: DropdownButtonProps<T>) {
  return (
    <FormControl variant={underline ? 'outlined' : 'standard'} disabled={!enabled} sx={{ width: '100%', minWidth: 120 }}>
      {hint && <InputLabel>{hint}</InputLabel>}
      <Select
        value={value ?? ''}
        onChange={(e) => onChanged?.(e.target.value as T)}
        label={hint}
        IconComponent={icon ? () => <>{icon}</> : undefined}
      >
        {items.map((item, index) => (
          <MenuItem key={index} value={item.value as any} disabled={!item.enabled}>
            {item.label}
          </MenuItem>
        ))}
      </Select>
      {!enabled && disabledHint && <FormHelperText>{disabledHint}</FormHelperText>}
    </FormControl>
  );
}
