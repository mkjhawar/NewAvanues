/**
 * MEL Expression Evaluator for Web
 * Evaluates MEL expressions with Tier 1/Tier 2 support
 *
 * @module AvaElements/Web/MEL
 * @since 3.3.0
 */

import {
  Expression,
  ExpressionNode,
  PluginState,
  PluginTier,
  EvaluationContext,
  LiteralNode,
  StateRefNode,
  FunctionCallNode,
  BinaryOpNode,
  UnaryOpNode,
  ConditionalNode,
  ArrayNode,
  ObjectNode,
  MELSyntaxError,
  MELSecurityError,
  MELRuntimeError,
  FunctionRegistry,
} from './types';

// ============================================================================
// BUILT-IN FUNCTION DEFINITIONS
// ============================================================================

const TIER1_FUNCTIONS: FunctionRegistry = {
  math: {
    add: { fn: (a: number, b: number) => a + b, tier: PluginTier.DATA },
    subtract: { fn: (a: number, b: number) => a - b, tier: PluginTier.DATA },
    multiply: { fn: (a: number, b: number) => a * b, tier: PluginTier.DATA },
    divide: { fn: (a: number, b: number) => b !== 0 ? a / b : NaN, tier: PluginTier.DATA },
    mod: { fn: (a: number, b: number) => a % b, tier: PluginTier.DATA },
    abs: { fn: (a: number) => Math.abs(a), tier: PluginTier.DATA },
    round: { fn: (a: number) => Math.round(a), tier: PluginTier.DATA },
    floor: { fn: (a: number) => Math.floor(a), tier: PluginTier.DATA },
    ceil: { fn: (a: number) => Math.ceil(a), tier: PluginTier.DATA },
    min: { fn: (...args: number[]) => Math.min(...args), tier: PluginTier.DATA },
    max: { fn: (...args: number[]) => Math.max(...args), tier: PluginTier.DATA },
    pow: { fn: (a: number, b: number) => Math.pow(a, b), tier: PluginTier.DATA },
    sqrt: { fn: (a: number) => Math.sqrt(a), tier: PluginTier.DATA },
    eval: { fn: (a: number, op: string, b: number) => {
      switch (op) {
        case '+': return a + b;
        case '-': return a - b;
        case '*': return a * b;
        case '/': return b !== 0 ? a / b : NaN;
        default: throw new MELRuntimeError(`Unknown operator: ${op}`);
      }
    }, tier: PluginTier.DATA },
  },
  string: {
    concat: { fn: (...args: string[]) => args.join(''), tier: PluginTier.DATA },
    length: { fn: (str: string) => str.length, tier: PluginTier.DATA },
    substring: { fn: (str: string, start: number, end?: number) => str.substring(start, end), tier: PluginTier.DATA },
    uppercase: { fn: (str: string) => str.toUpperCase(), tier: PluginTier.DATA },
    lowercase: { fn: (str: string) => str.toLowerCase(), tier: PluginTier.DATA },
    trim: { fn: (str: string) => str.trim(), tier: PluginTier.DATA },
    replace: { fn: (str: string, search: string, replacement: string) => str.replace(search, replacement), tier: PluginTier.DATA },
    split: { fn: (str: string, separator: string) => str.split(separator), tier: PluginTier.DATA },
    join: { fn: (arr: string[], separator: string) => arr.join(separator), tier: PluginTier.DATA },
    charAt: { fn: (str: string, index: number) => str.charAt(index), tier: PluginTier.DATA },
    indexOf: { fn: (str: string, search: string) => str.indexOf(search), tier: PluginTier.DATA },
  },
  array: {
    length: { fn: (arr: any[]) => arr.length, tier: PluginTier.DATA },
    get: { fn: (arr: any[], index: number) => arr[index], tier: PluginTier.DATA },
    first: { fn: (arr: any[]) => arr[0], tier: PluginTier.DATA },
    last: { fn: (arr: any[]) => arr[arr.length - 1], tier: PluginTier.DATA },
    append: { fn: (arr: any[], item: any) => [...arr, item], tier: PluginTier.DATA },
    prepend: { fn: (arr: any[], item: any) => [item, ...arr], tier: PluginTier.DATA },
    remove: { fn: (arr: any[], index: number) => arr.filter((_, i) => i !== index), tier: PluginTier.DATA },
    filter: { fn: (arr: any[], predicate: (item: any) => boolean) => arr.filter(predicate), tier: PluginTier.LOGIC },
    map: { fn: (arr: any[], transform: (item: any) => any) => arr.map(transform), tier: PluginTier.LOGIC },
    sort: { fn: (arr: any[]) => [...arr].sort(), tier: PluginTier.DATA },
    reverse: { fn: (arr: any[]) => [...arr].reverse(), tier: PluginTier.DATA },
    slice: { fn: (arr: any[], start: number, end?: number) => arr.slice(start, end), tier: PluginTier.DATA },
  },
  object: {
    get: { fn: (obj: any, key: string) => obj[key], tier: PluginTier.DATA },
    set: { fn: (obj: any, key: string, value: any) => ({ ...obj, [key]: value }), tier: PluginTier.DATA },
    keys: { fn: (obj: any) => Object.keys(obj), tier: PluginTier.DATA },
    values: { fn: (obj: any) => Object.values(obj), tier: PluginTier.DATA },
    merge: { fn: (...objects: any[]) => Object.assign({}, ...objects), tier: PluginTier.DATA },
    has: { fn: (obj: any, key: string) => key in obj, tier: PluginTier.DATA },
  },
  date: {
    now: { fn: () => new Date().toISOString(), tier: PluginTier.DATA },
    format: { fn: (date: string, format?: string) => new Date(date).toLocaleDateString(), tier: PluginTier.DATA },
    parse: { fn: (dateStr: string) => new Date(dateStr).toISOString(), tier: PluginTier.DATA },
  },
  logic: {
    if: { fn: (condition: any, thenValue: any, elseValue?: any) => condition ? thenValue : elseValue, tier: PluginTier.DATA },
    and: { fn: (...args: boolean[]) => args.every(Boolean), tier: PluginTier.DATA },
    or: { fn: (...args: boolean[]) => args.some(Boolean), tier: PluginTier.DATA },
    not: { fn: (value: boolean) => !value, tier: PluginTier.DATA },
    equals: { fn: (a: any, b: any) => a === b, tier: PluginTier.DATA },
    gt: { fn: (a: number, b: number) => a > b, tier: PluginTier.DATA },
    lt: { fn: (a: number, b: number) => a < b, tier: PluginTier.DATA },
    gte: { fn: (a: number, b: number) => a >= b, tier: PluginTier.DATA },
    lte: { fn: (a: number, b: number) => a <= b, tier: PluginTier.DATA },
  },
};

