<!--
filename: README-INSTRUCTIONS.md
created: 2025-01-23 18:54:00 PST
author: VOS4 Development Team
purpose: Maintenance guide for Agent Instructions system
last-modified: 2025-01-23
version: 1.0.0
-->

# Agent Instructions System - Maintenance Guide

## Overview
This document explains the Agent Instructions system architecture and how to maintain it.

## System Architecture

### **Three Entry Points (Minimal Pointers)**
All three files below are kept IDENTICAL and minimal:
- `/claude.md` - Primary agent entry point
- `/.warp.md` - Alternative agent entry point  
- `/.clinerules` - Cline-specific entry point

**Purpose:** These files act as routers, pointing agents to the actual instructions based on their current task.

### **Four Instruction Files (Actual Content)**
Located in `/Agent-Instructions/`:

1. **MASTER-STANDARDS.md**
   - Core rules ALL agents must follow
   - Critical principles (direct implementation, com.ai.*, ObjectBox)
   - Performance requirements
   - Common pitfalls to avoid

2. **CODING-GUIDE.md**
   - Detailed code examples
   - Patterns that work/don't work
   - File headers and formatting
   - Testing commands

3. **DOCUMENTATION-GUIDE.md**
   - How to write and update documentation
   - Living document requirements
   - Changelog formats
   - Naming conventions

4. **SESSION-LEARNINGS.md**
   - Recent fixes and solutions
   - Current development status
   - Migration progress
   - Gotchas discovered during development

## How This System Works

```
Agent reads claude.md → Sees task index → Goes to relevant instruction file
                     ↓
         "I need to write code"
                     ↓
    Reads MASTER-STANDARDS.md + CODING-GUIDE.md
```

## Maintenance Instructions

### **When to Update Each File:**

#### **Update MASTER-STANDARDS.md when:**
- Adding/changing core architectural principles
- Modifying critical rules (namespace, database, etc.)
- Adding performance requirements
- Discovering critical pitfalls all agents must avoid

#### **Update CODING-GUIDE.md when:**
- Adding new code patterns or examples
- Discovering better implementation approaches
- Adding testing procedures
- Updating build commands

#### **Update DOCUMENTATION-GUIDE.md when:**
- Changing documentation structure
- Adding new document types
- Modifying changelog requirements
- Updating naming conventions

#### **Update SESSION-LEARNINGS.md when:**
- Completing significant fixes
- Solving complex problems
- Making architectural decisions
- Recording why something was done a certain way

### **When to Update the Three Pointer Files:**
**Only update claude.md, .warp.md, .clinerules when:**
- Adding a new instruction file to Agent-Instructions
- Changing the project location or branch
- Major structural changes to the instruction system

**Important:** Keep all three pointer files IDENTICAL. When updating one, update all three.

## Living Document Requirements

### **For All Instruction Files:**

Each instruction file should include:
```markdown
<!-- Changelog - Most recent first -->
<!-- 
- 2025-01-24: Added new pattern for X - improves performance
- 2025-01-23: Initial creation - consolidated from three files
-->
```

### **Before Committing Changes:**
1. Update relevant instruction files
2. Ensure pointer files are still synchronized if changed
3. Update this README if structure changes

## File Structure Reasoning

### **Why Separate Files?**
- **Single Responsibility:** Each file has one clear purpose
- **Easier Maintenance:** Update only what changed
- **Better Discovery:** Agents find relevant info faster
- **Version Control:** See what changed in specific areas

### **Why Minimal Pointers?**
- **DRY Principle:** No duplication of instructions
- **Single Source of Truth:** Instructions live in one place
- **Easy Sync:** Three small files easy to keep identical
- **Clear Navigation:** Index shows what goes where

## Quick Reference for Updates

### **Adding a New Coding Rule:**
1. Open `/Agent-Instructions/CODING-GUIDE.md`
2. Add rule with example
3. Update changelog at top of file
4. Commit with message: "docs: Add [rule] to coding guide"

### **Recording a Problem Solution:**
1. Open `/Agent-Instructions/SESSION-LEARNINGS.md`
2. Add entry with date, problem, solution
3. Include code snippets if relevant
4. Commit with message: "docs: Document [problem] solution"

### **Changing Core Architecture:**
1. Open `/Agent-Instructions/MASTER-STANDARDS.md`
2. Update principle with clear explanation
3. Update changelog
4. Also update pointer files if major change
5. Commit with message: "docs: Update [principle] in master standards"

## Testing the System

### **Verify Instructions Work:**
```bash
# Check an agent can find instructions
cat claude.md  # Should see clear index

# Verify instruction files exist
ls Agent-Instructions/  # Should see 4 .md files + this README

# Check for broken references
grep -r "ProjectDocs/AI-Instructions" .  # Should be none (old path)
grep -r "Agent-Instructions" .  # Should see references
```

## Migration Notes

### **What We Migrated From:**
- Previously: Instructions scattered across claude.md, .warp.md, .clinerules
- Each file ~200+ lines with duplication
- Hard to maintain consistency

### **What We Migrated To:**
- Minimal pointer files (~30 lines each)
- Organized instruction files by purpose
- Single source of truth for each concern
- Living documents with changelogs

### **Why This Change:**
- Eliminates duplication
- Easier to maintain
- Clearer for agents
- Preserves all learnings
- Follows software best practices (SRP, DRY)

---

**Created:** 2025-01-23
**Purpose:** Ensure the Agent Instructions system is properly maintained
**Note:** This is a living document - update it when the system changes