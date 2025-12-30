import React from 'react';
import { Drawer, Box } from '@mui/material';

export interface BottomSheetProps {
  open: boolean;
  onClose?: () => void;
  children?: React.ReactNode;
  height?: number | string;
}

export const BottomSheet: React.FC<BottomSheetProps> = ({
  open,
  onClose,
  children,
  height = 'auto',
}) => {
  return (
    <Drawer
      anchor="bottom"
      open={open}
      onClose={onClose}
      PaperProps={{
        sx: {
          borderTopLeftRadius: 16,
          borderTopRightRadius: 16,
          maxHeight: '90vh',
        },
      }}
    >
      <Box
        sx={{
          p: 2,
          height,
          overflow: 'auto',
        }}
      >
        <Box
          sx={{
            width: 40,
            height: 4,
            bgcolor: 'grey.300',
            borderRadius: 2,
            mx: 'auto',
            mb: 2,
          }}
        />
        {children}
      </Box>
    </Drawer>
  );
};

export default BottomSheet;
