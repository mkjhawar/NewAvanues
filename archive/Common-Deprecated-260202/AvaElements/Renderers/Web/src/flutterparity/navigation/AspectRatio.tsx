/**
 * AspectRatio Component
 * Maintains aspect ratio container using padding-bottom technique
 */

import React from 'react';

export interface AspectRatioProps {
  ratio: number; // width / height (e.g., 16/9 = 1.778)
  children: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
}

export const AspectRatio: React.FC<AspectRatioProps> = ({
  ratio,
  children,
  className = '',
  style = {},
}) => {
  // Calculate padding-bottom percentage (height / width * 100)
  const paddingBottom = `${(1 / ratio) * 100}%`;

  return (
    <div
      className={`aspect-ratio-container ${className}`}
      style={{
        position: 'relative',
        width: '100%',
        paddingBottom,
        ...style,
      }}
    >
      <div
        className="aspect-ratio-content"
        style={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
        }}
      >
        {children}
      </div>
    </div>
  );
};

// Common aspect ratio presets
export const AspectRatioPresets = {
  square: 1, // 1:1
  video: 16 / 9, // 16:9
  widescreen: 21 / 9, // 21:9
  classic: 4 / 3, // 4:3
  portrait: 3 / 4, // 3:4
  golden: 1.618, // Golden ratio
} as const;
