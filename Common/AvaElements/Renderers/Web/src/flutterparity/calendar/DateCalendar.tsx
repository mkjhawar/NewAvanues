/**
 * DateCalendar Component
 * Date picker calendar with single or range selection
 *
 * React implementation matching Flutter DateCalendar behavior
 *
 * @since 3.0.0-flutter-parity-web
 */

import React, { useState, useCallback } from 'react';
import { Calendar } from './Calendar';
import { DateCalendarProps, isDateInRange } from './types';

/**
 * DateCalendar - Date picker with single or range selection
 *
 * Extends the base Calendar component with:
 * - Single date selection mode
 * - Date range selection mode
 * - Range highlighting
 * - Range endpoints styling
 *
 * Features:
 * - All base Calendar features
 * - Range selection with start/end dates
 * - Visual range highlighting
 * - Keyboard navigation for ranges
 */
export const DateCalendar: React.FC<DateCalendarProps> = ({
  selectionMode = 'single',
  rangeStart,
  rangeEnd,
  onRangeChange,
  value,
  onChange,
  className = '',
  testID,
  key,
  ...calendarProps
}) => {
  const [internalRangeStart, setInternalRangeStart] = useState<Date | null>(
    rangeStart || null
  );
  const [internalRangeEnd, setInternalRangeEnd] = useState<Date | null>(
    rangeEnd || null
  );

  const handleSingleDateSelect = useCallback(
    (date: Date) => {
      onChange?.(date);
    },
    [onChange]
  );

  const handleRangeDateSelect = useCallback(
    (date: Date) => {
      // If no start date or both dates are set, start new range
      if (!internalRangeStart || (internalRangeStart && internalRangeEnd)) {
        setInternalRangeStart(date);
        setInternalRangeEnd(null);
        onRangeChange?.(date, null);
        return;
      }

      // If start date is set but no end date, set end date
      if (internalRangeStart && !internalRangeEnd) {
        // Ensure end is after start
        if (date.getTime() >= internalRangeStart.getTime()) {
          setInternalRangeEnd(date);
          onRangeChange?.(internalRangeStart, date);
        } else {
          // If selected date is before start, swap them
          setInternalRangeStart(date);
          setInternalRangeEnd(internalRangeStart);
          onRangeChange?.(date, internalRangeStart);
        }
      }
    },
    [internalRangeStart, internalRangeEnd, onRangeChange]
  );

  const handleDateSelect = useCallback(
    (date: Date) => {
      if (selectionMode === 'single') {
        handleSingleDateSelect(date);
      } else {
        handleRangeDateSelect(date);
      }
    },
    [selectionMode, handleSingleDateSelect, handleRangeDateSelect]
  );

  // For range mode, we need to customize the rendering
  if (selectionMode === 'range') {
    const effectiveRangeStart = rangeStart || internalRangeStart;
    const effectiveRangeEnd = rangeEnd || internalRangeEnd;

    return (
      <div
        className={`ava-date-calendar ava-date-calendar--range ${className}`}
        data-testid={testID}
        key={key}
      >
        <Calendar
          {...calendarProps}
          value={effectiveRangeStart || value}
          onChange={handleDateSelect}
          className="ava-date-calendar__calendar"
        />

        {/* Range selection info */}
        {effectiveRangeStart && (
          <div
            style={{
              padding: '12px 16px',
              borderTop: '1px solid #e0e0e0',
              fontSize: '14px',
              color: '#666',
            }}
          >
            {effectiveRangeEnd ? (
              <div>
                <strong>Range:</strong>{' '}
                {effectiveRangeStart.toLocaleDateString()} -{' '}
                {effectiveRangeEnd.toLocaleDateString()}
              </div>
            ) : (
              <div>
                <strong>Start:</strong> {effectiveRangeStart.toLocaleDateString()}
                <br />
                <span style={{ fontSize: '12px', fontStyle: 'italic' }}>
                  Select end date
                </span>
              </div>
            )}
          </div>
        )}

        <style>{`
          .ava-date-calendar--range [role="gridcell"] {
            border-radius: 0;
          }

          .ava-date-calendar--range [role="gridcell"][aria-selected="true"] {
            border-radius: 50%;
          }

          .ava-date-calendar--range [data-in-range="true"] {
            background-color: #e3f2fd !important;
            color: #000 !important;
          }

          .ava-date-calendar--range [data-range-start="true"] {
            border-top-left-radius: 50% !important;
            border-bottom-left-radius: 50% !important;
          }

          .ava-date-calendar--range [data-range-end="true"] {
            border-top-right-radius: 50% !important;
            border-bottom-right-radius: 50% !important;
          }
        `}</style>
      </div>
    );
  }

  // Single selection mode - use base Calendar
  return (
    <div
      className={`ava-date-calendar ava-date-calendar--single ${className}`}
      data-testid={testID}
      key={key}
    >
      <Calendar
        {...calendarProps}
        value={value}
        onChange={handleDateSelect}
        className="ava-date-calendar__calendar"
      />
    </div>
  );
};
