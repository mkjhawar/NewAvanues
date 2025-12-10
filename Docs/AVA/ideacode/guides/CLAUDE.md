# Project AI Instructions

**Project:** ava
**Framework:** IDEACODE v5.3
**Last Updated:** 2511020515

---

## 🚀 SESSION START - MANDATORY ACKNOWLEDGMENT

**BEFORE doing ANYTHING, you MUST respond with:**

```
IDEACODE v5.3 Framework Acknowledged for ava

I will execute the following steps:
✅ Step 1: Read REGISTRY.md
✅ Step 2: Check docs/context/ for previous work
✅ Step 3: Run /contextsave before any code
✅ Step 4: Check for duplicate features across all projects
✅ Step 5: Create TodoWrite task plan

Zero-Tolerance Protocol (20 rules) - ACTIVE
Context Management Protocol V3 (Rule 15) - ACTIVE
Hierarchical Registry System (Rule 20) - ACTIVE

Ready to proceed.
```

---

## 📋 MANDATORY SESSION START CHECKLIST

Execute these steps IN ORDER at every session start:

### Step 1: Read REGISTRY.md
```bash
# Read project REGISTRY.md
cat REGISTRY.md

# Check module registries
ls -la docs/*/REGISTRY.md 2>/dev/null
```

**Why:** See what features already exist, prevent duplicate work

### Step 2: Check Previous Context
```bash
# Read recent context saves
ls -t docs/context/CONTEXT-*.md 2>/dev/null | head -3
cat docs/context/CONTEXT-*.md | head -100  # Read latest
```

**Why:** See what was done in previous sessions, resume work correctly

### Step 3: Context Save (Before Code)
```bash
# MANDATORY before ANY code changes
/contextsave
```

**Why:** Rule 15 - Context Management Protocol V3 (37% token savings)

### Step 4: Cross-Project Duplicate Check
```bash
# Check ALL related project registries
cat "/Volumes/M Drive/Coding/AVAConnect/REGISTRY.md" | grep -i "{keyword}"
cat "/Volumes/M Drive/Coding/voiceavanue/REGISTRY.md" | grep -i "{keyword}"
cat "/Volumes/M Drive/Coding/Warp/vos4/REGISTRY.md" | grep -i "{keyword}"
cat "/Users/manoj_mbpm14/Coding/ava/REGISTRY.md" | grep -i "{keyword}"
cat "/Volumes/M Drive/Coding/avanue4/REGISTRY.md" | grep -i "{keyword}"
cat "/Volumes/M Drive/Coding/ideacode/REGISTRY.md" | grep -i "{keyword}"
```

**Why:** Rule 20 - Prevent implementing features that exist elsewhere

### Step 5: Create Task Plan
```
Use TodoWrite tool to create task list for current work
```

**Why:** Track progress, organize work, show user what's being done

---

## 🚨 ZERO-TOLERANCE RULES (20 Rules)

**MANDATORY - Read before ANY code changes:**

### Original 5 Rules:
1. NO deletions without approval
2. NO AI references in commits (NEVER "Generated with Claude Code")
3. 100% functional equivalency for refactors
4. Documentation BEFORE code
5. Follow directory structure

