/**
 * MultiSelect Component - Multi-value select with chips
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import {
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Chip,
  Box,
  Checkbox,
  ListItemText,
  OutlinedInput,
} from '@mui/material';
import type { MultiSelectProps } from './types';

export const MultiSelect: React.FC<MultiSelectProps> = ({
  options,
  value = [],
  onChange,
  label = 'Select Items',
  placeholder = 'Select...',
  searchable = false,
  maxItems,
  disabled = false,
  required = false,
  showCheckbox = true,
  renderValue,
}) => {
  const handleChange = (selected: string[]) => {
    if (maxItems && selected.length > maxItems) {
      return; // Don't allow more than maxItems
    }
    onChange?.(selected);
  };

  const defaultRenderValue = (selected: string[]) => (
    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
      {selected.map((item) => {
        const option = options.find((opt) => opt.value === item);
        return (
          <Chip
            key={item}
            label={option?.label || item}
            size="small"
            onMouseDown={(e) => {
              e.stopPropagation(); // Prevent dropdown from opening
            }}
            onDelete={() => {
              handleChange(selected.filter((val) => val !== item));
            }}
          />
        );
      })}
    </Box>
  );

  return (
    <FormControl fullWidth disabled={disabled} required={required}>
      <InputLabel id="multi-select-label">{label}</InputLabel>
      <Select
        labelId="multi-select-label"
        multiple
        value={value}
        onChange={(e) => handleChange(e.target.value as string[])}
        input={<OutlinedInput label={label} />}
        renderValue={renderValue || defaultRenderValue}
        MenuProps={{
          PaperProps: {
            style: {
              maxHeight: 300,
            },
          },
        }}
      >
        {options.map((option) => (
          <MenuItem
            key={option.value}
            value={option.value}
            disabled={option.disabled}
          >
            {showCheckbox && (
              <Checkbox checked={value.indexOf(option.value) > -1} />
            )}
            <ListItemText primary={option.label} />
          </MenuItem>
        ))}
      </Select>
      {maxItems && (
        <Box
          sx={{
            mt: 0.5,
            fontSize: '0.75rem',
            color: 'text.secondary',
          }}
        >
          {value.length}/{maxItems} items selected
        </Box>
      )}
    </FormControl>
  );
};
