/**
 * MEL (MagicUI Expression Language) Type Definitions for Web
 * TypeScript interfaces for the Dual-Tier Plugin System
 *
 * @module AvaElements/Web/MEL
 * @since 3.3.0
 */

// ============================================================================
// PLUGIN DEFINITION
// ============================================================================

export interface PluginDefinition {
  metadata: PluginMetadata;
  tier: PluginTier;
  state: Record<string, StateVariable>;
  reducers: Record<string, Reducer>;
  scripts?: Record<string, Script>; // Tier 2 only
  ui: UINode;
}

export interface PluginMetadata {
  id: string;
  name: string;
  version: string;
  author?: string;
  description?: string;
  icon?: string;
}

export enum PluginTier {
  DATA = 'data',   // Tier 1: Apple-safe
  LOGIC = 'logic', // Tier 2: Full expressions
}

// ============================================================================
// STATE MANAGEMENT
// ============================================================================

export interface StateVariable {
  type: StateType;
  default: any;
  persist?: boolean;
}

export enum StateType {
  NUMBER = 'number',
  STRING = 'string',
  BOOLEAN = 'boolean',
  ARRAY = 'array',
  OBJECT = 'object',
  NULL = 'null',
}

export type PluginState = Record<string, any>;

// ============================================================================
// REDUCERS
// ============================================================================

export interface Reducer {
  params?: string[];
  next_state: Record<string, Expression>;
  effects?: Effect[]; // Tier 2 only
}

export interface Effect {
  type: 'http' | 'storage' | 'navigation' | 'clipboard' | 'haptics';
  action: string;
  params?: Record<string, any>;
}

// ============================================================================
// EXPRESSIONS
// ============================================================================

export interface Expression {
  raw: string;
  parsed?: ExpressionNode;
}

export type ExpressionNode =
  | LiteralNode
  | StateRefNode
  | FunctionCallNode
  | BinaryOpNode
  | UnaryOpNode
  | ConditionalNode
  | ArrayNode
  | ObjectNode;

export interface LiteralNode {
  type: 'literal';
  value: any;
}

export interface StateRefNode {
  type: 'state_ref';
  path: string[];
}

export interface FunctionCallNode {
  type: 'function_call';
  category: string; // math, string, array, object, date, logic, http, storage, etc.
  name: string;
  args: ExpressionNode[];
}

export interface BinaryOpNode {
  type: 'binary_op';
  operator: '+' | '-' | '*' | '/' | '%' | '==' | '!=' | '>' | '<' | '>=' | '<=' | '&&' | '||';
  left: ExpressionNode;
  right: ExpressionNode;
}

export interface UnaryOpNode {
  type: 'unary_op';
  operator: '!' | '-';
  operand: ExpressionNode;
}

export interface ConditionalNode {
  type: 'conditional';
  condition: ExpressionNode;
  thenBranch: ExpressionNode;
  elseBranch?: ExpressionNode;
}

export interface ArrayNode {
  type: 'array';
  elements: ExpressionNode[];
}

export interface ObjectNode {
  type: 'object';
  properties: Record<string, ExpressionNode>;
}

// ============================================================================
// UI NODES
// ============================================================================

export interface UINode {
  type: string; // Component type: Button, Text, Column, etc.
  props?: Record<string, any>;
  bindings?: Record<string, Expression>;
  events?: Record<string, EventHandler>;
  children?: UINode[];
}

export interface EventHandler {
  reducer: string;
  params?: Record<string, any>;
}

// ============================================================================
// SCRIPTS (Tier 2 Only)
// ============================================================================

export interface Script {
  params?: string[];
  body: string;
}

// ============================================================================
// RUNTIME
// ============================================================================

export interface PluginRuntime {
  definition: PluginDefinition;
  state: PluginState;
  tier: PluginTier;
  dispatch: (action: string, params?: Record<string, any>) => void;
  evaluate: (expr: Expression) => any;
  render: () => UINode;
}

// ============================================================================
// BUILT-IN FUNCTIONS
// ============================================================================

export type BuiltInFunction = (...args: any[]) => any;

export interface FunctionRegistry {
  [category: string]: {
    [name: string]: {
      fn: BuiltInFunction;
      tier: PluginTier;
    };
  };
}

// ============================================================================
// EVALUATION CONTEXT
// ============================================================================

export interface EvaluationContext {
  state: PluginState;
  params?: Record<string, any>;
  tier: PluginTier;
}

// ============================================================================
// ERRORS
// ============================================================================

export class MELError extends Error {
  constructor(
    message: string,
    public code: string,
    public context?: any
  ) {
    super(message);
    this.name = 'MELError';
  }
}

export class MELSyntaxError extends MELError {
  constructor(message: string, context?: any) {
    super(message, 'SYNTAX_ERROR', context);
    this.name = 'MELSyntaxError';
  }
}

export class MELSecurityError extends MELError {
  constructor(message: string, context?: any) {
    super(message, 'SECURITY_ERROR', context);
    this.name = 'MELSecurityError';
  }
}

export class MELRuntimeError extends MELError {
  constructor(message: string, context?: any) {
    super(message, 'RUNTIME_ERROR', context);
    this.name = 'MELRuntimeError';
  }
}