// Tier 2 only functions
const TIER2_FUNCTIONS: FunctionRegistry = {
  http: {
    get: { fn: async (url: string) => { throw new Error('HTTP not implemented'); }, tier: PluginTier.LOGIC },
    post: { fn: async (url: string, data: any) => { throw new Error('HTTP not implemented'); }, tier: PluginTier.LOGIC },
  },
  storage: {
    get: { fn: (key: string) => localStorage.getItem(key), tier: PluginTier.LOGIC },
    set: { fn: (key: string, value: string) => localStorage.setItem(key, value), tier: PluginTier.LOGIC },
    remove: { fn: (key: string) => localStorage.removeItem(key), tier: PluginTier.LOGIC },
    clear: { fn: () => localStorage.clear(), tier: PluginTier.LOGIC },
  },
};

// ============================================================================
// EXPRESSION PARSER
// ============================================================================

export class MELExpressionParser {
  private pos = 0;
  private text = '';

  parse(expression: string): ExpressionNode {
    this.text = expression.trim();
    this.pos = 0;

    if (this.text.length === 0) {
      throw new MELSyntaxError('Empty expression');
    }

    return this.parseExpression();
  }

  private parseExpression(): ExpressionNode {
    return this.parseLogicalOr();
  }

  private parseLogicalOr(): ExpressionNode {
    let left = this.parseLogicalAnd();

    while (this.peek() === '|' && this.peek(1) === '|') {
      this.consume(2);
      const right = this.parseLogicalAnd();
      left = {
        type: 'binary_op',
        operator: '||',
        left,
        right,
      };
    }

    return left;
  }

