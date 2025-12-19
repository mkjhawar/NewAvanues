/**
 * RichTextEditor Component - Basic rich text with formatting toolbar
 *
 * @since 3.0.0-flutter-parity
 */

import React, { useRef, useState } from 'react';
import { Box, IconButton, Divider, Paper } from '@mui/material';
import {
  FormatBold,
  FormatItalic,
  FormatUnderlined,
  FormatListBulleted,
  FormatListNumbered,
  Link as LinkIcon,
} from '@mui/icons-material';
import type { RichTextEditorProps } from './types';

export const RichTextEditor: React.FC<RichTextEditorProps> = ({
  value = '',
  onChange,
  placeholder = 'Start typing...',
  toolbar = ['bold', 'italic', 'underline', 'ul', 'ol', 'link'],
  minHeight = 200,
  maxHeight = 400,
  disabled = false,
}) => {
  const editorRef = useRef<HTMLDivElement>(null);
  const [isFocused, setIsFocused] = useState(false);

  const execCommand = (command: string, value?: string) => {
    document.execCommand(command, false, value);
    editorRef.current?.focus();
  };

  const handleInput = () => {
    const content = editorRef.current?.innerHTML || '';
    onChange?.(content);
  };

  const handleLink = () => {
    const url = prompt('Enter URL:');
    if (url) {
      execCommand('createLink', url);
    }
  };

  const toolbarButtons = [
    { id: 'bold', icon: <FormatBold />, command: 'bold', label: 'Bold' },
    { id: 'italic', icon: <FormatItalic />, command: 'italic', label: 'Italic' },
    { id: 'underline', icon: <FormatUnderlined />, command: 'underline', label: 'Underline' },
    { id: 'ul', icon: <FormatListBulleted />, command: 'insertUnorderedList', label: 'Bullet List' },
    { id: 'ol', icon: <FormatListNumbered />, command: 'insertOrderedList', label: 'Numbered List' },
    { id: 'link', icon: <LinkIcon />, command: 'link', label: 'Insert Link', action: handleLink },
  ];

  return (
    <Paper
      variant="outlined"
      sx={{
        borderColor: isFocused ? 'primary.main' : 'divider',
        borderWidth: isFocused ? 2 : 1,
        transition: 'border-color 0.2s',
      }}
    >
      <Box
        sx={{
          display: 'flex',
          gap: 0.5,
          p: 1,
          backgroundColor: 'grey.50',
          borderBottom: 1,
          borderColor: 'divider',
        }}
      >
        {toolbarButtons
          .filter((btn) => toolbar.includes(btn.id as any))
          .map((btn) => (
            <IconButton
              key={btn.id}
              size="small"
              onClick={() => (btn.action ? btn.action() : execCommand(btn.command))}
              disabled={disabled}
              aria-label={btn.label}
            >
              {btn.icon}
            </IconButton>
          ))}
      </Box>
      <Divider />
      <Box
        ref={editorRef}
        contentEditable={!disabled}
        onInput={handleInput}
        onFocus={() => setIsFocused(true)}
        onBlur={() => setIsFocused(false)}
        dangerouslySetInnerHTML={{ __html: value }}
        sx={{
          minHeight,
          maxHeight,
          overflow: 'auto',
          p: 2,
          outline: 'none',
          '&:empty:before': {
            content: `"${placeholder}"`,
            color: 'text.disabled',
          },
        }}
        role="textbox"
        aria-label="Rich text editor"
        aria-multiline="true"
      />
    </Paper>
  );
};
