import React, { useRef, useEffect, useState, useCallback } from 'react';

export interface GridViewBuilderProps {
  itemCount: number;
  itemBuilder: (index: number) => React.ReactNode;
  crossAxisCount: number;
  mainAxisSpacing?: number;
  crossAxisSpacing?: number;
  childAspectRatio?: number;
  overscan?: number;
  onEndReached?: () => void;
  onEndReachedThreshold?: number;
  className?: string;
  style?: React.CSSProperties;
  keyExtractor?: (index: number) => string | number;
}

/**
 * GridViewBuilder - Virtualized grid builder for efficient rendering of large grids
 *
 * Implements virtual scrolling for grid layouts, rendering only visible rows.
 */
export const GridViewBuilder: React.FC<GridViewBuilderProps> = ({
  itemCount,
  itemBuilder,
  crossAxisCount,
  mainAxisSpacing = 0,
  crossAxisSpacing = 0,
  childAspectRatio = 1,
  overscan = 2,
  onEndReached,
  onEndReachedThreshold = 0.8,
  className = '',
  style = {},
  keyExtractor = (index) => index,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [scrollTop, setScrollTop] = useState(0);
  const [containerHeight, setContainerHeight] = useState(0);
  const [containerWidth, setContainerWidth] = useState(0);

  // Calculate grid dimensions
  const itemWidth = (containerWidth - crossAxisSpacing * (crossAxisCount - 1)) / crossAxisCount;
  const itemHeight = itemWidth / childAspectRatio;
  const rowHeight = itemHeight + mainAxisSpacing;
  const totalRows = Math.ceil(itemCount / crossAxisCount);
  const totalHeight = totalRows * rowHeight - mainAxisSpacing;

  // Calculate visible range
  const startRow = Math.max(0, Math.floor(scrollTop / rowHeight) - overscan);
  const endRow = Math.min(
    totalRows - 1,
    Math.ceil((scrollTop + containerHeight) / rowHeight) + overscan
  );

  const offsetY = startRow * rowHeight;

  const handleScroll = useCallback(
    (e: React.UIEvent<HTMLDivElement>) => {
      const target = e.currentTarget;
      setScrollTop(target.scrollTop);

      // Check if end reached
      if (onEndReached) {
        const threshold = target.scrollHeight * onEndReachedThreshold;
        if (target.scrollTop + target.clientHeight >= threshold) {
          onEndReached();
        }
      }
    },
    [onEndReached, onEndReachedThreshold]
  );

  // Update container dimensions on resize
  useEffect(() => {
    const updateDimensions = () => {
      if (containerRef.current) {
        setContainerHeight(containerRef.current.clientHeight);
        setContainerWidth(containerRef.current.clientWidth);
      }
    };

    updateDimensions();
    window.addEventListener('resize', updateDimensions);
    return () => window.removeEventListener('resize', updateDimensions);
  }, []);

  // Keyboard navigation
  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (!containerRef.current) return;

    const currentScroll = containerRef.current.scrollTop;

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        containerRef.current.scrollTop = currentScroll + rowHeight;
        break;
      case 'ArrowUp':
        e.preventDefault();
        containerRef.current.scrollTop = currentScroll - rowHeight;
        break;
      case 'PageDown':
        e.preventDefault();
        containerRef.current.scrollTop = currentScroll + containerHeight;
        break;
      case 'PageUp':
        e.preventDefault();
        containerRef.current.scrollTop = currentScroll - containerHeight;
        break;
      case 'Home':
        e.preventDefault();
        containerRef.current.scrollTop = 0;
        break;
      case 'End':
        e.preventDefault();
        containerRef.current.scrollTop = totalHeight;
        break;
    }
  }, [rowHeight, containerHeight, totalHeight]);

  return (
    <div
      ref={containerRef}
      className={className}
      style={{
        overflow: 'auto',
        height: '100%',
        ...style,
      }}
      onScroll={handleScroll}
      onKeyDown={handleKeyDown}
      tabIndex={0}
      role="grid"
    >
      <div style={{ height: totalHeight, position: 'relative' }}>
        <div style={{ transform: `translateY(${offsetY}px)` }}>
          {Array.from({ length: endRow - startRow + 1 }, (_, rowIndex) => {
            const row = startRow + rowIndex;
            const startIndex = row * crossAxisCount;
            const endIndex = Math.min(startIndex + crossAxisCount, itemCount);

            return (
              <div
                key={`row-${row}`}
                style={{
                  display: 'flex',
                  gap: `${crossAxisSpacing}px`,
                  marginBottom: rowIndex < endRow - startRow ? `${mainAxisSpacing}px` : 0,
                }}
                role="row"
              >
                {Array.from({ length: endIndex - startIndex }, (_, i) => {
                  const index = startIndex + i;
                  return (
                    <div
                      key={keyExtractor(index)}
                      style={{
                        width: itemWidth,
                        height: itemHeight,
                        flexShrink: 0,
                      }}
                      role="gridcell"
                    >
                      {itemBuilder(index)}
                    </div>
                  );
                })}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
