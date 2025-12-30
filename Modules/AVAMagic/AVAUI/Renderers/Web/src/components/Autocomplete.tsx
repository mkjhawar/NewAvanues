import React from 'react';
import { Autocomplete as MuiAutocomplete, TextField } from '@mui/material';

export interface AutocompleteOption {
  label: string;
  value: string | number;
}

export interface AutocompleteProps {
  options: AutocompleteOption[];
  value?: AutocompleteOption | null;
  onChange?: (option: AutocompleteOption | null) => void;
  label?: string;
  placeholder?: string;
  disabled?: boolean;
  loading?: boolean;
  freeSolo?: boolean;
  multiple?: boolean;
}

export const Autocomplete: React.FC<AutocompleteProps> = ({
  options,
  value,
  onChange,
  label,
  placeholder,
  disabled = false,
  loading = false,
  freeSolo = false,
  multiple = false,
}) => {
  return (
    <MuiAutocomplete
      options={options}
      value={value}
      onChange={(_, newValue) => onChange?.(newValue as AutocompleteOption | null)}
      getOptionLabel={(option) => option.label}
      disabled={disabled}
      loading={loading}
      freeSolo={freeSolo}
      multiple={multiple}
      renderInput={(params) => (
        <TextField
          {...params}
          label={label}
          placeholder={placeholder}
          variant="outlined"
        />
      )}
      isOptionEqualToValue={(option, value) => option.value === value.value}
    />
  );
};

export default Autocomplete;
