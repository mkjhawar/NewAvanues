/**
 * Zoom Component
 *
 * Zoomable content wrapper with pinch and wheel zoom support.
 *
 * @since 3.0.0-flutter-parity-web
 * @module flutterparity/data
 */

import React, { useState, useRef, useCallback, useEffect } from 'react';
import type { ZoomProps } from './types';

/**
 * Zoom - Zoomable content wrapper component
 *
 * @example
 * ```tsx
 * <Zoom
 *   minZoom={0.5}
 *   maxZoom={3}
 *   initialZoom={1}
 *   enablePinch
 *   enableWheel
 *   onZoomChange={(zoom) => console.log('Zoom:', zoom)}
 * >
 *   <img src="large-image.jpg" alt="Zoomable" />
 * </Zoom>
 * ```
 */
export const Zoom: React.FC<ZoomProps> = ({
  children,
  minZoom = 0.5,
  maxZoom = 3,
  initialZoom = 1,
  step = 0.1,
  enablePinch = true,
  enableWheel = true,
  onZoomChange,
  className = '',
  style,
  testId,
}) => {
  const [zoom, setZoom] = useState(initialZoom);
  const [position, setPosition] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
  const containerRef = useRef<HTMLDivElement>(null);

  const clampZoom = useCallback(
    (value: number) => Math.max(minZoom, Math.min(maxZoom, value)),
    [minZoom, maxZoom]
  );

  const handleZoomChange = useCallback(
    (newZoom: number) => {
      const clampedZoom = clampZoom(newZoom);
      setZoom(clampedZoom);
      if (onZoomChange) {
        onZoomChange(clampedZoom);
      }
    },
    [clampZoom, onZoomChange]
  );

  // Wheel zoom
  const handleWheel = useCallback(
    (e: React.WheelEvent) => {
      if (!enableWheel) return;

      e.preventDefault();
      const delta = e.deltaY > 0 ? -step : step;
      handleZoomChange(zoom + delta);
    },
    [enableWheel, step, zoom, handleZoomChange]
  );

  // Touch/Pinch zoom
  const lastTouchDistance = useRef<number | null>(null);

  const getTouchDistance = (touches: React.TouchList) => {
    const touch1 = touches[0];
    const touch2 = touches[1];
    const dx = touch1.clientX - touch2.clientX;
    const dy = touch1.clientY - touch2.clientY;
    return Math.sqrt(dx * dx + dy * dy);
  };

  const handleTouchStart = useCallback(
    (e: React.TouchEvent) => {
      if (!enablePinch || e.touches.length !== 2) return;
      lastTouchDistance.current = getTouchDistance(e.touches);
    },
    [enablePinch]
  );

  const handleTouchMove = useCallback(
    (e: React.TouchEvent) => {
      if (!enablePinch || e.touches.length !== 2 || lastTouchDistance.current === null) return;

      e.preventDefault();
      const currentDistance = getTouchDistance(e.touches);
      const delta = (currentDistance - lastTouchDistance.current) * 0.01;
      lastTouchDistance.current = currentDistance;
      handleZoomChange(zoom + delta);
    },
    [enablePinch, zoom, handleZoomChange]
  );

  const handleTouchEnd = useCallback(() => {
    lastTouchDistance.current = null;
  }, []);

  // Mouse drag to pan
  const handleMouseDown = useCallback(
    (e: React.MouseEvent) => {
      if (zoom <= 1) return;
      setIsDragging(true);
      setDragStart({ x: e.clientX - position.x, y: e.clientY - position.y });
    },
    [zoom, position]
  );

  const handleMouseMove = useCallback(
    (e: React.MouseEvent) => {
      if (!isDragging) return;
      setPosition({
        x: e.clientX - dragStart.x,
        y: e.clientY - dragStart.y,
      });
    },
    [isDragging, dragStart]
  );

  const handleMouseUp = useCallback(() => {
    setIsDragging(false);
  }, []);

  // Reset position when zoom is 1
  useEffect(() => {
    if (zoom === 1) {
      setPosition({ x: 0, y: 0 });
    }
  }, [zoom]);

  const containerClasses = [
    'zoom',
    isDragging && 'zoom--dragging',
    className,
  ]
    .filter(Boolean)
    .join(' ');

  const baseStyle: React.CSSProperties = {
    position: 'relative',
    overflow: 'hidden',
    width: '100%',
    height: '100%',
    cursor: zoom > 1 ? (isDragging ? 'grabbing' : 'grab') : 'default',
    ...style,
  };

  const contentStyle: React.CSSProperties = {
    transform: `translate(${position.x}px, ${position.y}px) scale(${zoom})`,
    transformOrigin: 'center center',
    transition: isDragging ? 'none' : 'transform 0.2s ease',
    userSelect: 'none',
  };

  return (
    <div
      ref={containerRef}
      className={containerClasses}
      style={baseStyle}
      onWheel={handleWheel}
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
      onMouseDown={handleMouseDown}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      onMouseLeave={handleMouseUp}
      data-testid={testId}
    >
      <div className="zoom__content" style={contentStyle}>
        {children}
      </div>

      {/* Zoom controls */}
      <div
        className="zoom__controls"
        style={{
          position: 'absolute',
          bottom: '1rem',
          right: '1rem',
          display: 'flex',
          flexDirection: 'column',
          gap: '0.5rem',
          backgroundColor: '#ffffff',
          borderRadius: '0.5rem',
          padding: '0.5rem',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.15)',
        }}
      >
        <button
          onClick={() => handleZoomChange(zoom + step)}
          disabled={zoom >= maxZoom}
          style={{
            width: '2rem',
            height: '2rem',
            border: 'none',
            borderRadius: '0.25rem',
            backgroundColor: zoom >= maxZoom ? '#e5e7eb' : '#3b82f6',
            color: zoom >= maxZoom ? '#9ca3af' : '#ffffff',
            fontSize: '1.25rem',
            fontWeight: 700,
            cursor: zoom >= maxZoom ? 'not-allowed' : 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          +
        </button>
        <div
          style={{
            textAlign: 'center',
            fontSize: '0.875rem',
            fontWeight: 600,
            color: '#6b7280',
          }}
        >
          {Math.round(zoom * 100)}%
        </div>
        <button
          onClick={() => handleZoomChange(zoom - step)}
          disabled={zoom <= minZoom}
          style={{
            width: '2rem',
            height: '2rem',
            border: 'none',
            borderRadius: '0.25rem',
            backgroundColor: zoom <= minZoom ? '#e5e7eb' : '#3b82f6',
            color: zoom <= minZoom ? '#9ca3af' : '#ffffff',
            fontSize: '1.25rem',
            fontWeight: 700,
            cursor: zoom <= minZoom ? 'not-allowed' : 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          −
        </button>
        <button
          onClick={() => handleZoomChange(1)}
          disabled={zoom === 1}
          style={{
            width: '2rem',
            height: '2rem',
            border: 'none',
            borderRadius: '0.25rem',
            backgroundColor: zoom === 1 ? '#e5e7eb' : '#3b82f6',
            color: zoom === 1 ? '#9ca3af' : '#ffffff',
            fontSize: '0.75rem',
            fontWeight: 700,
            cursor: zoom === 1 ? 'not-allowed' : 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          1:1
        </button>
      </div>
    </div>
  );
};

Zoom.displayName = 'Zoom';
