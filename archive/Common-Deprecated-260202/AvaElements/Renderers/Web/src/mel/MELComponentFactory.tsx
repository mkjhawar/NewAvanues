/**
 * MEL Component Factory
 * Creates React components from UINode definitions
 * Maps MEL types to existing Web components
 *
 * @module AvaElements/Web/MEL
 * @since 3.3.0
 */

import React, { ComponentType } from 'react';
import { UINode, PluginRuntime, MELRuntimeError } from './types';

// Import existing components
import { Button } from '@mui/material';
import { Box, Stack, Card as MuiCard } from '@mui/material';
import Typography from '@mui/material/Typography';
import TextField from '@mui/material/TextField';
import Checkbox from '@mui/material/Checkbox';
import Switch from '@mui/material/Switch';
import Divider from '@mui/material/Divider';

// Type for component renderer function
type ComponentRenderer = (
  node: UINode,
  runtime: PluginRuntime,
  children?: React.ReactNode
) => React.ReactElement;

// ============================================================================
// COMPONENT RENDERERS
// ============================================================================

const renderText: ComponentRenderer = (node, runtime) => {
  const { props, bindings } = node;

  // Resolve bindings
  const content = bindings?.value
    ? runtime.evaluate(bindings.value)
    : props?.content || props?.value || '';

  const style = bindings?.style
    ? runtime.evaluate(bindings.style)
    : props?.style || {};

  return (
    <Typography
      variant={props?.variant || 'body1'}
      sx={{
        fontSize: style.fontSize,
        fontWeight: style.fontWeight,
        fontFamily: style.fontFamily,
        textAlign: style.textAlign,
        color: style.color,
        ...style,
      }}
    >
      {content}
    </Typography>
  );
};

const renderButton: ComponentRenderer = (node, runtime) => {
  const { props, bindings, events } = node;

  // Resolve bindings
  const label = bindings?.label
    ? runtime.evaluate(bindings.label)
    : props?.label || props?.text || '';

  const enabled = bindings?.enabled
    ? runtime.evaluate(bindings.enabled)
    : props?.enabled !== false;

  const variant = props?.variant || 'contained';
  const style = bindings?.style
    ? runtime.evaluate(bindings.style)
    : props?.style || {};

  // Handle events
  const handleClick = () => {
    if (events?.onTap) {
      // Parse reducer call with params (e.g., "increment(5)")
      const match = events.onTap.match(/^(\w+)(?:\((.*)\))?$/);
      if (match) {
        const [, action, paramsStr] = match;
        let params: Record<string, any> = {};

        if (paramsStr) {
          // Parse simple parameters (numbers, strings, booleans)
          const paramList = paramsStr.split(',').map(p => p.trim());
          const positionalArgs: any[] = [];

          paramList.forEach((param) => {
            // Try to parse as number
            if (/^-?\d+(\.\d+)?$/.test(param)) {
              positionalArgs.push(parseFloat(param));
            }
            // Try to parse as string
            else if (param.startsWith('"') || param.startsWith("'")) {
              positionalArgs.push(param.slice(1, -1));
            }
            // Try to parse as boolean
            else if (param === 'true' || param === 'false') {
              positionalArgs.push(param === 'true');
            }
            // Otherwise treat as identifier
            else {
              positionalArgs.push(param);
            }
          });

          // Map positional args to named params based on reducer definition
          params = mapToNamedParams(runtime, action, positionalArgs);
        }

        runtime.dispatch(action, params);
      }
    } else if (events?.onClick) {
      runtime.dispatch(events.onClick);
    }
  };

  return (
    <Button
      variant={variant as any}
      onClick={handleClick}
      disabled={!enabled}
      sx={{
        flex: style.flex,
        minWidth: style.minWidth,
        ...style,
      }}
    >
      {label}
    </Button>
  );
};

const renderTextField: ComponentRenderer = (node, runtime) => {
  const { props, bindings, events } = node;

  const value = bindings?.value
    ? runtime.evaluate(bindings.value)
    : props?.value || '';

  const label = props?.label || '';
  const placeholder = props?.placeholder || '';

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (events?.onChange) {
      runtime.dispatch(events.onChange, { value: e.target.value });
    }
  };

  return (
    <TextField
      value={value}
      label={label}
      placeholder={placeholder}
      onChange={handleChange}
      fullWidth
    />
  );
};

const renderCheckbox: ComponentRenderer = (node, runtime) => {
  const { props, bindings, events } = node;

  const checked = bindings?.checked
    ? runtime.evaluate(bindings.checked)
    : props?.checked || false;

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (events?.onChange) {
      runtime.dispatch(events.onChange, { checked: e.target.checked });
    }
  };

  return <Checkbox checked={checked} onChange={handleChange} />;
};

const renderSwitch: ComponentRenderer = (node, runtime) => {
  const { props, bindings, events } = node;

  const checked = bindings?.checked
    ? runtime.evaluate(bindings.checked)
    : props?.checked || false;

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (events?.onChange) {
      runtime.dispatch(events.onChange, { checked: e.target.checked });
    }
  };

  return <Switch checked={checked} onChange={handleChange} />;
};

