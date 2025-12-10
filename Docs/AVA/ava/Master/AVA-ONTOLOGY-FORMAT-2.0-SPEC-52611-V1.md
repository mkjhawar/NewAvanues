# AVA Ontology Format 2.0 Specification

**Version:** 2.0.0
**Date:** 2025-11-25
**Status:** Draft
**Extends:** AVA Universal Format 2.0 (.ava)

> **Note on Naming**: AON refers to the format specification (AVA Ontology), while `.aot` is the file extension (AVA Ontology Template/Text). Do not confuse `.aot` files with `.AON` files (uppercase), which are binary wrapper packages used for model distribution.

---

## Overview

The AVA Ontology Format (AON) extends the .ava format with semantic metadata for intelligent intent resolution. Files using this format have the `.aot` extension. It enables:

- **Zero-shot intent classification** via semantic descriptions
- **Synonym resolution** without explicit training examples
- **Multi-step action decomposition** for complex tasks
- **Capability-based app discovery** for dynamic action routing
- **Multilingual support** with mALBERT embeddings

---

## File Extension

`.aot` - AVA Ontology Template/Text

The file extension `.aot` represents text-based ontology definition files. Do not confuse with `.AON` (uppercase), which are binary wrapper packages.

---

## Schema

### Root Object

```json
{
  "schema": "ava-ontology-2.0",
  "version": "2.0.0",
  "locale": "en-US",
  "metadata": {
    "filename": "communication.aot",
    "category": "communication",
    "name": "Communication Intents",
    "description": "Email, messaging, and calling actions",
    "ontology_count": 3,
    "author": "AVA AI",
    "created_at": "2025-11-25T00:00:00Z"
  },
  "ontology": [
    {
      "id": "send_email",
      "canonical_form": "compose_and_send_email",
      "description": "User wants to compose and send an electronic message",
      "synonyms": ["send email", "compose email", "write email"],
      "action_type": "multi_step",
      "action_sequence": [
        {
          "step": 1,
          "action": "OPEN_APP",
          "capability_required": "email_client",
          "fallback": ["OPEN_BROWSER", "SHOW_ERROR"]
        },
        {
          "step": 2,
          "action": "COMPOSE_EMAIL",
          "intent_type": "ACTION_SENDTO",
          "intent_data": "mailto:",
          "extract_entities": ["recipient", "subject", "body"]
        }
      ],
      "required_capabilities": ["email_client"],
      "entity_schema": {
        "recipient": {
          "type": "PERSON",
          "patterns": ["to {recipient}", "send to {recipient}"],
          "optional": true
        }
      },
      "priority": 1,
      "tags": ["email", "communication", "compose"]
    }
  ],
  "global_synonyms": {
    "send": ["compose", "write", "create", "draft"],
    "email": ["message", "mail", "e-mail"]
  },
  "capability_mappings": {
    "email_client": {
      "apps": [
        {
          "package": "com.google.android.gm",
          "name": "Gmail",
          "actions": ["COMPOSE", "SEND", "VIEW", "SEARCH"]
        }
      ],
      "intent_filters": [
        {
          "action": "ACTION_SENDTO",
          "data": "mailto:"
        }
      ]
    }
  }
}
```

---

## Field Definitions

### Root Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `schema` | string | ✅ | Must be "ava-ontology-2.0" |
| `version` | string | ✅ | Semantic version (e.g., "2.0.0") |
| `locale` | string | ✅ | Language code (e.g., "en-US", "es-ES") |
| `metadata` | object | ✅ | File metadata |
| `ontology` | array | ✅ | Array of semantic intent definitions |
| `global_synonyms` | object | ❌ | Word-level synonyms across all intents |
| `capability_mappings` | object | ❌ | App capability to package mappings |

### Metadata Object

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `filename` | string | ✅ | Original filename |
| `category` | string | ✅ | Intent category |
| `name` | string | ✅ | Human-readable name |
| `description` | string | ✅ | Description of ontology contents |
| `ontology_count` | integer | ✅ | Number of ontology entries |
| `author` | string | ❌ | Author/creator |
| `created_at` | string | ❌ | ISO 8601 timestamp |

### Ontology Object

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | ✅ | Unique intent identifier |
| `canonical_form` | string | ✅ | Canonical representation |
| `description` | string | ✅ | Semantic description for zero-shot classification |
| `synonyms` | array<string> | ✅ | List of synonym phrases |
| `action_type` | enum | ✅ | "single_step", "multi_step", "conditional" |
| `action_sequence` | array<object> | ✅ | Ordered list of action steps |
| `required_capabilities` | array<string> | ✅ | Required app capabilities |
| `entity_schema` | object | ❌ | Entity extraction schema |
| `priority` | integer | ❌ | Intent priority (1-10) |
| `tags` | array<string> | ❌ | Searchable tags |

### Action Step Object

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `step` | integer | ✅ | Step number (1-indexed) |
| `action` | string | ✅ | Action name (e.g., "OPEN_APP", "COMPOSE_EMAIL") |
| `capability_required` | string | ❌ | Required capability |
| `intent_type` | string | ❌ | Android intent action |
| `intent_data` | string | ❌ | Android intent data URI |
| `extract_entities` | array<string> | ❌ | Entities to extract from utterance |
| `fallback` | array<string> | ❌ | Fallback actions if primary fails |

