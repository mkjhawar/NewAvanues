import React from 'react';
import { Typography, Box } from '@mui/material';

/**
 * MagicLabel - React Label Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface MagicLabelProps {
  text: string;
  required?: boolean;
  disabled?: boolean;
  className?: string;
}

export const MagicLabel: React.FC<MagicLabelProps> = ({
  text,
  required = false,
  disabled = false,
  className
}) => {
  return (
    <Typography
      component="label"
      className={className}
      sx={{
        fontSize: '0.875rem',
        fontWeight: 500,
        color: disabled ? 'text.disabled' : 'text.primary',
        mb: 0.5
      }}
    >
      {text}
      {required && (
        <Box component="span" sx={{ color: 'error.main', ml: 0.5 }}>
          *
        </Box>
      )}
    </Typography>
  );
};

export default MagicLabel;
