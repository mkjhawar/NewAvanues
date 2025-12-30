import React from 'react';
import { Box, Card, CardContent, Typography, IconButton, Stack } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';

export type NotificationSeverity = 'info' | 'success' | 'warning' | 'error';

export interface NotificationItem {
  id: string;
  message: string;
  title?: string;
  severity?: NotificationSeverity;
  dismissible?: boolean;
}

export interface NotificationCenterProps {
  notifications: NotificationItem[];
  maxDisplayed?: number;
  onDismiss?: (id: string) => void;
}

export const NotificationCenter: React.FC<NotificationCenterProps> = ({
  notifications,
  maxDisplayed = 5,
  onDismiss,
}) => {
  const getSeverityColor = (severity: NotificationSeverity = 'info') => {
    switch (severity) {
      case 'info': return '#E3F2FD';
      case 'success': return '#E8F5E9';
      case 'warning': return '#FFF8E1';
      case 'error': return '#FFEBEE';
    }
  };

  const displayedNotifications = notifications.slice(0, maxDisplayed);

  return (
    <Stack spacing={1}>
      {displayedNotifications.map((notification) => (
        <Card
          key={notification.id}
          sx={{
            bgcolor: getSeverityColor(notification.severity),
          }}
        >
          <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
            <Box sx={{ display: 'flex', alignItems: 'flex-start' }}>
              <Box sx={{ flex: 1 }}>
                {notification.title && (
                  <Typography variant="subtitle2" gutterBottom>
                    {notification.title}
                  </Typography>
                )}
                <Typography variant="body2">
                  {notification.message}
                </Typography>
              </Box>
              {notification.dismissible !== false && (
                <IconButton
                  size="small"
                  onClick={() => onDismiss?.(notification.id)}
                >
                  <CloseIcon fontSize="small" />
                </IconButton>
              )}
            </Box>
          </CardContent>
        </Card>
      ))}
    </Stack>
  );
};

export default NotificationCenter;
