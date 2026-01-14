import React, { useState, useRef, useCallback } from 'react';

export interface PullToRefreshProps {
  isRefreshing?: boolean;
  indicatorColor?: string;
  backgroundColor?: string;
  threshold?: number;
  onRefresh?: () => Promise<void> | void;
  children?: React.ReactNode;
}

export const PullToRefresh: React.FC<PullToRefreshProps> = ({
  isRefreshing: externalRefreshing,
  indicatorColor = '#007AFF',
  backgroundColor = '#f5f5f5',
  threshold = 80,
  onRefresh,
  children
}) => {
  const [pullDistance, setPullDistance] = useState(0);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const startY = useRef(0);

  const handleTouchStart = useCallback((e: React.TouchEvent) => {
    if (containerRef.current?.scrollTop === 0) {
      startY.current = e.touches[0].clientY;
    }
  }, []);

  const handleTouchMove = useCallback((e: React.TouchEvent) => {
    if (startY.current === 0 || isRefreshing) return;
    const currentY = e.touches[0].clientY;
    const diff = Math.max(0, Math.min(currentY - startY.current, threshold * 1.5));
    setPullDistance(diff);
  }, [isRefreshing, threshold]);

  const handleTouchEnd = useCallback(async () => {
    if (pullDistance >= threshold && onRefresh) {
      setIsRefreshing(true);
      await onRefresh();
      setIsRefreshing(false);
    }
    setPullDistance(0);
    startY.current = 0;
  }, [pullDistance, threshold, onRefresh]);

  const refreshing = externalRefreshing ?? isRefreshing;
  const indicatorVisible = pullDistance > 0 || refreshing;

  return (
    <div
      ref={containerRef}
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
      style={{ height: '100%', overflow: 'auto', backgroundColor }}
    >
      {indicatorVisible && (
        <div style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          height: refreshing ? 60 : pullDistance,
          overflow: 'hidden',
          transition: refreshing ? 'height 0.3s' : 'none'
        }}>
          <div style={{
            width: 24,
            height: 24,
            border: `3px solid ${indicatorColor}`,
            borderTopColor: 'transparent',
            borderRadius: '50%',
            animation: refreshing ? 'spin 1s linear infinite' : 'none',
            transform: `rotate(${(pullDistance / threshold) * 360}deg)`
          }} />
        </div>
      )}
      <div style={{ transform: `translateY(${refreshing ? 0 : pullDistance * 0.5}px)` }}>
        {children}
      </div>
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
};

export default PullToRefresh;
