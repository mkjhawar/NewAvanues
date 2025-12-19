/**
 * MagicElements Component Type Definitions
 * TypeScript definitions for Phase 1 components
 */

// ============================================================================
// CORE TYPES
// ============================================================================

export interface Theme {
  colors: {
    primary: string;
    secondary: string;
    background: string;
    surface: string;
    error: string;
    [key: string]: string;
  };
  typography: {
    [key: string]: any;
  };
  spacing: {
    [key: string]: number;
  };
}

export interface Component {
  type: string;
  id?: string;
}

// ============================================================================
// FORM COMPONENTS
// ============================================================================

export interface ButtonComponent extends Component {
  type: 'Button';
  text: string;
  enabled: boolean;
  onClick?: () => void;
}

export interface TextFieldComponent extends Component {
  type: 'TextField';
  value: string;
  label?: string;
  placeholder?: string;
  enabled: boolean;
  onChange?: (value: string) => void;
}

export interface CheckboxComponent extends Component {
  type: 'Checkbox';
  checked: boolean;
  label?: string;
  enabled: boolean;
  onChange?: (checked: boolean) => void;
}

export interface SwitchComponent extends Component {
  type: 'Switch';
  checked: boolean;
  label?: string;
  enabled: boolean;
  onChange?: (checked: boolean) => void;
}

// ============================================================================
// DISPLAY COMPONENTS
// ============================================================================

export interface TextComponent extends Component {
  type: 'Text';
  content: string;
  style?: 'body' | 'heading' | 'caption';
}

export interface ImageComponent extends Component {
  type: 'Image';
  source: string;
  alt?: string;
  width?: number;
  height?: number;
}

export interface IconComponent extends Component {
  type: 'Icon';
  name: string;
  size?: number;
  color?: string;
}

// ============================================================================
// LAYOUT COMPONENTS
// ============================================================================

export interface ContainerComponent extends Component {
  type: 'Container';
  children?: Component[];
  padding?: number;
}

export interface RowComponent extends Component {
  type: 'Row';
  children?: Component[];
  spacing?: number;
}

export interface ColumnComponent extends Component {
  type: 'Column';
  children?: Component[];
  spacing?: number;
}

export interface CardComponent extends Component {
  type: 'Card';
  children?: Component[];
  elevation?: number;
}

// ============================================================================
// NAVIGATION & DATA COMPONENTS
// ============================================================================

export interface ScrollViewComponent extends Component {
  type: 'ScrollView';
  children?: Component[];
  height?: number;
}

export interface ListComponent extends Component {
  type: 'List';
  items?: string[];
}

// ============================================================================
// UNION TYPES
// ============================================================================

export type AnyComponent =
  | ButtonComponent
  | TextFieldComponent
  | CheckboxComponent
  | SwitchComponent
  | TextComponent
  | ImageComponent
  | IconComponent
  | ContainerComponent
  | RowComponent
  | ColumnComponent
  | CardComponent
  | ScrollViewComponent
  | ListComponent;
