import React, { useEffect, useState, useCallback } from 'react';

export interface SliverAppBarProps {
  title?: React.ReactNode;
  leading?: React.ReactNode;
  actions?: React.ReactNode[];
  backgroundColor?: string;
  expandedHeight?: number;
  collapsedHeight?: number;
  pinned?: boolean;
  floating?: boolean;
  flexibleSpace?: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
}

/**
 * SliverAppBar - Collapsible app bar
 *
 * An app bar that can expand, collapse, float, or pin based on scroll position.
 * Typically used at the top of a scrollable view.
 */
export const SliverAppBar: React.FC<SliverAppBarProps> = ({
  title,
  leading,
  actions = [],
  backgroundColor = '#2196F3',
  expandedHeight = 200,
  collapsedHeight = 64,
  pinned = true,
  floating = false,
  flexibleSpace,
  className = '',
  style = {},
}) => {
  const [scrollY, setScrollY] = useState(0);
  const [isScrollingUp, setIsScrollingUp] = useState(false);
  const [lastScrollY, setLastScrollY] = useState(0);

  // Calculate current height based on scroll
  const scrollRange = expandedHeight - collapsedHeight;
  const scrollProgress = Math.min(Math.max(scrollY / scrollRange, 0), 1);
  const currentHeight = expandedHeight - scrollProgress * scrollRange;

  // Handle scroll events
  useEffect(() => {
    const handleScroll = () => {
      const currentScrollY = window.scrollY;
      setScrollY(currentScrollY);
      setIsScrollingUp(currentScrollY < lastScrollY);
      setLastScrollY(currentScrollY);
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, [lastScrollY]);

  // Determine visibility for floating app bar
  const shouldShow = pinned || (floating && (isScrollingUp || scrollY < expandedHeight));
  const opacity = shouldShow ? 1 : 0;
  const transform = shouldShow ? 'translateY(0)' : 'translateY(-100%)';

  return (
    <div
      className={className}
      style={{
        position: pinned ? 'sticky' : 'fixed',
        top: 0,
        left: 0,
        right: 0,
        height: currentHeight,
        backgroundColor,
        color: '#fff',
        zIndex: 1000,
        transition: floating ? 'transform 0.3s, opacity 0.3s' : 'height 0.3s',
        opacity,
        transform,
        ...style,
      }}
      role="banner"
    >
      {/* Flexible space background */}
      {flexibleSpace && (
        <div
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            opacity: 1 - scrollProgress,
            overflow: 'hidden',
          }}
        >
          {flexibleSpace}
        </div>
      )}

      {/* App bar content */}
      <div
        style={{
          position: 'absolute',
          bottom: 0,
          left: 0,
          right: 0,
          height: collapsedHeight,
          display: 'flex',
          alignItems: 'center',
          padding: '0 16px',
          gap: '16px',
        }}
      >
        {leading && (
          <div style={{ flexShrink: 0 }}>
            {leading}
          </div>
        )}

        <div style={{ flex: 1, overflow: 'hidden' }}>
          {typeof title === 'string' ? (
            <h1
              style={{
                margin: 0,
                fontSize: '20px',
                fontWeight: 500,
                whiteSpace: 'nowrap',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
              }}
            >
              {title}
            </h1>
          ) : (
            title
          )}
        </div>

        {actions.length > 0 && (
          <div
            style={{
              display: 'flex',
              gap: '8px',
              flexShrink: 0,
            }}
          >
            {actions.map((action, index) => (
              <div key={index}>{action}</div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
