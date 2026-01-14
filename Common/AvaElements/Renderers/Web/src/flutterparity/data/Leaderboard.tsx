/**
 * Leaderboard Component
 *
 * Ranked list of items with avatars, names, and scores.
 *
 * @since 3.0.0-flutter-parity-web
 * @module flutterparity/data
 */

import React from 'react';
import type { LeaderboardProps } from './types';
import { Ranking } from './Ranking';

/**
 * Leaderboard - Ranked list component
 *
 * @example
 * ```tsx
 * <Leaderboard
 *   items={[
 *     { rank: 1, name: 'John Doe', avatar: 'avatar1.jpg', score: 9523, change: 2 },
 *     { rank: 2, name: 'Jane Smith', avatar: 'avatar2.jpg', score: 8954, change: -1 },
 *     { rank: 3, name: 'Bob Johnson', avatar: 'avatar3.jpg', score: 8721, change: 1 }
 *   ]}
 *   showChange
 *   showAvatar
 *   highlightRank={1}
 *   maxItems={10}
 * />
 * ```
 */
export const Leaderboard: React.FC<LeaderboardProps> = ({
  items,
  showChange = true,
  showAvatar = true,
  highlightRank,
  onItemClick,
  maxItems,
  className = '',
  style,
  testId,
}) => {
  const displayItems = maxItems ? items.slice(0, maxItems) : items;

  const containerClasses = ['leaderboard', className].filter(Boolean).join(' ');

  const baseStyle: React.CSSProperties = {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.5rem',
    padding: '1rem',
    borderRadius: '0.75rem',
    backgroundColor: '#ffffff',
    border: '1px solid #e5e7eb',
    ...style,
  };

  return (
    <div className={containerClasses} style={baseStyle} data-testid={testId}>
      {/* Header */}
      <div
        className="leaderboard__header"
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          paddingBottom: '1rem',
          borderBottom: '2px solid #e5e7eb',
        }}
      >
        <h3
          style={{
            margin: 0,
            fontSize: '1.125rem',
            fontWeight: 700,
            color: '#111827',
          }}
        >
          Leaderboard
        </h3>
        <div
          style={{
            fontSize: '0.875rem',
            color: '#6b7280',
          }}
        >
          {displayItems.length} {displayItems.length === 1 ? 'entry' : 'entries'}
        </div>
      </div>

      {/* Rankings */}
      <div
        className="leaderboard__body"
        style={{
          display: 'flex',
          flexDirection: 'column',
          gap: '0.5rem',
        }}
      >
        {displayItems.length > 0 ? (
          displayItems.map((item) => (
            <Ranking
              key={`${item.rank}-${item.name}`}
              item={item}
              showChange={showChange}
              showAvatar={showAvatar}
              highlighted={highlightRank !== undefined && item.rank === highlightRank}
              onClick={onItemClick ? () => onItemClick(item) : undefined}
            />
          ))
        ) : (
          <div
            className="leaderboard__empty"
            style={{
              padding: '2rem',
              textAlign: 'center',
              color: '#6b7280',
              fontSize: '0.875rem',
            }}
          >
            No rankings available
          </div>
        )}
      </div>

      {/* Footer */}
      {maxItems && items.length > maxItems && (
        <div
          className="leaderboard__footer"
          style={{
            paddingTop: '1rem',
            borderTop: '1px solid #e5e7eb',
            textAlign: 'center',
            fontSize: '0.875rem',
            color: '#6b7280',
          }}
        >
          Showing top {maxItems} of {items.length} total
        </div>
      )}
    </div>
  );
};

Leaderboard.displayName = 'Leaderboard';
