/**
 * ImageCarousel Component - Flutter Parity Display
 *
 * Swipeable image gallery with navigation controls
 * Matches Flutter ImageCarousel behavior
 *
 * @package com.augmentalis.avaelements.flutter.material.display
 * @since 1.0.0-flutter-parity
 */

import React, { useState } from 'react';
import { BaseDisplayProps, ImageSource } from './types';

export interface ImageCarouselProps extends BaseDisplayProps {
  /** Array of images */
  images: ImageSource[];
  /** Show navigation arrows */
  showArrows?: boolean;
  /** Show dot indicators */
  showDots?: boolean;
  /** Auto-play interval (ms) */
  autoPlay?: number;
  /** Carousel height */
  height?: number;
  /** On image click */
  onImageClick?: (index: number) => void;
}

export const ImageCarousel: React.FC<ImageCarouselProps> = ({
  images,
  showArrows = true,
  showDots = true,
  autoPlay,
  height = 400,
  onImageClick,
  className,
  accessibilityLabel,
}) => {
  const [currentIndex, setCurrentIndex] = useState(0);

  React.useEffect(() => {
    if (!autoPlay) return;

    const interval = setInterval(() => {
      setCurrentIndex((prev) => (prev + 1) % images.length);
    }, autoPlay);

    return () => clearInterval(interval);
  }, [autoPlay, images.length]);

  const goToSlide = (index: number) => {
    setCurrentIndex(index);
  };

  const goToPrevious = () => {
    setCurrentIndex((prev) => (prev - 1 + images.length) % images.length);
  };

  const goToNext = () => {
    setCurrentIndex((prev) => (prev + 1) % images.length);
  };

  return (
    <div
      className={className}
      role="region"
      aria-label={accessibilityLabel || 'Image carousel'}
      style={{ position: 'relative', width: '100%', height, overflow: 'hidden' }}
    >
      {/* Images */}
      <div
        style={{
          display: 'flex',
          transform: `translateX(-${currentIndex * 100}%)`,
          transition: 'transform 0.3s ease-in-out',
          height: '100%',
        }}
      >
        {images.map((image, index) => (
          <img
            key={index}
            src={image.url}
            alt={image.alt || `Slide ${index + 1}`}
            onClick={() => onImageClick?.(index)}
            style={{
              minWidth: '100%',
              height: '100%',
              objectFit: 'cover',
              cursor: onImageClick ? 'pointer' : 'default',
            }}
          />
        ))}
      </div>

      {/* Navigation arrows */}
      {showArrows && images.length > 1 && (
        <>
          <button
            onClick={goToPrevious}
            aria-label="Previous image"
            style={{
              position: 'absolute',
              top: '50%',
              left: 16,
              transform: 'translateY(-50%)',
              backgroundColor: 'rgba(0, 0, 0, 0.5)',
              color: 'white',
              border: 'none',
              borderRadius: '50%',
              width: 40,
              height: 40,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            ←
          </button>
          <button
            onClick={goToNext}
            aria-label="Next image"
            style={{
              position: 'absolute',
              top: '50%',
              right: 16,
              transform: 'translateY(-50%)',
              backgroundColor: 'rgba(0, 0, 0, 0.5)',
              color: 'white',
              border: 'none',
              borderRadius: '50%',
              width: 40,
              height: 40,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            →
          </button>
        </>
      )}

      {/* Dot indicators */}
      {showDots && images.length > 1 && (
        <div
          style={{
            position: 'absolute',
            bottom: 16,
            left: '50%',
            transform: 'translateX(-50%)',
            display: 'flex',
            gap: 8,
          }}
        >
          {images.map((_, index) => (
            <button
              key={index}
              onClick={() => goToSlide(index)}
              aria-label={`Go to slide ${index + 1}`}
              aria-current={index === currentIndex}
              style={{
                width: 8,
                height: 8,
                borderRadius: '50%',
                border: 'none',
                backgroundColor: index === currentIndex ? 'white' : 'rgba(255, 255, 255, 0.5)',
                cursor: 'pointer',
                padding: 0,
              }}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export default ImageCarousel;
