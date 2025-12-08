/**
 * AppBar Component - Phase 3 Navigation Component
 *
 * Top application bar with title and actions
 * Matches Android/iOS AppBar behavior
 *
 * @package com.augmentalis.AvaMagic.elements.navigation
 * @since 3.0.0-phase3
 */

import React from 'react';
import { AppBar as MuiAppBar, Toolbar, Typography, IconButton, Box } from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';

export interface AppBarProps {
  /** Title text */
  title: string;
  /** Leading icon button */
  leading?: React.ReactNode;
  /** Trailing action buttons */
  actions?: React.ReactNode[];
  /** Menu button handler */
  onMenuClick?: () => void;
  /** Position */
  position?: 'fixed' | 'absolute' | 'sticky' | 'static' | 'relative';
  /** Color */
  color?: 'default' | 'inherit' | 'primary' | 'secondary' | 'transparent';
  /** Elevation */
  elevation?: number;
  /** Custom class name */
  className?: string;
}

export const AppBar: React.FC<AppBarProps> = ({
  title,
  leading,
  actions,
  onMenuClick,
  position = 'static',
  color = 'primary',
  elevation = 4,
  className,
}) => {
  return (
    <MuiAppBar
      position={position}
      color={color}
      elevation={elevation}
      className={className}
    >
      <Toolbar>
        {onMenuClick && !leading && (
          <IconButton
            edge="start"
            color="inherit"
            aria-label="menu"
            onClick={onMenuClick}
            sx={{ mr: 2 }}
          >
            <MenuIcon />
          </IconButton>
        )}
        {leading && <Box sx={{ mr: 2 }}>{leading}</Box>}
        <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
          {title}
        </Typography>
        {actions && (
          <Box sx={{ display: 'flex', gap: 1 }}>
            {actions.map((action, index) => (
              <Box key={index}>{action}</Box>
            ))}
          </Box>
        )}
      </Toolbar>
    </MuiAppBar>
  );
};

export default AppBar;
