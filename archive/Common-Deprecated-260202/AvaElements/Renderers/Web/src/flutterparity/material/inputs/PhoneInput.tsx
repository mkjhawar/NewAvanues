/**
 * PhoneInput Component - Phone number with country code
 *
 * @since 3.0.0-flutter-parity
 */

import React, { useState } from 'react';
import { FormControl, InputLabel, Select, MenuItem, TextField, Box } from '@mui/material';
import type { PhoneInputProps } from './types';

const countryCodes = [
  { code: '+1', country: 'US/CA', flag: '🇺🇸' },
  { code: '+44', country: 'UK', flag: '🇬🇧' },
  { code: '+91', country: 'IN', flag: '🇮🇳' },
  { code: '+86', country: 'CN', flag: '🇨🇳' },
  { code: '+81', country: 'JP', flag: '🇯🇵' },
  { code: '+49', country: 'DE', flag: '🇩🇪' },
  { code: '+33', country: 'FR', flag: '🇫🇷' },
  { code: '+39', country: 'IT', flag: '🇮🇹' },
  { code: '+61', country: 'AU', flag: '🇦🇺' },
  { code: '+55', country: 'BR', flag: '🇧🇷' },
];

export const PhoneInput: React.FC<PhoneInputProps> = ({
  value = '',
  onChange,
  countryCode = '+1',
  onCountryCodeChange,
  label = 'Phone Number',
  placeholder = 'Enter phone number',
  error,
  helperText,
  disabled = false,
  required = false,
}) => {
  const [localCountryCode, setLocalCountryCode] = useState(countryCode);

  const handleCountryCodeChange = (code: string) => {
    setLocalCountryCode(code);
    onCountryCodeChange?.(code);
  };

  const handlePhoneChange = (phone: string) => {
    // Allow only digits, spaces, and dashes
    const cleaned = phone.replace(/[^\d\s-]/g, '');
    onChange?.(cleaned);
  };

  return (
    <Box sx={{ display: 'flex', gap: 1, alignItems: 'flex-start', width: '100%' }}>
      <FormControl sx={{ minWidth: 120 }} disabled={disabled}>
        <InputLabel id="country-code-label">Code</InputLabel>
        <Select
          labelId="country-code-label"
          value={localCountryCode}
          onChange={(e) => handleCountryCodeChange(e.target.value)}
          label="Code"
        >
          {countryCodes.map(({ code, country, flag }) => (
            <MenuItem key={code} value={code}>
              {flag} {code} {country}
            </MenuItem>
          ))}
        </Select>
      </FormControl>
      <TextField
        fullWidth
        label={label}
        placeholder={placeholder}
        value={value}
        onChange={(e) => handlePhoneChange(e.target.value)}
        error={error}
        helperText={helperText}
        disabled={disabled}
        required={required}
        variant="outlined"
        type="tel"
        inputProps={{
          'aria-label': label,
        }}
      />
    </Box>
  );
};
