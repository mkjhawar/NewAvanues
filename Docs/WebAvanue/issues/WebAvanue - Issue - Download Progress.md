# Download Issues & Root Cause Analysis

This document explains the issues found in the download implementation involving
`DownloadViewModel`, `AndroidDownloadQueue`, and `DownloadManager`.

---

## 1. Download Progress Not Updating in Real Time

### ❌ Problem
- Download progress updates were not reflected in `DownloadViewModel` in real time.
- UI either stayed at `0%` or stopped updating midway.

### 🔍 Root Cause
- **Download ID mismatch**
  - `DownloadViewModel` generated its own `download.id`
  - `AndroidDownloadQueue.enqueue()` generated a different internal ID
- Progress updates emitted by the queue could not be matched back to the correct
  `Download` object in the ViewModel.

---

## 2. Download Status Never Becomes COMPLETED

### ❌ Problem
- Downloads completed at system level, but app never received `COMPLETED` status.

### 🔍 Root Cause
- Terminal states were filtered out in the queue
- Flow stopped before emitting `COMPLETED` / `FAILED`

---

## 3. Missing Completion Metadata

- File path and completion time were not stored
- UI could not open downloaded files reliably

---

## Summary
- ID mismatch
- Terminal states filtered
- Missing metadata
- Incorrect size assumptions
