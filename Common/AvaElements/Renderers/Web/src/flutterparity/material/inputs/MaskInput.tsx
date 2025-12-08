/**
 * MaskInput Component - Formatted input with mask patterns
 *
 * @since 3.0.0-flutter-parity
 */

import React, { useState, useEffect } from 'react';
import { TextField } from '@mui/material';
import type { MaskInputProps } from './types';

const masks = {
  phone: '(###) ###-####',
  ssn: '###-##-####',
  date: '##/##/####',
  time: '##:##',
  creditCard: '#### #### #### ####',
  zip: '#####',
  zipPlus4: '#####-####',
};

export const MaskInput: React.FC<MaskInputProps> = ({
  value = '',
  onChange,
  mask,
  maskType,
  placeholder,
  label,
  disabled = false,
  required = false,
  error,
  helperText,
}) => {
  const [displayValue, setDisplayValue] = useState('');

  const maskPattern = maskType ? masks[maskType] : mask;

  useEffect(() => {
    if (!maskPattern) {
      setDisplayValue(value);
      return;
    }

    // Remove all non-digit characters from value
    const cleanValue = value.replace(/\D/g, '');

    // Apply mask
    let formatted = '';
    let valueIndex = 0;

    for (let i = 0; i < maskPattern.length && valueIndex < cleanValue.length; i++) {
      if (maskPattern[i] === '#') {
        formatted += cleanValue[valueIndex];
        valueIndex++;
      } else {
        formatted += maskPattern[i];
      }
    }

    setDisplayValue(formatted);
  }, [value, maskPattern]);

  const handleChange = (input: string) => {
    // Extract only digits
    const cleanValue = input.replace(/\D/g, '');
    onChange?.(cleanValue);
  };

  const defaultPlaceholder = maskPattern || placeholder;

  return (
    <TextField
      fullWidth
      label={label}
      placeholder={defaultPlaceholder}
      value={displayValue}
      onChange={(e) => handleChange(e.target.value)}
      disabled={disabled}
      required={required}
      error={error}
      helperText={helperText}
      variant="outlined"
      inputProps={{
        'aria-label': label || 'Masked input',
      }}
    />
  );
};
