import React from 'react';
import { Accordion as MuiAccordion, AccordionSummary, AccordionDetails, Typography } from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';

export interface AccordionItem {
  id: string;
  title: string;
  content: React.ReactNode;
}

export interface AccordionProps {
  items: AccordionItem[];
  expandedIndices?: number[];
  allowMultiple?: boolean;
  onToggle?: (index: number) => void;
}

export const Accordion: React.FC<AccordionProps> = ({
  items,
  expandedIndices = [],
  allowMultiple = false,
  onToggle,
}) => {
  const [expanded, setExpanded] = React.useState<number[]>(expandedIndices);

  const handleChange = (index: number) => {
    if (allowMultiple) {
      setExpanded(prev =>
        prev.includes(index) ? prev.filter(i => i !== index) : [...prev, index]
      );
    } else {
      setExpanded(prev => (prev.includes(index) ? [] : [index]));
    }
    onToggle?.(index);
  };

  return (
    <>
      {items.map((item, index) => (
        <MuiAccordion
          key={item.id}
          expanded={expanded.includes(index)}
          onChange={() => handleChange(index)}
        >
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography>{item.title}</Typography>
          </AccordionSummary>
          <AccordionDetails>{item.content}</AccordionDetails>
        </MuiAccordion>
      ))}
    </>
  );
};

export default Accordion;
