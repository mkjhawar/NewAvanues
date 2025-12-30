import React from 'react';
import { Box, Typography } from '@mui/material';
import { Timeline as MuiTimeline, TimelineItem, TimelineSeparator, TimelineConnector, TimelineContent, TimelineDot, TimelineOppositeContent } from '@mui/lab';
import CheckIcon from '@mui/icons-material/Check';

export interface TimelineItemData {
  id: string;
  title: string;
  description?: string;
  timestamp?: string;
  icon?: React.ReactNode;
  completed?: boolean;
}

export interface TimelineProps {
  items: TimelineItemData[];
  orientation?: 'vertical' | 'horizontal';
}

export const Timeline: React.FC<TimelineProps> = ({
  items,
  orientation = 'vertical',
}) => {
  return (
    <MuiTimeline position="right">
      {items.map((item, index) => (
        <TimelineItem key={item.id}>
          <TimelineOppositeContent sx={{ flex: 0.2 }}>
            {item.timestamp && (
              <Typography variant="caption" color="text.secondary">
                {item.timestamp}
              </Typography>
            )}
          </TimelineOppositeContent>
          <TimelineSeparator>
            <TimelineDot color={item.completed ? 'primary' : 'grey'}>
              {item.icon || (item.completed && <CheckIcon fontSize="small" />)}
            </TimelineDot>
            {index < items.length - 1 && <TimelineConnector />}
          </TimelineSeparator>
          <TimelineContent>
            <Typography variant="subtitle2">{item.title}</Typography>
            {item.description && (
              <Typography variant="body2" color="text.secondary">
                {item.description}
              </Typography>
            )}
          </TimelineContent>
        </TimelineItem>
      ))}
    </MuiTimeline>
  );
};

export default Timeline;
