import React, { useRef, useCallback } from 'react';

export interface CustomScrollViewProps {
  children: React.ReactNode;
  scrollDirection?: 'vertical' | 'horizontal' | 'both';
  onScroll?: (scrollTop: number, scrollLeft: number) => void;
  className?: string;
  style?: React.CSSProperties;
  showScrollbar?: boolean;
}

/**
 * CustomScrollView - Custom scroll container with enhanced control
 *
 * Provides a scroll container with custom styling and behavior options.
 */
export const CustomScrollView: React.FC<CustomScrollViewProps> = ({
  children,
  scrollDirection = 'vertical',
  onScroll,
  className = '',
  style = {},
  showScrollbar = true,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);

  const handleScroll = useCallback(
    (e: React.UIEvent<HTMLDivElement>) => {
      if (onScroll) {
        const target = e.currentTarget;
        onScroll(target.scrollTop, target.scrollLeft);
      }
    },
    [onScroll]
  );

  const getOverflowStyle = () => {
    switch (scrollDirection) {
      case 'horizontal':
        return { overflowX: 'auto' as const, overflowY: 'hidden' as const };
      case 'both':
        return { overflow: 'auto' as const };
      case 'vertical':
      default:
        return { overflowY: 'auto' as const, overflowX: 'hidden' as const };
    }
  };

  // Keyboard navigation
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (!containerRef.current) return;

      const scrollAmount = 40;
      const pageScrollAmount = containerRef.current.clientHeight * 0.9;

      switch (e.key) {
        case 'ArrowDown':
          if (scrollDirection !== 'horizontal') {
            e.preventDefault();
            containerRef.current.scrollTop += scrollAmount;
          }
          break;
        case 'ArrowUp':
          if (scrollDirection !== 'horizontal') {
            e.preventDefault();
            containerRef.current.scrollTop -= scrollAmount;
          }
          break;
        case 'ArrowRight':
          if (scrollDirection !== 'vertical') {
            e.preventDefault();
            containerRef.current.scrollLeft += scrollAmount;
          }
          break;
        case 'ArrowLeft':
          if (scrollDirection !== 'vertical') {
            e.preventDefault();
            containerRef.current.scrollLeft -= scrollAmount;
          }
          break;
        case 'PageDown':
          e.preventDefault();
          containerRef.current.scrollTop += pageScrollAmount;
          break;
        case 'PageUp':
          e.preventDefault();
          containerRef.current.scrollTop -= pageScrollAmount;
          break;
        case 'Home':
          e.preventDefault();
          containerRef.current.scrollTop = 0;
          containerRef.current.scrollLeft = 0;
          break;
        case 'End':
          e.preventDefault();
          containerRef.current.scrollTop = containerRef.current.scrollHeight;
          break;
      }
    },
    [scrollDirection]
  );

  return (
    <div
      ref={containerRef}
      className={className}
      style={{
        height: '100%',
        width: '100%',
        ...getOverflowStyle(),
        scrollbarWidth: showScrollbar ? 'auto' : 'none',
        msOverflowStyle: showScrollbar ? 'auto' : 'none',
        ...style,
        ...(showScrollbar ? {} : {
          '::-webkit-scrollbar': {
            display: 'none',
          },
        }),
      }}
      onScroll={handleScroll}
      onKeyDown={handleKeyDown}
      tabIndex={0}
      role="region"
      aria-label="Scrollable content"
    >
      {children}
    </div>
  );
};
