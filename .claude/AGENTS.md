# AI Agent Instructions for {PROJECT_NAME}

**Framework:** IDEACODE v6.0
**Profile:** {PROFILE}
**Framework Location:** `/Volumes/M-Drive/Coding/ideacode`
**Last Updated:** {UPDATED_DATE}

---

## 🚀 Quick Start

When starting a new session in this project:

### 1. Load Project Context

```bash
# Check project-specific instructions
cat .ideacode/project-instructions.md

# Load project configuration
cat .ideacode-v2/config.yml
```

### 2. Verify IDEACODE Tools

Check which tools are available to you:

**Option A: MCP Tools (Autonomous)**
- If you have MCP support, you can use `ideacode_*` tools
- These are autonomous - you describe what you want, the tool does ALL the work

**Option B: Slash Commands (Manual)**
- If no MCP support, use `/ideacode.*` slash commands
- These provide step-by-step instructions you follow manually

### 3. Choose Your Workflow

**Autonomous (Recommended if MCP available):**
```
Use ideacode_specify to create a spec for "your feature"
Use ideacode_plan with spec_file "features/001-feature/spec.md"
Use ideacode_implement with plan_file "features/001-feature/plan.md"
```

**Manual (If MCP not available):**
```
/ideacode.specify
/ideacode.plan
/ideacode.implement
```

---

## 🛠️ Available Tools

### MCP Tools (19 Autonomous Tools)

If you have access to MCP tools, use these for autonomous execution:

#### Workflow Tools
- `ideacode_specify` - Create detailed feature specification with delta format
- `ideacode_validate` - Validate specification format and structure
- `ideacode_plan` - Generate implementation plan from specification
- `ideacode_implement` - Execute implementation following IDE Loop
- `ideacode_test` - Run tests and analyze failures
- `ideacode_commit` - Create properly formatted git commit
- `ideacode_archive` - Archive completed feature and merge to living specs
- `ideacode_list` - List active features, living specs, and archive

#### Analysis Tools
- `ideacode_think` - Extended thinking for complex problems
- `ideacode_issue` - Create comprehensive issue analysis using ToT/CoT

#### Vision Tools
- `ideacode_analyze_ui` - Analyze UI screenshots with Claude Vision
- `ideacode_from_mockup` - Generate code from design mockups
- `ideacode_debug_screenshot` - Debug errors from screenshots

#### Context Tools
- `ideacode_context_show` - Show current context usage
- `ideacode_context_save` - Save context to file
- `ideacode_context_reset` - Reset context window (adaptive: 90% for 1M, 75% for 200K)

#### Project Tools
- `ideacode_new_project` - Interactive new project setup
- `ideacode_research` - Web research on technologies/APIs
- `ideacode_port_module` - Port modules between projects with IPC integration

### Slash Commands (29 Manual Commands)

If MCP tools not available, use slash commands:

#### Workflow
- `/ideacode.specify` - Create specification (manual workflow)
- `/ideacode.plan` - Create implementation plan
- `/ideacode.implement` - Execute implementation
- `/ideacode.tasks` - Generate task breakdown

#### Thinking/Analysis
- `/ideacode.think` - Extended thinking mode
- `/ideacode.tot` - Tree of Thought reasoning
- `/ideacode.cot` - Chain of Thought analysis
- `/ideacode.rot` - Reverse of Thought debugging
- `/ideacode.tcr` - Test-Commit-Revert workflow
- `/ideacode.analyze` - Cross-artifact consistency check
- `/ideacode.issue` - Create issue analysis document

#### Context Management
- `/ideacode.contextshow` - Show context usage
- `/ideacode.contextsave` - Save context
- `/ideacode.contextreset` - Reset context
- `/ideacode.showprogress` - Show current progress

#### **NEW v6.0 Commands**
- `/ideacode.validate` - Validate spec format
- `/ideacode.archive` - Archive completed feature

---

## 📋 IDEACODE v6.0 Workflow

### Phase 1: Propose

**Create a feature proposal with delta specifications:**

```bash
# Autonomous (MCP)
Use ideacode_specify to create a spec for "add two-factor authentication"

# Manual (Slash Command)
/ideacode.specify
```

