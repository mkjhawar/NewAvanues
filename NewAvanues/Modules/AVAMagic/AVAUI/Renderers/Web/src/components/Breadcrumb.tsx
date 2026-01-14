import React from 'react';
import { Breadcrumbs, Link, Typography } from '@mui/material';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';

export interface BreadcrumbItem {
  label: string;
  href?: string;
  onClick?: () => void;
}

export interface BreadcrumbProps {
  items: BreadcrumbItem[];
  separator?: React.ReactNode;
  maxItems?: number;
}

export const Breadcrumb: React.FC<BreadcrumbProps> = ({
  items,
  separator = <NavigateNextIcon fontSize="small" />,
  maxItems,
}) => {
  return (
    <Breadcrumbs separator={separator} maxItems={maxItems} aria-label="breadcrumb">
      {items.map((item, index) => {
        const isLast = index === items.length - 1;

        if (isLast) {
          return (
            <Typography key={index} color="text.primary">
              {item.label}
            </Typography>
          );
        }

        return (
          <Link
            key={index}
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
