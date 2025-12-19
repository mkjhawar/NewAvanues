/**
 * Tooltip Component - Phase 3 Display Component
 *
 * Contextual information popup on hover/focus
 * Matches Android/iOS Tooltip behavior
 *
 * @package com.augmentalis.AvaMagic.elements.display
 * @since 3.0.0-phase3
 */

import React from 'react';
import { Tooltip as MuiTooltip, TooltipProps as MuiTooltipProps } from '@mui/material';

export interface TooltipProps {
  /** Tooltip content */
  title: React.ReactNode;
  /** Child element to attach tooltip to */
  children: React.ReactElement;
  /** Placement position */
  placement?: MuiTooltipProps['placement'];
  /** Arrow indicator */
  arrow?: boolean;
  /** Delay before showing (ms) */
  enterDelay?: number;
  /** Delay before hiding (ms) */
  leaveDelay?: number;
  /** Follow cursor */
  followCursor?: boolean;
  /** Disable tooltip */
  disabled?: boolean;
  /** Custom class name */
  className?: string;
}

export const Tooltip: React.FC<TooltipProps> = ({
  title,
  children,
  placement = 'top',
  arrow = true,
  enterDelay = 200,
  leaveDelay = 0,
  followCursor = false,
  disabled = false,
  className,
}) => {
  if (disabled) {
    return children;
  }

  return (
    <MuiTooltip
      title={title}
      placement={placement}
      arrow={arrow}
      enterDelay={enterDelay}
      leaveDelay={leaveDelay}
      followCursor={followCursor}
      className={className}
    >
      {children}
    </MuiTooltip>
  );
};

export default Tooltip;
