/**
 * VirtualScroll Component
 *
 * Virtualized list rendering for large datasets with high performance.
 *
 * @since 3.0.0-flutter-parity-web
 * @module flutterparity/data
 */

import React, { useState, useCallback } from 'react';
import type { VirtualScrollProps } from './types';

/**
 * VirtualScroll - Virtualized list component
 *
 * Only renders visible items plus overscan buffer for optimal performance.
 *
 * @example
 * ```tsx
 * <VirtualScroll
 *   items={largeDataArray}
 *   itemHeight={50}
 *   height={500}
 *   overscan={3}
 *   renderItem={(item, index) => (
 *     <div>{item.name}</div>
 *   )}
 * />
 * ```
 */
export function VirtualScroll<T = any>({
  items,
  itemHeight,
  height,
  renderItem,
  overscan = 3,
  onScroll,
  className = '',
  style,
  testId,
}: VirtualScrollProps<T>) {
  const [scrollTop, setScrollTop] = useState(0);

  const handleScroll = useCallback(
    (e: React.UIEvent<HTMLDivElement>) => {
      const newScrollTop = e.currentTarget.scrollTop;
      setScrollTop(newScrollTop);
      if (onScroll) {
        onScroll(newScrollTop);
      }
    },
    [onScroll]
  );

  // Calculate visible range with overscan
  const startIndex = Math.max(0, Math.floor(scrollTop / itemHeight) - overscan);
  const endIndex = Math.min(
    items.length,
    Math.ceil((scrollTop + height) / itemHeight) + overscan
  );

  const visibleItems = items.slice(startIndex, endIndex);
  const totalHeight = items.length * itemHeight;
  const offsetY = startIndex * itemHeight;

  const containerClasses = ['virtual-scroll', className].filter(Boolean).join(' ');

  const containerStyle: React.CSSProperties = {
    height: `${height}px`,
    overflow: 'auto',
    position: 'relative',
    ...style,
  };

  const spacerStyle: React.CSSProperties = {
    height: `${totalHeight}px`,
    position: 'relative',
  };

  const contentStyle: React.CSSProperties = {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    transform: `translateY(${offsetY}px)`,
  };

  return (
    <div
      className={containerClasses}
      style={containerStyle}
      onScroll={handleScroll}
      data-testid={testId}
    >
      <div style={spacerStyle}>
        <div style={contentStyle}>
          {visibleItems.map((item, i) => {
            const actualIndex = startIndex + i;
            return (
              <div
                key={actualIndex}
                className="virtual-scroll-item"
                style={{
                  height: `${itemHeight}px`,
                  overflow: 'hidden',
                }}
              >
                {renderItem(item, actualIndex)}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

VirtualScroll.displayName = 'VirtualScroll';
