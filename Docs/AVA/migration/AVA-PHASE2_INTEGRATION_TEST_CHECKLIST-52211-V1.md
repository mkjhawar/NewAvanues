# Phase 2 Integration Test Checklist
## RAG & Chat Module Integration Testing

### Build Status: CRITICAL ISSUES IDENTIFIED
- Primary compilation issues: FIXED (Teach, WakeWord, Chat modules)
- Secondary issues: Hilt injection errors (WakeWord ApiKeyManager)
- Test compilation errors: 8 errors in Chat module tests

---

## 1. RAG Settings & Persistence Tests

### 1.1 Settings Save/Load
- [ ] User can access RAG settings menu from Chat
- [ ] Enable/disable RAG toggle saves to preferences
- [ ] Preferences persist after app restart
- [ ] Default settings applied on first run

### 1.2 Document Selection
- [ ] Document picker opens and displays available documents
- [ ] Multi-select checkbox works for documents
- [ ] Selected documents persist across screens
- [ ] Clear selection button deselects all
- [ ] No documents selected state shows appropriate message

---

## 2. Document Management Tests

### 2.1 Document Upload
- [ ] File picker accepts PDF, DOCX, TXT formats
- [ ] Document metadata (name, size, upload date) displays
- [ ] Upload progress indicator shown for large files
- [ ] Duplicate document detection prevents re-upload
- [ ] Document upload fails gracefully with error message

### 2.2 Document Display
- [ ] Document list shows all uploaded documents
- [ ] Document count displayed correctly
- [ ] Delete action removes document from list
- [ ] Confirm dialog before deletion
- [ ] Empty state when no documents uploaded

---

## 3. RAG Retrieval Execution Tests

### 3.1 RAG Integration with Chat
- [ ] User question triggers RAG retrieval when enabled
- [ ] Related document chunks retrieved and ranked
- [ ] Retrieval happens before LLM response generation
- [ ] Retrieval timeout handled gracefully (5s default)
- [ ] Failed retrieval doesn't block chat response

### 3.2 Response Generation
- [ ] LLM receives context from RAG documents
- [ ] Response quality improved with RAG context
- [ ] Response includes proper citations
- [ ] LLM prompt includes relevant document chunks
- [ ] Token limit respected in context injection

---

## 4. Citation Display & Interaction Tests

### 4.1 Citation Rendering
- [ ] Citations appear below each RAG-enhanced response
- [ ] Citation sources show document name + page/section
- [ ] Confidence scores display (if available)
- [ ] Citation format: "[Source Name, p. X]"
- [ ] Multiple citations separated clearly

### 4.2 Citation Interaction
- [ ] Tap citation to view full source context
- [ ] Long-press citation to copy reference
- [ ] Citation expandable list shows all sources
- [ ] Collapsed state shows "N sources" preview
- [ ] Citation section scrollable if many sources

---

## 5. Graceful Degradation Tests

### 5.1 RAG Disabled Mode
- [ ] Chat works normally when RAG disabled
- [ ] No retrieval calls made when RAG off
- [ ] No performance impact without RAG
- [ ] Settings preserved when toggling RAG

### 5.2 No Documents Selected
- [ ] Graceful message: "No documents selected for RAG"
- [ ] User directed to select documents
- [ ] Response still generated without RAG
- [ ] No crash or frozen UI

### 5.3 Network/Retrieval Failures
- [ ] Timeout after 5 seconds returns user message
- [ ] Error toast shows: "Document retrieval failed"
- [ ] Retry button available for failed retrieval
- [ ] Chat continues without RAG context

### 5.4 Large Context Handling
- [ ] System handles 100+ page documents
- [ ] Retrieval performance acceptable (<2s for typical query)
- [ ] Memory usage stays under 500MB
- [ ] No app crashes with large document sets

---

## 6. Feature Interaction Tests

### 6.1 RAG + Voice Input
- [ ] Voice query triggers RAG retrieval
- [ ] Citation appears for voice-based responses
- [ ] TTS reads response (with/without citations)
- [ ] Works across lock screen (accessibility)

### 6.2 RAG + Conversation History
- [ ] RAG context used in multi-turn conversations
- [ ] Document context maintained across messages
- [ ] Follow-up questions use same documents
- [ ] Context switching when documents changed

### 6.3 RAG + Search/Filter
- [ ] Document search filters by name
- [ ] Search results highlight matching text
- [ ] Search updates selected document count
- [ ] Search state resets when closing

