/**
 * LoadingButton - Material Design Loading Button
 *
 * A button that displays a loading indicator during async operations.
 * Follows Material Design 3 patterns for loading states.
 *
 * @since 3.1.0-phase3
 */

import React from 'react';
import { Button, CircularProgress } from '@mui/material';
import type { LoadingButtonProps } from './types';

export const LoadingButton: React.FC<LoadingButtonProps> = ({
  text,
  icon,
  loading = false,
  loadingPosition = 'center',
  loadingIndicator,
  loadingText,
  enabled = true,
  onPressed,
  accessibilityLabel,
  ...rest
}) => {
  const isDisabled = !enabled || loading;

  const getStartIcon = () => {
    if (loading && loadingPosition === 'start') {
      return loadingIndicator || (
        <CircularProgress size={16} color="inherit" />
      );
    }
    return icon;
  };

  const getEndIcon = () => {
    if (loading && loadingPosition === 'end') {
      return loadingIndicator || (
        <CircularProgress size={16} color="inherit" />
      );
    }
    return undefined;
  };

  const getButtonText = () => {
    if (loading && loadingText) {
      return loadingText;
    }
    if (loading && loadingPosition === 'center') {
      return ''; // Hide text when loading indicator is in center
    }
    return text;
  };

  return (
    <Button
      variant="contained"
      onClick={!isDisabled ? onPressed : undefined}
      disabled={isDisabled}
      startIcon={getStartIcon()}
      endIcon={getEndIcon()}
      aria-label={accessibilityLabel || text}
      aria-busy={loading}
      sx={{
        textTransform: 'none',
        minHeight: 40,
        paddingX: 3,
        fontWeight: 600,
        position: 'relative',
        ...rest.sx,
      }}
      {...rest}
    >
      {loading && loadingPosition === 'center' && (
        <CircularProgress
          size={20}
          color="inherit"
          sx={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            marginTop: '-10px',
            marginLeft: '-10px',
          }}
        />
      )}
      <span
        style={{
          visibility:
            loading && loadingPosition === 'center' ? 'hidden' : 'visible',
        }}
      >
        {getButtonText()}
      </span>
    </Button>
  );
};

export default LoadingButton;
