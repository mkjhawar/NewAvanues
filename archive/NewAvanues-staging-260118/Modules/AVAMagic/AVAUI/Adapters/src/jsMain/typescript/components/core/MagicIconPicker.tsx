import React, { useState } from 'react';
import { Box, TextField, Grid, Paper, IconButton } from '@mui/material';
import * as MuiIcons from '@mui/icons-material';

/**
 * MagicIconPicker - React Icon Picker Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export enum IconLibrary {
  MATERIAL = 'material',
  FONT_AWESOME = 'fontAwesome',
  SF_SYMBOLS = 'sfSymbols',
  CUSTOM = 'custom'
}

export enum IconSize {
  SMALL = 'small',
  MEDIUM = 'medium',
  LARGE = 'large'
}

export interface MagicIconPickerProps {
  selectedIcon: string | null;
  onIconChange: (icon: string | null) => void;
  library?: IconLibrary;
  searchQuery?: string;
  category?: string;
  iconSize?: IconSize;
  columns?: number;
  label?: string;
  className?: string;
}

export const MagicIconPicker: React.FC<MagicIconPickerProps> = ({
  selectedIcon,
  onIconChange,
  library = IconLibrary.MATERIAL,
  searchQuery,
  category,
  iconSize = IconSize.MEDIUM,
  columns = 6,
  label,
  className
}) => {
  const [search, setSearch] = useState(searchQuery || '');

  // Get popular Material-UI icons
  const popularIcons = [
    'Home', 'Search', 'Settings', 'Person', 'Mail',
    'Notifications', 'Favorite', 'Star', 'Delete', 'Edit',
    'Add', 'Remove', 'Check', 'Close', 'ArrowBack',
    'ArrowForward', 'Menu', 'MoreVert', 'Share', 'Save',
    'Download', 'Upload', 'Cloud', 'Lock', 'Visibility'
  ];

  const filteredIcons = popularIcons.filter(icon =>
    icon.toLowerCase().includes(search.toLowerCase())
  );

  const getIconSize = () => {
    switch (iconSize) {
      case IconSize.SMALL: return 'small';
      case IconSize.MEDIUM: return 'medium';
      case IconSize.LARGE: return 'large';
      default: return 'medium';
    }
  };

  const renderIcon = (iconName: string) => {
    const IconComponent = (MuiIcons as any)[iconName];
    return IconComponent ? <IconComponent fontSize={getIconSize()} /> : null;
  };

  return (
    <Paper className={className} sx={{ p: 2 }}>
      {label && (
        <Box sx={{ mb: 2, fontWeight: 'bold' }}>{label}</Box>
      )}

      <TextField
        label="Search Icons"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        fullWidth
        sx={{ mb: 2 }}
      />

      <Grid container spacing={1}>
        {filteredIcons.map((iconName) => (
          <Grid item xs={12 / columns} key={iconName}>
            <IconButton
              onClick={() => onIconChange(iconName)}
              sx={{
                border: selectedIcon === iconName ? '2px solid primary.main' : '1px solid #ccc',
                borderRadius: 1,
                '&:hover': {
                  backgroundColor: 'action.hover'
                }
              }}
            >
              {renderIcon(iconName)}
            </IconButton>
          </Grid>
        ))}
      </Grid>

      {selectedIcon && (
        <Box sx={{ mt: 2, textAlign: 'center' }}>
          <Box sx={{ fontWeight: 'bold', mb: 1 }}>Selected: {selectedIcon}</Box>
          {renderIcon(selectedIcon)}
        </Box>
      )}
    </Paper>
  );
};

export default MagicIconPicker;
