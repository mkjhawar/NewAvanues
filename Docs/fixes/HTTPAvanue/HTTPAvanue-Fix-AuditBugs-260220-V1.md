# HTTPAvanue — Fix: Audit Bug Fixes
**Date:** 2026-02-20 | **Version:** V1 | **Branch:** HTTPAvanue

## Summary
Fixed 5 bugs discovered during comprehensive HTTPAvanue audit + wired HTTP/2 auto-detection into HttpServer.

## Fixes Applied

### Fix 1.1+1.2: Remove @Serializable from HttpRequest/HttpResponse (CRITICAL)
- **Problem:** Both classes had `@Serializable` with `body: ByteArray?` but no `ByteArraySerializer`. Also cascaded to require `@Serializable` on `HttpMethod` enum.
- **Root Cause:** These are server-internal pipeline types that flow through middleware/routing — they are never serialized to JSON.
- **Fix:** Removed `@Serializable` from both classes. `ClientModels.kt` already has properly annotated serializable versions for the client side.
- **Files:** `http/HttpRequest.kt`, `http/HttpResponse.kt`

### Fix 1.3: equals() null-body comparison bug (CRITICAL)
- **Problem:** `body?.contentEquals(other.body) == true` — when both bodies are null, `null == true` evaluates to `false`. Two identical requests with null bodies compared as NOT equal.
- **Fix:** Changed `== true` to `!= false`. `null != false` is `true`, correctly handling the both-null case. This pattern was already correct in `ClientModels.kt`.
- **Files:** `http/HttpRequest.kt` (line 34), `http/HttpResponse.kt` (line 64)

### Fix 1.4: HttpParser.readUtf8Line() missing EOF check (SIGNIFICANT)
- **Problem:** Called `readByte()` without checking `request(1)` first. When client disconnects mid-request, throws `EOFException` instead of returning null gracefully.
- **Fix:** Added `if (!request(1)) return if (length > 0) buffer.readUtf8() else null` before `readByte()`.
- **File:** `server/HttpParser.kt` (line 108)

### Fix 1.5: ErrorHandlerMiddleware hardcodes HTTP 400 (SIGNIFICANT)
- **Problem:** `catch (e: HttpException)` always returned `HttpStatus.BAD_REQUEST` regardless of `e.statusCode`.
- **Fix:** Replaced `HttpStatus.BAD_REQUEST` with `HttpStatus.from(e.statusCode)`.
- **File:** `middleware/ErrorHandlerMiddleware.kt` (line 20)

### Fix 1.6: Unused imports in SseEmitter.kt (MINOR)
- **Fix:** Removed unused `HttpRequest` and `Socket` imports. `HttpResponse` was kept (used by `createSseResponse()`).
- **File:** `sse/SseEmitter.kt`

## HTTP/2 Auto-Detection (Part 3)

### Changes to HttpServer.handleConnection()
- **Prior Knowledge**: Peek at first 24 bytes via `source.request(prefaceSize)` + `source.buffer.snapshot()`. If matches HTTP/2 connection preface, consume and delegate to `Http2ServerHandler.handlePriorKnowledge()`.
- **h2c Upgrade**: After parsing HTTP/1.1 request, check `Http2ServerHandler.isH2cUpgradeRequest()`. If matched, delegate to `Http2ServerHandler.handleH2cUpgrade()`.
- **ServerConfig**: Added `http2Enabled: Boolean = true` and `http2Settings: Http2Settings = Http2Settings()`.
- Also fixed `HttpException` catch to use `HttpStatus.from(e.statusCode)` instead of hardcoded `badRequest`.

## Build Verification
- `./gradlew :Modules:HTTPAvanue:compileKotlinDesktop` — BUILD SUCCESSFUL
- `./gradlew :Modules:HTTPAvanue:compileDebugKotlinAndroid` — BUILD SUCCESSFUL
