import React, { useRef, useEffect, useState, useCallback } from 'react';

export interface ListViewBuilderProps {
  itemCount: number;
  itemBuilder: (index: number) => React.ReactNode;
  itemHeight?: number;
  overscan?: number;
  onEndReached?: () => void;
  onEndReachedThreshold?: number;
  className?: string;
  style?: React.CSSProperties;
  keyExtractor?: (index: number) => string | number;
}

/**
 * ListViewBuilder - Virtualized list builder for efficient rendering of large lists
 *
 * Implements virtual scrolling to render only visible items, improving performance
 * for lists with hundreds or thousands of items.
 */
export const ListViewBuilder: React.FC<ListViewBuilderProps> = ({
  itemCount,
  itemBuilder,
  itemHeight = 50,
  overscan = 3,
  onEndReached,
  onEndReachedThreshold = 0.8,
  className = '',
  style = {},
  keyExtractor = (index) => index,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [scrollTop, setScrollTop] = useState(0);
  const [containerHeight, setContainerHeight] = useState(0);

  // Calculate visible range
  const startIndex = Math.max(0, Math.floor(scrollTop / itemHeight) - overscan);
  const endIndex = Math.min(
    itemCount - 1,
    Math.ceil((scrollTop + containerHeight) / itemHeight) + overscan
  );

  const totalHeight = itemCount * itemHeight;
  const offsetY = startIndex * itemHeight;

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

  // Update container height on resize
  useEffect(() => {
    const updateHeight = () => {
      if (containerRef.current) {
        setContainerHeight(containerRef.current.clientHeight);
      }
    };

    updateHeight();
    window.addEventListener('resize', updateHeight);
    return () => window.removeEventListener('resize', updateHeight);
  }, []);

  // Keyboard navigation
  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (!containerRef.current) return;

    const currentScroll = containerRef.current.scrollTop;

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        containerRef.current.scrollTop = currentScroll + itemHeight;
        break;
      case 'ArrowUp':
        e.preventDefault();
        containerRef.current.scrollTop = currentScroll - itemHeight;
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
  }, [itemHeight, containerHeight, totalHeight]);

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
      role="list"
    >
      <div style={{ height: totalHeight, position: 'relative' }}>
        <div style={{ transform: `translateY(${offsetY}px)` }}>
          {Array.from({ length: endIndex - startIndex + 1 }, (_, i) => {
            const index = startIndex + i;
            return (
              <div
                key={keyExtractor(index)}
                style={{ height: itemHeight }}
                role="listitem"
              >
                {itemBuilder(index)}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
