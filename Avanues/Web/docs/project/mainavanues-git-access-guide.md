# Git Access Guide - MainAvanues Monorepo

**Repository:** MainAvanues Monorepo
**Git Location:** `/Volumes/M-Drive/Coding/MainAvanues/.git`
**Remote:** GitLab - https://gitlab.com/AugmentalisES/mainavanues.git
**Current Branch:** WebAvanue-Develop

---

## 📍 Repository Structure

### Monorepo Architecture

MainAvanues uses a **single git repository (monorepo)** for all modules:

```
/Volumes/M-Drive/Coding/MainAvanues/
├── .git/                           # Single git repository for entire monorepo
├── android/
│   └── apps/
│       └── webavanue/             # WebAvanue Android app
├── common/
│   └── libs/
│       └── webavanue/             # WebAvanue shared code
│           ├── universal/         # 95% shared KMP code
│           └── coredata/          # Data layer
├── Modules/
│   └── WebAvanue/                 # Original module (to be archived)
└── docs/
    └── develop/
        └── webavanue/             # WebAvanue documentation

```

**Key Point:** There is **ONE git repository** at the root (`/Volumes/M-Drive/Coding/MainAvanues/.git`) that tracks all modules.

### No Separate Module Git

WebAvanue **does not have its own separate git repository**. It uses the parent monorepo's git.

```bash
# This returns the monorepo .git directory
cd /Volumes/M-Drive/Coding/MainAvanues/android/apps/webavanue
git rev-parse --git-dir
# Output: /Volumes/M-Drive/Coding/MainAvanues/.git
```

---

## 🔍 Accessing WebAvanue Git History

### From Command Line

**1. Navigate to monorepo root:**
```bash
cd /Volumes/M-Drive/Coding/MainAvanues
```

**2. View WebAvanue file history:**
```bash
# See commit history for a specific file
git log --follow -- android/apps/webavanue/src/main/kotlin/.../MainActivity.kt

# See commit history for entire WebAvanue module
git log --follow -- android/apps/webavanue/

# See commit history for Universal module
git log --follow -- common/libs/webavanue/universal/
```

**3. View line-by-line authorship:**
```bash
# See who wrote each line (with copy detection)
git blame -C -C -C android/apps/webavanue/src/main/kotlin/.../MainActivity.kt

# See blame for Universal module file
git blame -C -C -C common/libs/webavanue/universal/domain/model/Tab.kt
```

**4. Search WebAvanue commits:**
```bash
# Find all commits touching WebAvanue
git log --all -- "*webavanue*"

# Find commits by message
git log --all --grep="webavanue" -i

# Find commits in specific date range
git log --since="2025-11-01" --until="2025-11-25" -- android/apps/webavanue/
```

---

## 💻 Using Claude Code for Git Operations

### In Claude Code Terminal

Claude Code works in the monorepo root automatically. You can run any git command:

```bash
# View current branch
git branch --show-current

# View all branches
git branch -a

# View WebAvanue commit history
git log --oneline --follow -- android/apps/webavanue/ | head -20

# View recent changes to WebAvanue
git diff HEAD~5 -- android/apps/webavanue/

# Search for specific changes
git log -p --all -S "WebViewController" -- android/apps/webavanue/
```

### Via Claude Code Commands

You can ask Claude to run git commands for you:

```
User: "Show me the git history for MainActivity.kt"
Claude: [Runs git log --follow for that file]

User: "Who last modified the Tab.kt file?"
Claude: [Runs git blame -C -C -C for that file]

User: "Show me all commits in the last week for WebAvanue"
Claude: [Runs git log with date filters]
```

### Git Operations Claude Can Do

Claude Code has access to git operations and can:

- ✅ View commit history (`git log`)
- ✅ View file blame (`git blame`)
- ✅ View diffs (`git diff`)
- ✅ View branch info (`git branch`)
- ✅ View status (`git status`)
- ✅ Search commits (`git log --grep`)
- ✅ Create commits (`git commit`)
- ✅ Create branches (`git branch`)
- ✅ Push changes (`git push`)
- ✅ View remote info (`git remote`)

---

## 🌐 Accessing GitLab Repository

### Repository URL
**Web:** https://gitlab.com/AugmentalisES/mainavanues

