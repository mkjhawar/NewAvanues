/**
 * MarkdownEditor Component - Markdown editor with live preview
 *
 * @since 3.0.0-flutter-parity
 */

import React, { useState } from 'react';
import { Box, TextField, Paper, Tabs, Tab, Typography } from '@mui/material';
import type { MarkdownEditorProps } from './types';

export const MarkdownEditor: React.FC<MarkdownEditorProps> = ({
  value = '',
  onChange,
  placeholder = '# Start writing markdown...',
  minHeight = 300,
  showPreview = true,
  splitView = false,
  disabled = false,
}) => {
  const [activeTab, setActiveTab] = useState<'edit' | 'preview'>('edit');

  // Simple markdown to HTML converter (basic implementation)
  const renderMarkdown = (markdown: string): string => {
    let html = markdown;

    // Headers
    html = html.replace(/^### (.*$)/gim, '<h3>$1</h3>');
    html = html.replace(/^## (.*$)/gim, '<h2>$1</h2>');
    html = html.replace(/^# (.*$)/gim, '<h1>$1</h1>');

    // Bold
    html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/__(.+?)__/g, '<strong>$1</strong>');

    // Italic
    html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');
    html = html.replace(/_(.+?)_/g, '<em>$1</em>');

    // Links
    html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>');

    // Code inline
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');

    // Line breaks
    html = html.replace(/\n/g, '<br/>');

    return html;
  };

  const renderView = () => {
    if (splitView) {
      return (
        <Box sx={{ display: 'flex', gap: 2, height: minHeight }}>
          <TextField
            fullWidth
            multiline
            placeholder={placeholder}
            value={value}
            onChange={(e) => onChange?.(e.target.value)}
            disabled={disabled}
            variant="outlined"
            sx={{ flex: 1 }}
            inputProps={{
              style: { fontFamily: 'monospace', minHeight },
            }}
          />
          <Paper
            variant="outlined"
            sx={{
              flex: 1,
              p: 2,
              overflow: 'auto',
              backgroundColor: 'grey.50',
            }}
          >
            <div dangerouslySetInnerHTML={{ __html: renderMarkdown(value) }} />
          </Paper>
        </Box>
      );
    }

    return (
      <Box>
        {showPreview && (
          <Tabs value={activeTab} onChange={(_, newValue) => setActiveTab(newValue)}>
            <Tab label="Edit" value="edit" />
            <Tab label="Preview" value="preview" />
          </Tabs>
        )}
        {activeTab === 'edit' ? (
          <TextField
            fullWidth
            multiline
            placeholder={placeholder}
            value={value}
            onChange={(e) => onChange?.(e.target.value)}
            disabled={disabled}
            variant="outlined"
            inputProps={{
              style: { fontFamily: 'monospace', minHeight },
              'aria-label': 'Markdown editor',
            }}
          />
        ) : (
          <Paper
            variant="outlined"
            sx={{
              p: 2,
              minHeight,
              overflow: 'auto',
              backgroundColor: 'grey.50',
            }}
          >
            <div
              dangerouslySetInnerHTML={{ __html: renderMarkdown(value) }}
              role="region"
              aria-label="Markdown preview"
            />
          </Paper>
        )}
      </Box>
    );
  };

  return (
    <Box sx={{ width: '100%' }}>
      <Typography variant="caption" color="text.secondary" sx={{ mb: 1, display: 'block' }}>
        Supports: # Headers, **bold**, *italic*, [links](url), `code`
      </Typography>
      {renderView()}
    </Box>
  );
};
