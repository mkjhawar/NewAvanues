# CodeAvenue Terminal App V2 - Enhanced Design
ver: V1 | date: 260122 | status: draft

## Overview
Enhanced terminal app design incorporating Cline-like ease-of-use, full GitHub/GitLab integration, and multi-terminal consensus capabilities. This design transforms the app from a terminal manager into a full AI-powered development environment.

## Design Principles
1. **Human-in-the-loop** - Every file change and command requires approval (unless auto-approved)
2. **Context-aware** - Intelligent file analysis without overwhelming context
3. **Multi-agent** - Leverage multiple LLMs for diverse perspectives
4. **Git-native** - Deep integration with GitHub/GitLab workflows
5. **User-selectable** - Configurable layouts and behaviors

---

## Target Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        CodeAvenue Desktop V2                                │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         Header Bar                                   │   │
│  │  [Logo] [Project: repo] [Branch: main ▾] [🔄 Sync] [⚙️] [👤]       │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────┬───────────────────────────────────────────────┬───────────┐   │
│  │         │                                               │           │   │
│  │  File   │              Main Content Area                │  Context  │   │
│  │ Explorer│  ┌─────────────────────────────────────────┐  │   Panel   │   │
│  │         │  │  [Terminal Grid] [Diff View] [PR View]  │  │           │   │
│  │ 📁 src  │  │                                         │  │  Files:   │   │
│  │ 📁 docs │  │  ┌─────────┐ ┌─────────┐ ┌─────────┐   │  │  - app.ts │   │
│  │ 📄 pkg  │  │  │ T1:Claude│ │ T2:GPT  │ │ T3:Local│   │  │  - lib.ts │   │
│  │         │  │  └─────────┘ └─────────┘ └─────────┘   │  │           │   │
│  │ Changes │  │                                         │  │  Git:     │   │
│  │ ∙ 3 mod │  │  ───────────────────────────────────   │  │  +15 -8   │   │
│  │ ∙ 1 new │  │  Diff: app.ts                          │  │           │   │
│  │         │  │  - old line                            │  │  Tokens:  │   │
│  │ PRs     │  │  + new line                            │  │  12,450   │   │
│  │ ∙ #123  │  │                                         │  │           │   │
│  │ ∙ #124  │  └─────────────────────────────────────────┘  │           │   │
│  │         │                                               │           │   │
│  └─────────┴───────────────────────────────────────────────┴───────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    Unified Input Area                                │   │
│  │  [📎] [@mentions ▾] [What would you like to do?              ] [➤]  │   │
│  │  ┌──────────────────────────────────────────────────────────────┐   │   │
│  │  │ Quick: [/spawn] [/pr] [/review] [/fix] [/consensus]         │   │   │
│  │  └──────────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  [T: 3] [H: 3/3] [Consensus: 0] [Cost: $0.42] [Git: main +3]       │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Core Feature Modules

### 1. File Explorer Panel

**Purpose:** Navigate project, view changes, manage files

```typescript
interface FileExplorerState {
  rootPath: string;
  expandedFolders: Set<string>;
  selectedFile: string | null;
  viewMode: 'tree' | 'flat' | 'changes';

  // Git integration
  modifiedFiles: FileChange[];
  stagedFiles: FileChange[];
  untrackedFiles: string[];

  // Ignore patterns
  ignorePatterns: string[];
  showIgnored: boolean;
}

interface FileChange {
  path: string;
  status: 'modified' | 'added' | 'deleted' | 'renamed';
  additions: number;
  deletions: number;
  staged: boolean;
}
```

**Features:**
- File tree with expand/collapse
- Git status indicators (M, A, D, R)
- Quick filter/search
- Drag files to terminal for context
- Right-click context menu
- Show/hide ignored files

---

### 2. Diff View System

**Purpose:** Review and edit AI-proposed changes

```typescript
interface DiffViewState {
  mode: 'side-by-side' | 'inline' | 'unified';
  files: DiffFile[];
  activeFile: number;

  // Approval state
  approvals: Map<string, 'approved' | 'rejected' | 'pending'>;
  editedContent: Map<string, string>;
}

interface DiffFile {
  path: string;
  original: string;
  modified: string;
  hunks: DiffHunk[];
  language: string;
}

interface DiffHunk {
  startLine: number;
  endLine: number;
  additions: string[];
  deletions: string[];
  context: string[];
}
```

