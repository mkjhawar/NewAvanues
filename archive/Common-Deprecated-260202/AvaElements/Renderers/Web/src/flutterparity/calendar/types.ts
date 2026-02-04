/**
 * Calendar Types
 * Type definitions for all calendar components
 *
 * @since 3.0.0-flutter-parity-web
 */

export interface CalendarProps {
  /** Currently selected date */
  value?: Date;
  /** Callback when date is selected */
  onChange?: (date: Date) => void;
  /** Minimum selectable date */
  minDate?: Date;
  /** Maximum selectable date */
  maxDate?: Date;
  /** Locale for date formatting (default: user's locale) */
  locale?: string;
  /** First day of week: 0=Sunday, 1=Monday, 6=Saturday */
  firstDayOfWeek?: 0 | 1 | 6;
  /** Whether to show week numbers */
  showWeekNumbers?: boolean;
  /** Disabled dates - array or predicate function */
  disabledDates?: Date[] | ((date: Date) => boolean);
  /** Highlighted dates (e.g., holidays) */
  highlightedDates?: Date[];
  /** Additional CSS class name */
  className?: string;
  /** Test ID for testing */
  testID?: string;
  /** Key for React reconciliation */
  key?: string;
}

export interface DateCalendarProps extends CalendarProps {
  /** Selection mode: single date or date range */
  selectionMode?: 'single' | 'range';
  /** Range start date (when selectionMode is 'range') */
  rangeStart?: Date;
  /** Range end date (when selectionMode is 'range') */
  rangeEnd?: Date;
  /** Callback for range selection */
  onRangeChange?: (start: Date | null, end: Date | null) => void;
}

export interface MonthCalendarProps {
  /** Currently selected year */
  year: number;
  /** Callback when year changes */
  onYearChange?: (year: number) => void;
  /** Currently selected month (1-12) */
  selectedMonth?: number;
  /** Callback when month is selected */
  onMonthSelect?: (month: number, year: number) => void;
  /** Minimum selectable date */
  minDate?: Date;
  /** Maximum selectable date */
  maxDate?: Date;
  /** Locale for month names */
  locale?: string;
  /** Additional CSS class name */
  className?: string;
  /** Test ID for testing */
  testID?: string;
  /** Key for React reconciliation */
  key?: string;
}

export interface WeekCalendarProps {
  /** Start date of the week (typically Monday) */
  startDate: Date;
  /** Currently selected date */
  selectedDate?: Date;
  /** Callback when date is selected */
  onDateSelect?: (date: Date) => void;
  /** Callback when week changes */
  onWeekChange?: (startDate: Date) => void;
  /** First day of week: 0=Sunday, 1=Monday, 6=Saturday */
  firstDayOfWeek?: 0 | 1 | 6;
  /** Locale for day names */
  locale?: string;
  /** Additional CSS class name */
  className?: string;
  /** Test ID for testing */
  testID?: string;
  /** Key for React reconciliation */
  key?: string;
}

export interface CalendarEvent {
  /** Unique event ID */
  id: string;
  /** Event title */
  title: string;
  /** Event start date/time */
  start: Date;
  /** Event end date/time (optional for all-day events) */
  end?: Date;
  /** Whether this is an all-day event */
  allDay?: boolean;
  /** Event color (hex format) */
  color?: string;
  /** Event description */
  description?: string;
  /** Event location */
  location?: string;
}

export interface EventCalendarProps extends CalendarProps {
  /** List of events to display */
  events?: CalendarEvent[];
  /** Callback when event is clicked */
  onEventClick?: (event: CalendarEvent) => void;
  /** Callback when a date is clicked */
  onDateClick?: (date: Date) => void;
  /** View mode */
  view?: 'month' | 'week' | 'day';
  /** Whether to show event list below calendar */
  showEventList?: boolean;
  /** Maximum number of event markers per date */
  maxVisibleEvents?: number;
}

/**
 * Helper function to check if a date is disabled
 */
export function isDateDisabled(
  date: Date,
  disabledDates?: Date[] | ((date: Date) => boolean)
): boolean {
  if (!disabledDates) return false;

  if (typeof disabledDates === 'function') {
    return disabledDates(date);
  }

  return disabledDates.some(
    (d) =>
      d.getFullYear() === date.getFullYear() &&
      d.getMonth() === date.getMonth() &&
      d.getDate() === date.getDate()
  );
}

/**
 * Helper function to check if a date is highlighted
 */
export function isDateHighlighted(date: Date, highlightedDates?: Date[]): boolean {
  if (!highlightedDates) return false;

  return highlightedDates.some(
    (d) =>
      d.getFullYear() === date.getFullYear() &&
      d.getMonth() === date.getMonth() &&
      d.getDate() === date.getDate()
  );
}

/**
 * Helper function to check if a date is selected
 */
export function isDateSelected(date: Date, selectedDate?: Date): boolean {
  if (!selectedDate) return false;

  return (
    date.getFullYear() === selectedDate.getFullYear() &&
    date.getMonth() === selectedDate.getMonth() &&
    date.getDate() === selectedDate.getDate()
  );
}

/**
 * Helper function to check if a date is in range
 */
export function isDateInRange(date: Date, start?: Date, end?: Date): boolean {
  if (!start || !end) return false;

  const dateTime = date.getTime();
  const startTime = start.getTime();
  const endTime = end.getTime();

  return dateTime >= startTime && dateTime <= endTime;
}

/**
 * Helper function to check if a date is within min/max bounds
 */
export function isDateInBounds(date: Date, minDate?: Date, maxDate?: Date): boolean {
  const dateTime = date.getTime();

  if (minDate && dateTime < minDate.getTime()) {
    return false;
  }

  if (maxDate && dateTime > maxDate.getTime()) {
    return false;
  }

  return true;
}
