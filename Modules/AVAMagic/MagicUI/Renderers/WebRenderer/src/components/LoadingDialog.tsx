import React from 'react';
import { Dialog, DialogContent, CircularProgress, Typography, Box } from '@mui/material';

export interface LoadingDialogProps {
  open: boolean;
  message?: string;
  size?: number;
}

export const LoadingDialog: React.FC<LoadingDialogProps> = ({
  open,
  message = 'Loading...',
  size = 40,
}) => {
  return (
    <Dialog
      open={open}
      PaperProps={{
        sx: {
          p: 3,
          minWidth: 200,
          textAlign: 'center',
        },
      }}
    >
      <DialogContent>
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
          <CircularProgress size={size} />
          {message && <Typography>{message}</Typography>}
        </Box>
      </DialogContent>
    </Dialog>
  );
};

export default LoadingDialog;