  private parseLogicalAnd(): ExpressionNode {
    let left = this.parseComparison();

    while (this.peek() === '&' && this.peek(1) === '&') {
      this.consume(2);
      const right = this.parseComparison();
      left = {
        type: 'binary_op',
        operator: '&&',
        left,
        right,
      };
    }

    return left;
  }

  private parseComparison(): ExpressionNode {
    let left = this.parseAdditive();

    const operators = ['==', '!=', '>=', '<=', '>', '<'];
    for (const op of operators) {
      if (this.peekString(op)) {
        this.consume(op.length);
        const right = this.parseAdditive();
        return {
          type: 'binary_op',
          operator: op as any,
          left,
          right,
        };
      }
    }

    return left;
  }

  private parseAdditive(): ExpressionNode {
    let left = this.parseMultiplicative();

    while (this.peek() === '+' || this.peek() === '-') {
      const operator = this.consume();
      const right = this.parseMultiplicative();
      left = {
        type: 'binary_op',
        operator: operator as any,
        left,
        right,
      };
    }

    return left;
  }

  private parseMultiplicative(): ExpressionNode {
    let left = this.parseUnary();

    while (this.peek() === '*' || this.peek() === '/' || this.peek() === '%') {
      const operator = this.consume();
      const right = this.parseUnary();
      left = {
        type: 'binary_op',
        operator: operator as any,
        left,
        right,
      };
    }

    return left;
  }

  private parseUnary(): ExpressionNode {
    if (this.peek() === '!' || this.peek() === '-') {
      const operator = this.consume();
      const operand = this.parseUnary();
      return {
        type: 'unary_op',
        operator: operator as any,
        operand,
      };
    }

    return this.parsePrimary();
  }

  private parsePrimary(): ExpressionNode {
    this.skipWhitespace();

    // Numbers
    if (this.isDigit(this.peek()) || (this.peek() === '-' && this.isDigit(this.peek(1)))) {
      return this.parseNumber();
    }

    // Strings
    if (this.peek() === '"' || this.peek() === "'") {
      return this.parseString();
    }

    // State references ($state.x)
    if (this.peek() === '$') {
      return this.parseStateOrFunction();
    }

    // Booleans
    if (this.peekString('true')) {
      this.consume(4);
      return { type: 'literal', value: true };
    }
    if (this.peekString('false')) {
      this.consume(5);
      return { type: 'literal', value: false };
    }

    // null
    if (this.peekString('null')) {
      this.consume(4);
      return { type: 'literal', value: null };
    }

    // Arrays
    if (this.peek() === '[') {
      return this.parseArray();
    }

    // Objects
    if (this.peek() === '{') {
      return this.parseObject();
    }

    // Parentheses
    if (this.peek() === '(') {
      this.consume();
      const expr = this.parseExpression();
      if (this.peek() !== ')') {
        throw new MELSyntaxError('Expected closing parenthesis');
      }
      this.consume();
      return expr;
    }

    throw new MELSyntaxError(`Unexpected character: ${this.peek()}`);
  }

  private parseNumber(): LiteralNode {
    let numStr = '';
    while (this.isDigit(this.peek()) || this.peek() === '.') {
      numStr += this.consume();
    }
    return { type: 'literal', value: parseFloat(numStr) };
  }

  private parseString(): LiteralNode {
    const quote = this.consume(); // " or '
    let str = '';
    while (this.peek() !== quote && !this.isEOF()) {
      if (this.peek() === '\\') {
        this.consume();
        str += this.consume(); // Escape sequence
      } else {
        str += this.consume();
      }
    }
    if (this.peek() !== quote) {
      throw new MELSyntaxError('Unterminated string');
    }
    this.consume(); // closing quote
    return { type: 'literal', value: str };
  }

