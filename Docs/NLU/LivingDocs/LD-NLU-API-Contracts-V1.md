# LD-NLU-API-Contracts-V1

**Living Document** | NLU API Contracts
**Version:** 1.0 | **Created:** 2025-12-15 | **Status:** Active

---

## API Overview

NLU provides natural language processing APIs for intent and entity extraction.

---

## Core APIs

### Process Text
```python
def process_text(text: str, context: Dict) -> NLUResult:
    """
    Process natural language text.

    Args:
        text: Input text
        context: Conversation context

    Returns:
        NLUResult with intent and entities
    """
    pass
```

### NLU Result
```python
@dataclass
class NLUResult:
    intent: str
    confidence: float
    entities: Dict[str, Any]
    context: Dict[str, Any]
```

---

## Intent Classification

```python
def classify_intent(text: str) -> IntentResult:
    """
    Classify user intent from text.

    Returns:
        IntentResult(intent, confidence)
    """
    pass
```

---

## Entity Extraction

```python
def extract_entities(text: str) -> List[Entity]:
    """
    Extract named entities from text.

    Returns:
        List of Entity objects
    """
    pass

@dataclass
class Entity:
    type: str  # person, location, date, etc.
    value: str
    position: Tuple[int, int]
    confidence: float
```

---

## Kotlin Interop

```kotlin
// Kotlin interface for NLU service
interface NLUService {
    suspend fun processText(
        text: String,
        context: Map<String, Any>
    ): NLUResult
}
```

---

**Last Updated:** 2025-12-15 | **Version:** 12.0.0