const renderColumn: ComponentRenderer = (node, runtime, children) => {
  const { props, bindings } = node;

  const style = bindings?.style
    ? runtime.evaluate(bindings.style)
    : props?.style || {};

  const spacing = props?.spacing || props?.gap || 2;

  return (
    <Stack
      direction="column"
      spacing={spacing}
      sx={{
        padding: style.padding,
        gap: style.gap,
        ...style,
      }}
    >
      {children}
    </Stack>
  );
};

const renderRow: ComponentRenderer = (node, runtime, children) => {
  const { props, bindings } = node;

  const style = bindings?.style
    ? runtime.evaluate(bindings.style)
    : props?.style || {};

  const spacing = props?.spacing || props?.gap || 2;

  return (
    <Stack
      direction="row"
      spacing={spacing}
      sx={{
        gap: style.gap,
        ...style,
      }}
    >
      {children}
    </Stack>
  );
};

const renderContainer: ComponentRenderer = (node, runtime, children) => {
  const { props, bindings } = node;

  const style = bindings?.style
    ? runtime.evaluate(bindings.style)
    : props?.style || {};

  return (
    <Box
      sx={{
        padding: style.padding || props?.padding,
        margin: style.margin,
        backgroundColor: style.backgroundColor,
        borderRadius: style.borderRadius,
        ...style,
      }}
    >
      {children}
    </Box>
  );
};

const renderCard: ComponentRenderer = (node, runtime, children) => {
  const { props, bindings } = node;

  const style = bindings?.style
    ? runtime.evaluate(bindings.style)
    : props?.style || {};

  const elevation = props?.elevation || 2;

  return (
    <MuiCard
      elevation={elevation}
      sx={{
        padding: 2,
        ...style,
      }}
    >
      {children}
    </MuiCard>
  );
};

const renderDivider: ComponentRenderer = (node, runtime) => {
  const { props } = node;

  return <Divider sx={{ my: props?.margin || 1 }} />;
};

// ============================================================================
// COMPONENT REGISTRY
// ============================================================================

const COMPONENT_REGISTRY: Record<string, ComponentRenderer> = {
  // Display
  Text: renderText,

  // Buttons
  Button: renderButton,

  // Inputs
  TextField: renderTextField,
  Checkbox: renderCheckbox,
  Switch: renderSwitch,

  // Layout
  Column: renderColumn,
  Row: renderRow,
  Container: renderContainer,

  // Cards
  Card: renderCard,

  // Display
  Divider: renderDivider,
};

// ============================================================================
// COMPONENT FACTORY
// ============================================================================

export interface MELComponentFactoryProps {
  node: UINode;
  runtime: PluginRuntime;
}

/**
 * Recursively render a UINode tree
 */
export function renderUINode(
  node: UINode,
  runtime: PluginRuntime,
  key?: string | number
): React.ReactElement {
  const renderer = COMPONENT_REGISTRY[node.type];

  if (!renderer) {
    console.error(`Unknown component type: ${node.type}`);
    return <div key={key}>Unknown component: {node.type}</div>;
  }

  // Render children recursively
  const children = node.children?.map((child, index) =>
    renderUINode(child, runtime, index)
  );

  // Render component
  const element = renderer(node, runtime, children);

  // Add key if provided
  return key !== undefined ? React.cloneElement(element, { key }) : element;
}

/**
 * MEL Component Factory Component
 * Renders a UINode with runtime context
 */
export const MELComponentFactory: React.FC<MELComponentFactoryProps> = ({
  node,
  runtime,
}) => {
  return renderUINode(node, runtime);
};

/**
 * Register a custom component renderer
 */
export function registerComponent(
  type: string,
  renderer: ComponentRenderer
): void {
  COMPONENT_REGISTRY[type] = renderer;
}

/**
 * Get registered component types
 */
export function getRegisteredComponents(): string[] {
  return Object.keys(COMPONENT_REGISTRY);
}

/**
 * Map positional arguments to named parameters based on reducer definition
 *
 * @param runtime Plugin runtime with reducer definitions
 * @param reducerName Name of the reducer
 * @param positionalArgs Positional argument values
 * @returns Object with named parameters
 */
function mapToNamedParams(
  runtime: PluginRuntime,
  reducerName: string,
  positionalArgs: any[]
): Record<string, any> {
  // Get the reducer definition from runtime
  const reducer = runtime.getReducer?.(reducerName);

  if (!reducer || !reducer.params) {
    // Reducer not found or no params - use positional params as fallback
    const params: Record<string, any> = {};
    positionalArgs.forEach((value, index) => {
      params[`arg${index}`] = value;
    });
    return params;
  }

  // Map positional args to named params based on reducer.params
  const namedParams: Record<string, any> = {};
  reducer.params.forEach((paramName: string, index: number) => {
    if (index < positionalArgs.length) {
      namedParams[paramName] = positionalArgs[index];
    }
  });

  return namedParams;
}
