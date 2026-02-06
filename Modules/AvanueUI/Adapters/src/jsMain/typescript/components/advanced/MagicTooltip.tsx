import React from 'react';
import { Tooltip, TooltipProps } from '@mui/material';

/**
 * MagicTooltip - React/Material-UI Tooltip Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface MagicTooltipProps {
  children: React.ReactElement;
  text: string;
  placement?: 'top' | 'bottom' | 'left' | 'right';
  className?: string;
}

export const MagicTooltip: React.FC<MagicTooltipProps> = ({
  children,
  text,
  placement = 'top',
  className
}) => {
  const tooltipProps: TooltipProps = {
    title: text,
    placement,
    className
  };

  return <Tooltip {...tooltipProps}>{children}</Tooltip>;
};

export default MagicTooltip;
