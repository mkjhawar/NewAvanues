import React from 'react';
import { AppBar, Toolbar, Typography, IconButton } from '@mui/material';

/**
 * MagicAppBar - React/Material-UI App Bar Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface AppBarButton {
  icon: React.ReactNode;
  onClick: () => void;
}

export interface MagicAppBarProps {
  title: string;
  leadingButton?: AppBarButton;
  trailingButton?: AppBarButton;
  className?: string;
}

export const MagicAppBar: React.FC<MagicAppBarProps> = ({
  title,
  leadingButton,
  trailingButton,
  className
}) => {
  return (
    <AppBar position="static" className={className}>
      <Toolbar>
        {leadingButton && (
          <IconButton edge="start" color="inherit" onClick={leadingButton.onClick}>
            {leadingButton.icon}
          </IconButton>
        )}
        <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
          {title}
        </Typography>
        {trailingButton && (
          <IconButton edge="end" color="inherit" onClick={trailingButton.onClick}>
            {trailingButton.icon}
          </IconButton>
        )}
      </Toolbar>
    </AppBar>
  );
};

export default MagicAppBar;
