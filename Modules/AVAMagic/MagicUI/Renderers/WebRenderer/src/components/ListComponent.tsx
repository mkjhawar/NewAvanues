import React from 'react';
import { List, ListItem, ListItemButton, ListItemIcon, ListItemText, ListItemSecondaryAction, Divider } from '@mui/material';

export interface ListComponentItem {
  primary: string;
  secondary?: string;
  icon?: React.ReactNode;
  action?: React.ReactNode;
  onClick?: () => void;
  selected?: boolean;
  disabled?: boolean;
  divider?: boolean;
}

export interface ListComponentProps {
  items: ListComponentItem[];
  dense?: boolean;
  disablePadding?: boolean;
  subheader?: React.ReactNode;
}

export const ListComponent: React.FC<ListComponentProps> = ({
  items,
  dense = false,
  disablePadding = false,
  subheader,
}) => {
  return (
    <List dense={dense} disablePadding={disablePadding} subheader={subheader}>
      {items.map((item, index) => (
        <React.Fragment key={index}>
          {item.onClick ? (
            <ListItemButton
              selected={item.selected}
              disabled={item.disabled}
              onClick={item.onClick}
            >
              {item.icon && <ListItemIcon>{item.icon}</ListItemIcon>}
              <ListItemText primary={item.primary} secondary={item.secondary} />
              {item.action && <ListItemSecondaryAction>{item.action}</ListItemSecondaryAction>}
            </ListItemButton>
          ) : (
            <ListItem disabled={item.disabled}>
              {item.icon && <ListItemIcon>{item.icon}</ListItemIcon>}
              <ListItemText primary={item.primary} secondary={item.secondary} />
              {item.action && <ListItemSecondaryAction>{item.action}</ListItemSecondaryAction>}
            </ListItem>
          )}
          {item.divider && <Divider />}
        </React.Fragment>
      ))}
    </List>
  );
};

export default ListComponent;
