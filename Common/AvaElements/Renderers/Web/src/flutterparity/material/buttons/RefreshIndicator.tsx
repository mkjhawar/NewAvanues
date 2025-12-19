import React, { useState, useRef, useCallback } from 'react';

export interface RefreshIndicatorProps {
  children: React.ReactNode;
  onRefresh: () => Promise<void>;
  refreshing?: boolean;
  color?: string;
  backgroundColor?: string;
  displacement?: number;
  strokeWidth?: number;
  className?: string;
  style?: React.CSSProperties;
}

/**
 * RefreshIndicator - Pull-to-refresh indicator
 *
 * Wraps scrollable content and triggers a refresh callback when user pulls down.
 */
export const RefreshIndicator: React.FC<RefreshIndicatorProps> = ({
  children,
  onRefresh,
  refreshing: externalRefreshing,
  color = '#2196F3',
  backgroundColor = '#fff',
  displacement = 40,
  strokeWidth = 4,
  className = '',
  style = {},
}) => {
  const [internalRefreshing, setInternalRefreshing] = useState(false);
  const [pullDistance, setPullDistance] = useState(0);
  const [isDragging, setIsDragging] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const startY = useRef(0);
  const currentY = useRef(0);

  const isRefreshing = externalRefreshing ?? internalRefreshing;
  const maxPullDistance = 80;
  const triggerDistance = 60;

  // Handle touch start
  const handleTouchStart = useCallback((e: React.TouchEvent) => {
    if (containerRef.current && containerRef.current.scrollTop === 0 && !isRefreshing) {
      startY.current = e.touches[0].clientY;
      setIsDragging(true);
    }
  }, [isRefreshing]);

  // Handle touch move
  const handleTouchMove = useCallback((e: React.TouchEvent) => {
    if (!isDragging || !containerRef.current) return;

    currentY.current = e.touches[0].clientY;
    const diff = currentY.current - startY.current;

    if (diff > 0 && containerRef.current.scrollTop === 0) {
      e.preventDefault();
      const distance = Math.min(diff * 0.5, maxPullDistance);
      setPullDistance(distance);
    }
  }, [isDragging, maxPullDistance]);

  // Handle touch end
  const handleTouchEnd = useCallback(async () => {
    if (!isDragging) return;

    setIsDragging(false);

    if (pullDistance >= triggerDistance) {
      setInternalRefreshing(true);
      setPullDistance(displacement);

      try {
        await onRefresh();
      } finally {
        setInternalRefreshing(false);
        setPullDistance(0);
      }
    } else {
      setPullDistance(0);
    }
  }, [isDragging, pullDistance, triggerDistance, displacement, onRefresh]);

  // Handle mouse events for desktop testing
  const handleMouseDown = useCallback((e: React.MouseEvent) => {
    if (containerRef.current && containerRef.current.scrollTop === 0 && !isRefreshing) {
      startY.current = e.clientY;
      setIsDragging(true);
    }
  }, [isRefreshing]);

  const handleMouseMove = useCallback((e: React.MouseEvent) => {
    if (!isDragging || !containerRef.current) return;

    currentY.current = e.clientY;
    const diff = currentY.current - startY.current;

    if (diff > 0 && containerRef.current.scrollTop === 0) {
      e.preventDefault();
      const distance = Math.min(diff * 0.5, maxPullDistance);
      setPullDistance(distance);
    }
  }, [isDragging, maxPullDistance]);

  const handleMouseUp = useCallback(async () => {
    if (!isDragging) return;

    setIsDragging(false);

    if (pullDistance >= triggerDistance) {
      setInternalRefreshing(true);
      setPullDistance(displacement);

      try {
        await onRefresh();
      } finally {
        setInternalRefreshing(false);
        setPullDistance(0);
      }
    } else {
      setPullDistance(0);
    }
  }, [isDragging, pullDistance, triggerDistance, displacement, onRefresh]);

  const spinnerRotation = isRefreshing ? 'rotate(0deg)' : 'rotate(0deg)';
  const spinnerOpacity = pullDistance > 0 || isRefreshing ? 1 : 0;
  const progress = Math.min(pullDistance / triggerDistance, 1);

  return (
    <div
      className={className}
      style={{
        position: 'relative',
        height: '100%',
        overflow: 'hidden',
        ...style,
      }}
    >
      {/* Refresh indicator */}
      <div
        style={{
          position: 'absolute',
          top: 0,
          left: '50%',
          transform: `translateX(-50%) translateY(${pullDistance - displacement}px)`,
          transition: isDragging ? 'none' : 'transform 0.3s',
          opacity: spinnerOpacity,
          zIndex: 1,
        }}
      >
        <div
          style={{
            width: '40px',
            height: '40px',
            borderRadius: '50%',
            backgroundColor,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 2px 4px rgba(0, 0, 0, 0.2)',
          }}
        >
          <svg
            width="24"
            height="24"
            viewBox="0 0 24 24"
            style={{
              transform: spinnerRotation,
              animation: isRefreshing ? 'spin 1s linear infinite' : 'none',
            }}
          >
            <circle
              cx="12"
              cy="12"
              r="9"
              fill="none"
              stroke={color}
              strokeWidth={strokeWidth}
              strokeDasharray="56.5"
              strokeDashoffset={56.5 * (1 - progress)}
              strokeLinecap="round"
              style={{
                transition: isDragging ? 'none' : 'stroke-dashoffset 0.3s',
              }}
            />
          </svg>
        </div>
      </div>

      {/* Scrollable content */}
      <div
        ref={containerRef}
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
        style={{
          height: '100%',
          overflow: 'auto',
          transform: `translateY(${pullDistance}px)`,
          transition: isDragging ? 'none' : 'transform 0.3s',
        }}
      >
        {children}
      </div>

      {/* CSS for spinner animation */}
      <style>{`
        @keyframes spin {
          from {
            transform: rotate(0deg);
          }
          to {
            transform: rotate(360deg);
          }
        }
      `}</style>
    </div>
  );
};
