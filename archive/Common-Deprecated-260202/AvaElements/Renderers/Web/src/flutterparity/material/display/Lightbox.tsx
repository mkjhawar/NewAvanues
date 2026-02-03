/**
 * Lightbox Component - Flutter Parity Display
 *
 * Full-screen image viewer with navigation
 * Matches Flutter PhotoView behavior
 *
 * @package com.augmentalis.avaelements.flutter.material.display
 * @since 1.0.0-flutter-parity
 */

import React, { useState, useEffect } from 'react';
import { BaseDisplayProps, ImageSource } from './types';

export interface LightboxProps extends BaseDisplayProps {
  /** Array of images */
  images: ImageSource[];
  /** Initial image index */
  initialIndex?: number;
  /** Close callback */
  onClose: () => void;
  /** Show navigation arrows */
  showArrows?: boolean;
  /** Show thumbnails */
  showThumbnails?: boolean;
  /** Show image counter */
  showCounter?: boolean;
}

export const Lightbox: React.FC<LightboxProps> = ({
  images,
  initialIndex = 0,
  onClose,
  showArrows = true,
  showThumbnails = false,
  showCounter = true,
  className,
  accessibilityLabel,
}) => {
  const [currentIndex, setCurrentIndex] = useState(initialIndex);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      } else if (e.key === 'ArrowLeft') {
        goToPrevious();
      } else if (e.key === 'ArrowRight') {
        goToNext();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    document.body.style.overflow = 'hidden';

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = 'auto';
    };
  }, [currentIndex, images.length]);

  const goToPrevious = () => {
    setCurrentIndex((prev) => (prev - 1 + images.length) % images.length);
  };

  const goToNext = () => {
    setCurrentIndex((prev) => (prev + 1) % images.length);
  };

  return (
    <div
      className={className}
      role="dialog"
      aria-label={accessibilityLabel || 'Image lightbox'}
      aria-modal="true"
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.95)',
        zIndex: 10000,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
      }}
      onClick={onClose}
    >
      {/* Close button */}
      <button
        onClick={onClose}
        aria-label="Close lightbox"
        style={{
          position: 'absolute',
          top: 16,
          right: 16,
          backgroundColor: 'rgba(255, 255, 255, 0.1)',
          color: 'white',
          border: 'none',
          borderRadius: '50%',
          width: 40,
          height: 40,
          cursor: 'pointer',
          fontSize: 24,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 10001,
        }}
      >
        ×
      </button>

      {/* Counter */}
      {showCounter && images.length > 1 && (
        <div
          style={{
            position: 'absolute',
            top: 16,
            left: '50%',
            transform: 'translateX(-50%)',
            color: 'white',
            fontSize: 14,
            fontWeight: 500,
            zIndex: 10001,
          }}
        >
          {currentIndex + 1} / {images.length}
        </div>
      )}

      {/* Main image */}
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          maxWidth: '90vw',
          maxHeight: '90vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <img
          src={images[currentIndex].url}
          alt={images[currentIndex].alt || `Image ${currentIndex + 1}`}
          style={{
            maxWidth: '100%',
            maxHeight: '100%',
            objectFit: 'contain',
          }}
        />
      </div>

      {/* Navigation arrows */}
      {showArrows && images.length > 1 && (
        <>
          <button
            onClick={(e) => {
              e.stopPropagation();
              goToPrevious();
            }}
            aria-label="Previous image"
            style={{
              position: 'absolute',
              left: 16,
              top: '50%',
              transform: 'translateY(-50%)',
              backgroundColor: 'rgba(255, 255, 255, 0.1)',
              color: 'white',
              border: 'none',
              borderRadius: '50%',
              width: 48,
              height: 48,
              cursor: 'pointer',
              fontSize: 24,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            ←
          </button>
          <button
            onClick={(e) => {
              e.stopPropagation();
              goToNext();
            }}
            aria-label="Next image"
            style={{
              position: 'absolute',
              right: 16,
              top: '50%',
              transform: 'translateY(-50%)',
              backgroundColor: 'rgba(255, 255, 255, 0.1)',
              color: 'white',
              border: 'none',
              borderRadius: '50%',
              width: 48,
              height: 48,
              cursor: 'pointer',
              fontSize: 24,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            →
          </button>
        </>
      )}

      {/* Thumbnails */}
      {showThumbnails && images.length > 1 && (
        <div
          onClick={(e) => e.stopPropagation()}
          style={{
            position: 'absolute',
            bottom: 16,
            left: '50%',
            transform: 'translateX(-50%)',
            display: 'flex',
            gap: 8,
            maxWidth: '90vw',
            overflowX: 'auto',
            padding: 8,
          }}
        >
          {images.map((image, index) => (
            <img
              key={index}
              src={image.thumbnail || image.url}
              alt={`Thumbnail ${index + 1}`}
              onClick={() => setCurrentIndex(index)}
              style={{
                width: 60,
                height: 60,
                objectFit: 'cover',
                borderRadius: 4,
                cursor: 'pointer',
                border: index === currentIndex ? '2px solid white' : '2px solid transparent',
                opacity: index === currentIndex ? 1 : 0.6,
              }}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export default Lightbox;
