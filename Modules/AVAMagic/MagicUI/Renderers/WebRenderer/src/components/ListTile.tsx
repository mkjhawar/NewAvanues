import React from 'react';
import { ListItem, ListItemButton, ListItemIcon, ListItemText, ListItemSecondaryAction } from '@mui/material';

export interface ListTileProps {
  title: string;
  subtitle?: string;
  leading?: React.ReactNode;
  trailing?: React.ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  selected?: boolean;
  dense?: boolean;
}

export const ListTile: React.FC<ListTileProps> = ({
  title,
  subtitle,
  leading,
  trailing,
  onClick,
  disabled = false,
  selected = false,
  dense = false,
}) => {
  const content = (
    <>
      {leading && <ListItemIcon>{leading}</ListItemIcon>}
      <ListItemText primary={title} secondary={subtitle} />
      {trailing && <ListItemSecondaryAction>{trailing}</ListItemSecondaryAction>}
    </>
  );

  if (onClick) {
    return (
      <ListItemButton
        onClick={onClick}
        disabled={disabled}
        selected={selected}
        dense={dense}
      >
        {content}
      </ListItemButton>
    );
  }

  return (
    <ListItem dense={dense} disabled={disabled} selected={selected}>
      {content}
    </ListItem>
  );
};

export default ListTile;
