import React, { useState } from 'react';
import {
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Checkbox,
  ListItemText,
  OutlinedInput,
  Chip,
  Box,
  TextField,
  InputAdornment,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';

export interface MultiSelectProps {
  options: Array<{ value: string; label: string }>;
  selectedValues?: string[];
  label?: string;
  placeholder?: string;
  searchable?: boolean;
  maxSelections?: number;
  disabled?: boolean;
  onChange?: (values: string[]) => void;
}

export const MultiSelect: React.FC<MultiSelectProps> = ({
  options,
  selectedValues = [],
  label,
  placeholder = 'Select items',
  searchable = true,
  maxSelections,
  disabled = false,
  onChange,
}) => {
  const [searchQuery, setSearchQuery] = useState('');

  const handleChange = (event: any) => {
    const value = event.target.value as string[];
    if (maxSelections && value.length > maxSelections) {
      return;
    }
    onChange?.(value);
  };

  const filteredOptions = options.filter((option) =>
    searchQuery === '' || option.label.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <FormControl fullWidth disabled={disabled}>
      {label && <InputLabel>{label}</InputLabel>}
      <Select
        multiple
        value={selectedValues}
        onChange={handleChange}
        input={<OutlinedInput label={label} />}
        renderValue={(selected) => (
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
            {(selected as string[]).map((value) => {
              const option = options.find((o) => o.value === value);
              return <Chip key={value} label={option?.label || value} size="small" />;
            })}
          </Box>
        )}
        MenuProps={{
          PaperProps: {
            style: {
              maxHeight: 300,
            },
          },
        }}
      >
        {searchable && (
          <Box sx={{ p: 1, position: 'sticky', top: 0, bgcolor: 'background.paper', zIndex: 1 }}>
            <TextField
              size="small"
              fullWidth
              placeholder="Search..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onClick={(e) => e.stopPropagation()}
              onKeyDown={(e) => e.stopPropagation()}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon fontSize="small" />
                  </InputAdornment>
                ),
              }}
            />
          </Box>
        )}
        {filteredOptions.map((option) => (
          <MenuItem
            key={option.value}
            value={option.value}
            disabled={
              maxSelections !== undefined &&
              selectedValues.length >= maxSelections &&
              !selectedValues.includes(option.value)
            }
          >
            <Checkbox checked={selectedValues.includes(option.value)} />
            <ListItemText primary={option.label} />
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
};

export default MultiSelect;
