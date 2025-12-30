import React, { useState } from 'react';
import {
  Box,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Grid,
  IconButton,
  Typography,
} from '@mui/material';
import Icon from '@mui/material/Icon';

export interface IconData {
  name: string;
  label: string;
  category?: string;
}

export interface IconPickerProps {
  value?: string;
  label?: string;
  icons: IconData[];
  gridColumns?: number;
  showSearch?: boolean;
  disabled?: boolean;
  onChange?: (icon: string) => void;
}

export const IconPicker: React.FC<IconPickerProps> = ({
  value = '',
  label,
  icons,
  gridColumns = 6,
  showSearch = true,
  disabled = false,
  onChange,
}) => {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState('');

  const filteredIcons = icons.filter(
    (icon) =>
      search === '' ||
      icon.name.toLowerCase().includes(search.toLowerCase()) ||
      icon.label.toLowerCase().includes(search.toLowerCase())
  );

  const handleSelect = (iconName: string) => {
    onChange?.(iconName);
    setOpen(false);
  };

  return (
    <Box>
      {label && (
        <Typography variant="caption" color="textSecondary" gutterBottom>
          {label}
        </Typography>
      )}
      <Button
        variant="outlined"
        onClick={() => setOpen(true)}
        disabled={disabled}
        startIcon={value ? <Icon>{value}</Icon> : undefined}
      >
        {value || 'Select Icon'}
      </Button>

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{label || 'Select Icon'}</DialogTitle>
        <DialogContent>
          {showSearch && (
            <TextField
              fullWidth
              size="small"
              placeholder="Search icons..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              sx={{ mb: 2 }}
            />
          )}
          <Grid container spacing={1}>
            {filteredIcons.map((icon) => (
              <Grid item key={icon.name} xs={12 / gridColumns}>
                <IconButton
                  onClick={() => handleSelect(icon.name)}
                  sx={{
                    width: '100%',
                    aspectRatio: 1,
                    borderRadius: 1,
                    bgcolor: value === icon.name ? 'primary.light' : 'transparent',
                  }}
                >
                  <Icon>{icon.name}</Icon>
                </IconButton>
              </Grid>
            ))}
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default IconPicker;
