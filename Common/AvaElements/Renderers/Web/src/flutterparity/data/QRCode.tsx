/**
 * QRCode Component
 *
 * QR code generator using qrcode.react library.
 *
 * @since 3.0.0-flutter-parity-web
 * @module flutterparity/data
 */

import React from 'react';
import type { QRCodeProps } from './types';

// Note: This component assumes qrcode.react is installed
// Install with: npm install qrcode.react
// For TypeScript: npm install --save-dev @types/qrcode.react

/**
 * QRCode - QR code generator component
 *
 * @example
 * ```tsx
 * <QRCode
 *   value="https://example.com"
 *   size={256}
 *   level="H"
 *   bgColor="#ffffff"
 *   fgColor="#000000"
 *   includeMargin
 * />
 * ```
 */
export const QRCode: React.FC<QRCodeProps> = ({
  value,
  size = 128,
  level = 'M',
  bgColor = '#ffffff',
  fgColor = '#000000',
  includeMargin = false,
  imageSettings,
  className = '',
  style,
  testId,
}) => {
  const containerClasses = ['qrcode', className].filter(Boolean).join(' ');

  const baseStyle: React.CSSProperties = {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: includeMargin ? '1rem' : '0',
    backgroundColor: bgColor,
    borderRadius: '0.5rem',
    ...style,
  };

  // Fallback implementation using a simple canvas-based QR generator
  // In production, replace with actual qrcode.react import
  const FallbackQRCode = () => {
    const canvasRef = React.useRef<HTMLCanvasElement>(null);

    React.useEffect(() => {
      const canvas = canvasRef.current;
      if (!canvas) return;

      const ctx = canvas.getContext('2d');
      if (!ctx) return;

      // Simple placeholder rendering
      // In production, this would use a proper QR code library
      ctx.fillStyle = bgColor;
      ctx.fillRect(0, 0, size, size);

      ctx.fillStyle = fgColor;
      ctx.font = `${size / 10}px monospace`;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText('QR CODE', size / 2, size / 2 - size / 8);
      ctx.font = `${size / 15}px monospace`;
      ctx.fillText(value.substring(0, 20), size / 2, size / 2 + size / 8);
    }, []);

    return (
      <canvas
        ref={canvasRef}
        width={size}
        height={size}
        style={{ display: 'block' }}
      />
    );
  };

  // Attempt to import QRCodeSVG from qrcode.react
  let QRCodeSVG: any;
  try {
    // Dynamic import for optional dependency
    QRCodeSVG = require('qrcode.react').QRCodeSVG;
  } catch (e) {
    // qrcode.react not installed, use fallback
    console.warn(
      'qrcode.react not installed. Using fallback QR code renderer. Install with: npm install qrcode.react'
    );
  }

  return (
    <div className={containerClasses} style={baseStyle} data-testid={testId}>
      {QRCodeSVG ? (
        <QRCodeSVG
          value={value}
          size={size}
          level={level}
          bgColor={bgColor}
          fgColor={fgColor}
          includeMargin={false}
          imageSettings={imageSettings}
        />
      ) : (
        <FallbackQRCode />
      )}
    </div>
  );
};

QRCode.displayName = 'QRCode';

/**
 * Installation Instructions:
 *
 * To use the full QRCode functionality, install the qrcode.react library:
 *
 * ```bash
 * npm install qrcode.react
 * npm install --save-dev @types/qrcode.react
 * ```
 *
 * The component will automatically detect and use the library if available,
 * otherwise it will fall back to a simple placeholder renderer.
 */
