import React from 'react';
import { Box, IconButton, MobileStepper } from '@mui/material';
import KeyboardArrowLeft from '@mui/icons-material/KeyboardArrowLeft';
import KeyboardArrowRight from '@mui/icons-material/KeyboardArrowRight';

export interface CarouselProps {
  items: React.ReactNode[];
  autoPlay?: boolean;
  interval?: number;
  showIndicators?: boolean;
  showControls?: boolean;
  onChange?: (index: number) => void;
}

export const Carousel: React.FC<CarouselProps> = ({
  items,
  autoPlay = false,
  interval = 3000,
  showIndicators = true,
  showControls = true,
  onChange,
}) => {
  const [activeStep, setActiveStep] = React.useState(0);
  const maxSteps = items.length;

  React.useEffect(() => {
    if (autoPlay && maxSteps > 1) {
      const timer = setInterval(() => {
        setActiveStep((prev) => (prev + 1) % maxSteps);
      }, interval);
      return () => clearInterval(timer);
    }
  }, [autoPlay, interval, maxSteps]);

  React.useEffect(() => {
    onChange?.(activeStep);
  }, [activeStep, onChange]);

  const handleNext = () => {
    setActiveStep((prev) => Math.min(prev + 1, maxSteps - 1));
  };

  const handleBack = () => {
    setActiveStep((prev) => Math.max(prev - 1, 0));
  };

  return (
    <Box sx={{ width: '100%', position: 'relative' }}>
      <Box sx={{ overflow: 'hidden' }}>
        {items[activeStep]}
      </Box>

      {showControls && maxSteps > 1 && (
        <>
          <IconButton
            onClick={handleBack}
            disabled={activeStep === 0}
            sx={{ position: 'absolute', left: 8, top: '50%', transform: 'translateY(-50%)' }}
          >
            <KeyboardArrowLeft />
          </IconButton>
          <IconButton
            onClick={handleNext}
            disabled={activeStep === maxSteps - 1}
            sx={{ position: 'absolute', right: 8, top: '50%', transform: 'translateY(-50%)' }}
          >
            <KeyboardArrowRight />
          </IconButton>
        </>
      )}

      {showIndicators && (
        <MobileStepper
          steps={maxSteps}
          position="static"
          activeStep={activeStep}
          nextButton={<Box />}
          backButton={<Box />}
          sx={{ justifyContent: 'center', bgcolor: 'transparent' }}
        />
      )}
    </Box>
  );
};

export default Carousel;
