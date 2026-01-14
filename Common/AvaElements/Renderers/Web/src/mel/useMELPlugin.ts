/**
 * useMELPlugin - React Hook for MEL Plugin State Management
 * Manages plugin state, reducers, and re-renders
 *
 * @module AvaElements/Web/MEL
 * @since 3.3.0
 */

import { useState, useCallback, useMemo, useEffect } from 'react';
import { parse as parseYAML } from 'yaml';
import {
  PluginDefinition,
  PluginRuntime,
  PluginState,
  PluginTier,
  MELError,
} from './types';
import { MELExpressionEvaluator } from './MELExpressionEvaluator';

export interface UseMELPluginOptions {
  /** Enable state persistence in localStorage */
  persist?: boolean;
  /** Storage key for persisted state */
  storageKey?: string;
  /** Callback when errors occur */
  onError?: (error: MELError) => void;
}

export interface UseMELPluginResult {
  /** Current plugin state */
  state: PluginState;
  /** Dispatch a reducer action */
  dispatch: (action: string, params?: Record<string, any>) => void;
  /** Plugin runtime instance */
  runtime: PluginRuntime;
  /** Plugin definition */
  definition: PluginDefinition;
  /** Current tier (may be downgraded from definition) */
  tier: PluginTier;
  /** Loading state */
  loading: boolean;
  /** Error state */
  error: MELError | null;
}

/**
 * Parse plugin definition from YAML or JSON
 */
function parsePluginDefinition(input: string | PluginDefinition): PluginDefinition {
  if (typeof input === 'string') {
    // Try YAML first, fall back to JSON
    try {
      return parseYAML(input) as PluginDefinition;
    } catch {
      return JSON.parse(input) as PluginDefinition;
    }
  }
  return input;
}

/**
 * Initialize state from plugin definition
 */
function initializeState(definition: PluginDefinition): PluginState {
  const state: PluginState = {};

  for (const [key, variable] of Object.entries(definition.state)) {
    state[key] = variable.default;
  }

  return state;
}

/**
 * Load persisted state from localStorage
 */
function loadPersistedState(storageKey: string): PluginState | null {
  try {
    const stored = localStorage.getItem(storageKey);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (error) {
    console.warn('Failed to load persisted state:', error);
  }
  return null;
}

/**
 * Save state to localStorage
 */
function savePersistedState(storageKey: string, state: PluginState): void {
  try {
    localStorage.setItem(storageKey, JSON.stringify(state));
  } catch (error) {
    console.warn('Failed to persist state:', error);
  }
}

/**
 * Detect platform tier (Web always supports Tier 2)
 */
function detectTier(definition: PluginDefinition): PluginTier {
  // Web platform always supports full Tier 2
  return definition.tier || PluginTier.DATA;
}

/**
 * React hook for MEL plugin state management
 */
export function useMELPlugin(
  input: string | PluginDefinition,
  options: UseMELPluginOptions = {}
): UseMELPluginResult {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<MELError | null>(null);

  // Parse plugin definition (memoized)
  const definition = useMemo(() => {
    try {
      return parsePluginDefinition(input);
    } catch (err) {
      const melError = new MELError(
        `Failed to parse plugin definition: ${err}`,
        'PARSE_ERROR',
        { originalError: err }
      );
      setError(melError);
      options.onError?.(melError);
      // Return empty definition to prevent crashes
      return {
        metadata: { id: 'error', name: 'Error', version: '0.0.0' },
        tier: PluginTier.DATA,
        state: {},
        reducers: {},
        ui: { type: 'Text', props: { content: 'Error loading plugin' } },
      } as PluginDefinition;
    }
  }, [input]);

  // Detect effective tier
  const tier = useMemo(() => detectTier(definition), [definition]);

  // Storage key for persistence
  const storageKey = useMemo(
    () => options.storageKey || `mel_plugin_${definition.metadata.id}`,
    [definition.metadata.id, options.storageKey]
  );

  // Initialize state
  const [state, setState] = useState<PluginState>(() => {
    if (options.persist) {
      const persisted = loadPersistedState(storageKey);
      if (persisted) {
        return persisted;
      }
    }
    return initializeState(definition);
  });

  // Persist state changes
  useEffect(() => {
    if (options.persist) {
      savePersistedState(storageKey, state);
    }
  }, [state, options.persist, storageKey]);

  // Mark as loaded
  useEffect(() => {
    setLoading(false);
  }, []);

  // Dispatch function
  const dispatch = useCallback(
    (action: string, params?: Record<string, any>) => {
      try {
        const reducer = definition.reducers[action];
        if (!reducer) {
          throw new MELError(
            `Unknown reducer: ${action}`,
            'REDUCER_NOT_FOUND',
            { action }
          );
        }

        // Create evaluator with current state
        const evaluator = new MELExpressionEvaluator(tier, state, params);

        // Compute next state
        const nextState: PluginState = { ...state };
        for (const [key, expr] of Object.entries(reducer.next_state)) {
          nextState[key] = evaluator.evaluate(expr);
        }

        // Update state
        setState(nextState);

        // Execute effects (Tier 2 only)
        if (tier === PluginTier.LOGIC && reducer.effects) {
          for (const effect of reducer.effects) {
            // TODO: Implement effects
            console.warn('Effects not yet implemented:', effect);
          }
        }
      } catch (err) {
        const melError = err instanceof MELError
          ? err
          : new MELError(
              `Error dispatching action: ${err}`,
              'DISPATCH_ERROR',
              { action, params, originalError: err }
            );
        setError(melError);
        options.onError?.(melError);
      }
    },
    [definition.reducers, tier, state, options]
  );

  // Evaluate function
  const evaluate = useCallback(
    (expr: any) => {
      try {
        const evaluator = new MELExpressionEvaluator(tier, state);
        return evaluator.evaluate(expr);
      } catch (err) {
        const melError = err instanceof MELError
          ? err
          : new MELError(
              `Evaluation error: ${err}`,
              'EVALUATION_ERROR',
              { originalError: err }
            );
        setError(melError);
        options.onError?.(melError);
        return undefined;
      }
    },
    [tier, state, options]
  );

  // Render function
  const render = useCallback(() => {
    return definition.ui;
  }, [definition.ui]);

  // Build runtime object
  const runtime: PluginRuntime = useMemo(
    () => ({
      definition,
      state,
      tier,
      dispatch,
      evaluate,
      render,
    }),
    [definition, state, tier, dispatch, evaluate, render]
  );

  return {
    state,
    dispatch,
    runtime,
    definition,
    tier,
    loading,
    error,
  };
}