**Features:**
- Side-by-side diff (default)
- Inline editing in diff view
- Per-hunk approve/reject
- Syntax highlighting
- Line numbers
- Mini-map navigation
- Keyboard shortcuts (j/k navigate, a approve, r reject)

---

### 3. Approval Workflow

**Purpose:** Human-in-the-loop control over AI actions

```typescript
interface ApprovalConfig {
  // Auto-approve patterns
  autoApprovePatterns: AutoApproveRule[];

  // Require approval for
  requireApproval: {
    fileCreate: boolean;
    fileDelete: boolean;
    fileModify: boolean;
    commandExec: boolean;
    gitOperations: boolean;
    networkRequests: boolean;
  };

  // Trust levels per terminal
  terminalTrust: Map<string, TrustLevel>;
}

interface AutoApproveRule {
  id: string;
  name: string;
  enabled: boolean;
  type: 'file_pattern' | 'command_pattern' | 'operation_type';
  pattern: string;
  scope: 'read' | 'write' | 'execute' | 'all';
}

type TrustLevel = 'none' | 'read_only' | 'limited' | 'full';
```

**Approval UI:**
```
┌─────────────────────────────────────────────────────┐
│ 🔔 Approval Required                                │
├─────────────────────────────────────────────────────┤
│ Terminal: T1 (Claude Code)                          │
│ Action: Modify file                                 │
│ Path: src/components/Button.tsx                     │
│                                                     │
│ Changes:                                            │
│ ┌─────────────────────────────────────────────────┐ │
│ │ - export function Button({ label }) {           │ │
│ │ + export function Button({ label, onClick }) {  │ │
│ │     return (                                    │ │
│ │ -     <button>{label}</button>                  │ │
│ │ +     <button onClick={onClick}>{label}</button>│ │
│ │     );                                          │ │
│ │   }                                             │ │
│ └─────────────────────────────────────────────────┘ │
│                                                     │
│ [✅ Approve] [✏️ Edit] [❌ Reject] [⏸️ Pause All]   │
│                                                     │
│ □ Always approve .tsx file edits from this terminal │
└─────────────────────────────────────────────────────┘
```

---

### 4. GitHub/GitLab Integration

**Purpose:** Full git platform integration without leaving app

#### 4.1 Authentication
```typescript
interface GitPlatformAuth {
  provider: 'github' | 'gitlab' | 'bitbucket';
  method: 'oauth' | 'pat' | 'ssh';
  token?: string;
  sshKeyPath?: string;

  // OAuth flow
  clientId?: string;
  scopes: string[];
}
```

#### 4.2 Repository Operations
```typescript
interface GitOperations {
  // Repository
  clone(url: string, path: string): Promise<void>;
  fetch(): Promise<void>;
  pull(): Promise<MergeResult>;
  push(force?: boolean): Promise<void>;

  // Branches
  listBranches(): Promise<Branch[]>;
  createBranch(name: string, from?: string): Promise<void>;
  switchBranch(name: string): Promise<void>;
  deleteBranch(name: string): Promise<void>;

  // Commits
  commit(message: string, files?: string[]): Promise<string>;
  amend(message?: string): Promise<string>;

  // Stash
  stash(message?: string): Promise<void>;
  stashPop(): Promise<void>;
  listStashes(): Promise<Stash[]>;
}
```

#### 4.3 Pull Request Workflow
```typescript
interface PullRequestOperations {
  // List & View
  listPRs(filters?: PRFilter): Promise<PullRequest[]>;
  getPR(number: number): Promise<PullRequestDetail>;

  // Create & Update
  createPR(params: CreatePRParams): Promise<PullRequest>;
  updatePR(number: number, params: UpdatePRParams): Promise<void>;

  // Review
  addReview(number: number, review: PRReview): Promise<void>;
  addComment(number: number, comment: PRComment): Promise<void>;
  requestReviewers(number: number, reviewers: string[]): Promise<void>;

  // Actions
  merge(number: number, method?: MergeMethod): Promise<void>;
  close(number: number): Promise<void>;
  reopen(number: number): Promise<void>;

  // CI
  getChecks(number: number): Promise<Check[]>;
  rerunChecks(number: number): Promise<void>;
}

interface CreatePRParams {
  title: string;
  body: string;
  base: string;
  head: string;
  draft?: boolean;
  reviewers?: string[];
  labels?: string[];
}
```

