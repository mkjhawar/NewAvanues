import React from 'react';
import { Card, CardContent, Typography, Box, Icon } from '@mui/material';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import TrendingDownIcon from '@mui/icons-material/TrendingDown';
import RemoveIcon from '@mui/icons-material/Remove';

export type TrendDirection = 'up' | 'down' | 'neutral';

export interface StatCardProps {
  label: string;
  value: string;
  trend?: TrendDirection;
  trendValue?: number;
  icon?: string;
  subtitle?: string;
  clickable?: boolean;
  onClick?: () => void;
}

export const StatCard: React.FC<StatCardProps> = ({
  label,
  value,
  trend = 'neutral',
  trendValue,
  icon,
  subtitle,
  clickable = false,
  onClick,
}) => {
  const getTrendColor = () => {
    switch (trend) {
      case 'up': return '#4CAF50';
      case 'down': return '#F44336';
      default: return '#9E9E9E';
    }
  };

  const getTrendIcon = () => {
    switch (trend) {
      case 'up': return <TrendingUpIcon fontSize="small" />;
      case 'down': return <TrendingDownIcon fontSize="small" />;
      default: return <RemoveIcon fontSize="small" />;
    }
  };

  const formatTrend = () => {
    if (!trendValue) return '';
    const sign = trend === 'up' ? '+' : trend === 'down' ? '-' : '';
    return `${sign}${trendValue}%`;
  };

  return (
    <Card
      sx={{
        cursor: clickable ? 'pointer' : 'default',
        '&:hover': clickable ? { boxShadow: 4 } : {},
      }}
      onClick={clickable ? onClick : undefined}
    >
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
          {icon && (
            <Icon color="primary" sx={{ mr: 1 }}>
              {icon}
            </Icon>
          )}
          <Typography variant="caption" color="textSecondary">
            {label}
          </Typography>
        </Box>

        <Typography variant="h4" component="div">
          {value}
        </Typography>

        {trendValue !== undefined && trend !== 'neutral' && (
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              mt: 0.5,
              color: getTrendColor(),
            }}
          >
            {getTrendIcon()}
            <Typography variant="body2" sx={{ ml: 0.5 }}>
              {formatTrend()}
            </Typography>
          </Box>
        )}

        {subtitle && (
          <Typography variant="body2" color="textSecondary" sx={{ mt: 0.5 }}>
            {subtitle}
          </Typography>
        )}
      </CardContent>
    </Card>
  );
};

export default StatCard;
