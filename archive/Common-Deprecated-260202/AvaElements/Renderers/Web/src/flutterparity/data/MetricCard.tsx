/**
 * MetricCard Component
 *
 * Compact metric display with icon and change percentage.
 *
 * @since 3.0.0-flutter-parity-web
 * @module flutterparity/data
 */

import React from 'react';
import type { MetricCardProps } from './types';

/**
 * MetricCard - Metric card with icon and change percentage
 *
 * @example
 * ```tsx
 * <MetricCard
 *   title="Active Users"
 *   value={2543}
 *   change={12.5}
 *   changeLabel="vs last month"
 *   icon={<UserIcon />}
 *   isUpGood={true}
 * />
 * ```
 */
export const MetricCard: React.FC<MetricCardProps> = ({
  title,
  value,
  change,
  changeLabel = 'vs last period',
  icon,
  color = '#3b82f6',
  isUpGood = true,
  footer,
  className = '',
  style,
  testId,
}) => {
  const getChangeColor = () => {
    if (change === undefined) return '#6b7280';
    const isPositive = change >= 0;

    if (isPositive) {
      return isUpGood ? '#10b981' : '#ef4444';
    } else {
      return isUpGood ? '#ef4444' : '#10b981';
    }
  };

  const getChangeIcon = () => {
    if (change === undefined) return null;
    return change >= 0 ? '↑' : '↓';
  };

  const containerClasses = ['metric-card', className].filter(Boolean).join(' ');

  const baseStyle: React.CSSProperties = {
    display: 'flex',
    flexDirection: 'column',
    gap: '1rem',
    padding: '1.5rem',
    borderRadius: '0.75rem',
    backgroundColor: '#ffffff',
    border: '1px solid #e5e7eb',
    boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1)',
    transition: 'all 0.2s ease',
    ...style,
  };

  return (
    <div
      className={containerClasses}
      style={baseStyle}
      data-testid={testId}
      onMouseEnter={(e) => {
        e.currentTarget.style.boxShadow = '0 4px 6px -1px rgba(0, 0, 0, 0.1)';
        e.currentTarget.style.transform = 'translateY(-2px)';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.boxShadow = '0 1px 3px 0 rgba(0, 0, 0, 0.1)';
        e.currentTarget.style.transform = 'translateY(0)';
      }}
    >
      {/* Header */}
      <div
        className="metric-card__header"
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <h3
          className="metric-card__title"
          style={{
            margin: 0,
            fontSize: '0.875rem',
            fontWeight: 500,
            color: '#6b7280',
          }}
        >
          {title}
        </h3>
        {icon && (
          <div
            className="metric-card__icon"
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: '2.5rem',
              height: '2.5rem',
              borderRadius: '0.5rem',
              backgroundColor: `${color}15`,
              color: color,
            }}
          >
            {icon}
          </div>
        )}
      </div>

      {/* Value */}
      <div className="metric-card__body">
        <div
          className="metric-card__value"
          style={{
            fontSize: '2rem',
            fontWeight: 700,
            color: '#111827',
            lineHeight: 1.2,
          }}
        >
          {value}
        </div>
        {change !== undefined && (
          <div
            className="metric-card__change"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.25rem',
              marginTop: '0.5rem',
              fontSize: '0.875rem',
              fontWeight: 600,
              color: getChangeColor(),
            }}
          >
            <span>{getChangeIcon()}</span>
            <span>{Math.abs(change)}%</span>
            <span
              style={{
                fontWeight: 400,
                color: '#6b7280',
              }}
            >
              {changeLabel}
            </span>
          </div>
        )}
      </div>

      {/* Footer */}
      {footer && (
        <div
          className="metric-card__footer"
          style={{
            paddingTop: '1rem',
            borderTop: '1px solid #e5e7eb',
            fontSize: '0.875rem',
            color: '#6b7280',
          }}
        >
          {footer}
        </div>
      )}
    </div>
  );
};

MetricCard.displayName = 'MetricCard';
