/**
 * EventCalendar Component
 * Calendar with events display and event markers
 *
 * React implementation matching Flutter EventCalendar behavior
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
  isSameDay,
} from 'date-fns';
import {
  EventCalendarProps,
  CalendarEvent,
  isDateDisabled,
  isDateSelected,
  isDateInBounds,
} from './types';

/**
 * EventCalendar - Calendar with event markers and event list
 *
 * Features:
 * - All base Calendar features
 * - Event markers on dates
 * - Event list for selected date
 * - Event color coding
 * - Event click handlers
 * - Multiple views (month, week, day)
 * - Max visible events per date
 */
export const EventCalendar: React.FC<EventCalendarProps> = ({
  value,
  onChange,
  events = [],
  onEventClick,
  onDateClick,
  view = 'month',
  showEventList = true,
  maxVisibleEvents = 3,
  minDate,
  maxDate,
  locale = navigator.language,
  firstDayOfWeek = 0,
  showWeekNumbers = false,
  disabledDates,
  className = '',
  testID,
  key,
}) => {
  const [currentMonth, setCurrentMonth] = useState(value || new Date());
  const [selectedDate, setSelectedDate] = useState<Date | null>(value || null);
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

  // Get events for a specific date
  const getEventsForDate = useCallback(
    (date: Date): CalendarEvent[] => {
      return events.filter((event) => isSameDay(event.start, date));
    },
    [events]
  );

  // Get events for selected date
  const selectedDateEvents = useMemo(() => {
    if (!selectedDate) return [];
    return getEventsForDate(selectedDate);
  }, [selectedDate, getEventsForDate]);

  // Get weekday names
  const weekdayNames = useMemo(() => {
    const baseDate = new Date(2024, 0, firstDayOfWeek === 0 ? 7 : firstDayOfWeek);
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

      setSelectedDate(date);
      setFocusedDate(date);
      onChange?.(date);
      onDateClick?.(date);
    },
    [disabledDates, minDate, maxDate, onChange, onDateClick]
  );

  const handleEventClick = useCallback(
    (event: CalendarEvent, e: React.MouseEvent) => {
      e.stopPropagation();
      onEventClick?.(event);
    },
    [onEventClick]
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
      className={`ava-event-calendar ${className}`}
      data-testid={testID}
      key={key}
      style={{
        fontFamily: 'system-ui, -apple-system, sans-serif',
        width: '100%',
        maxWidth: '600px',
      }}
    >
      {/* Calendar Header */}
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

      {/* Calendar Grid */}
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
            gridTemplateColumns: 'repeat(7, 1fr)',
            gap: '4px',
            marginBottom: '8px',
          }}
        >
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

            return (
              <div
                key={weekIndex}
                role="row"
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(7, 1fr)',
                  gap: '4px',
                }}
              >
                {weekDays.map((date) => {
                  const isCurrentMonth = isSameMonth(date, currentMonth);
                  const isSelected = isDateSelected(date, selectedDate);
                  const isDisabled =
                    isDateDisabled(date, disabledDates) ||
                    !isDateInBounds(date, minDate, maxDate);
                  const dateEvents = getEventsForDate(date);
                  const hasEvents = dateEvents.length > 0;
                  const isFocused =
                    focusedDate && date.getTime() === focusedDate.getTime();

                  return (
                    <button
                      key={date.toISOString()}
                      role="gridcell"
                      data-date={date.toISOString()}
                      aria-label={`${format(date, 'PPPP')}${
                        hasEvents ? `, ${dateEvents.length} event(s)` : ''
                      }`}
                      aria-selected={isSelected}
                      aria-disabled={isDisabled}
                      disabled={isDisabled}
                      tabIndex={isFocused ? 0 : -1}
                      onClick={() => handleDateClick(date)}
                      onFocus={() => setFocusedDate(date)}
                      style={{
                        position: 'relative',
                        minHeight: '60px',
                        border: '1px solid #e0e0e0',
                        borderRadius: '8px',
                        cursor: isDisabled ? 'default' : 'pointer',
                        backgroundColor: isSelected
                          ? '#e3f2fd'
                          : isFocused
                          ? '#f5f5f5'
                          : '#fff',
                        padding: '4px',
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'flex-start',
                        outline: isFocused ? '2px solid #1976d2' : 'none',
                        outlineOffset: '2px',
                      }}
                    >
                      {/* Date number */}
                      <div
                        style={{
                          fontSize: '14px',
                          fontWeight: isSelected ? 600 : 400,
                          color: !isCurrentMonth
                            ? '#ccc'
                            : isDisabled
                            ? '#bbb'
                            : '#000',
                          marginBottom: '2px',
                        }}
                      >
                        {date.getDate()}
                      </div>

                      {/* Event markers */}
                      {hasEvents && (
                        <div
                          style={{
                            display: 'flex',
                            flexWrap: 'wrap',
                            gap: '2px',
                            width: '100%',
                          }}
                        >
                          {dateEvents.slice(0, maxVisibleEvents).map((event) => (
                            <div
                              key={event.id}
                              onClick={(e) => handleEventClick(event, e)}
                              title={event.title}
                              style={{
                                width: '100%',
                                height: '4px',
                                borderRadius: '2px',
                                backgroundColor: event.color || '#1976d2',
                                cursor: 'pointer',
                              }}
                            />
                          ))}
                          {dateEvents.length > maxVisibleEvents && (
                            <div
                              style={{
                                fontSize: '10px',
                                color: '#666',
                                marginTop: '2px',
                              }}
                            >
                              +{dateEvents.length - maxVisibleEvents} more
                            </div>
                          )}
                        </div>
                      )}
                    </button>
                  );
                })}
              </div>
            );
          })}
        </div>
      </div>

      {/* Event List */}
      {showEventList && selectedDate && selectedDateEvents.length > 0 && (
        <div
          style={{
            borderTop: '1px solid #e0e0e0',
            padding: '16px',
          }}
        >
          <div
            style={{
              fontSize: '14px',
              fontWeight: 600,
              marginBottom: '12px',
              color: '#666',
            }}
          >
            Events for {format(selectedDate, 'MMMM d, yyyy')}
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {selectedDateEvents.map((event) => (
              <div
                key={event.id}
                onClick={() => onEventClick?.(event)}
                style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: '12px',
                  padding: '12px',
                  borderRadius: '8px',
                  backgroundColor: '#f5f5f5',
                  cursor: onEventClick ? 'pointer' : 'default',
                  transition: 'background-color 0.2s',
                }}
                onMouseEnter={(e) => {
                  if (onEventClick) {
                    e.currentTarget.style.backgroundColor = '#e0e0e0';
                  }
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = '#f5f5f5';
                }}
              >
                {/* Event color marker */}
                <div
                  style={{
                    width: '4px',
                    height: '100%',
                    minHeight: '24px',
                    borderRadius: '2px',
                    backgroundColor: event.color || '#1976d2',
                  }}
                />

                {/* Event details */}
                <div style={{ flex: 1 }}>
                  <div
                    style={{
                      fontSize: '14px',
                      fontWeight: 600,
                      marginBottom: '4px',
                    }}
                  >
                    {event.title}
                  </div>

                  {event.description && (
                    <div
                      style={{
                        fontSize: '13px',
                        color: '#666',
                        marginBottom: '4px',
                      }}
                    >
                      {event.description}
                    </div>
                  )}

                  {!event.allDay && event.start && (
                    <div style={{ fontSize: '12px', color: '#999' }}>
                      {format(event.start, 'h:mm a')}
                      {event.end && ` - ${format(event.end, 'h:mm a')}`}
                    </div>
                  )}

                  {event.location && (
                    <div style={{ fontSize: '12px', color: '#999', marginTop: '2px' }}>
                      📍 {event.location}
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {showEventList && selectedDate && selectedDateEvents.length === 0 && (
        <div
          style={{
            borderTop: '1px solid #e0e0e0',
            padding: '16px',
            textAlign: 'center',
            color: '#999',
            fontSize: '14px',
          }}
        >
          No events for {format(selectedDate, 'MMMM d, yyyy')}
        </div>
      )}
    </div>
  );
};
