import React, { useRef } from 'react';
import { Button, Box } from '@mui/material';
import { CloudUpload } from '@mui/icons-material';

/**
 * MagicFileUpload - React File Upload Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface MagicFileUploadProps {
  onFileSelect: (file: File) => void;
  label?: string;
  accept?: string;
  className?: string;
}

export const MagicFileUpload: React.FC<MagicFileUploadProps> = ({
  onFileSelect,
  label = 'Choose File',
  accept,
  className
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      onFileSelect(file);
    }
  };

  return (
    <Box className={className}>
      <input
        ref={fileInputRef}
        type="file"
        accept={accept}
        onChange={handleFileChange}
        style={{ display: 'none' }}
      />
      <Button
        variant="contained"
        startIcon={<CloudUpload />}
        onClick={handleClick}
        fullWidth
      >
        {label}
      </Button>
    </Box>
  );
};

export default MagicFileUpload;
