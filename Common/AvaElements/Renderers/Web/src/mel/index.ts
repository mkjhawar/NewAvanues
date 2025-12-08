/**
 * MEL (MagicUI Expression Language) Plugin System - Web Renderer
 * Main export file for MEL plugin runtime
 *
 * @module AvaElements/Web/MEL
 * @since 3.3.0
 */

// ============================================================================
// TYPES
// ============================================================================

export type {
  PluginDefinition,
  PluginMetadata,
  PluginTier,
  StateVariable,
  StateType,
  PluginState,
  Reducer,
  Effect,
  Expression,
  ExpressionNode,
  LiteralNode,
  StateRefNode,
  FunctionCallNode,
  BinaryOpNode,
  UnaryOpNode,
  ConditionalNode,
  ArrayNode,
  ObjectNode,
  UINode,
  EventHandler,
  Script,
  PluginRuntime,
  BuiltInFunction,
  FunctionRegistry,
  EvaluationContext,
} from './types';

export {
  MELError,
  MELSyntaxError,
  MELSecurityError,
  MELRuntimeError,
} from './types';

// ============================================================================
// EXPRESSION EVALUATOR
// ============================================================================

export {
  MELExpressionParser,
  MELExpressionEvaluator,
} from './MELExpressionEvaluator';

// ============================================================================
// HOOKS
// ============================================================================

export { useMELPlugin } from './useMELPlugin';
export type { UseMELPluginOptions, UseMELPluginResult } from './useMELPlugin';

// ============================================================================
// COMPONENTS
// ============================================================================

export {
  MELComponentFactory,
  renderUINode,
  registerComponent,
  getRegisteredComponents,
} from './MELComponentFactory';
export type { MELComponentFactoryProps } from './MELComponentFactory';

export {
  MELPlugin,
  StatefulMELPlugin,
  MELPluginPreview,
} from './MELPluginRenderer';
export type {
  MELPluginProps,
  StatefulMELPluginProps,
  MELPluginPreviewProps,
} from './MELPluginRenderer';

// ============================================================================
// VERSION
// ============================================================================

export const MEL_VERSION = '1.0.0';
export const MEL_SUPPORTED_TIERS = ['data', 'logic'];
