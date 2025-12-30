/**
 * Flutter Parity List Mappers - Web/React
 *
 * Maps Flutter list tile components to React/Material-UI equivalents.
 * Includes ExpansionTile, CheckboxListTile, SwitchListTile, and RadioListTile.
 *
 * @module FlutterListMappers
 * @since 3.2.0
 */

import React, { useState } from 'react';
import {
  ListItem,
  ListItemButton,
  ListItemText,
  ListItemIcon,
  Checkbox,
  Switch,
  Radio,
  Collapse,
  List,
  Box,
} from '@mui/material';
import { ExpandMore, ExpandLess } from '@mui/icons-material';
import type {
  ExpansionTileComponent,
  CheckboxListTileComponent,
  SwitchListTileComponent,
  FlutterParityComponent,
} from '../../types';
import { renderChildren } from '../../renderer/ReactRenderer';

/**
 * ExpansionTile Component Mapper
 *
 * Maps Flutter ExpansionTile to MUI Collapse + ListItem
 * A single-line ListTile with a trailing button that expands to reveal more content.
 *
 * @param component - ExpansionTile component configuration
 * @returns React element
 */
export const ExpansionTileMapper: React.FC<{ component: ExpansionTileComponent }> = ({
  component,
}) => {
  const {
    title,
    subtitle,
    children = [],
    initiallyExpanded = false,
    key,
  } = component;

  const [expanded, setExpanded] = useState(initiallyExpanded);

  const handleExpandClick = () => {
    setExpanded(!expanded);
  };

  return (
    <Box key={key}>
      <ListItemButton onClick={handleExpandClick}>
        <ListItemText primary={title} secondary={subtitle} />
        {expanded ? <ExpandLess /> : <ExpandMore />}
      </ListItemButton>
      <Collapse in={expanded} timeout="auto" unmountOnExit>
        <List component="div" disablePadding sx={{ pl: 2 }}>
          {renderChildren(children)}
        </List>
      </Collapse>
    </Box>
  );
};

/**
 * CheckboxListTile Component Mapper
 *
 * Maps Flutter CheckboxListTile to MUI ListItem + Checkbox
 * A ListTile with a leading or trailing Checkbox.
 *
 * @param component - CheckboxListTile component configuration
 * @returns React element
 */
export const CheckboxListTileMapper: React.FC<{ component: CheckboxListTileComponent }> = ({
  component,
}) => {
  const { title, subtitle, value, onChanged, key } = component;

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (onChanged) {
      onChanged(event.target.checked);
    }
  };

  return (
    <ListItem
      key={key}
      secondaryAction={
        <Checkbox
          edge="end"
          checked={value}
          onChange={handleChange}
          inputProps={{ 'aria-labelledby': `checkbox-list-label-${title}` }}
        />
      }
      disablePadding
    >
      <ListItemButton role={undefined} onClick={() => onChanged?.(!value)} dense>
        <ListItemText id={`checkbox-list-label-${title}`} primary={title} secondary={subtitle} />
      </ListItemButton>
    </ListItem>
  );
};

/**
 * SwitchListTile Component Mapper
 *
 * Maps Flutter SwitchListTile to MUI ListItem + Switch
 * A ListTile with a trailing Switch.
 *
 * @param component - SwitchListTile component configuration
 * @returns React element
 */
export const SwitchListTileMapper: React.FC<{ component: SwitchListTileComponent }> = ({
  component,
}) => {
  const { title, subtitle, value, onChanged, key } = component;

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (onChanged) {
      onChanged(event.target.checked);
    }
  };

  return (
    <ListItem
      key={key}
      secondaryAction={
        <Switch
          edge="end"
          checked={value}
          onChange={handleChange}
          inputProps={{ 'aria-labelledby': `switch-list-label-${title}` }}
        />
      }
      disablePadding
    >
      <ListItemButton role={undefined} onClick={() => onChanged?.(!value)} dense>
        <ListItemText id={`switch-list-label-${title}`} primary={title} secondary={subtitle} />
      </ListItemButton>
    </ListItem>
  );
};

/**
 * RadioListTile Component Mapper
 *
 * Maps Flutter RadioListTile to MUI ListItem + Radio
 * A ListTile with a leading or trailing Radio button.
 *
 * @param component - RadioListTile component configuration
 * @returns React element
 */
export interface RadioListTileComponent extends FlutterParityComponent {
  type: 'RadioListTile';
  title: string;
  subtitle?: string;
  value: string;
  groupValue: string;
  onChanged?: (value: string) => void;
}

export const RadioListTileMapper: React.FC<{ component: RadioListTileComponent }> = ({
  component,
}) => {
  const { title, subtitle, value, groupValue, onChanged, key } = component;

  const handleChange = () => {
    if (onChanged) {
      onChanged(value);
    }
  };

  const isSelected = value === groupValue;

  return (
    <ListItem
      key={key}
      secondaryAction={
        <Radio
          edge="end"
          checked={isSelected}
          onChange={handleChange}
          value={value}
          inputProps={{ 'aria-labelledby': `radio-list-label-${title}` }}
        />
      }
      disablePadding
    >
      <ListItemButton role={undefined} onClick={handleChange} dense>
        <ListItemText id={`radio-list-label-${title}`} primary={title} secondary={subtitle} />
      </ListItemButton>
    </ListItem>
  );
};

/**
 * Export all list mappers
 */
export const FlutterListMappers = {
  ExpansionTile: ExpansionTileMapper,
  CheckboxListTile: CheckboxListTileMapper,
  SwitchListTile: SwitchListTileMapper,
  RadioListTile: RadioListTileMapper,
};

export default FlutterListMappers;
