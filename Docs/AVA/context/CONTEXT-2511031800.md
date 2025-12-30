# CONTEXT SAVE

**Timestamp:** 2511031800
**Token Count:** ~16000
**Project:** ava
**Task:** Find and fix unsafe casts in Universal/AVA

## Summary
Starting systematic search for unsafe type casts (`as`) in Kotlin codebase. Found 17 files with potential unsafe casts. Will analyze each, prioritize production code over tests, and replace with safe casts (`as?`) with proper null handling.

## Recent Changes
None yet - starting fresh task

## Next Steps
1. Read each file with unsafe casts
2. Analyze context and determine appropriate safe cast + null handling
3. Fix production code first, then test code
4. Run tests to verify changes
5. Document all changes

## Open Questions
None yet
