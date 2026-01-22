import React, { useState, KeyboardEvent } from 'react';
import {
  Box,
  TextField,
  Chip,
  Typography,
  InputAdornment,
} from '@mui/material';

export interface TagInputProps {
  tags?: string[];
  inputValue?: string;
  label?: string;
  placeholder?: string;
  allowDuplicates?: boolean;
  maxTags?: number;
  disabled?: boolean;
  onTagAdd?: (tag: string) => void;
  onTagRemove?: (tag: string) => void;
  onInputChange?: (value: string) => void;
}

export const TagInput: React.FC<TagInputProps> = ({
  tags = [],
  inputValue = '',
  label,
  placeholder = 'Add tag',
  allowDuplicates = false,
  maxTags,
  disabled = false,
  onTagAdd,
  onTagRemove,
  onInputChange,
}) => {
  const [localInput, setLocalInput] = useState(inputValue);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setLocalInput(value);
    onInputChange?.(value);
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && localInput.trim()) {
      e.preventDefault();
      const canAdd =
        (allowDuplicates || !tags.includes(localInput.trim())) &&
        (maxTags === undefined || tags.length < maxTags);

      if (canAdd) {
        onTagAdd?.(localInput.trim());
        setLocalInput('');
        onInputChange?.('');
      }
    } else if (e.key === 'Backspace' && !localInput && tags.length > 0) {
      const lastTag = tags[tags.length - 1];
      onTagRemove?.(lastTag);
    }
  };

  const handleDelete = (tagToDelete: string) => {
    onTagRemove?.(tagToDelete);
  };

  const isDisabled = disabled || (maxTags !== undefined && tags.length >= maxTags);

  return (
    <Box>
      {label && (
        <Typography variant="caption" color="textSecondary" gutterBottom>
          {label}
        </Typography>
      )}

      {tags.length > 0 && (
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mb: 1 }}>
          {tags.map((tag, index) => (
            <Chip
              key={`${tag}-${index}`}
              label={tag}
              size="small"
              onDelete={disabled ? undefined : () => handleDelete(tag)}
              disabled={disabled}
            />
          ))}
        </Box>
      )}

      <TextField
        fullWidth
        size="small"
        value={localInput}
        onChange={handleInputChange}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        disabled={isDisabled}
        helperText={
          maxTags ? `${tags.length}/${maxTags} tags` : undefined
        }
      />
    </Box>
  );
};

export default TagInput;
