# Pre-Compaction Reports Index

This directory contains pre-compaction reports for all ongoing projects. These reports are critical for context recovery after memory compaction events.

## 📋 Active Reports

| Project | Date | Status | Module | File |
|---------|------|--------|--------|------|
| Speech Engines Port | 2025-01-29 | 60% (2/3 engines) | SpeechRecognition | [Speech-Engines-Port-Precompaction-Report-2025-01-29.md](./Speech-Engines-Port-Precompaction-Report-2025-01-29.md) |
| Vivoka Complete Port | 2025-01-28 | 100% Complete ✅ | SpeechRecognition | [Vivoka-Complete-Precompaction-Report-2025-01-28.md](./Vivoka-Complete-Precompaction-Report-2025-01-28.md) |

## 🗑️ Archived Reports

| Project | Date | Status | Module | File |
|---------|------|--------|--------|------|
| Vivoka Port (Initial) | 2025-01-28 | Superseded | SpeechRecognition | [Vivoka-Port-Precompaction-Report-2025-01-28.md](./Vivoka-Port-Precompaction-Report-2025-01-28.md) |

## 📚 How to Use

1. **Creating a Report:** Use `/Agent-Instructions/PRECOMPACTION-PROTOCOL.md` as template
2. **Naming:** `[ProjectName]-Precompaction-Report-[YYYY-MM-DD].md`
3. **When to Create:** Before compaction or at major milestones
4. **Recovery:** Read report first after any compaction event

## 🗂️ Archive

Completed or obsolete reports are moved to the `Archive/` subdirectory.

## 🔍 Quick Recovery

If context was lost, read reports in this order:
1. Find most recent report for your project
2. Read the report's recovery instructions
3. Follow the verification checklist
4. Continue from documented next actions

---
*Last Updated: 2025-01-28*