  private parseStateOrFunction(): StateRefNode | FunctionCallNode {
    this.consume(); // $
    const identifier = this.parseIdentifier();

    // Check if it's a function call ($category.function)
    if (this.peek() === '.') {
      this.consume(); // .
      const functionName = this.parseIdentifier();

      // Parse arguments
      if (this.peek() === '(') {
        this.consume(); // (
        const args: ExpressionNode[] = [];

        if (this.peek() !== ')') {
          args.push(this.parseExpression());
          while (this.peek() === ',') {
            this.consume(); // ,
            args.push(this.parseExpression());
          }
        }

        if (this.peek() !== ')') {
          throw new MELSyntaxError('Expected closing parenthesis in function call');
        }
        this.consume(); // )

        return {
          type: 'function_call',
          category: identifier,
          name: functionName,
          args,
        };
      }
      // No parentheses - treat as property access (e.g., $state.count)
      return {
        type: 'state_ref',
        path: [identifier, functionName],
      };
    }

    // Simple state reference
    return {
      type: 'state_ref',
      path: [identifier],
    };
  }

  private parseArray(): ArrayNode {
    this.consume(); // [
    const elements: ExpressionNode[] = [];

    this.skipWhitespace();
    if (this.peek() !== ']') {
      elements.push(this.parseExpression());
      while (this.peek() === ',') {
        this.consume(); // ,
        this.skipWhitespace();
        if (this.peek() === ']') break; // trailing comma
        elements.push(this.parseExpression());
      }
    }

    if (this.peek() !== ']') {
      throw new MELSyntaxError('Expected closing bracket in array');
    }
    this.consume(); // ]

    return { type: 'array', elements };
  }

  private parseObject(): ObjectNode {
    this.consume(); // {
    const properties: Record<string, ExpressionNode> = {};

    this.skipWhitespace();
    if (this.peek() !== '}') {
      const key = this.parseObjectKey();
      this.skipWhitespace();
      if (this.peek() !== ':') {
        throw new MELSyntaxError('Expected colon in object');
      }
      this.consume(); // :
      properties[key] = this.parseExpression();

      while (this.peek() === ',') {
        this.consume(); // ,
        this.skipWhitespace();
        if (this.peek() === '}') break; // trailing comma
        const key2 = this.parseObjectKey();
        this.skipWhitespace();
        if (this.peek() !== ':') {
          throw new MELSyntaxError('Expected colon in object');
        }
        this.consume(); // :
        properties[key2] = this.parseExpression();
      }
    }

    if (this.peek() !== '}') {
      throw new MELSyntaxError('Expected closing brace in object');
    }
    this.consume(); // }

    return { type: 'object', properties };
  }

  private parseObjectKey(): string {
    this.skipWhitespace();
    if (this.peek() === '"' || this.peek() === "'") {
      const node = this.parseString();
      return node.value;
    }
    return this.parseIdentifier();
  }

  private parseIdentifier(): string {
    this.skipWhitespace();
    let identifier = '';
    while (this.isAlphaNumeric(this.peek()) || this.peek() === '_') {
      identifier += this.consume();
    }
    if (identifier.length === 0) {
      throw new MELSyntaxError('Expected identifier');
    }
    return identifier;
  }

  private peek(offset = 0): string {
    return this.text[this.pos + offset] || '';
  }

  private peekString(str: string): boolean {
    return this.text.substring(this.pos, this.pos + str.length) === str;
  }

  private consume(count = 1): string {
    const result = this.text.substring(this.pos, this.pos + count);
    this.pos += count;
    return result;
  }

  private skipWhitespace(): void {
    while (this.isWhitespace(this.peek())) {
      this.consume();
    }
  }

  private isWhitespace(char: string): boolean {
    return /\s/.test(char);
  }

  private isDigit(char: string): boolean {
    return /[0-9]/.test(char);
  }

  private isAlpha(char: string): boolean {
    return /[a-zA-Z]/.test(char);
  }

  private isAlphaNumeric(char: string): boolean {
    return this.isAlpha(char) || this.isDigit(char);
  }

  private isEOF(): boolean {
    return this.pos >= this.text.length;
  }
}

// ============================================================================
// EXPRESSION EVALUATOR
// ============================================================================

export class MELExpressionEvaluator {
  private parser = new MELExpressionParser();
  private functionRegistry: FunctionRegistry;

  constructor(
    private tier: PluginTier,
    private state: PluginState,
    private params?: Record<string, any>
  ) {
    // Merge Tier 1 and Tier 2 functions based on tier
    this.functionRegistry = { ...TIER1_FUNCTIONS };
    if (tier === PluginTier.LOGIC) {
      Object.keys(TIER2_FUNCTIONS).forEach((category) => {
        this.functionRegistry[category] = {
          ...this.functionRegistry[category],
          ...TIER2_FUNCTIONS[category],
        };
      });
    }
  }