### Rules 6-18:
6. NO duplicated responses (summarize, don't repeat)
7. Complete file reading (read ALL, no limits)
8. Folder context adherence (check platform-mapping.yml)
9. Strict file placement (ask in interactive, trust in YOLO)
10. Web research when uncertain (don't guess)
11. Phase-based git (docs→code→tests commits)
12. Context save after phase (mandatory checkpoint)
13. COT + ROT before coding (analyze then validate)
14. Living docs updated (SPEC/PLAN/TODO/STATUS)
15. **Context Management (V3)** - MANDATORY `/contextsave` BEFORE code, every 50k tokens
16. Stage only AI files (explicit paths, no `git add .`)
17. Auto-create planning files (if missing)
18. Collaborative discovery (present options when unclear)

### NEW v5.3 (Rules 19-20):
19. **Module Constitution required** - CONSTITUTION.md for permanent requirements
20. **Hierarchical Registry** - REGISTRY.md at project & module levels, check ALL before coding

**Full Protocol:** `/Volumes/M Drive/Coding/ideacode/protocols/Protocol-Zero-Tolerance-Pre-Code.md`

---

## 🔄 PRE-CODE CHECKLIST

**Complete this checklist BEFORE writing any code:**

- [ ] Read REGISTRY.md
- [ ] Read docs/context/ (latest context save)
- [ ] Read docs/{module}/REGISTRY.md for affected modules
- [ ] Check CONSTITUTION.md for core requirements
- [ ] Run `/contextsave`
- [ ] Check ALL project registries for duplicates
- [ ] Create TodoWrite task list
- [ ] Verify no Zero-Tolerance violations

---

## 💾 CONTEXT MANAGEMENT (Rule 15)

**MANDATORY context saves:**

### When to Save:
- **BEFORE any code changes** (CRITICAL)
- Every 50,000 tokens
- After each phase completion
- Before ending session
- After major feature completion

### How to Save:
```
/contextsave
```

This creates: `docs/context/CONTEXT-{timestamp}.md`

### Format (V3 Compressed):
```markdown
# CONTEXT SAVE

**Timestamp:** {YYMMDDHHMM}
**Token Count:** {tokens}
**Project:** ava
**Task:** {what_we're_doing}

## Summary
1-3 sentence summary

## Recent Changes
- File: path/to/file (what changed)

## Next Steps
1. Next task
2. Following task

## Open Questions
- Any blockers?
```

**Why:** 37% token reduction, prevents duplicate work, enables recovery

---

## 📦 HIERARCHICAL REGISTRY (Rule 20)

**Three-level documentation system:**

### Level 1: Master REGISTRY.md (Project Root)
- Complete project inventory
- All modules listed
- Links to other project registries
- Last scan timestamp

### Level 2: Module REGISTRY.md (docs/{module}/)
- Module-specific details
- Classes, functions, APIs
- Dependencies
- Platform support

### Level 3: CONSTITUTION.md (docs/{module}/)
- Permanent requirements
- Core invariants
- Critical features that must never change

**Commands:**
- `/scan-project` - Auto-discover and create registries
- `/init-constitution {module}` - Create Constitution for module
- `/check-registry {name}` - Search all registries for feature

---

## 📚 IDEACODE REFERENCE

**Framework Location:** `/Volumes/M Drive/Coding/ideacode/`

**Key Files:**
- `protocols/Protocol-Zero-Tolerance-Pre-Code.md` - All 20 rules detailed
- `protocols/Protocol-Context-Management-V3.md` - Context save requirements
- `IDEACODE-FEATURES-FUNCTIONS-{timestamp}.md` - All 28 commands, 21 protocols
- `AI-COMPLETE-v5.3-UPGRADE.md` - Complete automation guide
- `REGISTRY.md` - Framework inventory

**Available Commands:**
- `/scan-project` - Auto-discover modules
- `/contextsave` - Save conversation state
- `/check-registry {name}` - Search registries
- `/init-constitution {module}` - Create Constitution
- `/ideacode.init` - Initialize framework

**Full list:** Read `IDEACODE-FEATURES-FUNCTIONS-{timestamp}.md`

---

## 🎯 PROJECT-SPECIFIC INSTRUCTIONS

{INSERT_PROJECT_SPECIFIC_INSTRUCTIONS_HERE}

### Project Structure:
```
{INSERT_PROJECT_STRUCTURE}
```

### Key Modules:
- {MODULE_1}: {Description}
- {MODULE_2}: {Description}
- {MODULE_3}: {Description}

### Related Projects:
- **IDEACODE:** `/Volumes/M Drive/Coding/ideacode/` - Framework
- **AVAConnect:** `/Volumes/M Drive/Coding/AVAConnect/` - Connectivity library
- **VoiceAvanue:** `/Volumes/M Drive/Coding/voiceavanue/` - Multi-platform ecosystem
- **VOS4:** `/Volumes/M Drive/Coding/Warp/vos4/` - Voice OS
- **ava:** `/Users/manoj_mbpm14/Coding/ava/` - AI application
- **avanue4:** `/Volumes/M Drive/Coding/avanue4/` - Avenue project

---

## ✅ SUCCESS CRITERIA

**You're following IDEACODE correctly when:**

1. ✅ You acknowledge framework at session start
2. ✅ You read REGISTRY.md before every task
3. ✅ You check docs/context/ for previous work
4. ✅ You run `/contextsave` before coding
5. ✅ You check all project registries for duplicates
6. ✅ You create TodoWrite task lists
7. ✅ You follow all 20 Zero-Tolerance rules
8. ✅ You update REGISTRY.md when adding features

---

## 🚫 WHAT NOT TO DO

**NEVER:**
- ❌ Start coding without reading REGISTRY.md
- ❌ Skip `/contextsave` command
- ❌ Modify CONSTITUTION.md without user approval
- ❌ Create features that already exist in registries
- ❌ Ignore docs/context/ directory
- ❌ Skip Zero-Tolerance checklist
- ❌ Use incorrect timestamp format (use YYMMDDHHMM)
- ❌ Stage unrelated files (`git add .`)

---

## 📊 ENFORCEMENT

**If AI violates protocol:**

```
STOP. Read this CLAUDE.md file completely and follow ALL steps.

Show me:
1. Your framework acknowledgment
2. Which REGISTRY files you've read
3. Your /contextsave output
4. Your TodoWrite task list
5. Your pre-code checklist completion

Only then can you proceed.
```

---

**Version:** 5.3
**Framework:** IDEACODE
**Type:** Project AI Instructions
**Severity:** MANDATORY - Read at every session start

