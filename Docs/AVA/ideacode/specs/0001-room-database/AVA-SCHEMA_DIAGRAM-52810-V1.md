# AVA Database Schema v1.0

## Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         CONVERSATIONS                                    │
│─────────────────────────────────────────────────────────────────────────│
│ PK  id: TEXT (UUID)                                                     │
│     title: TEXT                                                         │
│     created_at: INTEGER (timestamp)          ◄─── Index                 │
│     updated_at: INTEGER (timestamp)          ◄─── Index                 │
│     message_count: INTEGER (denormalized)                               │
│     is_archived: BOOLEAN                     ◄─── Index                 │
│     metadata: TEXT (JSON)                                               │
└─────────────────────────────────────────────────────────────────────────┘
                               △
                               │
                               │ FK CASCADE DELETE
                               │
┌─────────────────────────────────────────────────────────────────────────┐
│                            MESSAGES                                      │
│─────────────────────────────────────────────────────────────────────────│
│ PK  id: TEXT (UUID)                                                     │
│ FK  conversation_id: TEXT                                               │
│     role: TEXT (USER, ASSISTANT, SYSTEM)     ◄─── Index                 │
│     content: TEXT                                                       │
│     timestamp: INTEGER                                                  │
│     intent: TEXT (nullable)                                             │
│     confidence: REAL (nullable)                                         │
│     metadata: TEXT (JSON, nullable)                                     │
│                                                                          │
│ COMPOSITE INDEX: (conversation_id, timestamp)  ◄─── VOS4 Pattern        │
└─────────────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────────┐
│                         TRAIN_EXAMPLES                                   │
│─────────────────────────────────────────────────────────────────────────│
│ PK  id: INTEGER (auto)                                                  │
│ UQ  example_hash: TEXT (MD5)                 ◄─── UNIQUE Index (VOS4)   │
│     utterance: TEXT                                                     │
│     intent: TEXT                             ◄─── Index                 │
│     locale: TEXT (e.g., "en-US")             ◄─── Index                 │
│     source: TEXT (MANUAL, AUTO_LEARN, CORRECTION)                       │
│     created_at: INTEGER (timestamp)          ◄─── Index                 │
│     usage_count: INTEGER (default 0)                                    │
│     last_used: INTEGER (nullable)                                       │
└─────────────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────────┐
│                           DECISIONS                                      │
│─────────────────────────────────────────────────────────────────────────│
│ PK  id: TEXT (UUID)                                                     │
│     conversation_id: TEXT                                               │
│     decision_type: TEXT                      ◄─── Index                 │
│     input_data: TEXT (JSON)                                             │
│     output_data: TEXT (JSON)                                            │
│     confidence: REAL                         ◄─── Index                 │
│     timestamp: INTEGER                                                  │
│     reasoning: TEXT (nullable)                                          │
│                                                                          │
│ COMPOSITE INDEX: (conversation_id, timestamp)  ◄─── VOS4 Pattern        │
└─────────────────────────────────────────────────────────────────────────┘
                               △
                               │
                               │ Logical Link
                               │
┌─────────────────────────────────────────────────────────────────────────┐
│                           LEARNING                                       │
│─────────────────────────────────────────────────────────────────────────│
│ PK  id: TEXT (UUID)                                                     │
│     decision_id: TEXT                        ◄─── Index (links above)   │
│     feedback_type: TEXT                      ◄─── Index                 │
│                    (POSITIVE, NEGATIVE, CORRECTION)                     │
│     user_correction: TEXT (JSON, nullable)                              │
│     timestamp: INTEGER                       ◄─── Index                 │
│     outcome: TEXT (SUCCESS, FAILURE, PARTIAL)                           │
│     notes: TEXT (nullable)                                              │
└─────────────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────────┐
│                            MEMORY                                        │
│─────────────────────────────────────────────────────────────────────────│
│ PK  id: TEXT (UUID)                                                     │
│     memory_type: TEXT                        ◄─── Index                 │
│                    (FACT, PREFERENCE, CONTEXT, SKILL)                   │
│     content: TEXT                                                       │
│     embedding: TEXT (JSON float array, nullable)                        │
│     importance: REAL (0.0 to 1.0)            ◄─── Index (VOS4)          │
│     created_at: INTEGER (timestamp)          ◄─── Index                 │
│     last_accessed: INTEGER (timestamp)       ◄─── Index                 │
│     access_count: INTEGER (default 0)                                   │
│     metadata: TEXT (JSON, nullable)                                     │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Index Strategy

### Composite Indices (VOS4 Pattern)
```sql
-- Messages: Fast pagination by conversation + timestamp
CREATE INDEX idx_messages_conv_time
ON messages(conversation_id, timestamp)

-- Decisions: Timeline queries by conversation
CREATE INDEX idx_decisions_conv_time
ON decisions(conversation_id, timestamp)
```

**Performance Benefit:**
- Single index serves both filtering and sorting
- Avoids multiple index lookups
- Enables O(log n) pagination across large datasets

### Unique Indices (VOS4 Pattern)
```sql
-- Train Examples: Hash-based deduplication
CREATE UNIQUE INDEX idx_train_examples_hash
ON train_examples(example_hash)
```

**Performance Benefit:**
- O(1) duplicate detection via hash lookup
- Prevents duplicate training data
- MD5 hash: `hash(utterance + intent)`