**Generates:**
```
.ideacode-v2/features/001-add-2fa/
├── proposal.md          # Rationale and overview
└── spec.md              # Delta format (ADDED/MODIFIED/REMOVED requirements)
```

---

### Phase 2: Validate

**Validate specification format:**

```bash
# Autonomous (MCP)
Use ideacode_validate with spec_file "features/001-add-2fa/spec.md"

# Manual (Slash Command)
/ideacode.validate
```

**Checks:**
- ✅ Every requirement has ≥1 scenario
- ✅ Delta format correct (ADDED/MODIFIED/REMOVED)
- ✅ Scenarios use GIVEN/WHEN/THEN structure
- ✅ Requirements use SHALL/MUST language

---

### Phase 3: Plan

**Generate implementation plan:**

```bash
# Autonomous (MCP)
Use ideacode_plan with spec_file "features/001-add-2fa/spec.md"

# Manual (Slash Command)
/ideacode.plan
```

**Generates:**
```
.ideacode-v2/features/001-add-2fa/
├── proposal.md
├── spec.md
├── plan.md              # NEW: Implementation plan
└── design.md            # Optional: Technical decisions
```

---

### Phase 4: Implement

**Execute implementation following IDE Loop:**

```bash
# Autonomous (MCP)
Use ideacode_implement with plan_file "features/001-add-2fa/plan.md"

# Manual (Slash Command)
/ideacode.implement
```

**IDE Loop (MANDATORY for each task):**
1. **Implement** - Build the feature
2. **Defend** - Create tests (automatic)
3. **Evaluate** - Verify requirements
4. **Commit** - Lock in progress

---

### Phase 5: Archive

**After deployment, archive the feature:**

```bash
# Autonomous (MCP)
Use ideacode_archive with feature_dir ".ideacode-v2/features/001-add-2fa"

# Manual (Slash Command)
/ideacode.archive
```

**Process:**
1. ✅ Reads delta spec from feature
2. 📝 Applies ADDED/MODIFIED/REMOVED to living spec
3. 📊 Increments spec version and updates history
4. 📁 Moves feature to archive/YYYY-MM/

**Result:**
```
.ideacode-v2/
├── specs/
│   └── auth/
│       └── spec.md          # Updated with 2FA requirements (v1.2.0)
├── features/
│   └── 002-stripe/          # Only active features
└── archive/
    └── 2025-11/
        └── 001-add-2fa/     # Complete history preserved
```

---

### Phase 6: Monitor Progress

**Check project status anytime:**

```bash
# Autonomous (MCP)
Use ideacode_list with type "all"

# Manual (Slash Command)
/ideacode.list
```

**Shows:**
- 📝 Active features (proposed/in-progress/ready)
- 📚 Living specs (version, requirement count)
- 📁 Archived features (grouped by month)
- 📊 Project summary

---

## 🚨 Zero Tolerance Rules (CRITICAL)

**BEFORE any code or file changes, check these 5 rules:**

### 1. Will I delete anything?
- **If YES** → STOP → Get explicit user approval first
- **If NO** → Continue

### 2. Am I using the right tool?
- Use `ideacode_*` MCP tools (autonomous) when available
- Or use `/ideacode.*` slash commands (manual)
- Don't reinvent workflows

### 3. Is context safe?
- Before major changes: Save context
- At 75% context (200K model): Alert user
- At 90% context (1M model): MANDATORY save + reset

### 4. File naming correct?
- **Living documents:** NO timestamp (README.md, spec.md, config.yml)
- **Static documents:** WITH timestamp (CONTEXT-2511060000.md)

### 5. Git staging safe?
- ONLY stage files YOU created/modified
- NEVER use `git add .` or `git add -A`
- Use explicit paths: `git add path/to/file.ts`

---

## 📂 Directory Structure (v6.0)

```
.ideacode-v2/
├── config.yml                  # Project configuration
│
├── specs/                      # Living specifications (source of truth)
│   ├── auth/
│   │   └── spec.md            # Current auth capabilities
│   ├── payments/
│   │   └── spec.md            # Current payment capabilities
│   └── notifications/
│       └── spec.md
│
├── features/                   # Proposed changes (active development)
│   ├── 001-add-2fa/
│   │   ├── proposal.md        # Rationale and overview
│   │   ├── spec.md            # Delta (ADDED/MODIFIED/REMOVED)
│   │   ├── plan.md            # Implementation plan
│   │   ├── tasks.md           # Task breakdown
│   │   └── design.md          # Optional technical decisions
│   │
│   └── 002-stripe-integration/
│       └── ... (same structure)
│
└── archive/                    # Completed features
    └── 001-add-2fa/
        └── ... (complete history)
```

