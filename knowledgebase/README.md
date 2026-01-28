# NewAvanues Knowledgebase

Central repository for documentation, learnings, and external research for the NewAvanues ecosystem.

## Structure

```
knowledgebase/
├── modules/          # Per-module documentation
│   ├── VoiceOSCore.md
│   ├── WebAvanue.md
│   └── ...
├── external/         # External APIs, integrations
└── anthropic/        # Anthropic/Claude updates
```

## Modules Index

| Module | Description | Status |
|--------|-------------|--------|
| [VoiceOSCore](modules/VoiceOSCore.md) | Core voice processing engine | Active |
| [WebAvanue](modules/WebAvanue.md) | Web platform components | Active |
| [AI](modules/AI.md) | AI/ML integrations | Active |
| [AvaMagic](modules/AvaMagic.md) | Magic automation features | Active |
| [DeviceManager](modules/DeviceManager.md) | Device handling | Active |
| [SpeechRecognition](modules/SpeechRecognition.md) | Speech-to-text | Active |
| [Database](modules/Database.md) | Data persistence | Active |
| [UniversalRPC](modules/UniversalRPC.md) | Cross-platform RPC | Active |

## Adding Knowledge

1. Create/update markdown file in appropriate folder
2. Run RAG indexing: `curl -X POST http://localhost:3850/rag/index -d '{"path": "/path/to/file.md"}'`
3. Knowledge becomes searchable via `/i.research` and RAG queries

## Related

- [CodeAvanue Knowledgebase](/Volumes/M-Drive/Coding/CodeAvanue/knowledgebase/) - IDEACODE framework knowledge
- [Docs](/Volumes/M-Drive/Coding/NewAvanues/Docs/) - Detailed specifications
