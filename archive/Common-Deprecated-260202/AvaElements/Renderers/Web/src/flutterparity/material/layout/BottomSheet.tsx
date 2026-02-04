import React, { useEffect, useRef } from 'react';

export interface BottomSheetProps {
  isOpen: boolean;
  title?: string;
  showDragHandle?: boolean;
  dismissible?: boolean;
  height?: 'auto' | 'half' | 'full' | 'fit-content';
  backgroundColor?: string;
  cornerRadius?: number;
  children?: React.ReactNode;
  onDismiss?: () => void;
}

export const BottomSheet: React.FC<BottomSheetProps> = ({
  isOpen,
  title,
  showDragHandle = true,
  dismissible = true,
  height = 'auto',
  backgroundColor = '#ffffff',
  cornerRadius = 16,
  children,
  onDismiss
}) => {
  const sheetRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => { document.body.style.overflow = ''; };
  }, [isOpen]);

  if (!isOpen) return null;

  const heightStyles: Record<string, string> = {
    auto: 'auto',
    half: '50vh',
    full: '100vh',
    'fit-content': 'fit-content'
  };

  return (
    <>
      {/* Backdrop */}
      <div
        style={{
          position: 'fixed',
          inset: 0,
          backgroundColor: 'rgba(0,0,0,0.5)',
          zIndex: 999
        }}
        onClick={dismissible ? onDismiss : undefined}
      />
      {/* Sheet */}
      <div
        ref={sheetRef}
        style={{
          position: 'fixed',
          bottom: 0,
          left: 0,
          right: 0,
          height: heightStyles[height],
          maxHeight: '90vh',
          backgroundColor,
          borderTopLeftRadius: cornerRadius,
          borderTopRightRadius: cornerRadius,
          zIndex: 1000,
          display: 'flex',
          flexDirection: 'column',
          animation: 'slideUp 0.3s ease-out'
        }}
      >
        {showDragHandle && (
          <div style={{
            width: 40,
            height: 4,
            backgroundColor: '#ccc',
            borderRadius: 2,
            margin: '12px auto'
          }} />
        )}
        {title && (
          <h3 style={{
            margin: '0 16px 16px',
            fontSize: 18,
            fontWeight: 600
          }}>{title}</h3>
        )}
        <div style={{ flex: 1, overflow: 'auto', padding: 16 }}>
          {children}
        </div>
      </div>
      <style>{`
        @keyframes slideUp {
          from { transform: translateY(100%); }
          to { transform: translateY(0); }
        }
      `}</style>
    </>
  );
};

export default BottomSheet;
