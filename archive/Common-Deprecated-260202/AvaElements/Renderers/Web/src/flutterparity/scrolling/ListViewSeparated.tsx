import React, { useRef, useCallback } from 'react';

export interface ListViewSeparatedProps {
  itemCount: number;
  itemBuilder: (index: number) => React.ReactNode;
  separatorBuilder: (index: number) => React.ReactNode;
  onEndReached?: () => void;
  onEndReachedThreshold?: number;
  className?: string;
  style?: React.CSSProperties;
  keyExtractor?: (index: number) => string | number;
}

/**
 * ListViewSeparated - List with separators between items
 *
 * Renders a list with custom separators between each item.
 * Does not use virtualization - suitable for smaller lists.
 */
export const ListViewSeparated: React.FC<ListViewSeparatedProps> = ({
  itemCount,
  itemBuilder,
  separatorBuilder,
  onEndReached,
  onEndReachedThreshold = 0.8,
  className = '',
  style = {},
  keyExtractor = (index) => index,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);

  const handleScroll = useCallback(
    (e: React.UIEvent<HTMLDivElement>) => {
      if (!onEndReached) return;

      const target = e.currentTarget;
      const threshold = target.scrollHeight * onEndReachedThreshold;

      if (target.scrollTop + target.clientHeight >= threshold) {
        onEndReached();
      }
    },
    [onEndReached, onEndReachedThreshold]
  );

  // Keyboard navigation
  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (!containerRef.current) return;

    const focusableElements = containerRef.current.querySelectorAll('[tabindex="0"]');
    const currentFocus = document.activeElement;
    const currentIndex = Array.from(focusableElements).indexOf(currentFocus as Element);

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        if (currentIndex < focusableElements.length - 1) {
          (focusableElements[currentIndex + 1] as HTMLElement).focus();
        }
        break;
      case 'ArrowUp':
        e.preventDefault();
        if (currentIndex > 0) {
          (focusableElements[currentIndex - 1] as HTMLElement).focus();
        }
        break;
      case 'Home':
        e.preventDefault();
        (focusableElements[0] as HTMLElement)?.focus();
        break;
      case 'End':
        e.preventDefault();
        (focusableElements[focusableElements.length - 1] as HTMLElement)?.focus();
        break;
    }
  }, []);

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
      role="list"
    >
      {Array.from({ length: itemCount }, (_, index) => (
        <React.Fragment key={keyExtractor(index)}>
          <div role="listitem" tabIndex={0}>
            {itemBuilder(index)}
          </div>
          {index < itemCount - 1 && (
            <div role="separator">{separatorBuilder(index)}</div>
          )}
        </React.Fragment>
      ))}
    </div>
  );
};
