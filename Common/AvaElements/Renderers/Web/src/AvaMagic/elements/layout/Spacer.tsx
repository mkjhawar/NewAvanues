/**
 * Spacer Component - Phase 3 Layout Component
 *
 * Flexible spacing element
 * Matches Android/iOS Spacer behavior
 *
 * @package com.augmentalis.AvaMagic.elements.layout
 * @since 3.0.0-phase3
 */

import React from 'react';
import { Box } from '@mui/material';

export interface SpacerProps {
  /** Width (for horizontal spacing) */
  width?: number | string;
  /** Height (for vertical spacing) */
  height?: number | string;
  /** Flex grow (flexible spacing) */
  flex?: number;
  /** Custom class name */
  className?: string;
}

export const Spacer: React.FC<SpacerProps> = ({
  width,
  height,
  flex = 1,
  className,
}) => {
  return (
    <Box
      className={className}
      sx={{
        width,
        height,
        flex: width || height ? undefined : flex,
      }}
    />
  );
};

export default Spacer;