#### 4.4 Issue Integration
```typescript
interface IssueOperations {
  listIssues(filters?: IssueFilter): Promise<Issue[]>;
  getIssue(number: number): Promise<IssueDetail>;
  createIssue(params: CreateIssueParams): Promise<Issue>;
  updateIssue(number: number, params: UpdateIssueParams): Promise<void>;
  addComment(number: number, body: string): Promise<void>;

  // Branch from issue
  createBranchFromIssue(number: number, branchName?: string): Promise<string>;

  // Link to PR
  linkIssueToPR(issueNumber: number, prNumber: number): Promise<void>;
}
```

#### 4.5 CI/CD Integration
```typescript
interface CICDOperations {
  // Pipelines
  listPipelines(): Promise<Pipeline[]>;
  getPipeline(id: string): Promise<PipelineDetail>;
  triggerPipeline(ref: string, variables?: Record<string, string>): Promise<Pipeline>;
  cancelPipeline(id: string): Promise<void>;
  retryPipeline(id: string): Promise<void>;

  // Jobs
  getJobLogs(jobId: string): Promise<string>;
  retryJob(jobId: string): Promise<void>;
  cancelJob(jobId: string): Promise<void>;
}
```

**PR Review UI:**
```
┌─────────────────────────────────────────────────────────────┐
│ PR #123: Add user authentication                            │
├─────────────────────────────────────────────────────────────┤
│ Branch: feature/auth → main                                 │
│ Author: @developer | Created: 2h ago                        │
│ Status: ✅ Checks passing | 👀 1 review requested           │
├─────────────────────────────────────────────────────────────┤
│ Files Changed (4)                     │ Conversation (3)    │
│ ┌───────────────────────────────────┐ │ ┌─────────────────┐ │
│ │ ✓ src/auth/login.ts       +45 -2 │ │ │ @user1: LGTM!   │ │
│ │ ○ src/auth/signup.ts      +120   │ │ │ @user2: Could...│ │
│ │ ○ src/middleware/auth.ts  +30 -5 │ │ │ @bot: CI passed │ │
│ │ ✓ tests/auth.test.ts      +80    │ │ └─────────────────┘ │
│ └───────────────────────────────────┘ │                     │
├─────────────────────────────────────────────────────────────┤
│ [✅ Approve] [💬 Comment] [🔄 Request Changes] [🔀 Merge ▾] │
└─────────────────────────────────────────────────────────────┘
```

---

### 5. Context Management

**Purpose:** Smart context for AI without overwhelming tokens

```typescript
interface ContextManager {
  // Active context
  files: ContextFile[];
  terminals: ContextTerminal[];
  git: GitContext;

  // Context budget
  maxTokens: number;
  usedTokens: number;

  // Smart inclusion
  autoInclude: {
    openFiles: boolean;
    recentEdits: boolean;
    gitChanges: boolean;
    relatedFiles: boolean;
  };

  // @mentions
  mentions: Mention[];
}

interface ContextFile {
  path: string;
  content: string;
  tokens: number;
  source: 'manual' | 'auto' | 'mention';
  ranges?: { start: number; end: number }[];
}

interface Mention {
  type: 'file' | 'folder' | 'symbol' | 'url' | 'terminal' | 'pr' | 'issue';
  reference: string;
  resolved: boolean;
  content?: string;
}
```

**@Mention Types:**
| Mention | Example | Description |
|---------|---------|-------------|
| @file | @src/app.ts | Include entire file |
| @folder | @src/components/ | Include folder structure |
| @symbol | @UserService | Find and include class/function |
| @url | @https://... | Fetch and include webpage |
| @terminal | @t1 | Include terminal context |
| @pr | @#123 | Include PR details |
| @issue | @#456 | Include issue details |
| @git | @git:diff | Include git changes |

---

### 6. Task History & Restore

**Purpose:** Track all AI interactions and restore state

