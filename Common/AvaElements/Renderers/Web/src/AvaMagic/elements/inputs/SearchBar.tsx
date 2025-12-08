/**
 * SearchBar Component - Phase 3 Input Component
 *
 * Search input with icon
 * Matches Android/iOS SearchBar behavior
 *
 * @package com.augmentalis.AvaMagic.elements.inputs
 * @since 3.0.0-phase3
 */

import React from 'react';
import {
  TextField,
  InputAdornment,
  IconButton,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import ClearIcon from '@mui/icons-material/Clear';

export interface SearchBarProps {
  /** Current search value */
  value: string;
  /** Change handler */
  onChange: (value: string) => void;
  /** Search submit handler */
  onSearch?: (value: string) => void;
  /** Clear handler */
  onClear?: () => void;
  /** Placeholder text */
  placeholder?: string;
  /** Disabled state */
  disabled?: boolean;
  /** Show clear button */
  showClearButton?: boolean;
  /** Variant */
  variant?: 'outlined' | 'filled' | 'standard';
  /** Full width */
  fullWidth?: boolean;
  /** Size */
  size?: 'small' | 'medium';
  /** Auto focus */
  autoFocus?: boolean;
  /** Custom class name */
  className?: string;
}

export const SearchBar: React.FC<SearchBarProps> = ({
  value,
  onChange,
  onSearch,
  onClear,
  placeholder = 'Search...',
  disabled = false,
  showClearButton = true,
  variant = 'outlined',
  fullWidth = true,
  size = 'medium',
  autoFocus = false,
  className,
}) => {
  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    onChange(event.target.value);
  };

  const handleKeyPress = (event: React.KeyboardEvent) => {
    if (event.key === 'Enter' && onSearch) {
      onSearch(value);
    }
  };

  const handleClear = () => {
    onChange('');
    onClear?.();
  };

  return (
    <TextField
      value={value}
      onChange={handleChange}
      onKeyPress={handleKeyPress}
      placeholder={placeholder}
      disabled={disabled}
      variant={variant}
      fullWidth={fullWidth}
      size={size}
      autoFocus={autoFocus}
      className={className}
      InputProps={{
        startAdornment: (
          <InputAdornment position="start">
            <SearchIcon />
          </InputAdornment>
        ),
        endAdornment: showClearButton && value && (
          <InputAdornment position="end">
            <IconButton
              onClick={handleClear}
              edge="end"
              size="small"
              aria-label="Clear search"
            >
              <ClearIcon />
            </IconButton>
          </InputAdornment>
        ),
      }}
    />
  );
};

export default SearchBar;
