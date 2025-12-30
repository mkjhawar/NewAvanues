/**
 * Jest Configuration for AvaElements Web Renderer Testing
 *
 * Test Coverage:
 * - 58 Flutter Parity components
 * - Unit tests with React Testing Library
 * - Accessibility tests with jest-axe
 * - Integration tests
 *
 * @since 1.0.0 (Week 5-6: Web Testing Framework)
 */

export default {
  preset: 'ts-jest',
  testEnvironment: 'jsdom',

  // Module paths
  roots: ['<rootDir>/src', '<rootDir>/__tests__'],

  // Setup files
  setupFilesAfterEnv: ['<rootDir>/__tests__/setupTests.ts'],

  // Module name mapping
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
    '\\.(css|less|scss|sass)$': 'identity-obj-proxy',
  },

  // Transform files
  transform: {
    '^.+\\.tsx?$': ['ts-jest', {
      tsconfig: {
        jsx: 'react-jsx',
        esModuleInterop: true,
        allowSyntheticDefaultImports: true,
      },
    }],
  },

  // Test match patterns
  testMatch: [
    '**/__tests__/**/*.test.(ts|tsx)',
    '**/?(*.)+(spec|test).(ts|tsx)',
  ],

  // Coverage configuration
  collectCoverageFrom: [
    'src/**/*.{ts,tsx}',
    '!src/**/*.d.ts',
    '!src/**/*.stories.tsx',
    '!src/index.ts',
  ],

  coverageThresholds: {
    global: {
      branches: 90,
      functions: 90,
      lines: 90,
      statements: 90,
    },
  },

  coverageReporters: [
    'text',
    'text-summary',
    'html',
    'lcov',
    'json-summary',
  ],

  // Performance
  maxWorkers: '50%',

  // Verbose output
  verbose: true,

  // Module file extensions
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json'],

  // Test timeout
  testTimeout: 10000,
};
