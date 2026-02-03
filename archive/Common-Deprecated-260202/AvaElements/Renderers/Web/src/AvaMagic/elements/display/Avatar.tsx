/**
 * Avatar Component - Phase 3 Display Component
 *
 * Displays user profile pictures, initials, or icons
 * Matches Android/iOS Avatar behavior
 *
 * @package com.augmentalis.AvaMagic.elements.display
 * @since 3.0.0-phase3
 */

import React from 'react';
import { Avatar as MuiAvatar } from '@mui/material';
import { styled } from '@mui/material/styles';

export interface AvatarProps {
  /** Image source URL */
  src?: string;
  /** Alternative text for accessibility */
  alt?: string;
  /** Size variant */
  size?: 'small' | 'medium' | 'large' | number;
  /** Shape variant */
  variant?: 'circle' | 'rounded' | 'square';
  /** Fallback content (text or icon) */
  fallback?: React.ReactNode;
  /** Background color */
  backgroundColor?: string;
  /** Text color */
  color?: string;
  /** Click handler */
  onClick?: () => void;
  /** Accessibility label */
  accessibilityLabel?: string;
  /** Custom class name */
  className?: string;
}

const getSizeValue = (size?: 'small' | 'medium' | 'large' | number): number => {
  if (typeof size === 'number') return size;
  switch (size) {
    case 'small': return 32;
    case 'large': return 56;
    case 'medium':
    default: return 40;
  }
};

const StyledAvatar = styled(MuiAvatar, {
  shouldForwardProp: (prop) => !['backgroundColor', 'color'].includes(prop as string),
})<{ backgroundColor?: string; color?: string }>(({ backgroundColor, color }) => ({
  backgroundColor: backgroundColor || undefined,
  color: color || undefined,
  cursor: 'pointer',
  transition: 'transform 0.2s ease-in-out',
  '&:hover': {
    transform: 'scale(1.05)',
  },
}));

export const Avatar: React.FC<AvatarProps> = ({
  src,
  alt,
  size = 'medium',
  variant = 'circle',
  fallback,
  backgroundColor,
  color,
  onClick,
  accessibilityLabel,
  className,
}) => {
  const sizeValue = getSizeValue(size);

  return (
    <StyledAvatar
      src={src}
      alt={alt || accessibilityLabel}
      variant={variant}
      backgroundColor={backgroundColor}
      color={color}
      onClick={onClick}
      className={className}
      aria-label={accessibilityLabel || alt}
      sx={{
        width: sizeValue,
        height: sizeValue,
        fontSize: sizeValue * 0.4,
      }}
    >
      {!src && fallback}
    </StyledAvatar>
  );
};

export default Avatar;
