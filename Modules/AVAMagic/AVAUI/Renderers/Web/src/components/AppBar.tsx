import React from 'react';
import { AppBar as MuiAppBar, Toolbar, Typography, IconButton, Box } from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';

export interface AppBarProps {
  title?: string;
  position?: 'fixed' | 'absolute' | 'sticky' | 'static' | 'relative';
  color?: 'default' | 'inherit' | 'primary' | 'secondary' | 'transparent';
  elevation?: number;
  leading?: React.ReactNode;
  actions?: React.ReactNode[];
  onMenuClick?: () => void;
}

export const AppBar: React.FC<AppBarProps> = ({
  title,
  position = 'static',
  color = 'primary',
  elevation = 4,
  leading,
  actions,
  onMenuClick,
}) => {
  return (
    <MuiAppBar position={position} color={color} elevation={elevation}>
      <Toolbar>
        {leading || (onMenuClick && (
          <IconButton
            edge="start"
            color="inherit"
            aria-label="menu"
            onClick={onMenuClick}
            sx={{ mr: 2 }}
          >
            <MenuIcon />
          </IconButton>
        ))}
        {title && (
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            {title}
          </Typography>
        )}
        {actions && (
          <Box sx={{ display: 'flex', gap: 1 }}>
            {actions.map((action, index) => (
              <React.Fragment key={index}>{action}</React.Fragment>
            ))}
          </Box>
        )}
      </Toolbar>
    </MuiAppBar>
  );
};

export default AppBar;
