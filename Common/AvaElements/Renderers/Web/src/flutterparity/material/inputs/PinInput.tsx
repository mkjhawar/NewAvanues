/**
 * PinInput Component - PIN/code entry with individual boxes
 *
 * @since 3.0.0-flutter-parity
 */

import React, { useRef, useEffect } from 'react';
import { Box, TextField } from '@mui/material';
import type { PinInputProps } from './types';

export const PinInput: React.FC<PinInputProps> = ({
  length = 4,
  value = '',
  onChange,
  onComplete,
  mask = false,
  autoFocus = false,
  disabled = false,
  error = false,
  helperText,
}) => {
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  useEffect(() => {
    if (autoFocus && inputRefs.current[0]) {
      inputRefs.current[0].focus();
    }
  }, [autoFocus]);

  const handleChange = (index: number, digit: string) => {
    // Allow only digits
    if (!/^\d*$/.test(digit)) return;

    const valueArray = value.split('');
    valueArray[index] = digit;
    const newValue = valueArray.join('').slice(0, length);

    onChange?.(newValue);

    // Auto-focus next input
    if (digit && index < length - 1) {
      inputRefs.current[index + 1]?.focus();
    }

    // Check if complete
    if (newValue.length === length) {
      onComplete?.(newValue);
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace') {
      const valueArray = value.split('');
      if (!valueArray[index] && index > 0) {
        // If current box is empty, move to previous box
        inputRefs.current[index - 1]?.focus();
      } else {
        // Clear current box
        valueArray[index] = '';
        onChange?.(valueArray.join(''));
      }
    } else if (e.key === 'ArrowLeft' && index > 0) {
      inputRefs.current[index - 1]?.focus();
    } else if (e.key === 'ArrowRight' && index < length - 1) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handlePaste = (e: React.ClipboardEvent) => {
    e.preventDefault();
    const pastedData = e.clipboardData.getData('text/plain').replace(/\D/g, '').slice(0, length);
    onChange?.(pastedData);

    if (pastedData.length === length) {
      onComplete?.(pastedData);
      inputRefs.current[length - 1]?.focus();
    } else if (pastedData.length > 0) {
      inputRefs.current[Math.min(pastedData.length, length - 1)]?.focus();
    }
  };

  return (
    <Box>
      <Box
        sx={{
          display: 'flex',
          gap: 1,
          justifyContent: 'center',
          alignItems: 'center',
        }}
      >
        {Array.from({ length }).map((_, index) => (
          <TextField
            key={index}
            inputRef={(el) => (inputRefs.current[index] = el)}
            type={mask ? 'password' : 'text'}
            inputProps={{
              inputMode: 'numeric',
              maxLength: 1,
              style: {
                textAlign: 'center',
                fontSize: '1.5rem',
                fontWeight: 'bold',
              },
              'aria-label': `PIN digit ${index + 1}`,
            }}
            value={value[index] || ''}
            onChange={(e) => handleChange(index, e.target.value)}
            onKeyDown={(e) => handleKeyDown(index, e)}
            onPaste={handlePaste}
            disabled={disabled}
            error={error}
            sx={{
              width: 56,
              '& input': {
                padding: '12px',
              },
            }}
          />
        ))}
      </Box>
      {helperText && (
        <Box
          sx={{
            mt: 1,
            fontSize: '0.75rem',
            color: error ? 'error.main' : 'text.secondary',
            textAlign: 'center',
          }}
        >
          {helperText}
        </Box>
      )}
    </Box>
  );
};
