import React from 'react';

export interface StickyHeaderProps {
  stickyOffset?: number;
  backgroundColor?: string;
  elevation?: number;
  zIndex?: number;
  children?: React.ReactNode;
}

export const StickyHeader: React.FC<StickyHeaderProps> = ({
  stickyOffset = 0,
  backgroundColor = '#fff',
  elevation = 4,
  zIndex = 100,
  children
}) => {
  return (
    <div
      style={{
        position: 'sticky',
        top: stickyOffset,
        backgroundColor,
        zIndex,
        boxShadow: elevation > 0 ? `0 ${elevation}px ${elevation * 2}px rgba(0,0,0,0.1)` : 'none'
      }}
    >
      {children}
    </div>
  );
};

export default StickyHeader;
