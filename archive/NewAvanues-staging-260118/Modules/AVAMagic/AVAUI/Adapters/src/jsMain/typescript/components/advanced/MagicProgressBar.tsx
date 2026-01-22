import React from 'react';
import { LinearProgress, LinearProgressProps, Box, Typography } from '@mui/material';

/**
 * MagicProgressBar - React/Material-UI Progress Bar Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface MagicProgressBarProps {
  value?: number;
  total?: number;
  label?: string;
  indeterminate?: boolean;
  className?: string;
}

export const MagicProgressBar: React.FC<MagicProgressBarProps> = ({
  value,
  total = 100,
  label,
  indeterminate = false,
  className
}) => {
  const percentage = value !== undefined ? (value / total) * 100 : undefined;

  const progressProps: LinearProgressProps = {
    variant: indeterminate || percentage === undefined ? 'indeterminate' : 'determinate',
    value: percentage,
    className
  };

  return (
    <Box sx={{ width: '100%' }}>
      {label && <Typography variant="body2" gutterBottom>{label}</Typography>}
      <LinearProgress {...progressProps} />
      {percentage !== undefined && (
        <Typography variant="caption">{Math.round(percentage)}%</Typography>
      )}
    </Box>
  );
};

export default MagicProgressBar;
