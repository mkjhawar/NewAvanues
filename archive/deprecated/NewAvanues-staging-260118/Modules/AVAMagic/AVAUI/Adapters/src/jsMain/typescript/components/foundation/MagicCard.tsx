import React from 'react';
import { Card, CardContent, CardProps } from '@mui/material';

/**
 * MagicCard - React/Material-UI Card Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export enum CardVariant {
  DEFAULT = 'elevation',
  OUTLINED = 'outlined',
  FILLED = 'filled'
}

export interface MagicCardProps {
  content: React.ReactNode;
  elevated?: boolean;
  variant?: CardVariant;
  onClick?: () => void;
  className?: string;
}

export const MagicCard: React.FC<MagicCardProps> = ({
  content,
  elevated = false,
  variant = CardVariant.DEFAULT,
  onClick,
  className
}) => {
  const cardProps: CardProps = {
    elevation: elevated ? 4 : 1,
    variant: variant === CardVariant.OUTLINED ? 'outlined' : 'elevation',
    onClick,
    className,
    sx: {
      cursor: onClick ? 'pointer' : 'default',
      backgroundColor: variant === CardVariant.FILLED ? 'action.hover' : undefined
    }
  };

  return (
    <Card {...cardProps}>
      <CardContent>
        {content}
      </CardContent>
    </Card>
  );
};

export default MagicCard;
