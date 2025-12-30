import React from 'react';
import { Rating, RatingProps } from '@mui/material';

/**
 * MagicRating - React/Material-UI Rating Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface MagicRatingProps {
  value: number;
  onChange: (value: number) => void;
  maxRating?: number;
  readOnly?: boolean;
  className?: string;
}

export const MagicRating: React.FC<MagicRatingProps> = ({
  value,
  onChange,
  maxRating = 5,
  readOnly = false,
  className
}) => {
  const ratingProps: RatingProps = {
    value,
    onChange: (_, newValue) => onChange(newValue || 0),
    max: maxRating,
    readOnly,
    className
  };

  return <Rating {...ratingProps} />;
};

export default MagicRating;
