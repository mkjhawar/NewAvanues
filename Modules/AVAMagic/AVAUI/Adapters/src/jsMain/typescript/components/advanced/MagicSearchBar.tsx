import React from 'react';
import { TextField, InputAdornment, IconButton } from '@mui/material';
import { Search, Clear } from '@mui/icons-material';

/**
 * MagicSearchBar - React Search Bar Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface MagicSearchBarProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  onSearch?: () => void;
  className?: string;
}

export const MagicSearchBar: React.FC<MagicSearchBarProps> = ({
  value,
  onChange,
  placeholder = 'Search...',
  onSearch,
  className
}) => {
  return (
    <TextField
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder}
      fullWidth
      className={className}
      InputProps={{
        startAdornment: (
          <InputAdornment position="start">
            <Search />
          </InputAdornment>
        ),
        endAdornment: value && (
          <InputAdornment position="end">
            <IconButton size="small" onClick={() => onChange('')}>
              <Clear />
            </IconButton>
          </InputAdornment>
        )
      }}
      onKeyPress={(e) => {
        if (e.key === 'Enter' && onSearch) {
          onSearch();
        }
      }}
    />
  );
};

export default MagicSearchBar;
