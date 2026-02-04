/**
 * MagicElements Web Renderer
 *
 * Renders MagicElements components using React and Material-UI.
 * Optimized for web browsers with responsive design patterns.
 *
 * Platform: Web (All modern browsers)
 * Framework: React 18 + Material-UI 5
 * Target: ES2020
 */

import React from 'react';
import type { AnyComponent, Theme } from './types/components';
import {
  RenderButton,
  RenderTextField,
  RenderCheckbox,
  RenderSwitch,
  RenderText,
  RenderImage,
  RenderIcon,
  RenderContainer,
  RenderRow,
  RenderColumn,
  RenderCard,
  RenderScrollView,
  RenderList,
} from './components/Phase1Components';

export interface RendererProps {
  component: AnyComponent;
  theme?: Theme;
}

/**
 * MagicElements Web Renderer Component
 *
 * Main renderer that dispatches components to their React implementations
 */
export const MagicElementsRenderer: React.FC<RendererProps> = ({ component, theme }) => {
  switch (component.type) {
    // Form Components
    case 'Button':
      return <RenderButton component={component} theme={theme} />;
    case 'TextField':
      return <RenderTextField component={component} theme={theme} />;
    case 'Checkbox':
      return <RenderCheckbox component={component} theme={theme} />;
    case 'Switch':
      return <RenderSwitch component={component} theme={theme} />;

    // Display Components
    case 'Text':
      return <RenderText component={component} theme={theme} />;
    case 'Image':
      return <RenderImage component={component} theme={theme} />;
    case 'Icon':
      return <RenderIcon component={component} theme={theme} />;

    // Layout Components
    case 'Container':
      return <RenderContainer component={component} theme={theme} />;
    case 'Row':
      return <RenderRow component={component} theme={theme} />;
    case 'Column':
      return <RenderColumn component={component} theme={theme} />;
    case 'Card':
      return <RenderCard component={component} theme={theme} />;

    // Navigation & Data Components
    case 'ScrollView':
      return <RenderScrollView component={component} theme={theme} />;
    case 'List':
      return <RenderList component={component} theme={theme} />;

    default:
      console.warn(`Unknown component type: ${component.type}`);
      return null;
  }
};

export default MagicElementsRenderer;
