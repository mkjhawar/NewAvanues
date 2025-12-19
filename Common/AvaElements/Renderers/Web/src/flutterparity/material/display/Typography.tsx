import React from 'react';

type HeadingLevel = 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
type TextAlign = 'start' | 'center' | 'end' | 'justify';

export interface HeadingTextProps {
  text: string;
  level?: HeadingLevel;
  color?: string;
  fontWeight?: string;
  textAlign?: TextAlign;
  maxLines?: number;
}

const headingSizes: Record<HeadingLevel, number> = {
  h1: 36, h2: 30, h3: 24, h4: 20, h5: 18, h6: 16
};

export const HeadingText: React.FC<HeadingTextProps> = ({
  text,
  level = 'h1',
  color,
  fontWeight = '700',
  textAlign = 'start',
  maxLines
}) => {
  const Tag = level;
  return (
    <Tag style={{
      fontSize: headingSizes[level],
      fontWeight,
      color,
      textAlign,
      margin: 0,
      ...(maxLines ? {
        overflow: 'hidden',
        display: '-webkit-box',
        WebkitLineClamp: maxLines,
        WebkitBoxOrient: 'vertical'
      } : {})
    }}>
      {text}
    </Tag>
  );
};

export interface DisplayTextProps {
  text: string;
  size?: 'small' | 'medium' | 'large' | 'xlarge';
  color?: string;
  gradient?: string[];
  textAlign?: TextAlign;
}

const displaySizes = { small: 32, medium: 48, large: 64, xlarge: 80 };

export const DisplayText: React.FC<DisplayTextProps> = ({
  text,
  size = 'medium',
  color,
  gradient,
  textAlign = 'start'
}) => {
  const gradientStyle = gradient ? {
    background: `linear-gradient(90deg, ${gradient.join(', ')})`,
    WebkitBackgroundClip: 'text',
    WebkitTextFillColor: 'transparent'
  } : {};

  return (
    <span style={{
      fontSize: displaySizes[size],
      fontWeight: 700,
      color: gradient ? undefined : color,
      textAlign,
      display: 'block',
      ...gradientStyle
    }}>
      {text}
    </span>
  );
};

export interface BodyTextProps {
  text: string;
  size?: 'small' | 'medium' | 'large';
  color?: string;
  lineHeight?: number;
}

const bodySizes = { small: 14, medium: 16, large: 18 };

export const BodyText: React.FC<BodyTextProps> = ({
  text,
  size = 'medium',
  color = '#333',
  lineHeight = 1.6
}) => (
  <p style={{ fontSize: bodySizes[size], color, lineHeight, margin: 0 }}>{text}</p>
);

export interface LabelTextProps {
  text: string;
  size?: 'small' | 'medium' | 'large';
  color?: string;
  isRequired?: boolean;
}

const labelSizes = { small: 12, medium: 14, large: 16 };

export const LabelText: React.FC<LabelTextProps> = ({
  text,
  size = 'medium',
  color = '#666',
  isRequired = false
}) => (
  <label style={{ fontSize: labelSizes[size], color, fontWeight: 500 }}>
    {text}{isRequired && <span style={{ color: '#FF3B30', marginLeft: 4 }}>*</span>}
  </label>
);

export interface CaptionTextProps {
  text: string;
  color?: string;
  icon?: string;
}

export const CaptionText: React.FC<CaptionTextProps> = ({
  text,
  color = '#999',
  icon
}) => (
  <span style={{ fontSize: 12, color, display: 'flex', alignItems: 'center', gap: 4 }}>
    {icon && <span>{icon}</span>}
    {text}
  </span>
);
