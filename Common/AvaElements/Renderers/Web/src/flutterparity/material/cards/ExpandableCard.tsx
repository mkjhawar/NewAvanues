/**
 * ExpandableCard Component - Flutter Parity Material Design
 *
 * A Material Design 3 card that can expand/collapse to show more content.
 * Commonly used in FAQs, settings panels, and collapsible content sections.
 *
 * @since 3.0.0-flutter-parity
 */

import React, { useState, useEffect } from 'react';
import { Card, CardHeader, CardContent, Collapse, Typography, IconButton, Icon, Box, Avatar } from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';

export interface HeaderAction {
  id: string;
  icon: string;
  label?: string;
  enabled?: boolean;
}

export interface ExpandableCardProps {
  title: string;
  subtitle?: string;
  icon?: string;
  summaryContent?: string;
  expandedContent: string;
  initiallyExpanded?: boolean;
  expanded?: boolean;
  showDivider?: boolean;
  expandIcon?: string;
  collapseIcon?: string;
  animationDuration?: number;
  headerActions?: HeaderAction[];
  contentDescription?: string;
  onExpansionChanged?: (expanded: boolean) => void;
  onHeaderActionPressed?: (actionId: string) => void;
  className?: string;
}

export const ExpandableCard: React.FC<ExpandableCardProps> = ({
  title,
  subtitle,
  icon,
  summaryContent,
  expandedContent,
  initiallyExpanded = false,
  expanded: controlledExpanded,
  showDivider = true,
  expandIcon,
  collapseIcon,
  animationDuration = 300,
  headerActions = [],
  contentDescription,
  onExpansionChanged,
  onHeaderActionPressed,
  className = '',
}) => {
  const isControlled = controlledExpanded !== undefined;
  const [internalExpanded, setInternalExpanded] = useState(initiallyExpanded);

  const isExpanded = isControlled ? controlledExpanded : internalExpanded;

  useEffect(() => {
    if (!isControlled && initiallyExpanded !== internalExpanded) {
      setInternalExpanded(initiallyExpanded);
    }
  }, [initiallyExpanded, isControlled, internalExpanded]);

  const handleExpandClick = () => {
    const newExpanded = !isExpanded;
    if (!isControlled) {
      setInternalExpanded(newExpanded);
    }
    onExpansionChanged?.(newExpanded);
  };

  const handleHeaderActionClick = (e: React.MouseEvent, actionId: string) => {
    e.stopPropagation();
    onHeaderActionPressed?.(actionId);
  };

  const ariaLabel =
    contentDescription ||
    `${title}${subtitle ? ', ' + subtitle : ''}, ${isExpanded ? 'expanded' : 'collapsed'}`;

  return (
    <Card
      elevation={2}
      className={`expandable-card ${className}`}
      sx={{
        width: '100%',
        transition: 'all 0.3s ease',
      }}
      role="region"
      aria-label={ariaLabel}
      aria-expanded={isExpanded}
    >
      <CardHeader
        avatar={
          icon ? (
            <Avatar sx={{ bgcolor: 'primary.main' }}>
              <Icon>{icon}</Icon>
            </Avatar>
          ) : undefined
        }
        action={
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            {headerActions.map((action) => (
              <IconButton
                key={action.id}
                onClick={(e) => handleHeaderActionClick(e, action.id)}
                disabled={action.enabled === false}
                aria-label={action.label || action.id}
                title={action.label}
              >
                <Icon>{action.icon}</Icon>
              </IconButton>
            ))}
            <IconButton
              onClick={handleExpandClick}
              aria-label={isExpanded ? 'Collapse' : 'Expand'}
              sx={{
                transform: isExpanded ? 'rotate(180deg)' : 'rotate(0deg)',
                transition: `transform ${animationDuration}ms`,
              }}
            >
              {isExpanded && collapseIcon ? (
                <Icon>{collapseIcon}</Icon>
              ) : expandIcon ? (
                <Icon>{expandIcon}</Icon>
              ) : (
                <ExpandMoreIcon />
              )}
            </IconButton>
          </Box>
        }
        title={
          <Typography variant="h6" component="h3" fontWeight="bold">
            {title}
          </Typography>
        }
        subheader={subtitle}
        sx={{
          cursor: 'pointer',
          '&:hover': {
            backgroundColor: 'action.hover',
          },
        }}
        onClick={handleExpandClick}
      />

      {summaryContent && !isExpanded && (
        <CardContent sx={{ pt: 0 }}>
          <Typography variant="body2" color="text.secondary">
            {summaryContent}
          </Typography>
        </CardContent>
      )}

      <Collapse
        in={isExpanded}
        timeout={animationDuration}
        unmountOnExit
        sx={{
          transition: `all ${animationDuration}ms cubic-bezier(0.4, 0, 0.2, 1)`,
        }}
      >
        {showDivider && (
          <Box
            sx={{
              height: 1,
              backgroundColor: 'divider',
              mx: 2,
            }}
          />
        )}
        <CardContent>
          <Typography variant="body2" color="text.primary">
            {expandedContent}
          </Typography>
        </CardContent>
      </Collapse>
    </Card>
  );
};

export default ExpandableCard;
