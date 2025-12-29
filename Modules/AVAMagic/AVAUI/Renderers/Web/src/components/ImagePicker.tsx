import React from 'react';
import { Button, Box, Typography, Avatar, IconButton, ImageList, ImageListItem } from '@mui/material';
import AddPhotoAlternateIcon from '@mui/icons-material/AddPhotoAlternate';
import DeleteIcon from '@mui/icons-material/Delete';

export interface ImagePickerProps {
  label?: string;
  multiple?: boolean;
  accept?: string;
  maxFiles?: number;
  disabled?: boolean;
  onChange?: (files: File[], previews: string[]) => void;
  showPreview?: boolean;
  previewSize?: number;
}

export const ImagePicker: React.FC<ImagePickerProps> = ({
  label = 'Select Image',
  multiple = false,
  accept = 'image/*',
  maxFiles = 10,
  disabled = false,
  onChange,
  showPreview = true,
  previewSize = 100,
}) => {
  const [files, setFiles] = React.useState<File[]>([]);
  const [previews, setPreviews] = React.useState<string[]>([]);
  const inputRef = React.useRef<HTMLInputElement>(null);

  const handleClick = () => {
    inputRef.current?.click();
  };

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = Array.from(event.target.files || []);

    const validFiles = multiple
      ? selectedFiles.slice(0, maxFiles - files.length)
      : selectedFiles.slice(0, 1);

    const newFiles = multiple ? [...files, ...validFiles] : validFiles;
    setFiles(newFiles);

    // Generate previews
    const newPreviews: string[] = [];
    validFiles.forEach(file => {
      const reader = new FileReader();
      reader.onload = (e) => {
        newPreviews.push(e.target?.result as string);
        if (newPreviews.length === validFiles.length) {
          const allPreviews = multiple ? [...previews, ...newPreviews] : newPreviews;
          setPreviews(allPreviews);
          onChange?.(newFiles, allPreviews);
        }
      };
      reader.readAsDataURL(file);
    });

    // Reset input
    if (inputRef.current) {
      inputRef.current.value = '';
    }
  };

  const handleRemove = (index: number) => {
    const newFiles = files.filter((_, i) => i !== index);
    const newPreviews = previews.filter((_, i) => i !== index);
    setFiles(newFiles);
    setPreviews(newPreviews);
    onChange?.(newFiles, newPreviews);
  };

  return (
    <Box>
      <input
        ref={inputRef}
        type="file"
        accept={accept}
        multiple={multiple}
        onChange={handleChange}
        style={{ display: 'none' }}
      />

      <Button
        variant="outlined"
        startIcon={<AddPhotoAlternateIcon />}
        onClick={handleClick}
        disabled={disabled || (multiple && files.length >= maxFiles)}
      >
        {label}
      </Button>

      {multiple && files.length > 0 && (
        <Typography variant="caption" sx={{ ml: 1 }}>
          {files.length}/{maxFiles} images
        </Typography>
      )}

      {showPreview && previews.length > 0 && (
        <Box sx={{ mt: 2 }}>
          {multiple ? (
            <ImageList cols={4} rowHeight={previewSize} sx={{ maxWidth: 500 }}>
              {previews.map((preview, index) => (
                <ImageListItem key={index} sx={{ position: 'relative' }}>
                  <img
                    src={preview}
                    alt={`Preview ${index + 1}`}
                    style={{
                      width: '100%',
                      height: '100%',
                      objectFit: 'cover',
                      borderRadius: 4
                    }}
                  />
                  <IconButton
                    size="small"
                    onClick={() => handleRemove(index)}
                    sx={{
                      position: 'absolute',
                      top: 2,
                      right: 2,
                      bgcolor: 'rgba(0,0,0,0.5)',
                      color: 'white',
                      '&:hover': { bgcolor: 'rgba(0,0,0,0.7)' }
                    }}
                  >
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </ImageListItem>
              ))}
            </ImageList>
          ) : (
            <Box sx={{ position: 'relative', display: 'inline-block' }}>
              <Avatar
                src={previews[0]}
                variant="rounded"
                sx={{ width: previewSize, height: previewSize }}
              />
              <IconButton
                size="small"
                onClick={() => handleRemove(0)}
                sx={{
                  position: 'absolute',
                  top: -8,
                  right: -8,
                  bgcolor: 'error.main',
                  color: 'white',
                  '&:hover': { bgcolor: 'error.dark' }
                }}
              >
                <DeleteIcon fontSize="small" />
              </IconButton>
            </Box>
          )}
        </Box>
      )}
    </Box>
  );
};

export default ImagePicker;
