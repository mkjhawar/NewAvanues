/**
 * MonthCalendar Component
 * Month selection calendar showing 12 months in grid
 *
 * React implementation matching Flutter MonthCalendar behavior
 *
 * @since 3.0.0-flutter-parity-web
 */

import React, { useState, useCallback, useMemo, useRef, useEffect } from 'react';
import { MonthCalendarProps } from './types';

/**
 * MonthCalendar - Month selection with year navigation
 *
 * Features:
 * - 12 months displayed in 3x4 grid
 * - Year navigation (prev/next)
 * - Month selection callback
 * - Keyboard navigation (arrow keys, Enter)
 * - ARIA grid pattern for accessibility
 * - Localized month names
 * - Min/max date constraints
 */
export const MonthCalendar: React.FC<MonthCalendarProps> = ({
  year,
  onYearChange,
  selectedMonth,
  onMonthSelect,
  minDate,
  maxDate,
  locale = navigator.language,
  className = '',
  testID,
  key,
}) => {
  const [currentYear, setCurrentYear] = useState(year);
  const [focusedMonth, setFocusedMonth] = useState<number | null>(selectedMonth || null);
  const gridRef = useRef<HTMLDivElement>(null);

  // Generate month names
  const monthNames = useMemo(() => {
    const names: string[] = [];
    for (let i = 0; i < 12; i++) {
      const date = new Date(2024, i, 1);
      names.push(
        new Intl.DateTimeFormat(locale, { month: 'short' }).format(date)
      );
    }
    return names;
  }, [locale]);

  // Check if a month is selectable based on min/max dates
  const isMonthSelectable = useCallback(
    (month: number): boolean => {
      const monthDate = new Date(currentYear, month, 1);
      const monthEnd = new Date(currentYear, month + 1, 0);

      if (minDate && monthEnd < minDate) {
        return false;
      }

      if (maxDate && monthDate > maxDate) {
        return false;
      }

      return true;
    },
    [currentYear, minDate, maxDate]
  );

  // Navigation handlers
  const handlePrevYear = useCallback(() => {
    const newYear = currentYear - 1;
    setCurrentYear(newYear);
    onYearChange?.(newYear);
  }, [currentYear, onYearChange]);

  const handleNextYear = useCallback(() => {
    const newYear = currentYear + 1;
    setCurrentYear(newYear);
    onYearChange?.(newYear);
  }, [currentYear, onYearChange]);

  const handleMonthClick = useCallback(
    (month: number) => {
      if (!isMonthSelectable(month)) {
        return;
      }

      setFocusedMonth(month);
      onMonthSelect?.(month, currentYear);
    },
    [currentYear, isMonthSelectable, onMonthSelect]
  );

  // Keyboard navigation
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (focusedMonth === null) return;

      let newMonth: number | null = null;

      switch (e.key) {
        case 'ArrowLeft':
          newMonth = focusedMonth > 0 ? focusedMonth - 1 : 11;
          if (newMonth === 11) {
            handlePrevYear();
          }
          break;
        case 'ArrowRight':
          newMonth = focusedMonth < 11 ? focusedMonth + 1 : 0;
          if (newMonth === 0) {
            handleNextYear();
          }
          break;
        case 'ArrowUp':
          newMonth = focusedMonth - 3;
          if (newMonth < 0) {
            newMonth = focusedMonth;
          }
          break;
        case 'ArrowDown':
          newMonth = focusedMonth + 3;
          if (newMonth > 11) {
            newMonth = focusedMonth;
          }
          break;
        case 'Enter':
        case ' ':
          handleMonthClick(focusedMonth);
          e.preventDefault();
          return;
        case 'PageUp':
          handlePrevYear();
          return;
        case 'PageDown':
          handleNextYear();
          return;
        default:
          return;
      }

      if (newMonth !== null) {
        e.preventDefault();
        setFocusedMonth(newMonth);
      }
    },
    [focusedMonth, handleMonthClick, handlePrevYear, handleNextYear]
  );

  // Focus management
  useEffect(() => {
    if (focusedMonth !== null && gridRef.current) {
      const monthButton = gridRef.current.querySelector(
        `[data-month="${focusedMonth}"]`
      ) as HTMLButtonElement;
      monthButton?.focus();
    }
  }, [focusedMonth]);

  return (
    <div
      className={`ava-month-calendar ${className}`}
      data-testid={testID}
      key={key}
      style={{
        fontFamily: 'system-ui, -apple-system, sans-serif',
        width: '100%',
        maxWidth: '400px',
      }}
    >
      {/* Header with year and navigation */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '16px',
          borderBottom: '1px solid #e0e0e0',
        }}
      >
        <button
          onClick={handlePrevYear}
          aria-label="Previous year"
          style={{
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            padding: '8px',
            fontSize: '20px',
          }}
        >
          ‹
        </button>

        <div style={{ fontSize: '18px', fontWeight: 600 }}>
          {currentYear}
        </div>

        <button
          onClick={handleNextYear}
          aria-label="Next year"
          style={{
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            padding: '8px',
            fontSize: '20px',
          }}
        >
          ›
        </button>
      </div>

      {/* Month grid */}
      <div
        ref={gridRef}
        role="grid"
        aria-label={`Month selection for ${currentYear}`}
        onKeyDown={handleKeyDown}
        style={{
          padding: '24px 16px',
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
          gap: '16px',
        }}
      >
        {monthNames.map((name, month) => {
          const isSelected = month === selectedMonth && currentYear === year;
          const isDisabled = !isMonthSelectable(month);
          const isFocused = month === focusedMonth;

          return (
            <button
              key={month}
              role="gridcell"
              data-month={month}
              aria-label={`${name} ${currentYear}`}
              aria-selected={isSelected}
              aria-disabled={isDisabled}
              disabled={isDisabled}
              tabIndex={isFocused ? 0 : -1}
              onClick={() => handleMonthClick(month)}
              onFocus={() => setFocusedMonth(month)}
              style={{
                padding: '16px 8px',
                border: '1px solid #e0e0e0',
                borderRadius: '8px',
                cursor: isDisabled ? 'default' : 'pointer',
                backgroundColor: isSelected
                  ? '#1976d2'
                  : isFocused
                  ? '#e3f2fd'
                  : '#fff',
                color: isSelected
                  ? '#fff'
                  : isDisabled
                  ? '#bbb'
                  : '#000',
                fontSize: '14px',
                fontWeight: isSelected ? 600 : 400,
                outline: isFocused ? '2px solid #1976d2' : 'none',
                outlineOffset: '2px',
                transition: 'all 0.2s ease',
              }}
              onMouseEnter={(e) => {
                if (!isDisabled && !isSelected) {
                  e.currentTarget.style.backgroundColor = '#f5f5f5';
                }
              }}
              onMouseLeave={(e) => {
                if (!isDisabled && !isSelected) {
                  e.currentTarget.style.backgroundColor = isFocused ? '#e3f2fd' : '#fff';
                }
              }}
            >
              {name}
            </button>
          );
        })}
      </div>
    </div>
  );
};
