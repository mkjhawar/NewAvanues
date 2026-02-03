/**
 * FileUpload Component - Phase 3 Input Component
 *
 * File selection and upload
 * Matches Android/iOS FileUpload behavior
 *
 * @package com.augmentalis.AvaMagic.elements.inputs
 * @since 3.0.0-phase3
 */

import React, { useRef } from 'react';
import { Button, Box, Typography, Chip } from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import CloseIcon from '@mui/icons-material/Close';

export interface FileUploadProps {
  /** Selected files */
  files: File[];
  /** Change handler */
  onChange: (files: File[]) => void;
  /** Accept file types */
  accept?: string;
  /** Multiple files */
  multiple?: boolean;
  /** Maximum file size in bytes */
  maxSize?: number;
  /** Maximum number of files */
  maxFiles?: number;
  /** Disabled state */
  disabled?: boolean;
  /** Button label */
  buttonLabel?: string;
  /** Helper text */
  helperText?: string;
  /** Error state */
  error?: boolean;
  /** Error message */
  errorMessage?: string;
  /** Custom class name */
  className?: string;
}

export const FileUpload: React.FC<FileUploadProps> = ({
  files,
  onChange,
  accept,
  multiple = false,
  maxSize,
  maxFiles,
  disabled = false,
  buttonLabel = 'Choose File',
  helperText,
  error = false,
  errorMessage,
  className,
}) => {
  const inputRef = useRef<HTMLInputElement>(null);

  const handleClick = () => {
    inputRef.current?.click();
  };

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = Array.from(event.target.files || []);

    // Validate max files
    if (maxFiles && selectedFiles.length > maxFiles) {
      return;
    }

    // Validate file sizes
    if (maxSize) {
      const validFiles = selectedFiles.filter(file => file.size <= maxSize);
      if (validFiles.length !== selectedFiles.length) {
        return;
      }
    }

    onChange(selectedFiles);
  };

  const handleRemove = (index: number) => {
    const newFiles = files.filter((_, i) => i !== index);
    onChange(newFiles);
  };

  return (
    <Box className={className}>
      <input
        ref={inputRef}
        type="file"
        accept={accept}
        multiple={multiple}
        onChange={handleChange}
        disabled={disabled}
        style={{ display: 'none' }}
      />
      <Button
        variant="outlined"
        startIcon={<CloudUploadIcon />}
        onClick={handleClick}
        disabled={disabled}
        color={error ? 'error' : 'primary'}
      >
        {buttonLabel}
      </Button>

      {(helperText || errorMessage) && (
        <Typography
          variant="caption"
          color={error ? 'error' : 'text.secondary'}
          sx={{ display: 'block', mt: 0.5 }}
        >
          {error ? errorMessage : helperText}
        </Typography>
      )}

      {files.length > 0 && (
        <Box sx={{ mt: 2, display: 'flex', flexWrap: 'wrap', gap: 1 }}>
          {files.map((file, index) => (
            <Chip
              key={index}
              label={file.name}
              onDelete={() => handleRemove(index)}
              deleteIcon={<CloseIcon />}
            />
          ))}
        </Box>
      )}
    </Box>
  );
};

export default FileUpload;
