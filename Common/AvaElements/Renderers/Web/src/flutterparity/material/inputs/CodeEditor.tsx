/**
 * CodeEditor Component - Code input with syntax highlighting placeholder
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { Box, TextField, Typography } from '@mui/material';
import type { CodeEditorProps } from './types';

export const CodeEditor: React.FC<CodeEditorProps> = ({
  value = '',
  onChange,
  language = 'javascript',
  placeholder = '// Start coding...',
  minHeight = 300,
  showLineNumbers = true,
  disabled = false,
  theme = 'light',
}) => {
  const getLineNumbers = () => {
    const lines = value.split('\n');
    return lines.map((_, index) => index + 1).join('\n');
  };

  return (
    <Box sx={{ width: '100%' }}>
      <Typography variant="caption" color="text.secondary" sx={{ mb: 1, display: 'block' }}>
        Language: {language}
      </Typography>
      <Box
        sx={{
          display: 'flex',
          border: 1,
          borderColor: 'divider',
          borderRadius: 1,
          overflow: 'hidden',
          backgroundColor: theme === 'dark' ? '#1e1e1e' : '#f5f5f5',
        }}
      >
        {showLineNumbers && (
          <Box
            sx={{
              p: 1,
              backgroundColor: theme === 'dark' ? '#252526' : '#e0e0e0',
              color: theme === 'dark' ? '#858585' : '#6e6e6e',
              fontFamily: 'monospace',
              fontSize: '0.875rem',
              lineHeight: '1.5',
              textAlign: 'right',
              userSelect: 'none',
              borderRight: 1,
              borderColor: 'divider',
              minWidth: 40,
            }}
          >
            {getLineNumbers()}
          </Box>
        )}
        <TextField
          fullWidth
          multiline
          placeholder={placeholder}
          value={value}
          onChange={(e) => onChange?.(e.target.value)}
          disabled={disabled}
          variant="standard"
          InputProps={{
            disableUnderline: true,
            style: {
              fontFamily: 'monospace',
              fontSize: '0.875rem',
              lineHeight: '1.5',
              color: theme === 'dark' ? '#d4d4d4' : '#000000',
              padding: '8px',
              minHeight,
            },
          }}
          inputProps={{
            'aria-label': `Code editor for ${language}`,
            spellCheck: false,
          }}
          sx={{
            '& .MuiInputBase-root': {
              alignItems: 'flex-start',
            },
          }}
        />
      </Box>
      <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
        Note: Full syntax highlighting requires integration with a library like Monaco or CodeMirror
      </Typography>
    </Box>
  );
};