```typescript
interface TaskHistory {
  tasks: Task[];
  currentTask: string | null;

  // Branching
  branches: Map<string, TaskBranch>;
}

interface Task {
  id: string;
  prompt: string;
  terminals: string[];
  startTime: number;
  endTime?: number;
  status: 'running' | 'completed' | 'failed' | 'cancelled';

  // Changes made
  fileChanges: FileChange[];
  commandsRun: CommandRecord[];

  // Metrics
  tokensUsed: number;
  cost: number;

  // Restore point
  canRestore: boolean;
  snapshotId?: string;
}

interface TaskBranch {
  id: string;
  parentTaskId: string;
  branchPoint: number;
  description: string;
}
```

**Task History UI:**
```
┌─────────────────────────────────────────────────────────┐
│ Task History                                    [⟳] [⋯] │
├─────────────────────────────────────────────────────────┤
│ ● Current: Implementing auth middleware                 │
│   └─ 3 files changed, 2 commands run                    │
│   └─ 5,230 tokens ($0.08)                               │
│                                                         │
│ ✓ 14:32 - Add login component                          │
│   └─ [Restore] [View Changes] [Branch From]             │
│                                                         │
│ ✓ 14:15 - Fix TypeScript errors                        │
│   └─ [Restore] [View Changes] [Branch From]             │
│                                                         │
│ ✗ 13:58 - Attempt database migration (failed)          │
│   └─ [View Error] [Retry]                               │
└─────────────────────────────────────────────────────────┘
```

---

### 7. MCP Server Integration

**Purpose:** Extend capabilities with custom tools

```typescript
interface MCPManager {
  servers: MCPServer[];
  tools: MCPTool[];

  // Server management
  installServer(config: MCPServerConfig): Promise<void>;
  removeServer(id: string): Promise<void>;
  restartServer(id: string): Promise<void>;

  // Tool invocation
  invokeTool(name: string, params: unknown): Promise<unknown>;
}

interface MCPServer {
  id: string;
  name: string;
  status: 'running' | 'stopped' | 'error';
  transport: 'stdio' | 'sse' | 'websocket';
  tools: MCPTool[];
  resources: MCPResource[];
}

interface MCPTool {
  name: string;
  description: string;
  inputSchema: JSONSchema;
  serverId: string;
}
```

---

### 8. Enhanced Terminal Features

**New Terminal Capabilities:**

```typescript
interface EnhancedTerminal extends Terminal {
  // Context sharing
  contextMode: 'isolated' | 'shared' | 'group';
  sharedContextGroup?: string;

  // Auto-approve
  trustLevel: TrustLevel;
  autoApproveRules: string[]; // Rule IDs

  // Task assignment
  assignedTask?: string;
  taskQueue: TaskItem[];

  // Collaboration
  canHandoff: boolean;
  handoffTarget?: string;

  // Output processing
  outputBuffer: OutputChunk[];
  pendingApprovals: PendingApproval[];
}

interface OutputChunk {
  type: 'text' | 'code' | 'diff' | 'command' | 'approval_request';
  content: string;
  timestamp: number;
  metadata?: unknown;
}
```

---

## Rust Backend Extensions

### New Modules

```
src-tauri/src/
├── lib.rs
├── terminal.rs          # Enhanced
├── coordination.rs      # Enhanced
├── health.rs
├── settings.rs
├── agent.rs
├── git/                 # NEW
│   ├── mod.rs
│   ├── operations.rs    # Git commands
│   ├── github.rs        # GitHub API
│   ├── gitlab.rs        # GitLab API
│   └── diff.rs          # Diff generation
├── files/               # NEW
│   ├── mod.rs
│   ├── watcher.rs       # File system watcher
│   ├── explorer.rs      # File tree operations
│   └── search.rs        # File search
├── context/             # NEW
│   ├── mod.rs
│   ├── manager.rs       # Context management
│   ├── mentions.rs      # @mention resolution
│   └── tokens.rs        # Token counting
├── approval/            # NEW
│   ├── mod.rs
│   ├── rules.rs         # Auto-approve rules
│   └── queue.rs         # Approval queue
├── tasks/               # NEW
│   ├── mod.rs
│   ├── history.rs       # Task history
│   └── restore.rs       # State restore
└── mcp/                 # NEW
    ├── mod.rs
    ├── client.rs        # MCP client
    └── registry.rs      # Tool registry
```

### Tauri Commands

