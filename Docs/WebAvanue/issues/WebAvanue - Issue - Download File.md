# Download Progress & Status Fixes

This document describes the fixes to ensure:
- Real-time progress updates
- Reliable COMPLETED / FAILED status

---

## 1. Unified Download ID

- `DownloadViewModel` generates the ID
- `AndroidDownloadQueue` uses the same ID

---

## 2. Correct Progress Query

Uses:
- BYTES_DOWNLOADED_SO_FAR
- TOTAL_SIZE_BYTES
- STATUS
- LOCAL_URI

---

## 3. Emit Terminal States

- Emit COMPLETED / FAILED before stopping observation

---

## 4. Store Raw Byte Progress

- Store bytes, not percentage
- Percentage calculated in UI

---

## 5. Handle Unknown Size

- TOTAL_SIZE_BYTES can be -1
- Use indeterminate progress when unknown

---

## Final Outcome

✔ Realtime progress  
✔ Correct final status  
✔ Future-ready architecture
