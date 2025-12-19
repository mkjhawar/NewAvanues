import React from 'react';
import { Box, IconButton, Tooltip, Fab } from '@mui/material';

export interface NavigationRailItem {
  label: string;
  icon: React.ReactNode;
  onClick?: () => void;
  selected?: boolean;
  disabled?: boolean;
}

export interface NavigationRailProps {
  items: NavigationRailItem[];
  selectedIndex?: number;
  onChange?: (index: number) => void;
  fab?: React.ReactNode;
  header?: React.ReactNode;
  showLabels?: boolean;
  width?: number;
}

export const NavigationRail: React.FC<NavigationRailProps> = ({
  items,
  selectedIndex,
  onChange,
  fab,
  header,
  showLabels = false,
  width = 72,
}) => {
  return (
    <Box
      sx={{
        width,
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        bgcolor: 'background.paper',
        borderRight: 1,
        borderColor: 'divider',
        py: 2,
      }}
    >
      {header && <Box sx={{ mb: 2 }}>{header}</Box>}

      {fab && <Box sx={{ mb: 2 }}>{fab}</Box>}

      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: 1,
          flexGrow: 1,
        }}
      >
        {items.map((item, index) => (
          <Tooltip key={index} title={item.label} placement="right">
            <Box
              sx={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                cursor: item.disabled ? 'default' : 'pointer',
                opacity: item.disabled ? 0.5 : 1,
                p: 1,
                borderRadius: 2,
                bgcolor: (selectedIndex === index || item.selected) ? 'action.selected' : 'transparent',
                '&:hover': {
                  bgcolor: item.disabled ? 'transparent' : 'action.hover',
                },
              }}
              onClick={() => {
                if (!item.disabled) {
                  item.onClick?.();
                  onChange?.(index);
                }
              }}
            >
              <IconButton
                color={(selectedIndex === index || item.selected) ? 'primary' : 'default'}
                disabled={item.disabled}
                sx={{ p: 0.5 }}
              >
                {item.icon}
              </IconButton>
              {showLabels && (
                <Box
                  component="span"
                  sx={{
                    fontSize: '0.625rem',
                    mt: 0.5,
                    color: (selectedIndex === index || item.selected) ? 'primary.main' : 'text.secondary',
                  }}
                >
                  {item.label}
                </Box>
              )}
            </Box>
          </Tooltip>
        ))}
      </Box>
    </Box>
  );
};

export default NavigationRail;
