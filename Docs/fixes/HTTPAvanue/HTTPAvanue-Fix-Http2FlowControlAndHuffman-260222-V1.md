# HTTPAvanue-Fix-Http2FlowControlAndHuffman-260222-V1

## Context

The HTTPAvanue HTTP/2 implementation had 5 issues discovered during code review. The original handover listed C1 (frame sync) and C2 (Huffman decoder) as outstanding, but source verification revealed both were already addressed. The REAL remaining issues were different.

## Issues Fixed

### 1. sendResponse Flow Control Violation (HIGH)
**File**: `Http2Connection.kt` (sendResponse)
**Before**: Wrote DATA frames in a loop capped only by `maxFrameSize`, ignoring stream and connection send windows. Violated RFC 7540 Section 6.9.
**After**: Uses `flowControl.availableSendBytes(stream, maxFrameSize)` to cap each DATA chunk. Waits up to 30s for WINDOW_UPDATE if window is exhausted. Truncates with error log on timeout.

### 2. Streams Map Thread Safety (HIGH)
**File**: `Http2Connection.kt`
**Before**: `streams: MutableMap` accessed from main frame loop AND child `dispatchRequest` coroutines concurrently. Potential HashMap corruption.
**After**: Added `streamsMutex: Mutex` protecting all `streams` reads/writes. Non-suspend handle methods converted to `suspend`. Lock ordering: streamsMutex → sinkMutex (prevents deadlocks).

### 3. sendResponse Per-Frame Mutex (Architectural)
**File**: `Http2Connection.kt` (sendResponse + dispatchRequest)
**Before**: `sinkMutex` held for entire response (headers + all DATA frames). Blocked all other streams.
**After**: `sinkMutex` acquired per-frame. HPACK encode + HEADERS write are one atomic unit. Each DATA chunk gets its own lock/unlock cycle, allowing interleaving between streams.

### 4. Huffman Encoder (MEDIUM)
**File**: `HpackHuffman.kt` + `HpackEncoder.kt`
**Before**: `HpackEncoder.encodeString()` always sent raw strings (H=0). ~30% bandwidth waste on HTTP header values.
**After**: `HpackHuffman.encode()` implements bit-level Huffman encoding with EOS padding. `encodeIfSmaller()` checks if Huffman saves space. `HpackEncoder.encodeString()` uses Huffman when beneficial (H=1).

### 5. Missing Imports + Dead Code Fix (LOW)
**File**: `HpackHuffman.kt`
**Before**: Referenced `Http2Exception` and `Http2ErrorCode` without imports. Padding validation check was unreachable (`bits % 8 > 7` always false).
**After**: Added proper imports. Replaced broken padding check with correct comment explaining tree-structural validation.

### 6. Encoder Pre-allocation (LOW)
**File**: `HpackEncoder.kt`
**Before**: `encode()` created `MutableList<Byte>()` with default capacity.
**After**: Pre-allocates with `ArrayList<Byte>(estimated)` based on header sizes.

## Files Modified

| File | Changes |
|------|---------|
| `http2/Http2Connection.kt` | streamsMutex, per-frame sinkMutex, flow control in sendResponse |
| `hpack/HpackHuffman.kt` | imports, encode(), encodeIfSmaller(), padding fix |
| `hpack/HpackEncoder.kt` | Huffman encoding in encodeString(), pre-allocation |

All files in `Modules/HTTPAvanue/src/commonMain/`.

## Already Fixed (Verified — NOT in this commit)

- **C1 Frame Sync**: `sinkMutex` already existed (line 30) wrapping all sink writes
- **C2 Huffman Decoder**: Fully implemented — 257-entry code table, bit-by-bit tree traversal, EOS handling