---

## 🎯 Profile-Specific Guidelines: {PROFILE}

{PROFILE_GUIDELINES}

---

## 📝 Specification Format (v6.0)

### Living Spec Format

```markdown
### Requirement: User Authentication
The system SHALL issue a JWT on successful login.

**Rationale:** Secure, stateless authentication.

#### Scenario: Valid credentials
**GIVEN** a user with valid credentials
**WHEN** the user attempts to login
**THEN** a JWT is issued
```

### Delta Format

```markdown
## ADDED Requirements
### Requirement: Two-Factor Authentication
The system SHALL support TOTP-based 2FA.

#### Scenario: Enable 2FA
**GIVEN** a logged-in user
**WHEN** the user enables 2FA
**THEN** a QR code is displayed

## MODIFIED Requirements
### Requirement: User Authentication
The system SHALL issue JWT after login and 2FA verification (if enabled).
[Complete updated requirement text]

## REMOVED Requirements
### Requirement: Cookie-Based Sessions
[Deprecated - migration path provided]
```

---

## ✅ Quality Gates (Profile: {PROFILE})

{QUALITY_GATES}

---

## 🔧 Common Tasks

### Start New Feature
```
Use ideacode_specify to create a spec for "{feature description}"
```

### Validate Specification
```
Use ideacode_validate with spec_file "features/{id}/spec.md" and strict false
```

### Continue Existing Feature
```
Use ideacode_implement with plan_file "features/{id}/plan.md"
```

### Check Project Status
```
Use ideacode_list with type "all"
```

### Archive Completed Feature
```
Use ideacode_archive with feature_dir ".ideacode-v2/features/{id}"
```

### Save Context Before Major Change
```
Use ideacode_context_save
```

---

## 🆘 Troubleshooting

### MCP Tools Not Available

**Problem:** Cannot use `ideacode_*` tools

**Solution:** Use slash commands instead (`/ideacode.*`)

### Specification Validation Fails

**Problem:** `ideacode_validate` reports errors

**Common Issues:**
- Missing scenarios (every requirement needs ≥1)
- Wrong scenario headers (use `####`)
- Missing GIVEN/WHEN/THEN structure
- Requirements don't use SHALL/MUST

### Context Window Full

**Problem:** Approaching context limits

**Solution:**
```
Use ideacode_context_save
[Then ask user to reset context]
```

---

## 📚 Documentation

- **Framework Location:** `/Volumes/M-Drive/Coding/ideacode`
- **Protocols:** `/Volumes/M-Drive/Coding/ideacode/protocols/`
- **Templates:** `/Volumes/M-Drive/Coding/ideacode/templates/v6/`
- **Project Instructions:** `.ideacode/project-instructions.md`

---

## 🎓 Learning Resources

### Understanding IDEACODE Workflow
1. Read: `/Volumes/M-Drive/Coding/ideacode/claude/CLAUDE.md`
2. Review: Templates in `templates/v6/`
3. Study: Example projects in archive/

### Profile-Specific Patterns
- android-app: Android best practices, Jetpack Compose
- backend-api: API design, Spring Boot patterns
- frontend-web: React/Vue patterns, accessibility
- library: API design, versioning, multiplatform

---

**Framework Version:** 6.0.0
**Template Version:** 6.0.0
**Last Updated:** {UPDATED_DATE}

---

## 🎯 Remember

1. **Always start** by loading project instructions
2. **Use MCP tools** when available (autonomous)
3. **Follow Zero Tolerance rules** before ANY changes
4. **Validate specs** before implementation
5. **Archive features** when complete
6. **Save context** at 75%/90% thresholds

**Questions?** Ask the user or check `/Volumes/M-Drive/Coding/ideacode/claude/CLAUDE.md`

🚀 **Happy coding with IDEACODE v6.0!** 🚀
