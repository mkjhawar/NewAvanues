/**
 * ImagePicker Component - Phase 3 Input Component
 *
 * Image file selection with preview
 * Matches Android/iOS ImagePicker behavior
 *
 * @package com.augmentalis.AvaMagic.elements.inputs
 * @since 3.0.0-phase3
 */

import React, { useRef, useState } from 'react';
import { Box, Button, IconButton, Paper } from '@mui/material';
import PhotoCameraIcon from '@mui/icons-material/PhotoCamera';
import DeleteIcon from '@mui/icons-material/Delete';

export interface ImagePickerProps {
  /** Selected image file */
  image: File | null;
  /** Change handler */
  onChange: (image: File | null) => void;
  /** Preview URL (for existing images) */
  previewUrl?: string;
  /** Disabled state */
  disabled?: boolean;
  /** Button label */
  buttonLabel?: string;
  /** Maximum file size in bytes */
  maxSize?: number;
  /** Preview width */
  previewWidth?: number;
  /** Preview height */
  previewHeight?: number;
  /** Custom class name */
  className?: string;
}

export const ImagePicker: React.FC<ImagePickerProps> = ({
  image,
  onChange,
  previewUrl,
  disabled = false,
  buttonLabel = 'Select Image',
  maxSize,
  previewWidth = 200,
  previewHeight = 200,
  className,
}) => {
  const inputRef = useRef<HTMLInputElement>(null);
  const [preview, setPreview] = useState<string | null>(previewUrl || null);

  const handleClick = () => {
    inputRef.current?.click();
  };

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    // Validate file size
    if (maxSize && file.size > maxSize) {
      return;
    }

    // Create preview
    const reader = new FileReader();
    reader.onloadend = () => {
      setPreview(reader.result as string);
    };
    reader.readAsDataURL(file);

    onChange(file);
  };

  const handleRemove = () => {
    setPreview(null);
    onChange(null);
    if (inputRef.current) {
      inputRef.current.value = '';
    }
  };

  return (
    <Box className={className}>
      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        onChange={handleChange}
        disabled={disabled}
        style={{ display: 'none' }}
      />

      {preview ? (
        <Paper
          elevation={2}
          sx={{
            position: 'relative',
            width: previewWidth,
            height: previewHeight,
            overflow: 'hidden',
            borderRadius: 1,
          }}
        >
          <img
            src={preview}
            alt="Preview"
            style={{
              width: '100%',
              height: '100%',
              objectFit: 'cover',
            }}
          />
          <IconButton
            onClick={handleRemove}
            disabled={disabled}
            sx={{
              position: 'absolute',
              top: 8,
              right: 8,
              backgroundColor: 'rgba(0, 0, 0, 0.5)',
              color: 'white',
              '&:hover': {
                backgroundColor: 'rgba(0, 0, 0, 0.7)',
              },
            }}
            size="small"
          >
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Paper>
      ) : (
        <Button
          variant="outlined"
          startIcon={<PhotoCameraIcon />}
          onClick={handleClick}
          disabled={disabled}
        >
          {buttonLabel}
        </Button>
      )}
    </Box>
  );
};

export default ImagePicker;
