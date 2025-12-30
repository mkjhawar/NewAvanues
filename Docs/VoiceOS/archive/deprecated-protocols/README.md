# Deprecated Protocol Files Archive

**Archive Created:** 2025-10-15 03:23 PDT
**Archived By:** Documentation Consolidation Agent
**Reason:** Protocol consolidation into comprehensive VOS4 protocol files

## Overview

This directory contains superseded protocol files that have been consolidated into new comprehensive protocol files. These files are kept for historical reference only and should NOT be used for new development.

## Archived Files

### Coding Group (2 files)
Superseded by: `/Volumes/M Drive/Coding/Warp/vos4/Docs/ProjectInstructions/Protocol-VOS4-Coding-Standards.md`

1. **CODING-GUIDE.md**
   - Original Purpose: Detailed coding patterns and examples
   - Last Modified: 2025-08-27
   - Size: 16KB
   - Archived: 2025-10-15 03:21 PDT

2. **CODING-STANDARDS.md**
   - Original Purpose: VOS4 coding standards and universal instructions
   - Last Modified: 2025-09-03
   - Size: 23KB
   - Archived: 2025-10-15 03:23 PDT

### Documentation Group (3 files)
Superseded by: `/Volumes/M Drive/Coding/Warp/vos4/Docs/ProjectInstructions/Protocol-VOS4-Documentation.md`

3. **DOCUMENTATION-GUIDE.md**
   - Original Purpose: Documentation standards and living document requirements
   - Last Modified: 2025-02-07
   - Size: 25KB
   - Archived: 2025-10-15 03:57 PDT

4. **DOCUMENT-STANDARDS.md**
   - Original Purpose: AI documentation creation standards
   - Last Modified: 2025-01-21
   - Size: 15KB
   - Archived: 2025-10-15 03:57 PDT

5. **DOCUMENTATION-CHECKLIST.md**
   - Original Purpose: Mandatory checklist for documentation updates before commits
   - Last Modified: 2025-01-27
   - Size: 7KB
   - Archived: 2025-10-15 03:57 PDT

### Agent Group (2 files)
Superseded by: `/Volumes/M Drive/Coding/Warp/vos4/Docs/ProjectInstructions/Protocol-VOS4-Agent-Deployment.md`

6. **AGENTIC-AGENT-INSTRUCTIONS.md**
   - Original Purpose: Mandatory instructions for agentic agents using Task tool
   - Last Modified: 2025-01-25
   - Size: 8.6KB
   - Archived: 2025-10-15 03:57 PDT

7. **MULTI-AGENT-REQUIREMENTS.md**
   - Original Purpose: Mandatory requirements for using specialized PhD-level agents
   - Last Modified: 2025-01-25
   - Size: 9.1KB
   - Archived: 2025-10-15 03:57 PDT

## Replacement Files

All archived files have been consolidated into three comprehensive protocol files:

### 1. Protocol-VOS4-Coding-Standards.md
**Location:** `/Volumes/M Drive/Coding/Warp/vos4/Docs/ProjectInstructions/Protocol-VOS4-Coding-Standards.md`

**Consolidates:**
- CODING-GUIDE.md
- CODING-STANDARDS.md

**Coverage:**
- COT/ROT/TOT analysis requirements
- Functional equivalency rules
- Duplicate code prevention
- Configuration access patterns
- Module structure patterns
- Database implementation (Room + Hybrid)
- Coroutine patterns
- Error handling
- Performance optimization
- Testing patterns

### 2. Protocol-VOS4-Documentation.md
**Location:** `/Volumes/M Drive/Coding/Warp/vos4/Docs/ProjectInstructions/Protocol-VOS4-Documentation.md`

**Consolidates:**
- DOCUMENTATION-GUIDE.md
- DOCUMENT-STANDARDS.md
- DOCUMENTATION-CHECKLIST.md

**Coverage:**
- Documentation naming conventions
- File structure rules
- Living document requirements
- Changelog formats
- Master inventory requirements
- Architecture map requirements
- Visual documentation requirements
- Pre-commit documentation checklist

### 3. Protocol-VOS4-Agent-Deployment.md
**Location:** `/Volumes/M Drive/Coding/Warp/vos4/Docs/ProjectInstructions/Protocol-VOS4-Agent-Deployment.md`

**Consolidates:**
- AGENTIC-AGENT-INSTRUCTIONS.md
- MULTI-AGENT-REQUIREMENTS.md

**Coverage:**
- When to use agentic agents
- Agent types and specializations
- Multi-agent collaboration protocol
- Task assessment and deployment
- Language-specific requirements
- Quality assurance requirements

## Git History Preservation

The original files remain in the git history and can be accessed via:

```bash
# View file history
git log --follow -- Agent-Instructions/CODING-GUIDE.md
git log --follow -- Agent-Instructions/DOCUMENTATION-GUIDE.md
git log --follow -- Agent-Instructions/AGENTIC-AGENT-INSTRUCTIONS.md

# View specific version
git show <commit-hash>:Agent-Instructions/CODING-GUIDE.md
```

## Migration Notes

### For Developers:
- Update bookmarks to point to new protocol files
- Reference consolidated protocols in documentation
- Original files available here for historical reference only

### For AI Agents:
- DO NOT reference deprecated files in this archive
- Use consolidated Protocol-VOS4-*.md files only
- Check `/Volumes/M Drive/Coding/Warp/vos4/Docs/ProjectInstructions/` for current protocols

## Archive Integrity

All archived files include:
- Deprecation notice at the top
- Link to replacement protocol
- Original content preserved in full
- Original metadata and headers intact

## Questions or Issues

If you need to reference historical protocol versions:
1. Check this archive directory first
2. Use git history for detailed change tracking
3. Consult CLAUDE.md for current protocol references

---

**Last Updated:** 2025-10-15 03:23 PDT
**Maintained By:** VOS4 Documentation Team
