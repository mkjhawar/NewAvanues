/**
 * ExpansionTile Component - Flutter Parity Material Design
 *
 * An expandable list tile with children that can be shown or hidden.
 * Features smooth expand/collapse animation and rotating trailing icon.
 *
 * @since 3.0.0-flutter-parity
 */

import React, { useState, useCallback, useRef, useEffect } from 'react';
import type { ExpansionTileProps, CrossAxisAlignment, Alignment } from './types';

const DEFAULT_ANIMATION_DURATION = 200;

export const ExpansionTile: React.FC<ExpansionTileProps> = ({
  id,
  title,
  subtitle,
  leading,
  trailing,
  children,
  initiallyExpanded = false,
  maintainState = true,
  expandedCrossAxisAlignment = 'center' as CrossAxisAlignment,
  expandedAlignment = 'start' as Alignment,
  childrenPadding,
  backgroundColor,
  collapsedBackgroundColor,
  textColor,
  collapsedTextColor,
  iconColor,
  collapsedIconColor,
  tileColor,
  dense = false,
  contentPadding,
  enabled = true,
  selected = false,
  contentDescription,
  className = '',
  style,
  onExpansionChanged,
}) => {
  const [expanded, setExpanded] = useState(initiallyExpanded);
  const [height, setHeight] = useState<number | undefined>(initiallyExpanded ? undefined : 0);
  const contentRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (contentRef.current) {
      if (expanded) {
        setHeight(contentRef.current.scrollHeight);
      } else {
        setHeight(0);
      }
    }
  }, [expanded]);

  const handleToggle = useCallback(() => {
    if (!enabled) return;

    const newState = !expanded;
    setExpanded(newState);
    onExpansionChanged?.(newState);
  }, [enabled, expanded, onExpansionChanged]);

  const handleKeyDown = useCallback((event: React.KeyboardEvent) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      handleToggle();
    }
  }, [handleToggle]);

  const effectiveBackgroundColor = expanded
    ? (backgroundColor || tileColor)
    : (collapsedBackgroundColor || tileColor);
  const effectiveTextColor = expanded ? textColor : collapsedTextColor;
  const effectiveIconColor = expanded ? iconColor : collapsedIconColor;

  const alignmentMap: Record<CrossAxisAlignment, string> = {
    start: 'flex-start',
    center: 'center',
    end: 'flex-end',
    stretch: 'stretch',
  };

  const justifyMap: Record<Alignment, string> = {
    start: 'flex-start',
    center: 'center',
    end: 'flex-end',
  };

  const accessibilityLabel = contentDescription ||
    (typeof title === 'string' ? title : 'Expansion tile');
  const accessibilityState = expanded ? 'expanded' : 'collapsed';

  return (
    <div
      id={id}
      className={`expansion-tile ${className}`}
      style={{
        backgroundColor: effectiveBackgroundColor,
        ...style,
      }}
      role="region"
      aria-label={`${accessibilityLabel}, ${accessibilityState}`}
    >
      <div
        className="expansion-tile-header"
        onClick={handleToggle}
        onKeyDown={handleKeyDown}
        role="button"
        tabIndex={enabled ? 0 : -1}
        aria-expanded={expanded}
        aria-disabled={!enabled}
        style={{
          display: 'flex',
          alignItems: 'center',
          padding: contentPadding || (dense ? '8px 16px' : '12px 16px'),
          cursor: enabled ? 'pointer' : 'default',
          opacity: enabled ? 1 : 0.6,
          minHeight: dense ? 48 : 56,
          backgroundColor: selected ? 'rgba(0, 0, 0, 0.08)' : 'transparent',
          transition: 'background-color 150ms cubic-bezier(0.4, 0, 0.2, 1)',
        }}
      >
        {leading && (
          <div
            className="expansion-tile-leading"
            style={{
              marginRight: 16,
              color: effectiveIconColor,
              display: 'flex',
              alignItems: 'center',
            }}
          >
            {leading}
          </div>
        )}
        <div
          className="expansion-tile-content"
          style={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'center',
            color: effectiveTextColor,
          }}
        >
          <div
            className="expansion-tile-title"
            style={{
              fontSize: 16,
              fontWeight: 500,
              lineHeight: '24px',
            }}
          >
            {title}
          </div>
          {subtitle && !expanded && (
            <div
              className="expansion-tile-subtitle"
              style={{
                fontSize: 14,
                lineHeight: '20px',
                opacity: 0.7,
                marginTop: 2,
              }}
            >
              {subtitle}
            </div>
          )}
        </div>
        <div
          className="expansion-tile-trailing"
          style={{
            marginLeft: 16,
            color: effectiveIconColor,
            display: 'flex',
            alignItems: 'center',
            transition: `transform ${DEFAULT_ANIMATION_DURATION}ms cubic-bezier(0.4, 0, 0.2, 1)`,
            transform: expanded ? 'rotate(180deg)' : 'rotate(0deg)',
          }}
        >
          {trailing || (
            <svg
              width="24"
              height="24"
              viewBox="0 0 24 24"
              fill="currentColor"
              aria-hidden="true"
            >
              <path d="M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z" />
            </svg>
          )}
        </div>
      </div>
      <div
        ref={contentRef}
        className="expansion-tile-body"
        style={{
          height: maintainState ? (expanded ? 'auto' : 0) : height,
          overflow: maintainState ? (expanded ? 'visible' : 'hidden') : 'hidden',
          transition: maintainState
            ? `height ${DEFAULT_ANIMATION_DURATION}ms cubic-bezier(0.4, 0, 0.2, 1)`
            : 'none',
        }}
        aria-hidden={!expanded}
      >
        <div
          className="expansion-tile-children"
          style={{
            padding: childrenPadding || '8px 16px 16px 16px',
            display: 'flex',
            flexDirection: 'column',
            alignItems: alignmentMap[expandedCrossAxisAlignment],
            justifyContent: justifyMap[expandedAlignment],
          }}
        >
          {(maintainState || expanded) && children}
        </div>
      </div>
    </div>
  );
};

export default ExpansionTile;
