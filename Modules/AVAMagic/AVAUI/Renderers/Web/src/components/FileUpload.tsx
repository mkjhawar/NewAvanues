import React from 'react';
import { Button, Box, Typography, List, ListItem, ListItemText, IconButton } from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import DeleteIcon from '@mui/icons-material/Delete';

export interface FileUploadProps {
  accept?: string;
  multiple?: boolean;
  maxSize?: number;
  disabled?: boolean;
  onChange?: (files: File[]) => void;
  label?: string;
}

export const FileUpload: React.FC<FileUploadProps> = ({
  accept,
  multiple = false,
  maxSize,
  disabled = false,
  onChange,
  label = 'Upload File',
}) => {
  const [files, setFiles] = React.useState<File[]>([]);
  const inputRef = React.useRef<HTMLInputElement>(null);

  const handleClick = () => {
    inputRef.current?.click();
  };

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = Array.from(event.target.files || []);

    const validFiles = maxSize
      ? selectedFiles.filter(file => file.size <= maxSize)
      : selectedFiles;

    setFiles(prev => multiple ? [...prev, ...validFiles] : validFiles);
    onChange?.(validFiles);
  };

  const handleRemove = (index: number) => {
    const newFiles = files.filter((_, i) => i !== index);
    setFiles(newFiles);
    onChange?.(newFiles);
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
        startIcon={<CloudUploadIcon />}
        onClick={handleClick}
        disabled={disabled}
      >
        {label}
      </Button>
      {files.length > 0 && (
        <List dense>
          {files.map((file, index) => (
            <ListItem
              key={index}
              secondaryAction={
                <IconButton edge="end" onClick={() => handleRemove(index)}>
                  <DeleteIcon />
                </IconButton>
              }
            >
              <ListItemText
                primary={file.name}
                secondary={`${(file.size / 1024).toFixed(1)} KB`}
              />
            </ListItem>
          ))}
        </List>
      )}
    </Box>
  );
};

export default FileUpload;
