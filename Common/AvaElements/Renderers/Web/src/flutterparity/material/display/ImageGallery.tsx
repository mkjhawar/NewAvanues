/**
 * ImageGallery Component - Flutter Parity Display
 *
 * Responsive grid of images with lightbox support
 * Matches Flutter ImageGallery behavior
 *
 * @package com.augmentalis.avaelements.flutter.material.display
 * @since 1.0.0-flutter-parity
 */

import React, { useState } from 'react';
import { BaseDisplayProps, ImageSource } from './types';
import { LazyImage } from './LazyImage';
import { Lightbox } from './Lightbox';

export interface ImageGalleryProps extends BaseDisplayProps {
  /** Array of images */
  images: ImageSource[];
  /** Number of columns */
  columns?: number;
  /** Gap between images */
  gap?: number;
  /** Enable lightbox on click */
  enableLightbox?: boolean;
  /** Image height */
  imageHeight?: number;
  /** Aspect ratio */
  aspectRatio?: string;
}

export const ImageGallery: React.FC<ImageGalleryProps> = ({
  images,
  columns = 3,
  gap = 16,
  enableLightbox = true,
  imageHeight = 200,
  aspectRatio,
  className,
  accessibilityLabel,
}) => {
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null);

  const handleImageClick = (index: number) => {
    if (enableLightbox) {
      setLightboxIndex(index);
    }
  };

  return (
    <>
      <div
        className={className}
        role="region"
        aria-label={accessibilityLabel || 'Image gallery'}
        style={{
          display: 'grid',
          gridTemplateColumns: `repeat(${columns}, 1fr)`,
          gap,
        }}
      >
        {images.map((image, index) => (
          <div
            key={index}
            onClick={() => handleImageClick(index)}
            style={{
              cursor: enableLightbox ? 'pointer' : 'default',
              aspectRatio: aspectRatio || undefined,
              height: aspectRatio ? undefined : imageHeight,
              borderRadius: 8,
              overflow: 'hidden',
              transition: 'transform 0.2s',
            }}
            onMouseEnter={(e) => {
              if (enableLightbox) {
                e.currentTarget.style.transform = 'scale(1.05)';
              }
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.transform = 'scale(1)';
            }}
          >
            <LazyImage
              src={image.url}
              alt={image.alt || `Image ${index + 1}`}
              placeholder={image.thumbnail}
              width="100%"
              height="100%"
              objectFit="cover"
            />
          </div>
        ))}
      </div>

      {enableLightbox && lightboxIndex !== null && (
        <Lightbox
          images={images}
          initialIndex={lightboxIndex}
          onClose={() => setLightboxIndex(null)}
        />
      )}
    </>
  );
};

export default ImageGallery;