---

## 7. Performance & Resource Tests

### 7.1 Memory Usage
- [ ] Initial: <50MB with RAG module loaded
- [ ] After retrieval: <150MB peak
- [ ] After response: returns to baseline
- [ ] No memory leaks after 20+ messages

### 7.2 Latency
- [ ] Document upload: <5s for 5MB file
- [ ] RAG retrieval: <2s for typical query
- [ ] Response generation: <3s (including RAG)
- [ ] UI remains responsive during operations

### 7.3 Battery Impact
- [ ] RAG enabled: <2% extra drain per hour
- [ ] No aggressive polling or background activity
- [ ] Network connections properly closed
- [ ] Temp files cleaned up after use

---

## 8. Accessibility & Compliance Tests

### 8.1 Screen Reader Support
- [ ] RAG toggle labeled properly
- [ ] Document list items announced correctly
- [ ] Citations announced with full context
- [ ] Citation count and source names read clearly

### 8.2 Visual Accessibility
- [ ] High contrast citations (WCAG AA)
- [ ] Touch targets minimum 48dp
- [ ] Font sizes readable (minimum 14sp for body)
- [ ] Citation styling doesn't rely on color alone

### 8.3 Keyboard Navigation
- [ ] Tab through document list
- [ ] Tab through citation sources
- [ ] Enter activates citations
- [ ] Escape closes expanded citation list

---

## 9. Data & Privacy Tests

### 9.1 Document Handling
- [ ] Documents stored encrypted
- [ ] Metadata (size, name) not logged externally
- [ ] Documents not accessible to other apps
- [ ] Clear cache option removes indexed data

### 9.2 LLM Context Privacy
- [ ] Document chunks not sent to analytics
- [ ] Citations only reference document names
- [ ] No full document content in logs
- [ ] Query + context not logged for training

### 9.3 Compliance
- [ ] User can delete documents anytime
- [ ] Deleted documents fully removed from cache
- [ ] No data retention beyond session
- [ ] Privacy policy updated for RAG feature

---

## 10. Edge Cases & Error Handling

### 10.1 Malformed Documents
- [ ] PDF with missing fonts handles gracefully
- [ ] Corrupted document shows error (don't crash)
- [ ] Scanned image PDF shows message
- [ ] Non-standard encoding detected and handled

### 10.2 Extreme Scenarios
- [ ] 1000+ page document upload
- [ ] Concurrent uploads (5+ documents)
- [ ] Query with 500+ token response
- [ ] Retrieval failure in background service

### 10.3 User Interruptions
- [ ] Cancel document upload mid-process
- [ ] Switch away during RAG retrieval (background)
- [ ] Delete document while retrieving
- [ ] Network disconnect during transfer

---

## Test Execution Notes

### Prerequisites
1. Chat module compiles successfully
2. All unit tests pass
3. RAG module integrated with Chat
4. Test documents prepared (PDF, DOCX, TXT)

### Test Data
- Sample documents: 3-50 page technical documents
- Test queries: 10 domain-specific questions
- Edge cases: Large files, corrupted files, special characters

### Success Criteria
- **Green**: All checklist items pass
- **Yellow**: Minor failures with workarounds available
- **Red**: Critical failures blocking feature release

---

## Known Issues (Phase 2)

1. **Hilt Injection Error**: `ApiKeyManager` missing provider
   - Impact: WakeWord module won't initialize
   - Workaround: Remove WakeWord from app startup
   - Fix: Provide ApiKeyManager in Hilt config

2. **Test Compilation Errors**: 8 errors in Chat tests
   - Impact: Tests can't run, manual testing required
   - Files: TTSManagerTest, TTSViewModelTest, ChatViewModelTest
   - Fix: Update test constructor calls with new parameters

3. **Build Completion**: assembleDebug blocked by ApiKeyManager
   - Impact: Cannot deploy to devices
   - Workaround: Create stub ApiKeyManager provider
   - Fix: Complete Phase 1 ApiKeyManager implementation

---

## Next Steps
1. Fix Hilt dependency injection issues
2. Update all test files with new ChatViewModel parameters
3. Run integration tests on staging environment
4. Performance load testing with 50+ documents
5. User acceptance testing with actual documents

---

**Prepared by**: Integration & Build Verification Specialist (Agent 6)
**Date**: 2025-11-22
**Status**: BLOCKED - Manual intervention required for build completion