### Clone Repository
```bash
# HTTPS (recommended)
git clone https://gitlab.com/AugmentalisES/mainavanues.git

# SSH (requires SSH key setup)
git clone git@gitlab.com:AugmentalisES/mainavanues.git
```

### Remote Configuration
```bash
# View remote
git remote -v
# Output:
# origin  https://gitlab.com/AugmentalisES/mainavanues.git (fetch)
# origin  https://gitlab.com/AugmentalisES/mainavanues.git (push)

# View remote details
git remote show origin
```

### WebAvanue Branch
```bash
# View WebAvanue development branch
git branch -a | grep -i webavanue
# Output:
# * WebAvanue-Develop
#   remotes/origin/WebAvanue-Develop

# Switch to WebAvanue branch
git checkout WebAvanue-Develop

# Pull latest changes
git pull origin WebAvanue-Develop
```

---

## 📂 Viewing WebAvanue-Specific History

### Filter History by Path

**View only WebAvanue commits:**
```bash
# All WebAvanue changes (old and new locations)
git log --all --oneline -- \
  "Modules/WebAvanue/*" \
  "android/apps/webavanue/*" \
  "common/libs/webavanue/*"

# Count WebAvanue commits
git log --all --oneline -- "*webavanue*" | wc -l
```

**View WebAvanue contributors:**
```bash
# See who contributed to WebAvanue
git shortlog -sn -- "*webavanue*"

# See detailed contributor stats
git log --all --pretty=format:"%an" -- "*webavanue*" | sort | uniq -c | sort -rn
```

**View WebAvanue files changed over time:**
```bash
# See what files changed in each commit
git log --stat --all -- android/apps/webavanue/

# See actual code changes
git log -p --all -- android/apps/webavanue/src/main/kotlin/.../MainActivity.kt
```

---

## 🔧 Git Configuration for WebAvanue

### Copy Detection Enabled

The repository is configured to track file history across the migration:

```bash
# View blame settings
git config --get blame.detectCopies
# Output: true

git config --get blame.detectCopiesHarder
# Output: true
```

This means git automatically detects when files were copied/moved from:
- `Modules/WebAvanue/` → `android/apps/webavanue/`
- `Modules/WebAvanue/universal/` → `common/libs/webavanue/universal/`
- `Modules/WebAvanue/BrowserCoreData/` → `common/libs/webavanue/coredata/`

### User Configuration
```bash
# View your git identity
git config user.name
# Output: Blueeaglebuyer

git config user.email
# Output: 40310017+mkjhawar@users.noreply.github.com
```

---

## 🎯 Common Use Cases

### 1. Find When a Feature Was Added

```bash
# Search commit messages
git log --all --grep="XR support" -i -- "*webavanue*"

# Search code changes
git log --all -S "XRManager" -- "*webavanue*"
```

### 2. Compare WebAvanue Between Branches

```bash
# Compare current branch with main
git diff main..WebAvanue-Develop -- android/apps/webavanue/

# Compare specific commits
git diff abc123..def456 -- android/apps/webavanue/
```

### 3. View Original File Location

```bash
# Blame with original paths shown
git blame -C -C -C android/apps/webavanue/src/main/kotlin/.../MainActivity.kt

# Output will show:
# 35c32a49 Modules/WebAvanue/app/src/main/.../MainActivity.kt (Blueeaglebuyer 2025-11-23)
```

### 4. Track File Across Rename

```bash
# Follow file history across renames/moves
git log --follow --oneline -- android/apps/webavanue/src/main/kotlin/.../MainActivity.kt

# This automatically finds the file in Modules/WebAvanue/app/...
```

### 5. See WebAvanue Migration Commits

```bash
# Find migration-related commits
git log --all --oneline --grep="migration\|migrate" -i -- "*webavanue*"

# See detailed migration changes
git show 4cdbb22  # Migration commit hash
```

---

## 📱 IDE Git Integration

### IntelliJ IDEA / Android Studio

**1. Open Project:**
```bash
# Open MainAvanues in Android Studio
open -a "Android Studio" /Volumes/M-Drive/Coding/MainAvanues
```

**2. View Git History:**
- Right-click any file → **Git** → **Show History**
- IDE automatically uses `--follow` to track across renames
- Shows commits from original `Modules/WebAvanue/` location

