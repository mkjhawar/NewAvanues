import React, { useState, useRef, useEffect, useCallback } from 'react';

export interface PageViewProps {
  children: React.ReactNode[];
  initialPage?: number;
  onPageChanged?: (page: number) => void;
  showDots?: boolean;
  className?: string;
  style?: React.CSSProperties;
  enableSwipe?: boolean;
}

/**
 * PageView - Swipeable page view component
 *
 * Displays pages that can be swiped left/right or navigated with pagination dots.
 */
export const PageView: React.FC<PageViewProps> = ({
  children,
  initialPage = 0,
  onPageChanged,
  showDots = true,
  className = '',
  style = {},
  enableSwipe = true,
}) => {
  const [currentPage, setCurrentPage] = useState(initialPage);
  const [touchStart, setTouchStart] = useState(0);
  const [touchEnd, setTouchEnd] = useState(0);
  const containerRef = useRef<HTMLDivElement>(null);

  const pageCount = children.length;

  const goToPage = useCallback(
    (page: number) => {
      if (page < 0 || page >= pageCount) return;
      setCurrentPage(page);
      onPageChanged?.(page);
    },
    [pageCount, onPageChanged]
  );

  // Handle swipe gestures
  const handleTouchStart = (e: React.TouchEvent) => {
    if (!enableSwipe) return;
    setTouchStart(e.touches[0].clientX);
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    if (!enableSwipe) return;
    setTouchEnd(e.touches[0].clientX);
  };

  const handleTouchEnd = () => {
    if (!enableSwipe) return;

    const swipeThreshold = 50;
    const diff = touchStart - touchEnd;

    if (Math.abs(diff) > swipeThreshold) {
      if (diff > 0) {
        // Swiped left
        goToPage(currentPage + 1);
      } else {
        // Swiped right
        goToPage(currentPage - 1);
      }
    }
  };

  // Keyboard navigation
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      switch (e.key) {
        case 'ArrowLeft':
          e.preventDefault();
          goToPage(currentPage - 1);
          break;
        case 'ArrowRight':
          e.preventDefault();
          goToPage(currentPage + 1);
          break;
        case 'Home':
          e.preventDefault();
          goToPage(0);
          break;
        case 'End':
          e.preventDefault();
          goToPage(pageCount - 1);
          break;
      }
    },
    [currentPage, pageCount, goToPage]
  );

  return (
    <div
      ref={containerRef}
      className={className}
      style={{
        position: 'relative',
        width: '100%',
        height: '100%',
        overflow: 'hidden',
        ...style,
      }}
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
      onKeyDown={handleKeyDown}
      tabIndex={0}
      role="region"
      aria-label="Page view"
    >
      <div
        style={{
          display: 'flex',
          width: `${pageCount * 100}%`,
          height: '100%',
          transform: `translateX(-${(currentPage / pageCount) * 100}%)`,
          transition: 'transform 0.3s ease-out',
        }}
      >
        {children.map((child, index) => (
          <div
            key={index}
            style={{
              width: `${100 / pageCount}%`,
              flexShrink: 0,
            }}
            role="tabpanel"
            aria-hidden={index !== currentPage}
          >
            {child}
          </div>
        ))}
      </div>

      {showDots && (
        <div
          style={{
            position: 'absolute',
            bottom: '16px',
            left: '50%',
            transform: 'translateX(-50%)',
            display: 'flex',
            gap: '8px',
            padding: '8px 16px',
            backgroundColor: 'rgba(0, 0, 0, 0.3)',
            borderRadius: '16px',
          }}
          role="tablist"
        >
          {children.map((_, index) => (
            <button
              key={index}
              onClick={() => goToPage(index)}
              style={{
                width: '8px',
                height: '8px',
                borderRadius: '50%',
                border: 'none',
                backgroundColor: index === currentPage ? '#fff' : 'rgba(255, 255, 255, 0.5)',
                cursor: 'pointer',
                padding: 0,
                transition: 'background-color 0.2s',
              }}
              role="tab"
              aria-selected={index === currentPage}
              aria-label={`Go to page ${index + 1}`}
            />
          ))}
        </div>
      )}
    </div>
  );
};
