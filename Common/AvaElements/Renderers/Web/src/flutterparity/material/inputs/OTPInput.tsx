/**
 * OTPInput Component - One-time password input (6 digits default)
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { PinInput } from './PinInput';
import type { OTPInputProps } from './types';

export const OTPInput: React.FC<OTPInputProps> = ({
  length = 6,
  value = '',
  onChange,
  onComplete,
  autoFocus = true,
  disabled = false,
  error = false,
  helperText = 'Enter the 6-digit code sent to your device',
  resendEnabled = false,
  onResend,
  resendText = 'Resend Code',
}) => {
  return (
    <div style={{ width: '100%', textAlign: 'center' }}>
      <PinInput
        length={length}
        value={value}
        onChange={onChange}
        onComplete={onComplete}
        mask={false}
        autoFocus={autoFocus}
        disabled={disabled}
        error={error}
        helperText={helperText}
      />
      {resendEnabled && onResend && (
        <div style={{ marginTop: '16px' }}>
          <button
            type="button"
            onClick={onResend}
            disabled={disabled}
            style={{
              background: 'none',
              border: 'none',
              color: '#1976d2',
              cursor: 'pointer',
              textDecoration: 'underline',
              fontSize: '0.875rem',
              padding: '8px',
            }}
            aria-label={resendText}
          >
            {resendText}
          </button>
        </div>
      )}
    </div>
  );
};