  evaluate(expr: Expression): any {
    if (!expr.parsed) {
      expr.parsed = this.parser.parse(expr.raw);
    }
    return this.evaluateNode(expr.parsed);
  }

  private evaluateNode(node: ExpressionNode): any {
    switch (node.type) {
      case 'literal':
        return (node as LiteralNode).value;

      case 'state_ref':
        return this.evaluateStateRef(node as StateRefNode);

      case 'function_call':
        return this.evaluateFunctionCall(node as FunctionCallNode);

      case 'binary_op':
        return this.evaluateBinaryOp(node as BinaryOpNode);

      case 'unary_op':
        return this.evaluateUnaryOp(node as UnaryOpNode);

      case 'conditional':
        return this.evaluateConditional(node as ConditionalNode);

      case 'array':
        return this.evaluateArray(node as ArrayNode);

      case 'object':
        return this.evaluateObject(node as ObjectNode);

      default:
        throw new MELRuntimeError(`Unknown node type: ${(node as any).type}`);
    }
  }

  private evaluateStateRef(node: StateRefNode): any {
    const [root, ...rest] = node.path;

    // Check if it's a state reference
    if (root === 'state') {
      let value = this.state;
      for (const key of rest) {
        if (value === null || value === undefined) {
          return undefined;
        }
        value = value[key];
      }
      return value;
    }

    // Check if it's a parameter reference
    if (this.params && root in this.params) {
      return this.params[root];
    }

    throw new MELRuntimeError(`Unknown reference: ${node.path.join('.')}`);
  }

  private evaluateFunctionCall(node: FunctionCallNode): any {
    const category = this.functionRegistry[node.category];
    if (!category) {
      throw new MELRuntimeError(`Unknown function category: ${node.category}`);
    }

    const func = category[node.name];
    if (!func) {
      throw new MELRuntimeError(`Unknown function: ${node.category}.${node.name}`);
    }

    // Check tier permissions
    if (this.tier === PluginTier.DATA && func.tier === PluginTier.LOGIC) {
      throw new MELSecurityError(
        `Function ${node.category}.${node.name} not allowed in Tier 1`,
        { function: `${node.category}.${node.name}`, tier: this.tier }
      );
    }

    // Evaluate arguments
    const args = node.args.map((arg) => this.evaluateNode(arg));

    // Execute function
    return func.fn(...args);
  }

  private evaluateBinaryOp(node: BinaryOpNode): any {
    const left = this.evaluateNode(node.left);
    const right = this.evaluateNode(node.right);

    switch (node.operator) {
      case '+': return left + right;
      case '-': return left - right;
      case '*': return left * right;
      case '/': return right !== 0 ? left / right : NaN;
      case '%': return left % right;
      case '==': return left === right;
      case '!=': return left !== right;
      case '>': return left > right;
      case '<': return left < right;
      case '>=': return left >= right;
      case '<=': return left <= right;
      case '&&': return left && right;
      case '||': return left || right;
      default:
        throw new MELRuntimeError(`Unknown operator: ${node.operator}`);
    }
  }

  private evaluateUnaryOp(node: UnaryOpNode): any {
    const operand = this.evaluateNode(node.operand);

    switch (node.operator) {
      case '!': return !operand;
      case '-': return -operand;
      default:
        throw new MELRuntimeError(`Unknown unary operator: ${node.operator}`);
    }
  }

  private evaluateConditional(node: ConditionalNode): any {
    const condition = this.evaluateNode(node.condition);
    if (condition) {
      return this.evaluateNode(node.thenBranch);
    } else if (node.elseBranch) {
      return this.evaluateNode(node.elseBranch);
    }
    return undefined;
  }

  private evaluateArray(node: ArrayNode): any[] {
    return node.elements.map((elem) => this.evaluateNode(elem));
  }

  private evaluateObject(node: ObjectNode): Record<string, any> {
    const result: Record<string, any> = {};
    for (const [key, valueNode] of Object.entries(node.properties)) {
      result[key] = this.evaluateNode(valueNode);
    }
    return result;
  }
}