```rust
// Git operations
#[tauri::command] async fn git_status() -> Result<GitStatus, String>;
#[tauri::command] async fn git_diff(path: Option<String>) -> Result<Vec<DiffFile>, String>;
#[tauri::command] async fn git_commit(message: String, files: Vec<String>) -> Result<String, String>;
#[tauri::command] async fn git_push(force: bool) -> Result<(), String>;
#[tauri::command] async fn git_pull() -> Result<MergeResult, String>;

// GitHub/GitLab
#[tauri::command] async fn gh_list_prs(filters: PRFilter) -> Result<Vec<PullRequest>, String>;
#[tauri::command] async fn gh_create_pr(params: CreatePRParams) -> Result<PullRequest, String>;
#[tauri::command] async fn gh_merge_pr(number: u32, method: MergeMethod) -> Result<(), String>;
#[tauri::command] async fn gh_add_review(number: u32, review: PRReview) -> Result<(), String>;

// File operations
#[tauri::command] async fn fs_read_dir(path: String) -> Result<Vec<FileEntry>, String>;
#[tauri::command] async fn fs_read_file(path: String) -> Result<String, String>;
#[tauri::command] async fn fs_write_file(path: String, content: String) -> Result<(), String>;
#[tauri::command] async fn fs_watch(paths: Vec<String>) -> Result<(), String>;

// Context
#[tauri::command] async fn ctx_resolve_mention(mention: String) -> Result<ContextFile, String>;
#[tauri::command] async fn ctx_count_tokens(content: String) -> Result<u32, String>;

// Approval
#[tauri::command] async fn approval_queue() -> Result<Vec<PendingApproval>, String>;
#[tauri::command] async fn approval_respond(id: String, action: ApprovalAction) -> Result<(), String>;

// Tasks
#[tauri::command] async fn task_history() -> Result<Vec<Task>, String>;
#[tauri::command] async fn task_restore(id: String) -> Result<(), String>;
```

---

## Frontend Components

### New Components

```
src/
├── components/
│   ├── layout/
│   │   ├── AppLayout.tsx        # Main layout wrapper
│   │   ├── HeaderBar.tsx        # Top header with project info
│   │   ├── SidePanel.tsx        # Collapsible side panels
│   │   └── ResizablePanes.tsx   # Resizable split panes
│   ├── files/
│   │   ├── FileExplorer.tsx     # File tree component
│   │   ├── FileTree.tsx         # Tree rendering
│   │   ├── FileEntry.tsx        # Single file entry
│   │   ├── FileSearch.tsx       # Quick file search
│   │   └── GitStatus.tsx        # Git status indicators
│   ├── diff/
│   │   ├── DiffView.tsx         # Main diff viewer
│   │   ├── DiffSideBySide.tsx   # Side-by-side mode
│   │   ├── DiffInline.tsx       # Inline mode
│   │   ├── DiffHunk.tsx         # Single hunk
│   │   └── DiffEditor.tsx       # Editable diff
│   ├── git/
│   │   ├── BranchSelector.tsx   # Branch dropdown
│   │   ├── PRList.tsx           # Pull request list
│   │   ├── PRDetail.tsx         # PR detail view
│   │   ├── PRReview.tsx         # Review interface
│   │   ├── CommitHistory.tsx    # Commit log
│   │   └── CIStatus.tsx         # CI/CD status
│   ├── context/
│   │   ├── ContextPanel.tsx     # Right-side context
│   │   ├── MentionInput.tsx     # @mention autocomplete
│   │   ├── TokenCounter.tsx     # Token usage display
│   │   └── FileChip.tsx         # Context file chip
│   ├── approval/
│   │   ├── ApprovalModal.tsx    # Approval dialog
│   │   ├── ApprovalQueue.tsx    # Pending approvals
│   │   └── AutoApproveConfig.tsx # Auto-approve settings
│   ├── tasks/
│   │   ├── TaskHistory.tsx      # Task list
│   │   ├── TaskDetail.tsx       # Task details
│   │   └── TaskRestore.tsx      # Restore dialog
│   ├── input/
│   │   ├── UnifiedInput.tsx     # Main input area
│   │   ├── CommandPalette.tsx   # Command palette (⌘P)
│   │   └── QuickActions.tsx     # Quick action buttons
│   └── terminal/                # Enhanced
│       ├── TerminalGrid.tsx
│       ├── TerminalPane.tsx
│       ├── TerminalOutput.tsx   # Output processing
│       └── TerminalApproval.tsx # Inline approvals
```

