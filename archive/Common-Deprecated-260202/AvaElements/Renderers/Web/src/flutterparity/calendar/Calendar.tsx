/**
 * Calendar Component
 * Base calendar component with month view
 *
 * React implementation matching Flutter Calendar behavior
 *
 * @since 3.0.0-flutter-parity-web
 */

import React, { useState, useMemo, useCallback, useRef, useEffect } from 'react';
import {
  startOfMonth,
  endOfMonth,
  startOfWeek,
  endOfWeek,
  addDays,
  addMonths,
  subMonths,
  format,
  isSameMonth,
  getWeek,
} from 'date-fns';
import {
  CalendarProps,
  isDateDisabled,
  isDateHighlighted,
  isDateSelected,
  isDateInBounds,
} from './types';

/**
 * Calendar - Base calendar component with month view
 *
 * Features:
 * - Month view with date selection
 * - Keyboard navigation (arrow keys)
 * - ARIA grid pattern for accessibility
 * - Week numbers (optional)
 * - Date constraints (min/max)
 * - Disabled dates
 * - Highlighted dates
 * - Localization via Intl.DateTimeFormat
 */
export const Calendar: React.FC<CalendarProps> = ({
  value,
  onChange,
  minDate,
  maxDate,
  locale = navigator.language,
  firstDayOfWeek = 0,
  showWeekNumbers = false,
  disabledDates,
  highlightedDates,
  className = '',
  testID,
  key,
}) => {
  const [currentMonth, setCurrentMonth] = useState(value || new Date());
  const [focusedDate, setFocusedDate] = useState<Date | null>(value || null);
  const gridRef = useRef<HTMLDivElement>(null);

  // Generate calendar days for current month
  const calendarDays = useMemo(() => {
    const monthStart = startOfMonth(currentMonth);
    const monthEnd = endOfMonth(currentMonth);
    const calendarStart = startOfWeek(monthStart, { weekStartsOn: firstDayOfWeek });
    const calendarEnd = endOfWeek(monthEnd, { weekStartsOn: firstDayOfWeek });

    const days: Date[] = [];
    let day = calendarStart;

    while (day <= calendarEnd) {
      days.push(day);
      day = addDays(day, 1);
    }

    return days;
  }, [currentMonth, firstDayOfWeek]);

  // Get weekday names
  const weekdayNames = useMemo(() => {
    const baseDate = new Date(2024, 0, firstDayOfWeek === 0 ? 7 : firstDayOfWeek); // Start from a Sunday or Monday
    const names: string[] = [];

    for (let i = 0; i < 7; i++) {
      const date = addDays(baseDate, i);
      names.push(
        new Intl.DateTimeFormat(locale, { weekday: 'short' }).format(date)
      );
    }

    return names;
  }, [locale, firstDayOfWeek]);

  // Navigation handlers
  const handlePrevMonth = useCallback(() => {
    setCurrentMonth((prev) => subMonths(prev, 1));
  }, []);

  const handleNextMonth = useCallback(() => {
    setCurrentMonth((prev) => addMonths(prev, 1));
  }, []);

  const handleDateClick = useCallback(
    (date: Date) => {
      if (
        isDateDisabled(date, disabledDates) ||
        !isDateInBounds(date, minDate, maxDate)
      ) {
        return;
      }

      setFocusedDate(date);
      onChange?.(date);
    },
    [disabledDates, minDate, maxDate, onChange]
  );

  // Keyboard navigation
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (!focusedDate) return;

      let newDate: Date | null = null;

      switch (e.key) {
        case 'ArrowLeft':
          newDate = addDays(focusedDate, -1);
          break;
        case 'ArrowRight':
          newDate = addDays(focusedDate, 1);
          break;
        case 'ArrowUp':
          newDate = addDays(focusedDate, -7);
          break;
        case 'ArrowDown':
          newDate = addDays(focusedDate, 7);
          break;
        case 'Enter':
        case ' ':
          handleDateClick(focusedDate);
          e.preventDefault();
          return;
        case 'PageUp':
          newDate = subMonths(focusedDate, 1);
          break;
        case 'PageDown':
          newDate = addMonths(focusedDate, 1);
          break;
        default:
          return;
      }

      if (newDate) {
        e.preventDefault();
        setFocusedDate(newDate);
        setCurrentMonth(newDate);
      }
    },
    [focusedDate, handleDateClick]
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

  return (
    <div
      className={`ava-calendar ${className}`}
      data-testid={testID}
      key={key}
      style={{
        fontFamily: 'system-ui, -apple-system, sans-serif',
        width: '100%',
        maxWidth: '400px',
      }}
    >
      {/* Header with month/year and navigation */}
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
          onClick={handlePrevMonth}
          aria-label="Previous month"
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
          {new Intl.DateTimeFormat(locale, {
            month: 'long',
            year: 'numeric',
          }).format(currentMonth)}
        </div>

        <button
          onClick={handleNextMonth}
          aria-label="Next month"
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

      {/* Calendar grid */}
      <div
        ref={gridRef}
        role="grid"
        aria-label={`Calendar for ${format(currentMonth, 'MMMM yyyy')}`}
        onKeyDown={handleKeyDown}
        style={{ padding: '16px' }}
      >
        {/* Weekday headers */}
        <div
          role="row"
          style={{
            display: 'grid',
            gridTemplateColumns: showWeekNumbers
              ? `40px repeat(7, 1fr)`
              : 'repeat(7, 1fr)',
            gap: '4px',
            marginBottom: '8px',
          }}
        >
          {showWeekNumbers && <div style={{ width: '40px' }} />}
          {weekdayNames.map((name, i) => (
            <div
              key={i}
              role="columnheader"
              style={{
                textAlign: 'center',
                fontSize: '12px',
                fontWeight: 600,
                color: '#666',
                padding: '4px',
              }}
            >
              {name}
            </div>
          ))}
        </div>

        {/* Calendar days */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
          {Array.from({ length: Math.ceil(calendarDays.length / 7) }).map((_, weekIndex) => {
            const weekDays = calendarDays.slice(weekIndex * 7, (weekIndex + 1) * 7);
            const weekNumber = getWeek(weekDays[0], { weekStartsOn: firstDayOfWeek });

            return (
              <div
                key={weekIndex}
                role="row"
                style={{
                  display: 'grid',
                  gridTemplateColumns: showWeekNumbers
                    ? `40px repeat(7, 1fr)`
                    : 'repeat(7, 1fr)',
                  gap: '4px',
                }}
              >
                {showWeekNumbers && (
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: '11px',
                      color: '#999',
                      width: '40px',
                    }}
                  >
                    {weekNumber}
                  </div>
                )}
                {weekDays.map((date) => {
                  const isCurrentMonth = isSameMonth(date, currentMonth);
                  const isSelected = isDateSelected(date, value);
                  const isDisabled =
                    isDateDisabled(date, disabledDates) ||
                    !isDateInBounds(date, minDate, maxDate);
                  const isHighlighted = isDateHighlighted(date, highlightedDates);
                  const isFocused =
                    focusedDate &&
                    date.getTime() === focusedDate.getTime();

                  return (
                    <button
                      key={date.toISOString()}
                      role="gridcell"
                      data-date={date.toISOString()}
                      aria-label={format(date, 'PPPP', { locale })}
                      aria-selected={isSelected}
                      aria-disabled={isDisabled}
                      disabled={isDisabled}
                      tabIndex={isFocused ? 0 : -1}
                      onClick={() => handleDateClick(date)}
                      onFocus={() => setFocusedDate(date)}
                      style={{
                        aspectRatio: '1',
                        border: 'none',
                        borderRadius: '50%',
                        cursor: isDisabled ? 'default' : 'pointer',
                        backgroundColor: isSelected
                          ? '#1976d2'
                          : isFocused
                          ? '#e3f2fd'
                          : 'transparent',
                        color: isSelected
                          ? '#fff'
                          : !isCurrentMonth
                          ? '#ccc'
                          : isDisabled
                          ? '#bbb'
                          : '#000',
                        fontSize: '14px',
                        fontWeight: isSelected ? 600 : 400,
                        position: 'relative',
                        outline: isFocused ? '2px solid #1976d2' : 'none',
                        outlineOffset: '2px',
                      }}
                    >
                      {date.getDate()}
                      {isHighlighted && (
                        <div
                          style={{
                            position: 'absolute',
                            bottom: '4px',
                            left: '50%',
                            transform: 'translateX(-50%)',
                            width: '4px',
                            height: '4px',
                            borderRadius: '50%',
                            backgroundColor: isSelected ? '#fff' : '#1976d2',
                          }}
                        />
                      )}
                    </button>
                  );
                })}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
