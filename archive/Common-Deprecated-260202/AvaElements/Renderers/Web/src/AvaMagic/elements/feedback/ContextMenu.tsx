/**
 * ContextMenu Component - Phase 3 Feedback Component
 *
 * Right-click or long-press context menu
 * Matches Android/iOS ContextMenu behavior
 *
 * @package com.augmentalis.AvaMagic.elements.feedback
 * @since 3.0.0-phase3
 */

import React, { useState, MouseEvent } from 'react';
import { Menu, MenuItem, ListItemIcon, ListItemText, Divider } from '@mui/material';

export interface ContextMenuItemType {
  /** Unique key */
  key: string;
  /** Label text */
  label: string;
  /** Icon element */
  icon?: React.ReactNode;
  /** Click handler */
  onClick: () => void;
  /** Disabled state */
  disabled?: boolean;
  /** Divider after this item */
  divider?: boolean;
  /** Destructive action styling */
  destructive?: boolean;
}

export interface ContextMenuProps {
  /** Menu items */
  items: ContextMenuItemType[];
  /** Child element to attach menu to */
  children: React.ReactElement;
  /** Custom class name */
  className?: string;
}

export const ContextMenu: React.FC<ContextMenuProps> = ({
  items,
  children,
  className,
}) => {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const handleContextMenu = (event: MouseEvent<HTMLElement>) => {
    event.preventDefault();
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleItemClick = (item: ContextMenuItemType) => {
    item.onClick();
    handleClose();
  };

  return (
    <>
      {React.cloneElement(children, { onContextMenu: handleContextMenu })}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleClose}
        className={className}
        anchorOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
      >
        {items.map((item) => (
          <React.Fragment key={item.key}>
            <MenuItem
              onClick={() => handleItemClick(item)}
              disabled={item.disabled}
              sx={{
                color: item.destructive ? 'error.main' : 'inherit',
              }}
            >
              {item.icon && <ListItemIcon>{item.icon}</ListItemIcon>}
              <ListItemText>{item.label}</ListItemText>
            </MenuItem>
            {item.divider && <Divider />}
          </React.Fragment>
        ))}
      </Menu>
    </>
  );
};

export default ContextMenu;
