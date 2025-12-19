/**
 * AvatarGroup Component - Flutter Parity Display
 *
 * Displays a stack of avatars with overlap and overflow count
 * Matches Flutter AvatarGroup behavior
 *
 * @package com.augmentalis.avaelements.flutter.material.display
 * @since 1.0.0-flutter-parity
 */

import React from 'react';
import { BaseDisplayProps, Size } from './types';

export interface Avatar {
  /** Image source URL */
  src?: string;
  /** Alternative text */
  alt?: string;
  /** Fallback content (initials) */
  fallback?: string;
  /** Background color */
  backgroundColor?: string;
}

export interface AvatarGroupProps extends BaseDisplayProps {
  /** Array of avatars to display */
  avatars: Avatar[];
  /** Maximum number of avatars to show */
  max?: number;
  /** Size of avatars */
  size?: Size | number;
  /** Spacing between avatars (overlap) */
  spacing?: number;
  /** Click handler for overflow count */
  onOverflowClick?: () => void;
}

const getSizeValue = (size?: Size | number): number => {
  if (typeof size === 'number') return size;
  switch (size) {
    case 'small': return 32;
    case 'large': return 56;
    case 'medium':
    default: return 40;
  }
};

export const AvatarGroup: React.FC<AvatarGroupProps> = ({
  avatars,
  max = 5,
  size = 'medium',
  spacing = 8,
  onOverflowClick,
  className,
  accessibilityLabel,
}) => {
  const sizeValue = getSizeValue(size);
  const visibleAvatars = avatars.slice(0, max);
  const overflowCount = avatars.length - max;

  return (
    <div
      className={className}
      role="group"
      aria-label={accessibilityLabel || 'Avatar group'}
      style={{
        display: 'flex',
        flexDirection: 'row-reverse',
        justifyContent: 'flex-end',
      }}
    >
      {overflowCount > 0 && (
        <div
          onClick={onOverflowClick}
          role={onOverflowClick ? 'button' : undefined}
          tabIndex={onOverflowClick ? 0 : undefined}
          style={{
            width: sizeValue,
            height: sizeValue,
            borderRadius: '50%',
            backgroundColor: '#9ca3af',
            color: 'white',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: sizeValue * 0.4,
            fontWeight: 500,
            cursor: onOverflowClick ? 'pointer' : 'default',
            border: '2px solid white',
            zIndex: 0,
          }}
          aria-label={`${overflowCount} more avatars`}
        >
          +{overflowCount}
        </div>
      )}
      {visibleAvatars.map((avatar, index) => (
        <div
          key={index}
          style={{
            width: sizeValue,
            height: sizeValue,
            borderRadius: '50%',
            backgroundColor: avatar.backgroundColor || '#e5e7eb',
            color: '#374151',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: sizeValue * 0.4,
            fontWeight: 500,
            border: '2px solid white',
            marginLeft: index === visibleAvatars.length - 1 ? 0 : -spacing,
            zIndex: visibleAvatars.length - index,
            backgroundImage: avatar.src ? `url(${avatar.src})` : undefined,
            backgroundSize: 'cover',
            backgroundPosition: 'center',
          }}
          aria-label={avatar.alt}
        >
          {!avatar.src && avatar.fallback}
        </div>
      ))}
    </div>
  );
};

export default AvatarGroup;
