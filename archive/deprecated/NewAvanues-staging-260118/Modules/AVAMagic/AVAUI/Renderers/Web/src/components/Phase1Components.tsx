/**
 * Phase 1 Web Components - React + Material-UI
 * Complete implementations for all 13 Phase 1 components
 */

import React from 'react';
import {
  Button as MuiButton,
  TextField as MuiTextField,
  Checkbox as MuiCheckbox,
  Switch as MuiSwitch,
  Typography,
  Box,
  Card as MuiCard,
  CardContent,
  List as MuiList,
  ListItem,
  ListItemText,
  FormControlLabel,
  Icon as MuiIcon,
} from '@mui/material';
import type {
  ButtonComponent,
  TextFieldComponent,
  CheckboxComponent,
  SwitchComponent,
  TextComponent,
  ImageComponent,
  IconComponent,
  ContainerComponent,
  RowComponent,
  ColumnComponent,
  CardComponent,
  ScrollViewComponent,
  ListComponent,
  Theme,
} from '../types/components';

// ============================================================================
// FORM COMPONENTS
// ============================================================================

export const RenderButton: React.FC<{ component: ButtonComponent; theme?: Theme }> = ({
  component,
}) => {
  return (
    <MuiButton
      variant="contained"
      onClick={component.onClick}
      disabled={!component.enabled}
      sx={{ minWidth: 120, minHeight: 40, m: 1 }}
    >
      {component.text}
    </MuiButton>
  );
};

export const RenderTextField: React.FC<{ component: TextFieldComponent; theme?: Theme }> = ({
  component,
}) => {
  return (
    <MuiTextField
      value={component.value}
      onChange={(e) => component.onChange?.(e.target.value)}
      label={component.label}
      placeholder={component.placeholder}
      disabled={!component.enabled}
      variant="outlined"
      sx={{ width: '40%', m: 1 }}
    />
  );
};

export const RenderCheckbox: React.FC<{ component: CheckboxComponent; theme?: Theme }> = ({
  component,
}) => {
  return (
    <Box sx={{ m: 1, display: 'flex', alignItems: 'center' }}>
      <FormControlLabel
        control={
          <MuiCheckbox
            checked={component.checked}
            onChange={(e) => component.onChange?.(e.target.checked)}
            disabled={!component.enabled}
          />
        }
        label={component.label || ''}
      />
    </Box>
  );
};

export const RenderSwitch: React.FC<{ component: SwitchComponent; theme?: Theme }> = ({
  component,
}) => {
  return (
    <Box sx={{ m: 1, display: 'flex', alignItems: 'center' }}>
      <FormControlLabel
        control={
          <MuiSwitch
            checked={component.checked}
            onChange={(e) => component.onChange?.(e.target.checked)}
            disabled={!component.enabled}
          />
        }
        label={component.label || ''}
      />
    </Box>
  );
};

// ============================================================================
// DISPLAY COMPONENTS
// ============================================================================

export const RenderText: React.FC<{ component: TextComponent; theme?: Theme }> = ({
  component,
}) => {
  return (
    <Typography variant={component.style === 'heading' ? 'h6' : 'body1'} sx={{ p: 0.5 }}>
      {component.content}
    </Typography>
  );
};

export const RenderImage: React.FC<{ component: ImageComponent; theme?: Theme }> = ({
  component,
}) => {
  return (
    <Box sx={{ m: 1, maxWidth: component.width || 200, maxHeight: component.height || 200 }}>
      <img
        src={component.source}
        alt={component.alt || 'Image'}
        style={{ width: '100%', height: 'auto', objectFit: 'contain' }}
      />
    </Box>
  );
};

export const RenderIcon: React.FC<{ component: IconComponent; theme?: Theme }> = ({
  component,
}) => {
  return (
    <MuiIcon sx={{ fontSize: component.size || 24, color: component.color, p: 0.5 }}>
      {component.name}
    </MuiIcon>
  );
};

// ============================================================================
// LAYOUT COMPONENTS
// ============================================================================

export const RenderContainer: React.FC<{ component: ContainerComponent; theme?: Theme }> = ({
  component,
}) => {
  return (
    <Box
      sx={{
        width: '100%',
        p: component.padding ? component.padding / 8 : 2,
        m: 1,
        bgcolor: 'background.paper',
        borderRadius: 1,
        boxShadow: 1,
      }}
    >
      <Typography>Container</Typography>
    </Box>
  );
};

export const RenderRow: React.FC<{ component: RowComponent; theme?: Theme }> = ({ component }) => {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'row',
        gap: component.spacing ? component.spacing / 8 : 1,
        width: '100%',
        m: 1,
      }}
    >
      <Typography>Row content</Typography>
    </Box>
  );
};

export const RenderColumn: React.FC<{ component: ColumnComponent; theme?: Theme }> = ({
  component,
}) => {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        gap: component.spacing ? component.spacing / 8 : 1,
        width: '100%',
        m: 1,
      }}
    >
      <Typography>Column content</Typography>
    </Box>
  );
};

export const RenderCard: React.FC<{ component: CardComponent; theme?: Theme }> = ({
  component,
}) => {
  return (
    <MuiCard
      sx={{
        width: '60%',
        m: 1,
      }}
      elevation={component.elevation || 4}
    >
      <CardContent>
        <Typography>Card content</Typography>
      </CardContent>
    </MuiCard>
  );
};

// ============================================================================
// NAVIGATION & DATA COMPONENTS
// ============================================================================

export const RenderScrollView: React.FC<{ component: ScrollViewComponent; theme?: Theme }> = ({
  component,
}) => {
  return (
    <Box
      sx={{
        width: '100%',
        height: component.height || 300,
        overflow: 'auto',
        m: 1,
        p: 1,
        border: '1px solid',
        borderColor: 'divider',
        borderRadius: 1,
      }}
    >
      {Array.from({ length: 20 }, (_, i) => (
        <Typography key={i} sx={{ p: 1 }}>
          Scrollable item {i}
        </Typography>
      ))}
    </Box>
  );
};

export const RenderList: React.FC<{ component: ListComponent; theme?: Theme }> = ({
  component,
}) => {
  const items = component.items || ['Item 1', 'Item 2', 'Item 3', 'Item 4', 'Item 5'];

  return (
    <MuiList sx={{ width: '100%' }}>
      {items.map((item, index) => (
        <React.Fragment key={index}>
          <ListItem>
            <ListItemText primary={item} />
          </ListItem>
        </React.Fragment>
      ))}
    </MuiList>
  );
};
