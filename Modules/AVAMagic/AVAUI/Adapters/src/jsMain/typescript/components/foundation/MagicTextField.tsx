import React from 'react';
import { TextField, TextFieldProps, InputAdornment } from '@mui/material';

/**
 * MagicTextField - React/Material-UI TextField Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export enum TextFieldType {
  TEXT = 'text',
  EMAIL = 'email',
  PASSWORD = 'password',
  NUMBER = 'number',
  PHONE = 'tel',
  URL = 'url'
}

export interface MagicTextFieldProps {
  value: string;
  onValueChange: (value: string) => void;
  label?: string;
  placeholder?: string;
  enabled?: boolean;
  readOnly?: boolean;
  type?: TextFieldType;
  error?: string;
  helperText?: string;
  leadingIcon?: React.ReactNode;
  trailingIcon?: React.ReactNode;
  maxLines?: number;
  className?: string;
}

export const MagicTextField: React.FC<MagicTextFieldProps> = ({
  value,
  onValueChange,
  label,
  placeholder,
  enabled = true,
  readOnly = false,
  type = TextFieldType.TEXT,
  error,
  helperText,
  leadingIcon,
  trailingIcon,
  maxLines = 1,
  className
}) => {
  const textFieldProps: TextFieldProps = {
    value,
    onChange: (e) => onValueChange(e.target.value),
    label,
    placeholder,
    disabled: !enabled,
    type: type as any,
    error: !!error,
    helperText: error || helperText,
    multiline: maxLines > 1,
    rows: maxLines > 1 ? maxLines : undefined,
    fullWidth: true,
    className,
    InputProps: {
      readOnly,
      startAdornment: leadingIcon ? (
        <InputAdornment position="start">{leadingIcon}</InputAdornment>
      ) : undefined,
      endAdornment: trailingIcon ? (
        <InputAdornment position="end">{trailingIcon}</InputAdornment>
      ) : undefined
    }
  };

  return <TextField {...textFieldProps} />;
};

export default MagicTextField;