---

## View Modes

User-selectable layouts:

### 1. Terminals Focus
```
┌───────────────────────────────────────────┐
│ Header                                    │
├─────────┬─────────┬─────────┬─────────────┤
│ T1      │ T2      │ T3      │ Context     │
│         │         │         │             │
│         │         │         │             │
├─────────┴─────────┴─────────┴─────────────┤
│ Input                                     │
└───────────────────────────────────────────┘
```

### 2. IDE Layout
```
┌───────────────────────────────────────────┐
│ Header                                    │
├─────────┬───────────────────┬─────────────┤
│ Files   │ Editor/Diff       │ Context     │
│         ├───────────────────┤             │
│         │ Terminal (bottom) │             │
├─────────┴───────────────────┴─────────────┤
│ Input                                     │
└───────────────────────────────────────────┘
```

### 3. PR Review
```
┌───────────────────────────────────────────┐
│ Header                                    │
├───────────────────────────────────────────┤
│ PR Info                                   │
├───────────┬───────────────────────────────┤
│ Files     │ Diff View                     │
│           │                               │
│           │                               │
├───────────┴───────────────────────────────┤
│ Comments / Review Input                   │
└───────────────────────────────────────────┘
```

### 4. Consensus View
```
┌───────────────────────────────────────────┐
│ Header                                    │
├───────────────────────────────────────────┤
│ Task: "Review authentication approach"    │
├─────────┬─────────┬─────────┬─────────────┤
│ Claude  │ GPT-4   │ Groq    │ Synthesis   │
│         │         │         │             │
│ Response│ Response│ Response│ Combined    │
├─────────┴─────────┴─────────┴─────────────┤
│ [Approve Claude] [Approve GPT] [Synthesize]│
└───────────────────────────────────────────┘
```

---

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| ⌘K | Focus input |
| ⌘P | Command palette |
| ⌘B | Toggle file explorer |
| ⌘J | Toggle terminal panel |
| ⌘\\ | Split terminal |
| ⌘1-9 | Focus terminal N |
| ⌘Enter | Submit input |
| ⌘Shift+Enter | Submit and run (YOLO) |
| Escape | Cancel current |
| ⌘Z | Undo last change |
| ⌘Shift+Z | Redo |
| ⌘S | Save all |
| ⌘Shift+P | Create PR |

---

## Implementation Phases

### Phase 1: Core Infrastructure (2 weeks)
| Task | Effort |
|------|--------|
| Resizable pane layout | 2d |
| File explorer component | 3d |
| File watcher (Rust) | 2d |
| Git status integration | 2d |
| Layout persistence | 1d |

### Phase 2: Diff & Approval (2 weeks)
| Task | Effort |
|------|--------|
| Diff view component | 3d |
| Diff generation (Rust) | 2d |
| Approval workflow | 2d |
| Auto-approve rules | 2d |
| Inline editing | 1d |

### Phase 3: GitHub/GitLab (2 weeks)
| Task | Effort |
|------|--------|
| Git operations (Rust) | 2d |
| GitHub API client | 2d |
| GitLab API client | 2d |
| PR view component | 2d |
| CI status integration | 2d |

### Phase 4: Context & Mentions (1 week)
| Task | Effort |
|------|--------|
| Context manager | 2d |
| @mention autocomplete | 2d |
| Token counting | 1d |

### Phase 5: Task History (1 week)
| Task | Effort |
|------|--------|
| Task tracking | 2d |
| History UI | 2d |
| State restore | 1d |

### Phase 6: Polish (1 week)
| Task | Effort |
|------|--------|
| Keyboard shortcuts | 1d |
| Command palette | 2d |
| Settings UI | 2d |

**Total:** ~9 weeks

---

## Success Metrics

| Metric | Target |
|--------|--------|
| Time to first action | < 2s |
| Approval response time | < 500ms |
| File tree load time | < 1s (10k files) |
| Diff generation | < 200ms |
| Context token accuracy | > 95% |
| PR creation time | < 5s |

---
Author: Manoj Jhawar | v1 | 260122
