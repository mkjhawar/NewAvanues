/**
 * Input Component Type Definitions
 *
 * @since 3.0.0-flutter-parity
 */

import type { ReactNode } from 'react';
import type { TextFieldProps as MuiTextFieldProps, AutocompleteProps as MuiAutocompleteProps } from '@mui/material';

export interface TextFieldProps extends Omit<MuiTextFieldProps, 'onChange'> {
  value?: string;
  onChanged?: (value: string) => void;
  decoration?: InputDecoration;
  maxLength?: number;
  maxLines?: number;
  minLines?: number;
  obscureText?: boolean;
  autocorrect?: boolean;
  enabled?: boolean;
}

export interface TextFormFieldProps extends TextFieldProps {
  validator?: (value?: string) => string | null;
  onSaved?: (value?: string) => void;
  autovalidateMode?: 'always' | 'onUserInteraction' | 'disabled';
}

export interface InputDecoration {
  labelText?: string;
  hintText?: string;
  helperText?: string;
  errorText?: string;
  prefixIcon?: ReactNode;
  suffixIcon?: ReactNode;
  border?: 'outline' | 'underline' | 'none';
  filled?: boolean;
}

export interface FormProps {
  children: ReactNode;
  onChanged?: () => void;
  autovalidateMode?: 'always' | 'onUserInteraction' | 'disabled';
}

export interface FormFieldProps<T> {
  initialValue?: T;
  onSaved?: (value?: T) => void;
  validator?: (value?: T) => string | null;
  builder: (state: FormFieldState<T>) => ReactNode;
  enabled?: boolean;
  autovalidateMode?: 'always' | 'onUserInteraction' | 'disabled';
}

export interface FormFieldState<T> {
  value?: T;
  errorText?: string | null;
  hasError: boolean;
  isValid: boolean;
  save: () => void;
  validate: () => boolean;
  reset: () => void;
  didChange: (value?: T) => void;
}

export interface AutocompleteProps<T> {
  options: T[];
  value?: T | null;
  onChanged?: (value: T | null) => void;
  getOptionLabel?: (option: T) => string;
  renderOption?: (option: T) => ReactNode;
  label?: string;
  placeholder?: string;
  enabled?: boolean;
}

export interface DropdownButtonProps<T> {
  value?: T;
  items: Array<{ value: T; label: string; enabled?: boolean }>;
  onChanged?: (value?: T) => void;
  hint?: string;
  disabledHint?: string;
  enabled?: boolean;
  icon?: ReactNode;
  underline?: boolean;
}

// Advanced Input Types

export interface PhoneInputProps {
  value?: string;
  onChange?: (value: string) => void;
  countryCode?: string;
  onCountryCodeChange?: (code: string) => void;
  label?: string;
  placeholder?: string;
  error?: boolean;
  helperText?: string;
  disabled?: boolean;
  required?: boolean;
}

export interface UrlInputProps {
  value?: string;
  onChange?: (value: string) => void;
  onValidationChange?: (isValid: boolean | null) => void;
  label?: string;
  placeholder?: string;
  required?: boolean;
  disabled?: boolean;
  validateOnChange?: boolean;
  allowHttp?: boolean;
}

export interface ComboBoxProps {
  options: any[];
  value?: any;
  onChange?: (value: any) => void;
  label?: string;
  placeholder?: string;
  multiple?: boolean;
  disabled?: boolean;
  required?: boolean;
  freeSolo?: boolean;
  filterSelectedOptions?: boolean;
  renderTags?: (value: any[], getTagProps: any) => ReactNode;
  getOptionLabel?: (option: any) => string;
}

export interface PinInputProps {
  length?: number;
  value?: string;
  onChange?: (value: string) => void;
  onComplete?: (value: string) => void;
  mask?: boolean;
  autoFocus?: boolean;
  disabled?: boolean;
  error?: boolean;
  helperText?: string;
}

export interface OTPInputProps {
  length?: number;
  value?: string;
  onChange?: (value: string) => void;
  onComplete?: (value: string) => void;
  autoFocus?: boolean;
  disabled?: boolean;
  error?: boolean;
  helperText?: string;
  resendEnabled?: boolean;
  onResend?: () => void;
  resendText?: string;
}

export interface MaskInputProps {
  value?: string;
  onChange?: (value: string) => void;
  mask?: string;
  maskType?: 'phone' | 'ssn' | 'date' | 'time' | 'creditCard' | 'zip' | 'zipPlus4';
  placeholder?: string;
  label?: string;
  disabled?: boolean;
  required?: boolean;
  error?: boolean;
  helperText?: string;
}

export interface RichTextEditorProps {
  value?: string;
  onChange?: (value: string) => void;
  placeholder?: string;
  toolbar?: ('bold' | 'italic' | 'underline' | 'ul' | 'ol' | 'link')[];
  minHeight?: number;
  maxHeight?: number;
  disabled?: boolean;
}

export interface MarkdownEditorProps {
  value?: string;
  onChange?: (value: string) => void;
  placeholder?: string;
  minHeight?: number;
  showPreview?: boolean;
  splitView?: boolean;
  disabled?: boolean;
}

export interface CodeEditorProps {
  value?: string;
  onChange?: (value: string) => void;
  language?: string;
  placeholder?: string;
  minHeight?: number;
  showLineNumbers?: boolean;
  disabled?: boolean;
  theme?: 'light' | 'dark';
}

export interface FormSectionProps {
  title?: string;
  description?: string;
  children: ReactNode;
  divider?: boolean;
  elevation?: number;
  variant?: 'default' | 'paper' | 'outlined';
  spacing?: number;
}

export interface MultiSelectProps {
  options: Array<{ value: string; label: string; disabled?: boolean }>;
  value?: string[];
  onChange?: (value: string[]) => void;
  label?: string;
  placeholder?: string;
  searchable?: boolean;
  maxItems?: number;
  disabled?: boolean;
  required?: boolean;
  showCheckbox?: boolean;
  renderValue?: (selected: string[]) => ReactNode;
}
