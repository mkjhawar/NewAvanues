/**
 * AVAMagic Flutter Parity React Renderer
 *
 * Core renderer component that converts AVAMagic component configurations
 * into React elements. Supports all 58 Flutter Parity components.
 *
 * Features:
 * - Type-safe component rendering with TypeScript
 * - Error boundaries for graceful degradation
 * - Component factory pattern for extensibility
 * - Theme integration support
 * - Children rendering with recursive support
 * - SSR compatible (no window/document in module scope)
 *
 * Platform: Web (React 18+)
 * Target: ES2020
 *
 * @module ReactRenderer
 * @since 2.1.0
 */

import React, { ErrorInfo, ReactElement, ReactNode } from 'react';
import { getComponentRegistry, ComponentType, BaseComponent } from './ComponentRegistry';
import type { FlutterParityComponent, Theme } from '../types';

/**
 * Renderer configuration
 */
export interface RendererConfig {
  /** Theme configuration */
  theme?: Theme;
  /** Error handler callback */
  onError?: (error: Error, componentType: string) => void;
  /** Enable strict mode validation */
  strict?: boolean;
  /** Enable performance monitoring */
  enablePerformanceMonitoring?: boolean;
  /** Custom error fallback component */
  ErrorFallback?: React.ComponentType<ErrorFallbackProps>;
}

/**
 * Renderer props
 */
export interface ReactRendererProps {
  /** Component configuration to render */
  component: FlutterParityComponent;
  /** Renderer configuration */
  config?: RendererConfig;
}

/**
 * Error fallback props
 */
export interface ErrorFallbackProps {
  error: Error;
  componentType: string;
  reset: () => void;
}

/**
 * Default error fallback component
 */
const DefaultErrorFallback: React.FC<ErrorFallbackProps> = ({
  error,
  componentType,
  reset,
}) => {
  return (
    <div
      style={{
        padding: '16px',
        margin: '8px',
        border: '2px solid #ff5252',
        borderRadius: '4px',
        backgroundColor: '#ffebee',
        color: '#c62828',
      }}
    >
      <h3 style={{ margin: '0 0 8px 0', fontSize: '16px', fontWeight: 600 }}>
        Rendering Error: {componentType}
      </h3>
      <p style={{ margin: '0 0 8px 0', fontSize: '14px' }}>{error.message}</p>
      <button
        onClick={reset}
        style={{
          padding: '6px 12px',
          backgroundColor: '#c62828',
          color: 'white',
          border: 'none',
          borderRadius: '4px',
          cursor: 'pointer',
          fontSize: '14px',
        }}
      >
        Retry
      </button>
    </div>
  );
};

/**
 * Error boundary state
 */
interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
  componentType: string;
}

/**
 * Error boundary for component rendering
 */
class ComponentErrorBoundary extends React.Component<
  {
    componentType: string;
    onError?: (error: Error, componentType: string) => void;
    ErrorFallback: React.ComponentType<ErrorFallbackProps>;
    children: ReactNode;
  },
  ErrorBoundaryState
> {
  constructor(props: {
    componentType: string;
    onError?: (error: Error, componentType: string) => void;
    ErrorFallback: React.ComponentType<ErrorFallbackProps>;
    children: ReactNode;
  }) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      componentType: props.componentType,
    };
  }

  static getDerivedStateFromError(error: Error): Partial<ErrorBoundaryState> {
    return {
      hasError: true,
      error,
    };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    const { onError, componentType } = this.props;
    console.error(`Error rendering component ${componentType}:`, error, errorInfo);
    onError?.(error, componentType);
  }

  reset = (): void => {
    this.setState({
      hasError: false,
      error: null,
    });
  };

  render(): ReactNode {
    const { hasError, error, componentType } = this.state;
    const { children, ErrorFallback } = this.props;

    if (hasError && error) {
      return <ErrorFallback error={error} componentType={componentType} reset={this.reset} />;
    }

    return children;
  }
}

