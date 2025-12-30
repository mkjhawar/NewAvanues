# VSCode Extension - Phase 1 Tasks: Foundation & Infrastructure

**Project:** AVAMagic Studio - VSCode Extension
**Phase:** 1 of 9 - Foundation & Infrastructure
**Status:** Ready for Implementation
**Duration:** 2 weeks
**Effort:** 100 hours
**Total Tasks:** 32
**Dependencies:** None
**Risk Level:** Low

---

## Overview

Phase 1 establishes the foundation for the VSCode extension, including project structure, build system, testing infrastructure, core architecture, and developer tools.

**Objectives:**
- ✅ Set up project structure and build system
- ✅ Implement core extension architecture
- ✅ Establish development workflow
- ✅ Create testing infrastructure

---

## Task Groups

### 1.1 Project Setup (10 hours, 7 tasks)

#### [1.1.1] Initialize VSCode extension project (1h)
**Priority:** P0 - Blocking
**Dependencies:** None
**Assignee:** Foundation Agent

**Subtasks:**
- [ ] Create project structure using `yo code` generator (Yeoman)
- [ ] Configure TypeScript with strict mode
- [ ] Set up Git repository and .gitignore
- [ ] Initialize npm package.json with project metadata

**Acceptance Criteria:**
- Project directory created: `tools/vscode-extension/`
- TypeScript configured with strict mode enabled
- Git repository initialized with proper .gitignore
- package.json has correct metadata (name, version, description, publisher)

**Output:**
```
tools/vscode-extension/
├── package.json
├── tsconfig.json
├── .gitignore
├── .vscode/
│   ├── launch.json
│   └── tasks.json
└── src/
    └── extension.ts
```

---

#### [1.1.2] Configure build system (2h)
**Priority:** P0 - Blocking
**Dependencies:** [1.1.1]
**Assignee:** Foundation Agent

**Subtasks:**
- [ ] Install Webpack 5 and TypeScript loader
- [ ] Configure webpack.config.js for extension host bundle
- [ ] Configure webpack.config.js for language server bundle (separate)
- [ ] Add source maps for debugging
- [ ] Create production build configuration (minification, optimization)
- [ ] Add build scripts to package.json

**Acceptance Criteria:**
- Webpack 5 installed and configured
- Two separate bundles: extension host and language server
- Source maps generated for both debug and production
- Build scripts work: `npm run compile`, `npm run watch`, `npm run build`

**Output:**
```
webpack.config.js
webpack.production.config.js
package.json (updated with build scripts)
```

---

#### [1.1.3] Set up linting and formatting (1h)
**Priority:** P1
**Dependencies:** [1.1.1]
**Assignee:** Foundation Agent

**Subtasks:**
- [ ] Install ESLint with @typescript-eslint parser
- [ ] Configure .eslintrc.json with strict rules
- [ ] Install Prettier
- [ ] Configure .prettierrc with consistent formatting
- [ ] Add Husky for pre-commit hooks
- [ ] Create .editorconfig for consistent editor settings

**Acceptance Criteria:**
- ESLint configured with TypeScript support
- Prettier configured and integrated with ESLint
- Pre-commit hook runs linting and formatting
- All existing code passes linting

**Output:**
```
.eslintrc.json
.prettierrc
.editorconfig
.husky/pre-commit
```

---

#### [1.1.4] Configure TypeScript (2h)
**Priority:** P0 - Blocking
**Dependencies:** [1.1.1]
**Assignee:** Foundation Agent

**Subtasks:**
- [ ] Set up strict mode with all strict flags enabled
- [ ] Configure path aliases (@src, @types, @utils, etc.)
- [ ] Set up composite projects for extension and language server
- [ ] Add type definitions for VSCode API (@types/vscode)
- [ ] Add type definitions for Node.js (@types/node)
- [ ] Configure module resolution and target (ES2020)

