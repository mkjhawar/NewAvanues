import React, { useState } from 'react';
import {
  Box,
  TextField,
  Typography,
  Popover,
  Button,
  IconButton,
} from '@mui/material';
import CalendarTodayIcon from '@mui/icons-material/CalendarToday';
import { DatePicker, LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';

export interface DateRangePickerProps {
  startDate?: Date | null;
  endDate?: Date | null;
  label?: string;
  minDate?: Date;
  maxDate?: Date;
  disabled?: boolean;
  onStartDateChange?: (date: Date | null) => void;
  onEndDateChange?: (date: Date | null) => void;
}

export const DateRangePicker: React.FC<DateRangePickerProps> = ({
  startDate = null,
  endDate = null,
  label,
  minDate,
  maxDate,
  disabled = false,
  onStartDateChange,
  onEndDateChange,
}) => {
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);
  const [tempStartDate, setTempStartDate] = useState<Date | null>(startDate);
  const [tempEndDate, setTempEndDate] = useState<Date | null>(endDate);

  const handleClick = (event: React.MouseEvent<HTMLElement>) => {
    if (!disabled) {
      setAnchorEl(event.currentTarget);
    }
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleApply = () => {
    onStartDateChange?.(tempStartDate);
    onEndDateChange?.(tempEndDate);
    handleClose();
  };

  const formatDate = (date: Date | null) => {
    if (!date) return '';
    return date.toLocaleDateString();
  };

  const displayValue = () => {
    if (startDate && endDate) {
      return `${formatDate(startDate)} - ${formatDate(endDate)}`;
    } else if (startDate) {
      return `From: ${formatDate(startDate)}`;
    } else if (endDate) {
      return `To: ${formatDate(endDate)}`;
    }
    return 'Select date range';
  };

  const open = Boolean(anchorEl);

  return (
    <LocalizationProvider dateAdapter={AdapterDateFns}>
      <Box>
        {label && (
          <Typography variant="caption" color="textSecondary" gutterBottom>
            {label}
          </Typography>
        )}
        <TextField
          fullWidth
          value={displayValue()}
          onClick={handleClick}
          disabled={disabled}
          InputProps={{
            readOnly: true,
            endAdornment: (
              <IconButton size="small" onClick={handleClick} disabled={disabled}>
                <CalendarTodayIcon />
              </IconButton>
            ),
          }}
        />
        <Popover
          open={open}
          anchorEl={anchorEl}
          onClose={handleClose}
          anchorOrigin={{
            vertical: 'bottom',
            horizontal: 'left',
          }}
        >
          <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
            <DatePicker
              label="Start Date"
              value={tempStartDate}
              onChange={setTempStartDate}
              minDate={minDate}
              maxDate={tempEndDate || maxDate}
            />
            <DatePicker
              label="End Date"
              value={tempEndDate}
              onChange={setTempEndDate}
              minDate={tempStartDate || minDate}
              maxDate={maxDate}
            />
            <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
              <Button onClick={handleClose}>Cancel</Button>
              <Button variant="contained" onClick={handleApply}>
                Apply
              </Button>
            </Box>
          </Box>
        </Popover>
      </Box>
    </LocalizationProvider>
  );
};

export default DateRangePicker;
