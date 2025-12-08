/**
 * Breadcrumb Component - Phase 3 Navigation Component
 *
 * Navigation breadcrumb trail
 * Matches Android/iOS Breadcrumb behavior
 *
 * @package com.augmentalis.AvaMagic.elements.navigation
 * @since 3.0.0-phase3
 */

import React from 'react';
import { Breadcrumbs, Link, Typography } from '@mui/material';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';

export interface BreadcrumbItem {
  /** Item key */
  key: string;
  /** Label */
  label: string;
  /** Link URL (if clickable) */
  href?: string;
  /** Click handler */
  onClick?: () => void;
}

export interface BreadcrumbProps {
  /** Breadcrumb items */
  items: BreadcrumbItem[];
  /** Separator icon */
  separator?: React.ReactNode;
  /** Maximum items to display */
  maxItems?: number;
  /** Items to show before collapse */
  itemsBeforeCollapse?: number;
  /** Items to show after collapse */
  itemsAfterCollapse?: number;
  /** Custom class name */
  className?: string;
}

export const Breadcrumb: React.FC<BreadcrumbProps> = ({
  items,
  separator = <NavigateNextIcon fontSize="small" />,
  maxItems,
  itemsBeforeCollapse = 1,
  itemsAfterCollapse = 1,
  className,
}) => {
  return (
    <Breadcrumbs
      separator={separator}
      maxItems={maxItems}
      itemsBeforeCollapse={itemsBeforeCollapse}
      itemsAfterCollapse={itemsAfterCollapse}
      className={className}
      aria-label="breadcrumb"
    >
      {items.map((item, index) => {
        const isLast = index === items.length - 1;

        if (isLast) {
          return (
            <Typography key={item.key} color="text.primary">
              {item.label}
            </Typography>
          );
        }

        return (
          <Link
            key={item.key}
            underline="hover"
            color="inherit"
            href={item.href}
            onClick={item.onClick}
            sx={{ cursor: item.onClick || item.href ? 'pointer' : 'default' }}
          >
            {item.label}
          </Link>
        );
      })}
    </Breadcrumbs>
  );
};

export default Breadcrumb;