**Acceptance Criteria:**
- tsconfig.json has strict mode enabled
- Path aliases work (@src/* resolves correctly)
- Composite project setup for extension and language server
- All VSCode API types available

**Output:**
```
tsconfig.json
tsconfig.extension.json
tsconfig.languageServer.json
```

---

#### [1.1.5] Create directory structure (1h)
**Priority:** P0 - Blocking
**Dependencies:** [1.1.1]
**Assignee:** Foundation Agent

**Subtasks:**
- [ ] Create src/extension/ directory (extension host code)
- [ ] Create src/languageServer/ directory (LSP server)
- [ ] Create src/webviews/ directory (React webviews)
- [ ] Create src/ai/ directory (AI services)
- [ ] Create src/components/ directory (component system)
- [ ] Create src/templates/ directory (template system)
- [ ] Create src/utils/ directory (shared utilities)
- [ ] Create test/ directory with subdirectories
- [ ] Create assets/ directory for icons, images
- [ ] Create docs/ directory for documentation

**Acceptance Criteria:**
- All directories created with README.md placeholders
- Directory structure matches architecture design

**Output:**
```
tools/vscode-extension/
├── src/
│   ├── extension/          # Extension host code
│   ├── languageServer/     # LSP server
│   ├── webviews/          # React webviews
│   ├── ai/                # AI services
│   ├── components/        # Component system
│   ├── templates/         # Template system
│   └── utils/            # Shared utilities
├── test/
│   ├── unit/
│   ├── integration/
│   └── e2e/
├── assets/
│   ├── icons/
│   └── images/
└── docs/
```

---

#### [1.1.6] Set up CI/CD pipeline (2h)
**Priority:** P1
**Dependencies:** [1.1.2], [1.1.3]
**Assignee:** Foundation Agent

**Subtasks:**
- [ ] Create .github/workflows/ci.yml for GitHub Actions
- [ ] Add build validation workflow (runs on PRs)
- [ ] Add test execution workflow
- [ ] Add linting and formatting checks
- [ ] Set up automated VSIX packaging on main branch
- [ ] Configure deployment to VSCode Marketplace (staging) - manual trigger

**Acceptance Criteria:**
- CI workflow runs on every PR
- Build, test, and lint steps all pass
- VSIX package created on successful main branch builds
- Workflow badges added to README

**Output:**
```
.github/workflows/ci.yml
.github/workflows/release.yml
```

---

#### [1.1.7] Create package.json configuration (1h)
**Priority:** P0 - Blocking
**Dependencies:** [1.1.1]
**Assignee:** Foundation Agent

**Subtasks:**
- [ ] Define extension metadata (name, displayName, description, version)
- [ ] Configure activation events (onLanguage, onCommand, etc.)
- [ ] Set up contribution points:
  - [ ] Commands
  - [ ] Languages (avamagic)
  - [ ] Grammars
  - [ ] Configuration
  - [ ] Menus
  - [ ] Keybindings
- [ ] Define runtime dependencies
- [ ] Define dev dependencies
- [ ] Add extension capabilities (workspace, untrusted workspaces)

**Acceptance Criteria:**
- package.json has all required VSCode extension fields
- Activation events properly configured
- Contribution points defined (even if empty implementations)
- Dependencies list is complete

**Output:**
```json
{
  "name": "avamagic-studio",
  "displayName": "AVAMagic Studio",
  "description": "Visual UI design tools for AVAMagic cross-platform framework",
  "version": "0.1.0",
  "publisher": "augmentalis",
  "engines": {
    "vscode": "^1.80.0"
  },
  "categories": ["Programming Languages", "Snippets", "Other"],
  "activationEvents": [
    "onLanguage:avamagic",
    "onCommand:avamagic.ai.generateFromPrompt",
    "workspaceContains:**/*.vos",
    "workspaceContains:**/*.ava"
  ],
  "main": "./dist/extension.js",
  "contributes": {
    "languages": [...],
    "grammars": [...],
    "commands": [...],
    "configuration": {...}
  }
}
```

---

### 1.2 Testing Infrastructure (15 hours, 6 tasks)

#### [1.2.1] Set up Jest testing framework (2h)
**Priority:** P0 - Blocking
**Dependencies:** [1.1.1], [1.1.4]
**Assignee:** Testing Agent

**Subtasks:**
- [ ] Install Jest and ts-jest
- [ ] Configure jest.config.js with TypeScript support
- [ ] Set up coverage thresholds (90% for critical paths)
- [ ] Add test scripts to package.json (test, test:watch, test:coverage)
- [ ] Create example unit test

**Acceptance Criteria:**
- Jest configured and working with TypeScript
- Coverage thresholds enforced
- Test scripts run successfully
- Example test passes

**Output:**
```
jest.config.js
test/unit/example.test.ts
```

---

#### [1.2.2] Configure VSCode Extension Test Runner (3h)
**Priority:** P0 - Blocking
**Dependencies:** [1.1.1], [1.1.4]
**Assignee:** Testing Agent

**Subtasks:**
- [ ] Install @vscode/test-electron
- [ ] Create test harness (test/runTest.ts)
- [ ] Set up integration test environment
- [ ] Configure test workspace with sample files
- [ ] Create example integration test

**Acceptance Criteria:**
- VSCode Extension Test Runner configured
- Test harness runs tests in real VSCode instance
- Test workspace has sample .vos/.ava files
- Example integration test passes

**Output:**
```
test/runTest.ts
test/integration/example.integration.test.ts
test/fixtures/sample.vos
```

---

#### [1.2.3] Create test utilities (2h)
**Priority:** P1
**Dependencies:** [1.2.1], [1.2.2]
**Assignee:** Testing Agent

**Subtasks:**
- [ ] Create mock VSCode API utilities
- [ ] Create test fixtures (sample .vos/.ava files)
- [ ] Add assertion helpers for common scenarios
- [ ] Create mock AI providers (returns hardcoded responses)

**Acceptance Criteria:**
- Test utilities package created
- Mock VSCode API covers common extension APIs
- At least 5 test fixtures created
- Mock AI providers ready for testing

**Output:**
```
test/utils/mockVSCode.ts
test/utils/assertions.ts
test/fixtures/*.vos
test/mocks/mockAIProvider.ts
```

---

#### [1.2.4] Set up E2E testing framework (4h)
**Priority:** P2
**Dependencies:** [1.2.2]
**Assignee:** Testing Agent

**Subtasks:**
- [ ] Evaluate E2E frameworks (Playwright vs Puppeteer)
- [ ] Install chosen framework
- [ ] Configure E2E test environment
- [ ] Create page objects for extension UI
- [ ] Add smoke test (extension activates)

**Acceptance Criteria:**
- E2E framework installed and configured
- Page objects created for key UI elements
- Smoke test passes (extension loads without errors)

**Output:**
```
test/e2e/smoke.e2e.test.ts
test/e2e/pageObjects/ComponentPalette.ts
playwright.config.ts (or equivalent)
```

---

#### [1.2.5] Create test documentation (2h)
**Priority:** P2
**Dependencies:** [1.2.1], [1.2.2], [1.2.3]
**Assignee:** Testing Agent

**Subtasks:**
- [ ] Document testing strategy (unit, integration, E2E)
- [ ] Create test writing guidelines
- [ ] Add examples of each test type
- [ ] Document mocking strategies
- [ ] Add troubleshooting guide for test failures

**Acceptance Criteria:**
- Testing documentation is comprehensive
- Examples provided for each test type
- Mocking strategies clearly explained

**Output:**
```
docs/testing/TESTING-STRATEGY.md
docs/testing/WRITING-TESTS.md
docs/testing/EXAMPLES.md
```

---

#### [1.2.6] Set up code coverage reporting (2h)
**Priority:** P1
**Dependencies:** [1.2.1]
**Assignee:** Testing Agent

**Subtasks:**
- [ ] Configure Istanbul/NYC for coverage
- [ ] Set up coverage reports (HTML, LCOV, JSON)
- [ ] Add coverage badges to README
- [ ] Configure CI to fail on coverage drops below 90%
- [ ] Create coverage report scripts

**Acceptance Criteria:**
- Coverage reports generated in multiple formats
- HTML report viewable in browser
- CI enforces 90% coverage threshold
- Coverage badges visible in README

**Output:**
```
coverage/ (generated)
.nycrc (or jest coverage config)
README.md (updated with badges)
```

---

### 1.3 Core Extension Architecture (25 hours, 7 tasks)

#### [1.3.1] Implement extension activation (3h)
**Priority:** P0 - Blocking
**Dependencies:** [1.1.7]
**Assignee:** Architecture Agent

**Subtasks:**
- [ ] Create activate() function in extension.ts
- [ ] Set up ExtensionContext
- [ ] Initialize core services (logging, config, DI container)
- [ ] Register commands (placeholder implementations)
- [ ] Register language providers (placeholder)
- [ ] Create deactivate() function for cleanup
- [ ] Add activation logging

**Acceptance Criteria:**
- Extension activates successfully in development host
- All services initialized without errors
- Commands registered and visible in command palette
- Deactivation cleans up resources properly

**Output:**
```typescript
// src/extension.ts
export async function activate(context: vscode.ExtensionContext): Promise<void> {
  const logger = new Logger();
  const config = new ConfigurationManager();
  const container = new ServiceContainer();

  // Register services
  container.registerSingleton('logger', logger);
  container.registerSingleton('config', config);

  // Register commands
  context.subscriptions.push(
    vscode.commands.registerCommand('avamagic.ai.generateFromPrompt', () => {
      vscode.window.showInformationMessage('Generate from prompt');
    })
  );

  logger.info('AVAMagic Studio extension activated');
}

export function deactivate(): void {
  // Cleanup
}
```

---

#### [1.3.2] Create configuration manager (4h)
**Priority:** P0 - Blocking
**Dependencies:** [1.3.1]
**Assignee:** Architecture Agent

**Subtasks:**
- [ ] Create ConfigurationManager class
- [ ] Implement workspace configuration access (vscode.workspace.getConfiguration)
- [ ] Add configuration validation
- [ ] Create configuration change listeners
- [ ] Add default configuration values
- [ ] Implement typed configuration getters
- [ ] Add unit tests

**Acceptance Criteria:**
- Configuration manager provides type-safe access to settings
- Validation catches invalid configuration values
- Change listeners trigger on configuration updates
- Default values used when settings not provided
- 90%+ test coverage

**Output:**
```typescript
// src/extension/ConfigurationManager.ts
export class ConfigurationManager {
  getAIProvider(): AIProviderType;
  getClaudeAPIKey(): string | undefined;
  getGeminiAPIKey(): string | undefined;
  getTemplatePreferences(): TemplatePreferences;
  validate(): ValidationResult;
  onDidChange(callback: (changed: ConfigChange) => void): vscode.Disposable;
}
```

---

#### [1.3.3] Implement logging system (3h)
**Priority:** P0 - Blocking
**Dependencies:** [1.3.1]
**Assignee:** Architecture Agent

**Subtasks:**
- [ ] Create Logger class with log levels (DEBUG, INFO, WARN, ERROR)
- [ ] Add file logging to extension storage path
- [ ] Implement log rotation (max 10MB per file, keep 5 files)
- [ ] Create debug output channel in VSCode
- [ ] Add structured logging with context
- [ ] Add unit tests

**Acceptance Criteria:**
- Logger supports all log levels
- File logging writes to extension storage
- Log rotation works correctly
- Output channel visible in VSCode Output panel
- Structured logging includes timestamps and context
- 90%+ test coverage

**Output:**
```typescript
// src/extension/Logger.ts
export class Logger {
  debug(message: string, context?: Record<string, any>): void;
  info(message: string, context?: Record<string, any>): void;
  warn(message: string, context?: Record<string, any>): void;
  error(message: string, error?: Error, context?: Record<string, any>): void;
  setLevel(level: LogLevel): void;
}
```

---

#### [1.3.4] Create service container/DI system (5h)
**Priority:** P1
**Dependencies:** [1.3.1]
**Assignee:** Architecture Agent

**Subtasks:**
- [ ] Create ServiceContainer class
- [ ] Implement service registration (singleton, transient)
- [ ] Add service lifecycle management (init, dispose)
- [ ] Implement dependency resolution
- [ ] Add circular dependency detection
- [ ] Create service decorators (@injectable, @inject)
- [ ] Add unit tests

**Acceptance Criteria:**
- Services can be registered as singleton or transient
- Dependency injection resolves dependencies automatically
- Circular dependencies detected and throw errors
- Lifecycle methods called appropriately
- Decorators work for TypeScript classes
- 90%+ test coverage

**Output:**
```typescript
// src/extension/ServiceContainer.ts
export class ServiceContainer {
  registerSingleton<T>(id: string, factory: () => T): void;
  registerTransient<T>(id: string, factory: () => T): void;
  resolve<T>(id: string): T;
  dispose(): Promise<void>;
}

// Decorators
export function injectable(): ClassDecorator;
export function inject(id: string): ParameterDecorator;
```

---

#### [1.3.5] Implement telemetry system (4h)
**Priority:** P2
**Dependencies:** [1.3.1], [1.3.2]
**Assignee:** Architecture Agent

**Subtasks:**
- [ ] Create Telemetry class
- [ ] Add event tracking (extension activation, command execution, etc.)
- [ ] Implement opt-in/opt-out via configuration
- [ ] Create anonymized usage metrics (no PII)
- [ ] Add error reporting with stack traces
- [ ] Implement event batching and throttling
- [ ] Add unit tests

**Acceptance Criteria:**
- Telemetry respects user opt-in/opt-out preference
- Events are anonymized (no user data collected)
- Error reporting captures useful diagnostic info
- Batching reduces network overhead
- 90%+ test coverage

**Output:**
```typescript
// src/extension/Telemetry.ts
export class Telemetry {
  trackEvent(name: string, properties?: Record<string, any>): void;
  trackError(error: Error, properties?: Record<string, any>): void;
  trackMetric(name: string, value: number): void;
  setEnabled(enabled: boolean): void;
}
```

---

#### [1.3.6] Create extension API (3h)
**Priority:** P2
**Dependencies:** [1.3.1]
**Assignee:** Architecture Agent

**Subtasks:**
- [ ] Define public API interfaces
- [ ] Implement API versioning (v1)
- [ ] Add API documentation with JSDoc
- [ ] Create usage examples
- [ ] Expose API via extension exports
- [ ] Add deprecation warnings for future breaking changes

**Acceptance Criteria:**
- Public API is well-documented
- API versioning allows future changes
- Usage examples are clear
- API can be consumed by other extensions

**Output:**
```typescript
// src/extension/api.ts
export interface AVAMagicAPI {
  version: string;
  generateCode(prompt: string, options?: GenerateOptions): Promise<string>;
  getComponents(): Component[];
  getTemplates(): Template[];
}

export function getAPI(version: 'v1'): AVAMagicAPI;
```

---

#### [1.3.7] Set up workspace management (3h)
**Priority:** P1
**Dependencies:** [1.3.2]
**Assignee:** Architecture Agent

**Subtasks:**
- [ ] Create WorkspaceManager class
- [ ] Implement workspace detection (checks for .vos/.ava files)
- [ ] Add multi-root workspace support
- [ ] Create workspace configuration (.avamagic/config.json)
- [ ] Add workspace-specific settings (overrides global settings)
- [ ] Implement workspace file watching
- [ ] Add unit tests

**Acceptance Criteria:**
- Workspace detection works in single and multi-root workspaces
- Workspace-specific settings override global settings
- File watching detects .vos/.ava file changes
- 90%+ test coverage

**Output:**
```typescript
// src/extension/WorkspaceManager.ts
export class WorkspaceManager {
  detectWorkspace(): boolean;
  getWorkspaceFolders(): vscode.WorkspaceFolder[];
  getWorkspaceConfig(): WorkspaceConfig;
  watchFiles(pattern: string, callback: (uri: vscode.Uri) => void): vscode.Disposable;
}
```

---

### 1.4 Documentation & Developer Experience (15 hours, 6 tasks)

#### [1.4.1] Create README.md (2h)
**Priority:** P1
**Dependencies:** [1.1.1]
**Assignee:** Documentation Agent

**Subtasks:**
- [ ] Add feature overview with bullet points
- [ ] Document installation steps (from marketplace, from VSIX)
- [ ] Create usage guide with examples
- [ ] Add screenshots/GIFs (placeholder for now)
- [ ] Include link to full documentation
- [ ] Add badges (build status, version, downloads)

**Acceptance Criteria:**
- README is clear and well-structured
- Installation steps are accurate
- Usage examples are helpful
- Badges display correctly

**Output:**
```markdown
# AVAMagic Studio - VSCode Extension

Visual UI design tools for the AVAMagic cross-platform framework.

## Features
- Multi-provider AI assistance (Claude, Gemini, GPT-4)
- 263 pre-built components
- Code generation for Android, iOS, Web, Desktop
- 65+ templates
- Full IntelliSense support

## Installation
...

## Usage
...
```

---

#### [1.4.2] Write CONTRIBUTING.md (2h)
**Priority:** P2
**Dependencies:** [1.1.1], [1.1.3]
**Assignee:** Documentation Agent

**Subtasks:**
- [ ] Document development setup steps
- [ ] Add coding standards (based on ESLint config)
- [ ] Create PR guidelines (branch naming, commit messages)
- [ ] Add issue templates
- [ ] Include testing requirements
- [ ] Add code review checklist

**Acceptance Criteria:**
- Development setup is clear
- Coding standards are documented
- PR process is well-defined
- Issue templates are useful

**Output:**
```markdown
# Contributing to AVAMagic Studio

## Development Setup
...

## Coding Standards
...

## Pull Request Process
...
```

---

#### [1.4.3] Create architecture documentation (4h)
**Priority:** P2
**Dependencies:** [1.3.1] - [1.3.7]
**Assignee:** Documentation Agent

**Subtasks:**
- [ ] Document system architecture with diagrams
- [ ] Add sequence diagrams for key flows
- [ ] Create component diagrams
- [ ] Document design decisions and rationale
- [ ] Add extension lifecycle documentation
- [ ] Document service container and DI system

**Acceptance Criteria:**
- Architecture diagrams are clear
- Design decisions are well-explained
- Documentation matches implementation

**Output:**
```markdown
# Architecture

## System Overview
[Diagram]

## Extension Lifecycle
[Sequence diagram]

## Service Container
[Component diagram]

## Design Decisions
...
```

---

#### [1.4.4] Set up API documentation (3h)
**Priority:** P2
**Dependencies:** [1.3.6]
**Assignee:** Documentation Agent

**Subtasks:**
- [ ] Install TypeDoc
- [ ] Configure typedoc.json
- [ ] Add JSDoc comments to all public APIs
- [ ] Generate HTML documentation
- [ ] Set up documentation hosting (GitHub Pages or similar)

**Acceptance Criteria:**
- TypeDoc generates documentation successfully
- All public APIs have JSDoc comments
- HTML documentation is navigable
- Documentation is published

**Output:**
```
typedoc.json
docs/api/ (generated HTML)
```

---

#### [1.4.5] Create development guides (2h)
**Priority:** P2
**Dependencies:** [1.1.2], [1.2.1]
**Assignee:** Documentation Agent

**Subtasks:**
- [ ] Write debugging guide (using VSCode debugger)
- [ ] Document testing best practices
- [ ] Add troubleshooting guide
- [ ] Create release checklist

**Acceptance Criteria:**
- Debugging guide is helpful
- Testing best practices are clear
- Troubleshooting covers common issues
- Release checklist is comprehensive

**Output:**
```markdown
# Developer Guides

## Debugging
...

## Testing Best Practices
...

## Troubleshooting
...

## Release Checklist
...
```

---

#### [1.4.6] Set up changelog automation (2h)
**Priority:** P2
**Dependencies:** [1.1.1]
**Assignee:** Documentation Agent

**Subtasks:**
- [ ] Install conventional-changelog
- [ ] Configure commit message format (conventional commits)
- [ ] Add changelog generation script
- [ ] Create release notes template
- [ ] Update CONTRIBUTING.md with commit message guidelines

**Acceptance Criteria:**
- Conventional commits enforced
- Changelog generates automatically
- Release notes template is useful

**Output:**
```
.changelogrc
scripts/generate-changelog.sh
CHANGELOG.md (template)
```

---

### 1.5 Security & Performance Foundation (10 hours, 4 tasks)

#### [1.5.1] Implement SecretStorage integration (3h)
**Priority:** P0 - Blocking
**Dependencies:** [1.3.1]
**Assignee:** Security Agent

**Subtasks:**
- [ ] Create SecretManager class wrapping VSCode SecretStorage
- [ ] Implement API key storage methods
- [ ] Add encryption (handled by VSCode)
- [ ] Create key retrieval methods
- [ ] Add key deletion methods
- [ ] Add unit tests (with mock SecretStorage)

**Acceptance Criteria:**
- SecretManager wraps VSCode SecretStorage API
- API keys stored securely (encrypted by OS keychain)
- Retrieval and deletion work correctly
- 90%+ test coverage

**Output:**
```typescript
// src/extension/SecretManager.ts
export class SecretManager {
  async storeAPIKey(provider: AIProviderType, key: string): Promise<void>;
  async getAPIKey(provider: AIProviderType): Promise<string | undefined>;
  async deleteAPIKey(provider: AIProviderType): Promise<void>;
}
```

---

#### [1.5.2] Set up performance monitoring (3h)
**Priority:** P1
**Dependencies:** [1.3.1], [1.3.3]
**Assignee:** Performance Agent

**Subtasks:**
- [ ] Create PerformanceMonitor class
- [ ] Add performance marks/measures (User Timing API)
- [ ] Implement startup time tracking
- [ ] Create performance dashboard (simple logging for now)
- [ ] Add performance tests (threshold-based)
- [ ] Log performance metrics

**Acceptance Criteria:**
- Performance marks track key operations
- Startup time measured accurately
- Performance tests fail if thresholds exceeded
- Metrics logged for analysis

**Output:**
```typescript
// src/extension/PerformanceMonitor.ts
export class PerformanceMonitor {
  mark(name: string): void;
  measure(name: string, startMark: string, endMark: string): number;
  getStartupTime(): number;
  logMetrics(): void;
}
```

---

#### [1.5.3] Implement rate limiting (2h)
**Priority:** P1
**Dependencies:** [1.3.1]
**Assignee:** Security Agent

**Subtasks:**
- [ ] Create RateLimiter class (token bucket algorithm)
- [ ] Add per-provider limits
- [ ] Implement backoff strategies (exponential backoff)
- [ ] Add rate limit UI indicators (status bar item)
- [ ] Add unit tests

**Acceptance Criteria:**
- Rate limiter prevents exceeding provider limits
- Backoff strategy works correctly
- UI shows rate limit status
- 90%+ test coverage

**Output:**
```typescript
// src/utils/RateLimiter.ts
export class RateLimiter {
  async acquire(tokens: number): Promise<void>;
  getRemainingTokens(): number;
  getResetTime(): Date;
}
```

---

#### [1.5.4] Create security audit (2h)
**Priority:** P1
**Dependencies:** [1.1.1]
**Assignee:** Security Agent

**Subtasks:**
- [ ] Run npm audit and fix vulnerabilities
- [ ] Review dependencies for known issues
- [ ] Document security considerations (API key storage, input sanitization)
- [ ] Create security update process
- [ ] Add security section to CONTRIBUTING.md

**Acceptance Criteria:**
- No high or critical vulnerabilities in dependencies
- Security considerations documented
- Update process defined

**Output:**
```markdown
# Security

## Vulnerability Management
...

## API Key Security
...

## Input Sanitization
...

## Security Update Process
...
```

---

### 1.6 Development Tools (25 hours, 7 tasks)

#### [1.6.1] Create development extension (3h)
**Priority:** P2
**Dependencies:** [1.3.1]
**Assignee:** DevTools Agent

**Subtasks:**
- [ ] Build dev tools extension (separate extension for development)
- [ ] Add state inspector (view extension state)
- [ ] Create AI request logger (log all AI API calls)
- [ ] Add performance profiler (visualize performance metrics)

**Acceptance Criteria:**
- Dev tools extension activates alongside main extension
- State inspector shows current extension state
- AI request logger captures all requests/responses
- Performance profiler displays metrics

**Output:**
```
tools/dev-extension/
├── package.json
├── src/
│   ├── stateInspector.ts
│   ├── aiLogger.ts
│   └── profiler.ts
```

---

#### [1.6.2] Set up hot reload (3h)
**Priority:** P2
**Dependencies:** [1.1.2]
**Assignee:** DevTools Agent

**Subtasks:**
- [ ] Configure webpack-dev-server
- [ ] Add extension reload on save
- [ ] Implement webview hot reload
- [ ] Add language server reload
- [ ] Create reload script

**Acceptance Criteria:**
- Extension reloads automatically on file save
- Webviews reload without full extension restart
- Language server restarts on changes

**Output:**
```
webpack.dev.config.js
scripts/reload.sh
```

---

#### [1.6.3] Create debugging configurations (2h)
**Priority:** P1
**Dependencies:** [1.1.1]
**Assignee:** DevTools Agent

**Subtasks:**
- [ ] Add .vscode/launch.json configurations
- [ ] Create debug config for extension host
- [ ] Create debug config for language server
- [ ] Create debug config for webviews
- [ ] Document debugging workflows

**Acceptance Criteria:**
- All debug configurations work
- Breakpoints hit correctly
- Debugging documentation is clear

**Output:**
```json
// .vscode/launch.json
{
  "configurations": [
    {
      "name": "Run Extension",
      "type": "extensionHost",
      "request": "launch"
    },
    {
      "name": "Attach to Language Server",
      "type": "node",
      "request": "attach"
    }
  ]
}
```

---

#### [1.6.4] Implement code generation tools (5h)
**Priority:** P2
**Dependencies:** [1.1.1]
**Assignee:** DevTools Agent

**Subtasks:**
- [ ] Create command generator (scaffolds new commands)
- [ ] Add provider template generator (scaffolds AI providers)
- [ ] Create webview template generator (scaffolds React webviews)
- [ ] Build component definition generator (generates component JSON)
- [ ] Add CLI scripts for generators

**Acceptance Criteria:**
- Generators create properly structured code
- Generated code follows project standards
- CLI scripts are easy to use

**Output:**
```
scripts/generators/
├── command.ts
├── provider.ts
├── webview.ts
└── component.ts
```

---

#### [1.6.5] Set up local testing environment (4h)
**Priority:** P1
**Dependencies:** [1.2.2]
**Assignee:** DevTools Agent

**Subtasks:**
- [ ] Create test workspace
- [ ] Add sample .vos/.ava files
- [ ] Create test components
- [ ] Build test scenarios (auth flow, component creation, etc.)
- [ ] Add test data fixtures

**Acceptance Criteria:**
- Test workspace has realistic sample files
- Test scenarios cover key user workflows
- Test data is comprehensive

**Output:**
```
test/workspace/
├── .avamagic/
│   └── config.json
├── screens/
│   ├── login.vos
│   ├── profile.vos
│   └── settings.vos
└── components/
    └── custom-button.ava
```

---

#### [1.6.6] Create mock data generators (4h)
**Priority:** P2
**Dependencies:** [1.2.3]
**Assignee:** DevTools Agent

**Subtasks:**
- [ ] Build component mock data generator
- [ ] Create AI response mocks (hardcoded responses for each AI method)
- [ ] Add template test data
- [ ] Generate fixture files

**Acceptance Criteria:**
- Mock data generators produce realistic data
- AI response mocks cover all 16 methods
- Fixtures are reusable in tests

**Output:**
```typescript
// test/mocks/mockData.ts
export function generateComponentMock(): Component;
export function generateAIResponseMock(method: string): AIResponse;
export function generateTemplateMock(): Template;
```

---

#### [1.6.7] Set up extension packaging (4h)
**Priority:** P1
**Dependencies:** [1.1.2], [1.1.7]
**Assignee:** DevTools Agent

**Subtasks:**
- [ ] Configure vsce (VSCode Extension Manager)
- [ ] Create VSIX build script
- [ ] Add pre-publish validation (checks version, README, etc.)
- [ ] Test installation from VSIX
- [ ] Document packaging process

**Acceptance Criteria:**
- vsce configured correctly
- VSIX package builds successfully
- Installation from VSIX works
- Pre-publish validation catches issues

**Output:**
```
scripts/package.sh
.vscodeignore
```

---

## Phase 1 Success Criteria

### Technical Deliverables ✅
- [ ] Fully configured TypeScript project with strict mode
- [ ] Working build pipeline (Webpack, source maps)
- [ ] Complete testing infrastructure (Jest, VSCode Test Runner, E2E)
- [ ] Core extension architecture implemented
- [ ] Comprehensive documentation

### Quality Gates ✅
- [ ] Project builds without errors
- [ ] All tests pass (90%+ coverage)
- [ ] Extension activates in development IDE
- [ ] CI/CD pipeline runs successfully
- [ ] All linting and formatting passes
- [ ] No security vulnerabilities (high/critical)

### Functional Requirements ✅
- [ ] Extension activates on .vos/.ava files
- [ ] Commands registered in command palette
- [ ] Configuration system works
- [ ] Logging system operational
- [ ] Service container functional
- [ ] SecretStorage integrated

---

## Dependencies for Next Phase

Phase 2 (Language Support & LSP) requires:
- ✅ Phase 1 complete (all tasks)
- ✅ Build system working
- ✅ Testing infrastructure in place
- ✅ Core architecture implemented

---

## Risk Mitigation

**Low-Risk Phase:** Phase 1 is primarily setup and infrastructure with low technical risk.

**Potential Issues:**
1. **Webpack Configuration Complexity** - Mitigation: Use established templates, thorough testing
2. **VSCode API Changes** - Mitigation: Pin VSCode engine version, monitor breaking changes
3. **Testing Environment Setup** - Mitigation: Follow VSCode extension testing best practices

**Estimated Completion:** 2 weeks with 2 developers working in parallel

---

**Phase Status:** Ready for Implementation
**Next Phase:** Phase 2 - Language Support & LSP (3 weeks, 120 hours, 30 tasks)
