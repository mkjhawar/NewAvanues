/**
 * Ranking Component
 *
 * Single ranked item display for leaderboards.
 *
 * @since 3.0.0-flutter-parity-web
 * @module flutterparity/data
 */

import React from 'react';
import type { RankingProps } from './types';

/**
 * Ranking - Single rank item component
 *
 * @example
 * ```tsx
 * <Ranking
 *   item={{
 *     rank: 1,
 *     name: 'John Doe',
 *     avatar: 'https://example.com/avatar.jpg',
 *     score: 9523,
 *     change: 2
 *   }}
 *   showChange
 *   showAvatar
 *   highlighted
 * />
 * ```
 */
export const Ranking: React.FC<RankingProps> = ({
  item,
  showChange = true,
  showAvatar = true,
  highlighted = false,
  onClick,
  className = '',
  style,
  testId,
}) => {
  const { rank, name, avatar, score, change } = item;

  const getRankBadgeStyle = (): React.CSSProperties => {
    const baseStyle: React.CSSProperties = {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      width: '2.5rem',
      height: '2.5rem',
      borderRadius: '50%',
      fontWeight: 700,
      fontSize: '1rem',
    };

    if (rank === 1) {
      return { ...baseStyle, backgroundColor: '#fbbf24', color: '#78350f' };
    } else if (rank === 2) {
      return { ...baseStyle, backgroundColor: '#d1d5db', color: '#374151' };
    } else if (rank === 3) {
      return { ...baseStyle, backgroundColor: '#d97706', color: '#ffffff' };
    } else {
      return { ...baseStyle, backgroundColor: '#f3f4f6', color: '#6b7280' };
    }
  };

  const getChangeIndicator = () => {
    if (!change || change === 0) return null;

    return (
      <div
        className="ranking__change"
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.25rem',
          fontSize: '0.875rem',
          fontWeight: 600,
          color: change > 0 ? '#10b981' : '#ef4444',
        }}
      >
        <span>{change > 0 ? '↑' : '↓'}</span>
        <span>{Math.abs(change)}</span>
      </div>
    );
  };

  const containerClasses = [
    'ranking',
    highlighted && 'ranking--highlighted',
    onClick && 'ranking--clickable',
    className,
  ]
    .filter(Boolean)
    .join(' ');

  const baseStyle: React.CSSProperties = {
    display: 'flex',
    alignItems: 'center',
    gap: '1rem',
    padding: '1rem',
    borderRadius: '0.5rem',
    backgroundColor: highlighted ? '#f0f9ff' : '#ffffff',
    border: highlighted ? '2px solid #3b82f6' : '1px solid #e5e7eb',
    cursor: onClick ? 'pointer' : 'default',
    transition: 'all 0.2s ease',
    ...style,
  };

  return (
    <div
      className={containerClasses}
      style={baseStyle}
      onClick={onClick}
      data-testid={testId}
      onMouseEnter={(e) => {
        if (onClick) {
          e.currentTarget.style.backgroundColor = highlighted ? '#e0f2fe' : '#f9fafb';
          e.currentTarget.style.transform = 'translateX(4px)';
        }
      }}
      onMouseLeave={(e) => {
        if (onClick) {
          e.currentTarget.style.backgroundColor = highlighted ? '#f0f9ff' : '#ffffff';
          e.currentTarget.style.transform = 'translateX(0)';
        }
      }}
    >
      {/* Rank Badge */}
      <div className="ranking__rank" style={getRankBadgeStyle()}>
        {rank}
      </div>

      {/* Avatar */}
      {showAvatar && (
        <div className="ranking__avatar">
          {avatar ? (
            <img
              src={avatar}
              alt={name}
              style={{
                width: '2.5rem',
                height: '2.5rem',
                borderRadius: '50%',
                objectFit: 'cover',
              }}
            />
          ) : (
            <div
              style={{
                width: '2.5rem',
                height: '2.5rem',
                borderRadius: '50%',
                backgroundColor: '#e5e7eb',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '1rem',
                fontWeight: 600,
                color: '#6b7280',
              }}
            >
              {name.charAt(0).toUpperCase()}
            </div>
          )}
        </div>
      )}

      {/* Name */}
      <div
        className="ranking__name"
        style={{
          flex: 1,
          fontSize: '1rem',
          fontWeight: 600,
          color: '#111827',
        }}
      >
        {name}
      </div>

      {/* Change Indicator */}
      {showChange && getChangeIndicator()}

      {/* Score */}
      <div
        className="ranking__score"
        style={{
          fontSize: '1.25rem',
          fontWeight: 700,
          color: '#111827',
        }}
      >
        {score.toLocaleString()}
      </div>
    </div>
  );
};

Ranking.displayName = 'Ranking';
