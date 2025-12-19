/**
 * MEL Plugin Renderer
 * React component that renders MEL plugins
 *
 * @module AvaElements/Web/MEL
 * @since 3.3.0
 */

import React, { useEffect, useState } from 'react';
import { Box, Alert, CircularProgress } from '@mui/material';
import { useMELPlugin, UseMELPluginOptions } from './useMELPlugin';
import { MELComponentFactory } from './MELComponentFactory';
import { PluginDefinition, MELError } from './types';

// ============================================================================
// COMPONENT PROPS
// ============================================================================

export interface MELPluginProps {
  /** Plugin definition (YAML string, JSON string, or object) */
  plugin: string | PluginDefinition;

  /** Plugin options */
  options?: UseMELPluginOptions;

  /** Custom error handler */
  onError?: (error: MELError) => void;

  /** Custom loading component */
  loadingComponent?: React.ReactNode;

  /** Custom error component */
  errorComponent?: (error: MELError) => React.ReactNode;

  /** Wrapper styles */
  sx?: any;

  /** Additional className */
  className?: string;
}

// ============================================================================
// DEFAULT ERROR COMPONENT
// ============================================================================

const DefaultErrorComponent: React.FC<{ error: MELError }> = ({ error }) => (
  <Alert severity="error" sx={{ my: 2 }}>
    <strong>{error.name}:</strong> {error.message}
    {error.context && (
      <pre style={{ fontSize: '0.8em', marginTop: 8 }}>
        {JSON.stringify(error.context, null, 2)}
      </pre>
    )}
  </Alert>
);

// ============================================================================
// DEFAULT LOADING COMPONENT
// ============================================================================

const DefaultLoadingComponent: React.FC = () => (
  <Box
    sx={{
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      padding: 4,
    }}
  >
    <CircularProgress />
  </Box>
);

// ============================================================================
// MEL PLUGIN RENDERER COMPONENT
// ============================================================================

/**
 * MELPlugin Component
 * Renders a MEL plugin with state management
 *
 * @example
 * ```tsx
 * import { MELPlugin } from '@augmentalis/avaelements-web';
 *
 * function Calculator() {
 *   return <MELPlugin plugin={calculatorYaml} />;
 * }
 * ```
 */
export const MELPlugin: React.FC<MELPluginProps> = ({
  plugin,
  options = {},
  onError,
  loadingComponent,
  errorComponent,
  sx,
  className,
}) => {
  // Merge error handlers
  const mergedOptions: UseMELPluginOptions = {
    ...options,
    onError: (error) => {
      options.onError?.(error);
      onError?.(error);
    },
  };

  // Use MEL plugin hook
  const { runtime, loading, error } = useMELPlugin(plugin, mergedOptions);

  // Show loading state
  if (loading) {
    return <>{loadingComponent || <DefaultLoadingComponent />}</>;
  }

  // Show error state
  if (error) {
    return (
      <>
        {errorComponent ? (
          errorComponent(error)
        ) : (
          <DefaultErrorComponent error={error} />
        )}
      </>
    );
  }

  // Render plugin UI
  return (
    <Box sx={sx} className={className}>
      <MELComponentFactory node={runtime.render()} runtime={runtime} />
    </Box>
  );
};

// ============================================================================
// STATEFUL MEL PLUGIN (with external state control)
// ============================================================================

export interface StatefulMELPluginProps extends MELPluginProps {
  /** External state (controlled mode) */
  state?: Record<string, any>;

  /** State change handler */
  onStateChange?: (state: Record<string, any>) => void;
}

/**
 * Stateful MEL Plugin Component
 * Allows external control of plugin state
 */
export const StatefulMELPlugin: React.FC<StatefulMELPluginProps> = ({
  state: externalState,
  onStateChange,
  ...props
}) => {
  const { runtime, loading, error } = useMELPlugin(props.plugin, props.options);

  // Sync external state
  useEffect(() => {
    if (externalState && JSON.stringify(runtime.state) !== JSON.stringify(externalState)) {
      // TODO: Implement state override mechanism
      console.warn('External state sync not yet implemented');
    }
  }, [externalState, runtime.state]);

  // Notify state changes
  useEffect(() => {
    onStateChange?.(runtime.state);
  }, [runtime.state, onStateChange]);

  // Show loading state
  if (loading) {
    return <>{props.loadingComponent || <DefaultLoadingComponent />}</>;
  }

  // Show error state
  if (error) {
    return (
      <>
        {props.errorComponent ? (
          props.errorComponent(error)
        ) : (
          <DefaultErrorComponent error={error} />
        )}
      </>
    );
  }

  // Render plugin UI
  return (
    <Box sx={props.sx} className={props.className}>
      <MELComponentFactory node={runtime.render()} runtime={runtime} />
    </Box>
  );
};

// ============================================================================
// PLUGIN PREVIEW (for plugin marketplace)
// ============================================================================

export interface MELPluginPreviewProps {
  /** Plugin definition */
  plugin: string | PluginDefinition;

  /** Preview height */
  height?: number | string;

  /** Show metadata header */
  showMetadata?: boolean;
}

/**
 * MEL Plugin Preview Component
 * Shows plugin with metadata (for marketplace)
 */
export const MELPluginPreview: React.FC<MELPluginPreviewProps> = ({
  plugin,
  height = 400,
  showMetadata = true,
}) => {
  const { runtime, definition, loading, error } = useMELPlugin(plugin);

  if (loading) {
    return <DefaultLoadingComponent />;
  }

  if (error) {
    return <DefaultErrorComponent error={error} />;
  }

  return (
    <Box>
      {showMetadata && (
        <Box sx={{ mb: 2, p: 2, bgcolor: 'background.paper', borderRadius: 1 }}>
          <h3>{definition.metadata.name}</h3>
          <p>{definition.metadata.description}</p>
          <small>
            v{definition.metadata.version} • {definition.metadata.author} • Tier{' '}
            {runtime.tier === 'data' ? '1' : '2'}
          </small>
        </Box>
      )}
      <Box
        sx={{
          height,
          overflow: 'auto',
          border: '1px solid',
          borderColor: 'divider',
          borderRadius: 1,
          p: 2,
        }}
      >
        <MELComponentFactory node={runtime.render()} runtime={runtime} />
      </Box>
    </Box>
  );
};

export default MELPlugin;
