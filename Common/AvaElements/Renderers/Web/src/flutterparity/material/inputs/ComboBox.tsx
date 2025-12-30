/**
 * ComboBox Component - Searchable dropdown
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { Autocomplete, TextField, Chip } from '@mui/material';
import type { ComboBoxProps } from './types';

export const ComboBox: React.FC<ComboBoxProps> = ({
  options,
  value,
  onChange,
  label = 'Select',
  placeholder = 'Search...',
  multiple = false,
  disabled = false,
  required = false,
  freeSolo = false,
  filterSelectedOptions = true,
  renderTags,
  getOptionLabel = (option) => typeof option === 'string' ? option : option.label || String(option),
}) => {
  return (
    <Autocomplete
      multiple={multiple}
      options={options}
      value={value}
      onChange={(_, newValue) => onChange?.(newValue)}
      disabled={disabled}
      freeSolo={freeSolo}
      filterSelectedOptions={filterSelectedOptions}
      getOptionLabel={getOptionLabel}
      renderTags={
        renderTags ||
        (multiple
          ? (tagValue, getTagProps) =>
              (tagValue as any[]).map((option, index) => (
                <Chip
                  label={getOptionLabel(option)}
                  {...getTagProps({ index })}
                  key={index}
                />
              ))
          : undefined)
      }
      renderInput={(params) => (
        <TextField
          {...params}
          label={label}
          placeholder={placeholder}
          required={required}
          variant="outlined"
        />
      )}
      sx={{ width: '100%' }}
    />
  );
};
