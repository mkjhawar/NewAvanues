import React from 'react';
import { TextField, InputAdornment, IconButton, TextFieldProps } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import ClearIcon from '@mui/icons-material/Clear';

/**
 * SearchBar - Search Input Component
 *
 * A specialized text field for search functionality with search icon and clear button.
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export enum SearchBarVariant {
  OUTLINED = 'outlined',
  FILLED = 'filled',
  STANDARD = 'standard'
}

export enum SearchBarSize {
  SMALL = 'small',
  MEDIUM = 'medium'
}

export interface SearchBarProps {
  /** Current search value */
  value: string;
  /** Change handler */
  onChange: (value: string) => void;
  /** Placeholder text */
  placeholder?: string;
  /** Search handler (called on Enter or search icon click) */
  onSearch?: (value: string) => void;
  /** Visual variant */
  variant?: SearchBarVariant;
  /** Size */
  size?: SearchBarSize;
  /** Whether the field is disabled */
  disabled?: boolean;
  /** Whether to show the clear button */
  showClearButton?: boolean;
  /** Whether to show the search icon */
  showSearchIcon?: boolean;
  /** Whether to take full width */
  fullWidth?: boolean;
  /** Debounce delay in milliseconds (0 = no debounce) */
  debounceMs?: number;
  /** Additional CSS class name */
  className?: string;
  /** Custom styles */
  sx?: TextFieldProps['sx'];
}

/**
 * SearchBar component for search functionality
 *
 * @example
 * ```tsx
 * // Basic search bar
 * const [query, setQuery] = useState('');
 * <SearchBar
 *   value={query}
 *   onChange={setQuery}
 *   placeholder="Search products..."
 * />
 *
 * // With search handler
 * <SearchBar
 *   value={query}
 *   onChange={setQuery}
 *   onSearch={(q) => performSearch(q)}
 *   placeholder="Search..."
 * />
 *
 * // With debouncing
 * <SearchBar
 *   value={query}
 *   onChange={setQuery}
 *   onSearch={handleSearch}
 *   debounceMs={300}
 *   placeholder="Type to search..."
 * />
 *
 * // Compact variant
 * <SearchBar
 *   value={query}
 *   onChange={setQuery}
 *   variant="filled"
 *   size="small"
 *   fullWidth={false}
 * />
 * ```
 */
export const SearchBar: React.FC<SearchBarProps> = ({
  value,
  onChange,
  placeholder = 'Search...',
  onSearch,
  variant = SearchBarVariant.OUTLINED,
  size = SearchBarSize.MEDIUM,
  disabled = false,
  showClearButton = true,
  showSearchIcon = true,
  fullWidth = true,
  debounceMs = 0,
  className,
  sx
}) => {
  const [debounceTimer, setDebounceTimer] = React.useState<NodeJS.Timeout | null>(null);

  const handleChange = (newValue: string) => {
    onChange(newValue);

    if (debounceMs > 0 && onSearch) {
      if (debounceTimer) {
        clearTimeout(debounceTimer);
      }
      const timer = setTimeout(() => {
        onSearch(newValue);
      }, debounceMs);
      setDebounceTimer(timer);
    }
  };

  const handleClear = () => {
    onChange('');
    if (onSearch) {
      onSearch('');
    }
  };

  const handleSearchClick = () => {
    if (onSearch) {
      onSearch(value);
    }
  };

  const handleKeyPress = (event: React.KeyboardEvent) => {
    if (event.key === 'Enter' && onSearch) {
      onSearch(value);
    }
  };

  return (
    <TextField
      value={value}
      onChange={(e) => handleChange(e.target.value)}
      onKeyPress={handleKeyPress}
      placeholder={placeholder}
      variant={variant as TextFieldProps['variant']}
      size={size as TextFieldProps['size']}
      disabled={disabled}
      fullWidth={fullWidth}
      className={className}
      sx={sx}
      InputProps={{
        startAdornment: showSearchIcon ? (
          <InputAdornment position="start">
            <IconButton
              onClick={handleSearchClick}
              disabled={disabled}
              size="small"
              aria-label="search"
            >
              <SearchIcon />
            </IconButton>
          </InputAdornment>
        ) : undefined,
        endAdornment: showClearButton && value ? (
          <InputAdornment position="end">
            <IconButton
              onClick={handleClear}
              disabled={disabled}
              size="small"
              aria-label="clear"
            >
              <ClearIcon />
            </IconButton>
          </InputAdornment>
        ) : undefined
      }}
    />
  );
};

export default SearchBar;
