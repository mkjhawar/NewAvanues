import React, { useState } from 'react';
import { Box, TextField, Stack, Paper, Grid } from '@mui/material';

/**
 * MagicColorPicker - React Color Picker Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export enum ColorPickerMode {
  PALETTE = 'palette',
  HUE_WHEEL = 'hueWheel',
  SPECTRUM = 'spectrum',
  SLIDERS = 'sliders'
}

export interface MagicColorPickerProps {
  selectedColor: string;
  onColorChange: (color: string) => void;
  mode?: ColorPickerMode;
  showAlpha?: boolean;
  presetColors?: string[];
  label?: string;
  className?: string;
}

export const MagicColorPicker: React.FC<MagicColorPickerProps> = ({
  selectedColor,
  onColorChange,
  mode = ColorPickerMode.PALETTE,
  showAlpha = true,
  presetColors,
  label,
  className
}) => {
  const [localColor, setLocalColor] = useState(selectedColor);

  const handleColorChange = (color: string) => {
    setLocalColor(color);
    onColorChange(color);
  };

  const renderPalette = () => {
    const colors = presetColors || [
      '#F44336', '#E91E63', '#9C27B0', '#673AB7',
      '#3F51B5', '#2196F3', '#03A9F4', '#00BCD4',
      '#009688', '#4CAF50', '#8BC34A', '#CDDC39',
      '#FFEB3B', '#FFC107', '#FF9800', '#FF5722'
    ];

    return (
      <Grid container spacing={1}>
        {colors.map((color) => (
          <Grid item key={color}>
            <Box
              onClick={() => handleColorChange(color)}
              sx={{
                width: 32,
                height: 32,
                backgroundColor: color,
                borderRadius: 1,
                cursor: 'pointer',
                border: selectedColor === color ? '3px solid #000' : '1px solid #ccc',
                '&:hover': {
                  transform: 'scale(1.1)',
                  transition: 'transform 0.2s'
                }
              }}
            />
          </Grid>
        ))}
      </Grid>
    );
  };

  const renderSliders = () => (
    <Stack spacing={2}>
      <TextField
        label="Color (Hex)"
        value={localColor}
        onChange={(e) => handleColorChange(e.target.value)}
        fullWidth
      />
      <Box
        sx={{
          width: '100%',
          height: 80,
          backgroundColor: localColor,
          borderRadius: 1,
          border: '1px solid #ccc'
        }}
      />
    </Stack>
  );

  return (
    <Paper className={className} sx={{ p: 2 }}>
      {label && (
        <Box sx={{ mb: 2, fontWeight: 'bold' }}>{label}</Box>
      )}

      {mode === ColorPickerMode.PALETTE && renderPalette()}
      {mode === ColorPickerMode.SLIDERS && renderSliders()}
      {mode === ColorPickerMode.HUE_WHEEL && renderSliders()} {/* Simplified - full wheel would need canvas */}
      {mode === ColorPickerMode.SPECTRUM && renderSliders()} {/* Simplified */}
    </Paper>
  );
};

export default MagicColorPicker;
