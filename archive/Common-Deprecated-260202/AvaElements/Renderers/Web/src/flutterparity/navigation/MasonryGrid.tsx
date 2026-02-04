/**
 * MasonryGrid Component
 * Pinterest-style grid layout using CSS columns
 */

import React, { useMemo } from 'react';
import { ResponsiveColumns } from './types';

export interface MasonryGridProps {
  columns?: ResponsiveColumns;
  gap?: number | string;
  children: React.ReactNode;
  className?: string;
}

export const MasonryGrid: React.FC<MasonryGridProps> = ({
  columns = 3,
  gap = 16,
  children,
  className = '',
}) => {
  const columnCount = useMemo(() => {
    if (typeof columns === 'number') return columns;

    // Simple responsive logic based on window width
    if (typeof window !== 'undefined') {
      const width = window.innerWidth;
      if (width < 640) return columns.sm || 1;
      if (width < 1024) return columns.md || 2;
      return columns.lg || 3;
    }

    return 3;
  }, [columns]);

  const gapValue = typeof gap === 'number' ? `${gap}px` : gap;

  return (
    <div
      className={`masonry-grid ${className}`}
      style={{
        columnCount,
        columnGap: gapValue,
        width: '100%',
      }}
    >
      {React.Children.map(children, (child) => (
        <div
          className="masonry-item"
          style={{
            breakInside: 'avoid',
            marginBottom: gapValue,
            display: 'inline-block',
            width: '100%',
          }}
        >
          {child}
        </div>
      ))}
    </div>
  );
};
