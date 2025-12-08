/**
 * SplitButton - Material Design Split Button
 *
 * A button with a primary action and a dropdown menu for additional actions.
 * Follows Material Design 3 patterns for split buttons.
 *
 * @since 3.1.0-phase3
 */

import React, { useState, useRef } from 'react';
import {
  Button,
  ButtonGroup,
  ClickAwayListener,
  Grow,
  Paper,
  Popper,
  MenuItem,
  MenuList,
} from '@mui/material';
import ArrowDropDownIcon from '@mui/icons-material/ArrowDropDown';
import type { SplitButtonProps } from './types';

export const SplitButton: React.FC<SplitButtonProps> = ({
  text,
  icon,
  menuItems = [],
  enabled = true,
  onPressed,
  onMenuItemPressed,
  menuPosition = 'bottom',
  accessibilityLabel,
  ...rest
}) => {
  const [open, setOpen] = useState(false);
  const anchorRef = useRef<HTMLDivElement>(null);

  const handleToggle = () => {
    if (enabled) {
      setOpen((prevOpen) => !prevOpen);
    }
  };

  const handleClose = (event: Event) => {
    if (
      anchorRef.current &&
      anchorRef.current.contains(event.target as HTMLElement)
    ) {
      return;
    }
    setOpen(false);
  };

  const handleMenuItemClick = (value: string, itemOnPressed?: () => void) => {
    setOpen(false);
    if (itemOnPressed) {
      itemOnPressed();
    } else if (onMenuItemPressed) {
      onMenuItemPressed(value);
    }
  };

  const handleMainButtonClick = () => {
    if (enabled && onPressed) {
      onPressed();
    }
  };

  return (
    <>
      <ButtonGroup
        variant="contained"
        ref={anchorRef}
        aria-label={accessibilityLabel || 'split button'}
        disabled={!enabled}
        {...rest}
      >
        <Button
          onClick={handleMainButtonClick}
          startIcon={icon}
          sx={{
            textTransform: 'none',
            minHeight: 40,
            paddingX: 3,
            fontWeight: 600,
          }}
        >
          {text}
        </Button>
        <Button
          size="small"
          aria-controls={open ? 'split-button-menu' : undefined}
          aria-expanded={open ? 'true' : undefined}
          aria-label="select action"
          aria-haspopup="menu"
          onClick={handleToggle}
          sx={{
            paddingX: 1,
            minWidth: 32,
          }}
        >
          <ArrowDropDownIcon />
        </Button>
      </ButtonGroup>
      <Popper
        sx={{
          zIndex: 1300,
        }}
        open={open}
        anchorEl={anchorRef.current}
        role={undefined}
        transition
        disablePortal
        placement={menuPosition}
      >
        {({ TransitionProps, placement }) => (
          <Grow
            {...TransitionProps}
            style={{
              transformOrigin:
                placement === 'bottom' ? 'center top' : 'center bottom',
            }}
          >
            <Paper>
              <ClickAwayListener onClickAway={handleClose}>
                <MenuList id="split-button-menu" autoFocusItem>
                  {menuItems.map((item) => (
                    <MenuItem
                      key={item.value}
                      disabled={item.enabled === false}
                      onClick={() =>
                        handleMenuItemClick(item.value, item.onPressed)
                      }
                    >
                      {item.icon && (
                        <span style={{ marginRight: 8 }}>{item.icon}</span>
                      )}
                      {item.label}
                    </MenuItem>
                  ))}
                </MenuList>
              </ClickAwayListener>
            </Paper>
          </Grow>
        )}
      </Popper>
    </>
  );
};

export default SplitButton;
