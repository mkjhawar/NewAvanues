/**
 * WeekCalendar Component
 * Week view calendar with 7 days displayed horizontally
 *
 * React implementation matching Flutter WeekCalendar behavior
 *
 * @since 3.0.0-flutter-parity-web
 */

import React, { useState, useCallback, useMemo, useRef, useEffect } from 'react';
import { addDays, subDays, addWeeks, subWeeks, startOfWeek, format } from 'date-fns';
import { WeekCalendarProps } from './types';

/**
 * WeekCalendar - Week view with horizontal day display
 *
 * Features:
 * - 7 days displayed horizontally
 * - Week navigation (prev/next buttons)
 * - Day selection
 * - Keyboard navigation (arrow keys)
 * - ARIA grid pattern for accessibility
 * - Localized day names
 * - Today highlighting
 */
export const WeekCalendar: React.FC<WeekCalendarProps> = ({
  startDate,
  selectedDate,
  onDateSelect,
  onWeekChange,
  firstDayOfWeek = 0,
  locale = navigator.language,
  className = '',
  testID,
  key,
}) => {
  const [currentWeekStart, setCurrentWeekStart] = useState(() =>
    startOfWeek(startDate, { weekStartsOn: firstDayOfWeek })
  );
  const [focusedDate, setFocusedDate] = useState<Date | null>(selectedDate || null);
  const gridRef = useRef<HTMLDivElement>(null);

  const today = useMemo(() => new Date(), []);

  // Generate week days
  const weekDays = useMemo(() => {
    const days: Date[] = [];
    for (let i = 0; i < 7; i++) {
      days.push(addDays(currentWeekStart, i));
    }
    return days;
  }, [currentWeekStart]);

  // Navigation handlers
  const handlePrevWeek = useCallback(() => {
    const newWeekStart = subWeeks(currentWeekStart, 1);
    setCurrentWeekStart(newWeekStart);
    onWeekChange?.(newWeekStart);
  }, [currentWeekStart, onWeekChange]);

  const handleNextWeek = useCallback(() => {
    const newWeekStart = addWeeks(currentWeekStart, 1);
    setCurrentWeekStart(newWeekStart);
    onWeekChange?.(newWeekStart);
  }, [currentWeekStart, onWeekChange]);

  const handleDateClick = useCallback(
    (date: Date) => {
      setFocusedDate(date);
      onDateSelect?.(date);
    },
    [onDateSelect]
  );

  // Keyboard navigation
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (!focusedDate) return;

      let newDate: Date | null = null;

      switch (e.key) {
        case 'ArrowLeft':
          newDate = subDays(focusedDate, 1);
          break;
        case 'ArrowRight':
          newDate = addDays(focusedDate, 1);
          break;
        case 'ArrowUp':
          newDate = subWeeks(focusedDate, 1);
          break;
        case 'ArrowDown':
          newDate = addWeeks(focusedDate, 1);
          break;
        case 'Enter':
        case ' ':
          handleDateClick(focusedDate);
          e.preventDefault();
          return;
        case 'PageUp':
          handlePrevWeek();
          return;
        case 'PageDown':
          handleNextWeek();
          return;
        default:
          return;
      }

      if (newDate) {
        e.preventDefault();
        setFocusedDate(newDate);

        // Navigate to new week if needed
        const newWeekStart = startOfWeek(newDate, { weekStartsOn: firstDayOfWeek });
        if (newWeekStart.getTime() !== currentWeekStart.getTime()) {
          setCurrentWeekStart(newWeekStart);
          onWeekChange?.(newWeekStart);
        }
      }
    },
    [
      focusedDate,
      firstDayOfWeek,
      currentWeekStart,
      handleDateClick,
      handlePrevWeek,
      handleNextWeek,
      onWeekChange,
    ]
  );

  // Focus management
  useEffect(() => {
    if (focusedDate && gridRef.current) {
      const dateButton = gridRef.current.querySelector(
        `[data-date="${focusedDate.toISOString()}"]`
      ) as HTMLButtonElement;
      dateButton?.focus();
    }
  }, [focusedDate]);

  // Check if date is today
  const isToday = (date: Date): boolean => {
    return (
      date.getFullYear() === today.getFullYear() &&
      date.getMonth() === today.getMonth() &&
      date.getDate() === today.getDate()
    );
  };

  // Check if date is selected
  const isSelected = (date: Date): boolean => {
    if (!selectedDate) return false;
    return (
      date.getFullYear() === selectedDate.getFullYear() &&
      date.getMonth() === selectedDate.getMonth() &&
      date.getDate() === selectedDate.getDate()
    );
  };

  return (
    <div
      className={`ava-week-calendar ${className}`}
      data-testid={testID}
      key={key}
      style={{
        fontFamily: 'system-ui, -apple-system, sans-serif',
        width: '100%',
        maxWidth: '600px',
      }}
    >
      {/* Header with week range and navigation */}
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
          onClick={handlePrevWeek}
          aria-label="Previous week"
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

        <div style={{ fontSize: '16px', fontWeight: 600 }}>
          {format(weekDays[0], 'MMM d', { locale })} -{' '}
          {format(weekDays[6], 'MMM d, yyyy', { locale })}
        </div>

        <button
          onClick={handleNextWeek}
          aria-label="Next week"
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

      {/* Week grid */}
      <div
        ref={gridRef}
        role="grid"
        aria-label={`Week of ${format(weekDays[0], 'MMMM d, yyyy')}`}
        onKeyDown={handleKeyDown}
        style={{
          padding: '16px',
          display: 'grid',
          gridTemplateColumns: 'repeat(7, 1fr)',
          gap: '8px',
        }}
      >
        {weekDays.map((date) => {
          const isTodayDate = isToday(date);
          const isSelectedDate = isSelected(date);
          const isFocused =
            focusedDate &&
            date.getTime() === focusedDate.getTime();

          return (
            <button
              key={date.toISOString()}
              role="gridcell"
              data-date={date.toISOString()}
              aria-label={format(date, 'EEEE, MMMM d, yyyy', { locale })}
              aria-selected={isSelectedDate}
              tabIndex={isFocused ? 0 : -1}
              onClick={() => handleDateClick(date)}
              onFocus={() => setFocusedDate(date)}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                padding: '16px 8px',
                border: isTodayDate ? '2px solid #1976d2' : '1px solid #e0e0e0',
                borderRadius: '12px',
                cursor: 'pointer',
                backgroundColor: isSelectedDate
                  ? '#1976d2'
                  : isFocused
                  ? '#e3f2fd'
                  : '#fff',
                color: isSelectedDate ? '#fff' : '#000',
                outline: isFocused ? '2px solid #1976d2' : 'none',
                outlineOffset: '2px',
                transition: 'all 0.2s ease',
              }}
              onMouseEnter={(e) => {
                if (!isSelectedDate) {
                  e.currentTarget.style.backgroundColor = isFocused ? '#e3f2fd' : '#f5f5f5';
                }
              }}
              onMouseLeave={(e) => {
                if (!isSelectedDate) {
                  e.currentTarget.style.backgroundColor = isFocused ? '#e3f2fd' : '#fff';
                }
              }}
            >
              {/* Weekday name */}
              <div
                style={{
                  fontSize: '12px',
                  fontWeight: 600,
                  textTransform: 'uppercase',
                  marginBottom: '4px',
                  opacity: isSelectedDate ? 1 : 0.7,
                }}
              >
                {format(date, 'EEE', { locale })}
              </div>

              {/* Date number */}
              <div
                style={{
                  fontSize: '24px',
                  fontWeight: isSelectedDate || isTodayDate ? 600 : 400,
                }}
              >
                {date.getDate()}
              </div>

              {/* Month (if first day of month) */}
              {date.getDate() === 1 && (
                <div
                  style={{
                    fontSize: '11px',
                    marginTop: '2px',
                    opacity: isSelectedDate ? 1 : 0.6,
                  }}
                >
                  {format(date, 'MMM', { locale })}
                </div>
              )}

              {/* Today indicator */}
              {isTodayDate && !isSelectedDate && (
                <div
                  style={{
                    marginTop: '4px',
                    width: '6px',
                    height: '6px',
                    borderRadius: '50%',
                    backgroundColor: '#1976d2',
                  }}
                />
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
};