### Entity Schema Object

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | enum | ✅ | "PERSON", "TEXT", "NUMBER", "DATE", "LOCATION" |
| `patterns` | array<string> | ❌ | Extraction patterns with {entity} placeholders |
| `optional` | boolean | ❌ | Whether entity is optional |

---

## Action Types

### `single_step`
Single action execution (e.g., "show time")

```json
{
  "id": "show_time",
  "action_type": "single_step",
  "action_sequence": [
    {
      "step": 1,
      "action": "SHOW_TIME"
    }
  ]
}
```

### `multi_step`
Multiple actions in sequence (e.g., "send email")

```json
{
  "id": "send_email",
  "action_type": "multi_step",
  "action_sequence": [
    {"step": 1, "action": "OPEN_EMAIL_APP"},
    {"step": 2, "action": "COMPOSE_EMAIL"}
  ]
}
```

### `conditional`
Action depends on runtime conditions (e.g., "play music")

```json
{
  "id": "play_music",
  "action_type": "conditional",
  "action_sequence": [
    {
      "step": 1,
      "action": "CHECK_MUSIC_APP",
      "fallback": ["INSTALL_MUSIC_APP", "OPEN_BROWSER"]
    },
    {
      "step": 2,
      "action": "PLAY_MUSIC"
    }
  ]
}
```

---

## Required Capabilities

Standard capability types:

| Capability | Description | Example Apps |
|------------|-------------|--------------|
| `email_client` | Email composition and sending | Gmail, Outlook |
| `web_browser` | Web browsing | Chrome, Firefox |
| `media_player` | Audio/video playback | Spotify, YouTube |
| `messaging` | Text messaging | Messages, WhatsApp |
| `phone_dialer` | Phone calls | Phone, Dialer |
| `navigation` | GPS navigation | Maps, Waze |
| `calendar` | Calendar management | Calendar, Outlook |
| `camera` | Photo/video capture | Camera |
| `contacts` | Contact management | Contacts |

---

## Global Synonyms

Word-level synonyms applied across all intents:

```json
{
  "global_synonyms": {
    "send": ["compose", "write", "create", "draft", "start"],
    "email": ["message", "mail", "e-mail"],
    "open": ["launch", "start", "run", "show"],
    "check": ["show", "display", "tell", "what's", "get"]
  }
}
```

These are applied during synonym resolution:
- "send email" → matches "compose email", "write email"
- "open browser" → matches "launch browser", "start browser"

---

## Capability Mappings

Maps capabilities to Android app packages:

```json
{
  "capability_mappings": {
    "email_client": {
      "apps": [
        {
          "package": "com.google.android.gm",
          "name": "Gmail",
          "actions": ["COMPOSE", "SEND", "VIEW", "SEARCH"]
        },
        {
          "package": "com.microsoft.office.outlook",
          "name": "Outlook",
          "actions": ["COMPOSE", "SEND", "VIEW", "SEARCH"]
        }
      ],
      "intent_filters": [
        {
          "action": "ACTION_SENDTO",
          "data": "mailto:"
        },
        {
          "action": "ACTION_SEND",
          "type": "message/rfc822"
        }
      ]
    }
  }
}
```

---

## Multilingual Support

Each AON file (`.aot` extension) contains one locale:

```
communication-en-US.aot  → English (United States)
communication-es-ES.aot  → Spanish (Spain)
communication-fr-FR.aot  → French (France)
communication-de-DE.aot  → German (Germany)
```

mALBERT embeddings are computed per-locale and stored in the database.

---

## Example: Complete AON File

