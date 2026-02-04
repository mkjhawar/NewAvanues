/**
 * FormSection Component - Grouped form fields with label and description
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { Box, Typography, Divider, Paper } from '@mui/material';
import type { FormSectionProps } from './types';

export const FormSection: React.FC<FormSectionProps> = ({
  title,
  description,
  children,
  divider = true,
  elevation = 0,
  variant = 'outlined',
  spacing = 2,
}) => {
  const content = (
    <>
      {title && (
        <Box sx={{ mb: description ? 1 : 2 }}>
          <Typography variant="h6" component="h3" sx={{ fontWeight: 600 }}>
            {title}
          </Typography>
          {description && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              {description}
            </Typography>
          )}
        </Box>
      )}
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          gap: spacing,
        }}
      >
        {children}
      </Box>
      {divider && <Divider sx={{ mt: 3 }} />}
    </>
  );

  if (variant === 'paper') {
    return (
      <Paper elevation={elevation} sx={{ p: 3, mb: 2 }}>
        {content}
      </Paper>
    );
  }

  if (variant === 'outlined') {
    return (
      <Paper variant="outlined" sx={{ p: 3, mb: 2 }}>
        {content}
      </Paper>
    );
  }

  return (
    <Box sx={{ mb: 3 }} role="group" aria-labelledby={title ? 'form-section-title' : undefined}>
      {content}
    </Box>
  );
};