### Single-Column Indices
```sql
-- Conversations
CREATE INDEX idx_conversations_created ON conversations(created_at)
CREATE INDEX idx_conversations_updated ON conversations(updated_at)
CREATE INDEX idx_conversations_archived ON conversations(is_archived)

-- Messages
CREATE INDEX idx_messages_role ON messages(role)

-- Train Examples
CREATE INDEX idx_train_examples_intent ON train_examples(intent)
CREATE INDEX idx_train_examples_locale ON train_examples(locale)
CREATE INDEX idx_train_examples_created ON train_examples(created_at)

-- Decisions
CREATE INDEX idx_decisions_type ON decisions(decision_type)
CREATE INDEX idx_decisions_confidence ON decisions(confidence)

-- Learning
CREATE INDEX idx_learning_decision ON learning(decision_id)
CREATE INDEX idx_learning_feedback ON learning(feedback_type)
CREATE INDEX idx_learning_timestamp ON learning(timestamp)

-- Memory
CREATE INDEX idx_memory_type ON memory(memory_type)
CREATE INDEX idx_memory_importance ON memory(importance)
CREATE INDEX idx_memory_created ON memory(created_at)
CREATE INDEX idx_memory_accessed ON memory(last_accessed)
```

---

## Foreign Key Relationships

### Cascade Delete (VOS4 Pattern)
```sql
-- Messages → Conversations
ALTER TABLE messages ADD CONSTRAINT fk_messages_conversation
FOREIGN KEY (conversation_id) REFERENCES conversations(id)
ON DELETE CASCADE

-- When conversation deleted → All messages auto-deleted
```

**Benefit:** Automatic cleanup, data integrity guaranteed

### Logical Links (No FK)
```sql
-- Learning → Decisions (no FK constraint)
-- Allows decision deletion without cascade

-- Memory → (standalone, no links)
-- Long-term memory independent of conversations
```

---

## Data Flow Diagrams

### Message Creation Flow
```
┌─────────┐
│  User   │
└────┬────┘
     │ 1. Send message
     ▼
┌─────────────────┐
│ MessageRepo     │
└────┬────────────┘
     │ 2. Insert message
     ▼
┌─────────────────┐       ┌──────────────────┐
│ MessageDao      │──────►│ ConversationDao  │
│ insertMessage() │       │ incrementCount() │
└─────────────────┘       └──────────────────┘
     │                             │
     │ 3a. Insert row             │ 3b. Update count
     ▼                             ▼
┌───────────────────────────────────────┐
│         MESSAGES table                │
│  New row with conversation_id FK      │
└───────────────────────────────────────┘
                    │
                    │ 4. FK validates
                    ▼
┌───────────────────────────────────────┐
│      CONVERSATIONS table              │
│  message_count incremented            │
│  updated_at timestamp updated         │
└───────────────────────────────────────┘
```

### Training Example Deduplication Flow
```
┌─────────┐
│  User   │
└────┬────┘
     │ 1. Add training example
     ▼
┌─────────────────────┐
│ TrainExampleRepo    │
│ - Generate MD5 hash │
└────┬────────────────┘
     │ 2. Check duplicate
     ▼
┌─────────────────────┐
│ TrainExampleDao     │
│ findDuplicate(hash) │
└────┬────────────────┘
     │ 3. Query by hash (UNIQUE index)
     ▼
┌───────────────────────────────────────┐
│      TRAIN_EXAMPLES table             │
│  WHERE example_hash = ?               │
└─────┬─────────────────────────────────┘
      │
      ├─► Found? → Return Error (duplicate)
      │
      └─► Not found? → Insert new example
```

### Conversation Deletion Cascade
```
┌─────────┐
│  User   │
└────┬────┘
     │ 1. Delete conversation
     ▼
┌─────────────────────┐
│ ConversationRepo    │
└────┬────────────────┘
     │ 2. Delete by ID
     ▼
┌─────────────────────┐
│ ConversationDao     │
│ deleteConversation()│
└────┬────────────────┘
     │ 3. DELETE FROM conversations WHERE id = ?
     ▼
┌───────────────────────────────────────┐
│      CONVERSATIONS table              │
│  Row deleted                          │
└─────┬─────────────────────────────────┘
      │
      │ 4. CASCADE DELETE triggered by FK
      ▼
┌───────────────────────────────────────┐
│         MESSAGES table                │
│  All messages with conversation_id    │
│  automatically deleted                │
└───────────────────────────────────────┘
```

---

## VOS4 Patterns Summary

| Pattern | Location | Benefit |
|---------|----------|---------|
| **Composite Index** | `messages(conversation_id, timestamp)` | Fast pagination, single index for filter+sort |
| **Hash Uniqueness** | `train_examples(example_hash)` | O(1) deduplication, prevents duplicate training |
| **Cascade Delete** | `messages → conversations` | Automatic cleanup, data integrity |
| **Denormalized Count** | `conversations.message_count` | O(1) count lookup, avoids COUNT(*) |
| **Usage Tracking** | `train_examples.usage_count`, `memory.access_count` | Analytics, learning optimization |
| **Importance Index** | `memory.importance` | Priority-based retrieval, relevance sorting |

---

## Storage Estimates

### Per Entity
- **Conversation**: ~200 bytes (UUID + title + metadata)
- **Message**: ~500 bytes (UUID + content ~300 chars avg)
- **TrainExample**: ~150 bytes (hash + utterance + intent)
- **Decision**: ~1 KB (input/output JSON)
- **Learning**: ~500 bytes (feedback + correction JSON)
- **Memory**: ~800 bytes (content + embedding stub)

### Capacity Planning
```
1000 conversations × 100 messages each:
  Conversations: 1000 × 200 bytes = 200 KB
  Messages: 100,000 × 500 bytes = 50 MB
  Total: ~50.2 MB ✅ (Budget: < 50 MB)

10,000 training examples:
  10,000 × 150 bytes = 1.5 MB

Total database with 1000 conversations: ~52 MB ✅
```

---

*Schema v1.0 - Implemented 2025-10-28*
*Following Clean Architecture + VOS4 Patterns*