```json
{
  "schema": "ava-ontology-2.0",
  "version": "2.0.0",
  "locale": "en-US",
  "metadata": {
    "filename": "communication.aot",
    "category": "communication",
    "name": "Communication Intents",
    "description": "Email, messaging, and calling actions",
    "ontology_count": 3,
    "author": "AVA AI",
    "created_at": "2025-11-25T00:00:00Z"
  },
  "ontology": [
    {
      "id": "send_email",
      "canonical_form": "compose_and_send_email",
      "description": "User wants to compose and send an electronic message",
      "synonyms": [
        "send email",
        "compose email",
        "write email",
        "create email",
        "new email",
        "email someone",
        "send message",
        "send mail",
        "draft email"
      ],
      "action_type": "multi_step",
      "action_sequence": [
        {
          "step": 1,
          "action": "OPEN_APP",
          "capability_required": "email_client",
          "fallback": ["OPEN_BROWSER", "SHOW_ERROR"]
        },
        {
          "step": 2,
          "action": "COMPOSE_EMAIL",
          "intent_type": "ACTION_SENDTO",
          "intent_data": "mailto:",
          "extract_entities": ["recipient", "subject", "body"]
        }
      ],
      "required_capabilities": ["email_client"],
      "entity_schema": {
        "recipient": {
          "type": "PERSON",
          "patterns": ["to {recipient}", "send to {recipient}"],
          "optional": true
        },
        "subject": {
          "type": "TEXT",
          "patterns": ["about {subject}", "regarding {subject}"],
          "optional": true
        },
        "body": {
          "type": "TEXT",
          "patterns": ["saying {body}", "message {body}"],
          "optional": true
        }
      },
      "priority": 1,
      "tags": ["email", "communication", "compose", "send"]
    },
    {
      "id": "send_text_message",
      "canonical_form": "send_sms_message",
      "description": "User wants to send a text message",
      "synonyms": [
        "send text",
        "send sms",
        "text someone",
        "send a message",
        "message someone"
      ],
      "action_type": "multi_step",
      "action_sequence": [
        {
          "step": 1,
          "action": "OPEN_APP",
          "capability_required": "messaging"
        },
        {
          "step": 2,
          "action": "COMPOSE_MESSAGE",
          "intent_type": "ACTION_SENDTO",
          "intent_data": "sms:",
          "extract_entities": ["recipient", "body"]
        }
      ],
      "required_capabilities": ["messaging"],
      "entity_schema": {
        "recipient": {
          "type": "PERSON",
          "patterns": ["to {recipient}"],
          "optional": false
        },
        "body": {
          "type": "TEXT",
          "patterns": ["saying {body}"],
          "optional": true
        }
      },
      "priority": 1,
      "tags": ["sms", "messaging", "text", "send"]
    },
    {
      "id": "make_phone_call",
      "canonical_form": "initiate_phone_call",
      "description": "User wants to make a phone call",
      "synonyms": [
        "call",
        "phone",
        "dial",
        "call someone",
        "make a call",
        "ring"
      ],
      "action_type": "single_step",
      "action_sequence": [
        {
          "step": 1,
          "action": "INITIATE_CALL",
          "intent_type": "ACTION_DIAL",
          "intent_data": "tel:",
          "extract_entities": ["recipient"]
        }
      ],
      "required_capabilities": ["phone_dialer"],
      "entity_schema": {
        "recipient": {
          "type": "PERSON",
          "patterns": ["{recipient}", "call {recipient}"],
          "optional": false
        }
      },
      "priority": 1,
      "tags": ["phone", "call", "dial"]
    }
  ],
  "global_synonyms": {
    "send": ["compose", "write", "create", "draft", "start"],
    "email": ["message", "mail", "e-mail"],
    "call": ["phone", "dial", "ring"],
    "text": ["message", "sms"]
  },
  "capability_mappings": {
    "email_client": {
      "apps": [
        {
          "package": "com.google.android.gm",
          "name": "Gmail",
          "actions": ["COMPOSE", "SEND", "VIEW", "SEARCH"]
        },
        {
          "package": "com.microsoft.office.outlook",
          "name": "Outlook",
          "actions": ["COMPOSE", "SEND", "VIEW", "SEARCH"]
        }
      ],
      "intent_filters": [
        {
          "action": "ACTION_SENDTO",
          "data": "mailto:"
        }
      ]
    },
    "messaging": {
      "apps": [
        {
          "package": "com.google.android.apps.messaging",
          "name": "Messages",
          "actions": ["COMPOSE", "SEND", "VIEW"]
        }
      ],
      "intent_filters": [
        {
          "action": "ACTION_SENDTO",
          "data": "sms:"
        }
      ]
    },
    "phone_dialer": {
      "apps": [
        {
          "package": "com.android.dialer",
          "name": "Phone",
          "actions": ["DIAL", "CALL"]
        }
      ],
      "intent_filters": [
        {
          "action": "ACTION_DIAL",
          "data": "tel:"
        }
      ]
    }
  }
}
```

---

## Integration with Database

### Loading Flow

1. **Parse AON file (`.aot`)** → JSON object
2. **For each ontology entry:**
   - Insert into `semantic_intent_ontology` table
   - Compute mALBERT embedding from description + synonyms
   - Insert into `intent_embeddings` table
3. **Link via ontology_id** for fast lookups

### Runtime Flow

1. **User utterance** → mALBERT embedding
2. **Vector similarity search** in `intent_embeddings`
3. **Retrieve top-K intents**
4. **Lookup ontology** in `semantic_intent_ontology`
5. **Execute action sequence** via capability discovery

---

## File Location

AON files (`.aot` extension) stored in:

```
/assets/ontology/
  ├── en-US/
  │   ├── communication.aot
  │   ├── information.aot
  │   ├── navigation.aot
  │   └── system.aot
  ├── es-ES/
  │   ├── communication.aot
  │   └── ...
  └── fr-FR/
      └── ...
```

---

## Version History

- **2.0.0** (2025-11-25): Initial AON specification (`.aot` file extension)
  - Extends AVA 2.0 format
  - Adds semantic metadata
  - Supports mALBERT embeddings
  - Multi-step action sequences
  - Capability-based app discovery

---

## References

- [AVA Universal Format 2.0 Specification](./UNIVERSAL-FILE-FORMAT-SPEC.md)
- [mALBERT Model Documentation](https://huggingface.co/bert-base-multilingual-cased)
- [TVM 0.22 Tokenizer Integration](https://tvm.apache.org/)
