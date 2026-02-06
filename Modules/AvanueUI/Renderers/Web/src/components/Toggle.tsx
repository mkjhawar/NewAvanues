import React from 'react';
import {
  Box,
  Switch,
  Typography,
  FormControlLabel,
} from '@mui/material';

export interface ToggleProps {
  label: string;
  checked?: boolean;
  description?: string;
  disabled?: boolean;
  onChange?: (checked: boolean) => void;
}

export const Toggle: React.FC<ToggleProps> = ({
  label,
  checked = false,
  description,
  disabled = false,
  onChange,
}) => {
  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    onChange?.(event.target.checked);
  };

  return (
    <Box
      sx={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        py: 1,
      }}
    >
      <Box sx={{ flex: 1 }}>
        <Typography variant="body1">{label}</Typography>
        {description && (
          <Typography variant="body2" color="textSecondary">
            {description}
          </Typography>
        )}
      </Box>
      <Switch
        checked={checked}
        onChange={handleChange}
        disabled={disabled}
      />
    </Box>
  );
};

export default Toggle;