**3. View Blame (Annotate):**
- Right-click any file → **Git** → **Annotate with Git Blame**
- Shows who wrote each line with original file paths

**4. Configure Copy Detection:**
- **Settings** → **Version Control** → **Git**
- Enable "Detect copies in Git"
- Enable "Detect renames"

### VS Code

**1. Git Lens Extension:**
```bash
# Install GitLens extension
code --install-extension eamodio.gitlens
```

**2. View History:**
- Right-click file → **GitLens: Open File History**
- Click on any line → See commit info

**3. View Blame:**
- Hover over any line → See inline blame annotation
- Shows original file path if copied/moved

---

## 🔐 Authentication

### HTTPS (Current Method)
```bash
# Clone/pull/push using HTTPS
git clone https://gitlab.com/AugmentalisES/mainavanues.git
```

GitLab will prompt for username/password or personal access token.

### Personal Access Token (Recommended)

1. **Create Token:**
   - Go to https://gitlab.com/-/profile/personal_access_tokens
   - Name: "MainAvanues Development"
   - Scopes: `read_repository`, `write_repository`
   - Create token

2. **Use Token:**
```bash
# When prompted for password, use personal access token
git push origin WebAvanue-Develop
# Username: your-gitlab-username
# Password: [paste personal access token]
```

3. **Cache Credentials:**
```bash
# Cache credentials for 1 hour
git config --global credential.helper 'cache --timeout=3600'

# Or use macOS keychain
git config --global credential.helper osxkeychain
```

---

## 📊 Repository Statistics

### WebAvanue Module Stats
```bash
# Count Kotlin files
find android/apps/webavanue common/libs/webavanue -name "*.kt" | wc -l
# Output: 121 files

# Count commits touching WebAvanue
git log --all --oneline -- "*webavanue*" | wc -l
# Output: 50+ commits

# Count lines of Kotlin code
find android/apps/webavanue common/libs/webavanue -name "*.kt" -exec wc -l {} + | tail -1
```

---

## 🆘 Troubleshooting

### Issue: Can't see history after migration

**Solution:** Use copy detection flags:
```bash
git log --follow -- android/apps/webavanue/src/main/.../MainActivity.kt
git blame -C -C -C android/apps/webavanue/src/main/.../MainActivity.kt
```

### Issue: Git says "not a git repository"

**Solution:** Make sure you're in the monorepo root or subdirectory:
```bash
cd /Volumes/M-Drive/Coding/MainAvanues
# Now all git commands will work
```

### Issue: Can't push changes

**Solution:** Check remote and branch:
```bash
git remote -v  # Verify remote URL
git branch -a  # Verify branch exists on remote
git pull origin WebAvanue-Develop  # Pull latest first
git push origin WebAvanue-Develop  # Then push
```

---

## 📚 Additional Resources

### Documentation
- **Git History Verification:** `docs/develop/webavanue/WebAvanue-Git-History-Verification-202511250350.md`
- **Migration Guide:** `docs/migration-analysis/COMPLETE-MIGRATION-GUIDE.md`
- **Migration Summary:** `docs/develop/webavanue/WebAvanue-Migration-Complete-Summary.md`

### Tags & Branches
```bash
# View migration tag
git show pre-monorepo-migration

# View backup branch
git log backup-webavanue-before-history-verification

# View WebAvanue development branch
git log WebAvanue-Develop
```

---

## ✅ Quick Reference

| Task | Command |
|------|---------|
| View file history | `git log --follow -- <file>` |
| View blame | `git blame -C -C -C <file>` |
| View WebAvanue commits | `git log --all -- "*webavanue*"` |
| Switch to WebAvanue branch | `git checkout WebAvanue-Develop` |
| Pull latest changes | `git pull origin WebAvanue-Develop` |
| View remote | `git remote -v` |
| View branches | `git branch -a` |
| Search commits | `git log --grep="keyword"` |
| Search code | `git log -S "code"` |

---

**Last Updated:** 2025-11-25
**Repository:** https://gitlab.com/AugmentalisES/mainavanues
**Current Branch:** WebAvanue-Develop
**Git Location:** `/Volumes/M-Drive/Coding/MainAvanues/.git`
