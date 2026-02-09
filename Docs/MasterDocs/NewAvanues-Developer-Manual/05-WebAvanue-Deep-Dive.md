---
title: "Chapter 05 — WebAvanue Deep Dive"
owner: "Web Platform"
status: "active"
last_reviewed: "2026-02-09"
source_of_truth: true
---

# Chapter 05 — WebAvanue Deep Dive

## 5.1 Purpose

WebAvanue is the voice-enabled browser surface that allows VoiceOS-style interaction over web content by mapping speech intent to DOM element actions.

## 5.2 Structural Model

WebAvanue is composed around:
- shared KMP browser logic,
- platform render layer (Android now, iOS/desktop phased),
- data persistence layer,
- VoiceOS integration bridge.

## 5.3 Voice Execution Path on Web

```text
Utterance
  -> intent classification
  -> DOM scrape for interactive elements
  -> VUID generation per candidate
  -> fuzzy/semantic match against element metadata
  -> execute click/input/scroll on resolved selector
  -> feedback + ambiguity handling
```

## 5.4 Key Components

| Component | Role |
|---|---|
| `DOMScraperBridge` | Extract DOM interactive elements + metadata |
| `VoiceCommandGenerator` | Match utterances to candidate elements |
| `VoiceOSWebCallback` | Contract between browser layer and voice engine |
| Browser data managers | Persist tab/history/bookmark state |

## 5.5 Why WebAvanue Matters to Platform Vision

Without WebAvanue, voice-first is limited to native surfaces. WebAvanue extends the “any app, any time” commitment to browser-driven workflows.

## 5.6 Ambiguity and Disambiguation on Web

Web pages frequently contain many similar controls (“login”, “submit”, “continue”).

Recommended disambiguation policy:
1. rank by visibility + semantic fit,
2. if confidence low, request clarification,
3. preserve safe fallback (do not blindly click).

## 5.7 Data and Security Notes

- Local persistence for browser entities (tabs/history/bookmarks).
- Encryption-at-rest strategy where applicable.
- Keep command execution and DOM actions auditable for debugging.

## 5.8 Developer Extension Points

1. Add richer attribute extraction in DOM scraper.
2. Improve ranking model in command generator.
3. Add custom command packs for domain-heavy web apps.
4. Add telemetry hooks for false-positive/false-negative tuning.

## 5.9 Performance Checklist

- [ ] DOM scrape bounded by viewport scope when possible
- [ ] Avoid unnecessary full-tree rescans
- [ ] Cache candidate maps by page state fingerprint
- [ ] Keep command resolution under interactive latency budget