/**
 * Main React Renderer Component
 *
 * Renders AVAMagic Flutter Parity components using the component registry.
 * Automatically handles error boundaries and theme propagation.
 *
 * @example
 * ```tsx
 * import { ReactRenderer } from '@avaelements/web-renderer';
 *
 * function App() {
 *   const component = {
 *     type: 'Center',
 *     child: {
 *       type: 'Padding',
 *       padding: { all: 16 },
 *       child: {
 *         type: 'Text',
 *         content: 'Hello, World!'
 *       }
 *     }
 *   };
 *
 *   return <ReactRenderer component={component} />;
 * }
 * ```
 */
export const ReactRenderer: React.FC<ReactRendererProps> = ({ component, config = {} }) => {
  const {
    theme,
    onError,
    strict = false,
    enablePerformanceMonitoring = false,
    ErrorFallback = DefaultErrorFallback,
  } = config;

  const registry = getComponentRegistry();

  // Performance monitoring
  const startTime = enablePerformanceMonitoring ? performance.now() : 0;

  // Validate component structure
  if (!component || typeof component !== 'object') {
    const error = new Error('Invalid component: component must be an object');
    console.error(error);
    return <ErrorFallback error={error} componentType="Unknown" reset={() => {}} />;
  }

  if (!component.type || typeof component.type !== 'string') {
    const error = new Error('Invalid component: type field is required and must be a string');
    console.error(error);
    return <ErrorFallback error={error} componentType="Unknown" reset={() => {}} />;
  }

  const componentType = component.type;

  // Get renderer from registry
  const renderer = registry.getRenderer(componentType);

  if (!renderer) {
    const error = new Error(`Unsupported component type: ${componentType}`);

    if (strict) {
      console.error(error);
      return <ErrorFallback error={error} componentType={componentType} reset={() => {}} />;
    } else {
      console.warn(error.message);
      return null;
    }
  }

  // Performance logging
  if (enablePerformanceMonitoring) {
    const endTime = performance.now();
    console.debug(`[ReactRenderer] ${componentType} rendered in ${(endTime - startTime).toFixed(2)}ms`);
  }

  // Render with error boundary
  return (
    <ComponentErrorBoundary
      componentType={componentType}
      onError={onError}
      ErrorFallback={ErrorFallback}
    >
      {renderer(component as BaseComponent)}
    </ComponentErrorBoundary>
  );
};

/**
 * Render child components recursively
 *
 * @param child - Child component or array of children
 * @param config - Renderer configuration
 * @returns Rendered React elements
 */
export function renderChildren(
  child: FlutterParityComponent | FlutterParityComponent[] | undefined,
  config?: RendererConfig
): ReactNode {
  if (!child) {
    return null;
  }

  if (Array.isArray(child)) {
    return child.map((c, index) => (
      <ReactRenderer key={c.key || index} component={c} config={config} />
    ));
  }

  return <ReactRenderer component={child} config={config} />;
}

/**
 * HOC to wrap component renderers with common functionality
 *
 * @param ComponentRenderer - Component renderer function
 * @returns Wrapped component with error handling
 */
export function withRendererSupport<P extends { component: BaseComponent }>(
  ComponentRenderer: React.ComponentType<P>
): React.FC<P> {
  const WrappedComponent: React.FC<P> = (props) => {
    return <ComponentRenderer {...props} />;
  };

  WrappedComponent.displayName = `withRendererSupport(${
    ComponentRenderer.displayName || ComponentRenderer.name || 'Component'
  })`;

  return WrappedComponent;
}

/**
 * Hook to access renderer configuration
 */
export function useRendererConfig(): RendererConfig {
  // This would typically use React Context for theme/config propagation
  // For now, return default config
  return {
    strict: false,
    enablePerformanceMonitoring: false,
  };
}

export default ReactRenderer;
