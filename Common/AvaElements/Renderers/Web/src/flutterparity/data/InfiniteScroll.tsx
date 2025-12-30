/**
 * InfiniteScroll Component
 *
 * Automatically loads more content when scrolling near the bottom.
 *
 * @since 3.0.0-flutter-parity-web
 * @module flutterparity/data
 */

import React, { useEffect, useRef, useCallback, useState } from 'react';
import type { InfiniteScrollProps } from './types';

/**
 * InfiniteScroll - Infinite scroll component
 *
 * Uses IntersectionObserver to detect when to load more content.
 *
 * @example
 * ```tsx
 * <InfiniteScroll
 *   hasMore={hasMore}
 *   loadMore={fetchMoreData}
 *   loader={<div>Loading...</div>}
 *   threshold={0.8}
 * >
 *   {items.map(item => <ItemCard key={item.id} item={item} />)}
 * </InfiniteScroll>
 * ```
 */
export const InfiniteScroll: React.FC<InfiniteScrollProps> = ({
  children,
  hasMore,
  loadMore,
  loader = <div>Loading...</div>,
  threshold = 0.8,
  scrollableTarget,
  inverse = false,
  className = '',
  style,
  testId,
}) => {
  const [isLoading, setIsLoading] = useState(false);
  const sentinelRef = useRef<HTMLDivElement>(null);
  const scrollableRef = useRef<HTMLElement | null>(null);

  const handleLoadMore = useCallback(async () => {
    if (isLoading || !hasMore) return;

    setIsLoading(true);
    try {
      await loadMore();
    } finally {
      setIsLoading(false);
    }
  }, [isLoading, hasMore, loadMore]);

  useEffect(() => {
    // Find scrollable container
    if (scrollableTarget) {
      scrollableRef.current = document.getElementById(scrollableTarget);
    } else {
      scrollableRef.current = null;
    }

    const sentinel = sentinelRef.current;
    if (!sentinel) return;

    const options: IntersectionObserverInit = {
      root: scrollableRef.current,
      rootMargin: '0px',
      threshold: threshold,
    };

    const observer = new IntersectionObserver((entries) => {
      const [entry] = entries;
      if (entry.isIntersecting && hasMore && !isLoading) {
        handleLoadMore();
      }
    }, options);

    observer.observe(sentinel);

    return () => {
      if (sentinel) {
        observer.unobserve(sentinel);
      }
    };
  }, [hasMore, isLoading, threshold, scrollableTarget, handleLoadMore]);

  const containerClasses = [
    'infinite-scroll',
    inverse && 'infinite-scroll--inverse',
    className,
  ]
    .filter(Boolean)
    .join(' ');

  const baseStyle: React.CSSProperties = {
    display: 'flex',
    flexDirection: inverse ? 'column-reverse' : 'column',
    ...style,
  };

  return (
    <div className={containerClasses} style={baseStyle} data-testid={testId}>
      {inverse && hasMore && (
        <div className="infinite-scroll__loader" ref={sentinelRef}>
          {isLoading && loader}
        </div>
      )}

      <div className="infinite-scroll__content">{children}</div>

      {!inverse && hasMore && (
        <div className="infinite-scroll__loader" ref={sentinelRef}>
          {isLoading && loader}
        </div>
      )}

      {!hasMore && (
        <div
          className="infinite-scroll__end"
          style={{
            padding: '1rem',
            textAlign: 'center',
            color: '#6b7280',
            fontSize: '0.875rem',
          }}
        >
          No more items to load
        </div>
      )}
    </div>
  );
};

InfiniteScroll.displayName = 'InfiniteScroll';
