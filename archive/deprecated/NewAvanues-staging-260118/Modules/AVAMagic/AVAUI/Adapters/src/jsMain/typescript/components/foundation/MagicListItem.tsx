import React from 'react';
import { ListItem, ListItemButton, ListItemIcon, ListItemText, Divider } from '@mui/material';

/**
 * MagicListItem - React/Material-UI List Item Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface MagicListItemProps {
  title: string;
  subtitle?: string;
  leadingIcon?: React.ReactNode;
  trailingIcon?: React.ReactNode;
  onClick?: () => void;
  showDivider?: boolean;
  className?: string;
}

export const MagicListItem: React.FC<MagicListItemProps> = ({
  title,
  subtitle,
  leadingIcon,
  trailingIcon,
  onClick,
  showDivider = false,
  className
}) => {
  const content = (
    <>
      {leadingIcon && <ListItemIcon>{leadingIcon}</ListItemIcon>}
      <ListItemText primary={title} secondary={subtitle} />
      {trailingIcon && <ListItemIcon sx={{ minWidth: 'auto' }}>{trailingIcon}</ListItemIcon>}
    </>
  );

  return (
    <>
      {onClick ? (
        <ListItemButton onClick={onClick} className={className}>
          {content}
        </ListItemButton>
      ) : (
        <ListItem className={className}>
          {content}
        </ListItem>
      )}
      {showDivider && <Divider />}
    </>
  );
};

export default MagicListItem;
