/**
 * UrlInput Component - URL input with validation
 *
 * @since 3.0.0-flutter-parity
 */

import React, { useState, useEffect } from 'react';
import { TextField, InputAdornment } from '@mui/material';
import { Link as LinkIcon, CheckCircle, Error } from '@mui/icons-material';
import type { UrlInputProps } from './types';

export const UrlInput: React.FC<UrlInputProps> = ({
  value = '',
  onChange,
  onValidationChange,
  label = 'URL',
  placeholder = 'https://example.com',
  required = false,
  disabled = false,
  validateOnChange = true,
  allowHttp = true,
}) => {
  const [isValid, setIsValid] = useState<boolean | null>(null);
  const [errorText, setErrorText] = useState<string>('');

  useEffect(() => {
    if (!validateOnChange || !value) {
      setIsValid(null);
      setErrorText('');
      onValidationChange?.(null);
      return;
    }

    try {
      const url = new URL(value);
      const validProtocol = allowHttp
        ? url.protocol === 'http:' || url.protocol === 'https:'
        : url.protocol === 'https:';

      if (!validProtocol) {
        setIsValid(false);
        setErrorText(allowHttp ? 'URL must start with http:// or https://' : 'URL must start with https://');
        onValidationChange?.(false);
      } else {
        setIsValid(true);
        setErrorText('');
        onValidationChange?.(true);
      }
    } catch {
      setIsValid(false);
      setErrorText('Invalid URL format');
      onValidationChange?.(false);
    }
  }, [value, validateOnChange, allowHttp, onValidationChange]);

  return (
    <TextField
      fullWidth
      label={label}
      placeholder={placeholder}
      value={value}
      onChange={(e) => onChange?.(e.target.value)}
      error={isValid === false}
      helperText={errorText}
      required={required}
      disabled={disabled}
      variant="outlined"
      type="url"
      InputProps={{
        startAdornment: (
          <InputAdornment position="start">
            <LinkIcon />
          </InputAdornment>
        ),
        endAdornment: isValid !== null && (
          <InputAdornment position="end">
            {isValid ? (
              <CheckCircle color="success" />
            ) : (
              <Error color="error" />
            )}
          </InputAdornment>
        ),
      }}
      inputProps={{
        'aria-label': label,
      }}
    />
  );
};
