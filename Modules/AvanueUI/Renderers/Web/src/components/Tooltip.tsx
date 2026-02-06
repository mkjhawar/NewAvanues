import React from 'react';
import { Tooltip as MuiTooltip, TooltipProps as MuiTooltipProps } from '@mui/material';

export interface TooltipProps {
  title: string | React.ReactNode;
  placement?: MuiTooltipProps['placement'];
  arrow?: boolean;
  children: React.ReactElement;
}

export const Tooltip: React.FC<TooltipProps> = ({
  title,
  placement = 'top',
  arrow = false,
  children,
}) => {
  return (
    <MuiTooltip title={title} placement={placement} arrow={arrow}>
      {children}
    </MuiTooltip>
  );
};

export default Tooltip;
