# AVA AI - User Manual

**Version:** 1.0 (Phase 1.0 MVP)
**Last Updated:** 2025-11-21

---

## Welcome to AVA AI! 👋

AVA AI is your intelligent voice assistant that respects your privacy. Unlike other assistants, AVA runs primarily on your device, keeping your conversations and data private and secure.

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Features](#features)
3. [Using AVA](#using-ava)
4. [Teach AVA](#teach-ava)
5. [Settings](#settings)
6. [Privacy & Data](#privacy--data)
7. [Troubleshooting](#troubleshooting)
8. [FAQ](#faq)

---

## Getting Started

### First Launch

When you first open AVA, you'll go through a quick onboarding process:

1. **Welcome Screen** - Learn about AVA's features
2. **Privacy Policy** - Understand how your data is protected
3. **Data Collection** - Choose whether to share crash reports and analytics (optional)
4. **Feature Overview** - See what AVA can do
5. **Get Started** - Start chatting!

### Privacy-First Design

AVA is built with privacy at its core:
- ✅ All processing happens on your device
- ✅ Your conversations never leave your phone (unless you choose cloud providers)
- ✅ All data is encrypted and stored securely
- ✅ You can export or delete your data anytime

---

## Features

### 1. Smart Conversations 💬

Chat naturally with AVA and get intelligent responses.

**What you can do:**
- Ask questions
- Get help with tasks
- Have contextual conversations
- Switch between on-device and cloud AI models

**Example conversations:**
```
You: What's the weather like?
AVA: I can help with that! Let me check the weather for you.

You: Turn on the lights
AVA: I'll help you control your smart lights.

You: Tell me a joke
AVA: Why did the developer go broke? Because he used up all his cache!
```

### 2. Teach AVA 🎓

Train AVA to understand your custom commands and intents.

**Why teach AVA?**
- Customize AVA to your needs
- Add domain-specific commands
- Improve AVA's understanding over time

**How it works:**
1. AVA encounters a phrase it doesn't understand well
2. You teach AVA what that phrase means
3. AVA learns and remembers for next time

### 3. Choose Your AI Model 🤖

AVA supports multiple AI models:

**On-Device (Free, Private):**
- Gemma 2B - Fast, private, no internet required
- Perfect for basic conversations and commands

**Cloud Providers (Requires API key):**
- **Claude (Anthropic)** - Most intelligent, great for complex tasks
- **GPT-4 (OpenAI)** - Versatile and powerful
- **Gemini (Google)** - Extended context, multimodal
- **OpenRouter** - Access to 100+ models
- **HuggingFace** - Open-source models

**Cost comparison (per 1M tokens):**
| Provider | Input | Output | Speed | Privacy |
|----------|-------|--------|-------|---------|
| Gemma 2B (Local) | Free | Free | Fast | 🔒 Best |
| GPT-3.5 | $0.50 | $1.50 | Very Fast | ☁️ Cloud |
| Claude 3.5 Sonnet | $3 | $15 | Fast | ☁️ Cloud |
| GPT-4 | $30 | $60 | Medium | ☁️ Cloud |

### 4. Document Chat (RAG) 📚

Chat with your documents using retrieval-augmented generation.

**Supported formats:**
- PDF
- Word (DOCX)
- Text (TXT)
- Markdown (MD)
- HTML
- RTF

**How to use:**
1. Upload your document
2. Ask questions about it
3. Get accurate answers with citations

---

## Using AVA

### Starting a Conversation

1. Open AVA AI
2. Type or speak your message in the input field
3. Press Send or hit Enter
4. AVA will respond in real-time

### Confidence Indicators

AVA shows how confident it is about understanding you:

- 🟢 **High Confidence (>80%)** - AVA understands well
- 🟡 **Medium Confidence (50-80%)** - AVA is fairly sure
- 🔴 **Low Confidence (<50%)** - AVA suggests teaching

### When AVA Doesn't Understand

If AVA has low confidence:
1. AVA will suggest teaching it
2. Tap "Teach AVA"
3. Select or enter the correct intent
4. AVA learns for next time!

---

## Teach AVA

### Adding Training Examples

**Step 1:** Go to Teach AVA screen
1. Tap the menu icon (☰)
2. Select "Teach AVA"

**Step 2:** Add an example
1. Tap the "+" button
2. Enter what you said
3. Choose or create an intent
4. Save

**Step 3:** Repeat for variations
- Add multiple ways to say the same thing
- AVA learns from patterns

### Best Practices

**DO:**
- ✅ Add multiple variations of the same command
- ✅ Use natural language (how you actually talk)
- ✅ Be specific with intents

**DON'T:**
- ❌ Add duplicate examples
- ❌ Use overly formal language
- ❌ Mix different intents in one example

### Example Training Set

```
Intent: home.lights.on
Examples:
- "turn on the lights"
- "lights on"
- "turn the lights on please"
- "can you turn on the lights"
- "I need the lights on"

Intent: home.lights.off
Examples:
- "turn off the lights"
- "lights off"
- "turn the lights off"
- "shut off the lights"
```

---

## Settings

### Access Settings

Tap the menu icon (☰) → Settings

### Available Settings

#### Natural Language Understanding
- **Enable NLU**: Toggle on-device intent classification
- **Confidence Threshold**: Minimum confidence for automatic responses (50-95%)

#### Language Model
- **LLM Provider**: Choose between:
  - Local (On-Device) - Gemma 2B
  - Anthropic (Claude)
  - OpenRouter (100+ models)
  - OpenAI (GPT-4, GPT-3.5)
  - HuggingFace (Open models)
  - Google AI (Gemini)
- **Enable Streaming**: Real-time response generation
- **API Keys**: Enter your cloud provider API keys

#### Chat Preferences
- **Conversation Mode**:
  - Append: Continue in same conversation
  - New: Start fresh conversation each time

#### Privacy & Data
- **Crash Reporting**: Send anonymous crash reports (opt-in)
- **Analytics**: Share anonymous usage statistics (opt-in)
- **Data Export**: Export all your data
- **Data Delete**: Permanently delete all data

#### About
- Version information
- Credits
- Open source licenses

---

## Privacy & Data

### What Data Does AVA Collect?

**By Default (On-Device Only):**
- Your conversations (stored locally)
- Training examples you add
- App preferences
- Usage statistics (local only)

**Optional (If You Enable):**
- Anonymous crash reports
- Anonymous usage analytics

**Never Collected:**
- Personal identifiable information
- Your API keys (stored securely on device)
- Conversations sent to cloud (unless you use cloud providers)

### Data Storage

All data is stored encrypted on your device:
- Location: `/data/data/com.augmentalis.ava/`
- Encryption: Android Keystore
- Backup: Excluded from cloud backups

### Exporting Your Data

1. Settings → Privacy & Data
2. Tap "Export Data"
3. Choose export format (JSON/CSV)
4. Save to your preferred location

**Exported data includes:**
- All conversations
- Training examples
- Settings and preferences

### Deleting Your Data

1. Settings → Privacy & Data
2. Tap "Delete All Data"
3. Confirm deletion
4. All data is permanently erased

**Warning:** This cannot be undone!

---

## Troubleshooting

### AVA Isn't Understanding Me

**Solutions:**
1. Teach AVA the phrase (Teach AVA screen)
2. Check NLU is enabled (Settings → NLU)
3. Try rephrasing your request
4. Add more training examples

### Slow Responses

**On-Device (Gemma 2B):**
- First response may be slow (loading model)
- Subsequent responses should be faster
- Check available RAM (need ~2GB free)

**Cloud Providers:**
- Check internet connection
- Verify API key is correct
- Some models are slower than others

### App Crashes

1. Enable crash reporting (Settings → Privacy & Data)
2. Reproduce the crash
3. Report via GitHub issues
4. Include crash report ID

### Model Not Loading

**Local Model (Gemma 2B):**
1. Check storage space (need ~2GB free)
2. Clear app cache
3. Reinstall model from Settings

**Cloud Models:**
1. Verify API key
2. Check API balance/limits
3. Try different provider

---

## FAQ

### Is AVA really private?

Yes! By default, AVA processes everything on your device. Your conversations never leave your phone unless you explicitly choose to use cloud providers.

### Do I need internet?

**For on-device mode:** No! AVA works completely offline with the Gemma 2B model.

**For cloud providers:** Yes, you need internet to access OpenAI, Claude, etc.

### How much does it cost?

**AVA App:** Free and open source

**On-Device (Gemma 2B):** Completely free, no limits

**Cloud Providers:** Varies by provider
- Free tier: HuggingFace
- Paid: OpenAI, Claude, Gemini (you pay provider directly)

### Can AVA control my smart home?

Yes! Through the intent system, AVA can trigger smart home actions. You'll need to:
1. Teach AVA your smart home commands
2. Configure intent actions (requires developer setup)

### What languages does AVA support?

Currently: English (en-US)

Planned: Multi-language support in future releases

### Can I use AVA without training it?

Yes! AVA comes with pre-trained intents for common tasks. However, teaching AVA makes it much more personalized and accurate.

### Is my data backed up?

By default, AVA data is **excluded** from cloud backups for privacy. You can manually export your data using Settings → Privacy & Data → Export Data.

### Can I use multiple AI models?

Yes! AVA supports cascading fallback - if one provider fails, it automatically tries the next.

### How do I get API keys?

**OpenRouter:** https://openrouter.ai
**Anthropic:** https://console.anthropic.com
**OpenAI:** https://platform.openai.com
**Google AI:** https://ai.google.dev
**HuggingFace:** https://huggingface.co

### Is AVA open source?

Yes! AVA AI is open source. You can view the code, contribute, and build your own version.

---

## Getting Help

### Support Channels

- **GitHub Issues**: Report bugs and request features
- **Documentation**: `/docs/` directory
- **Community**: Join our Discord/Slack (coming soon)

### Reporting Issues

When reporting issues, include:
1. AVA version (Settings → About)
2. Device model and Android version
3. Steps to reproduce
4. Screenshots (if applicable)
5. Crash report ID (if crash occurred)

---

## What's Next?

### Upcoming Features (Phase 1.1)

- Multi-turn conversation context
- Voice input and output
- Conversation history browsing
- Dark mode
- More language support

### Roadmap

- **Phase 2**: RAG improvements, document management
- **Phase 3**: Constitutional AI, advanced context
- **Phase 4**: Cross-platform (iOS, Desktop)
- **Phase 5**: Smart glasses integration
- **Phase 6**: Enterprise features

---

## Credits

**AVA AI** is developed by the AVA AI Team

**Open Source Libraries:**
- Jetpack Compose (Google)
- ONNX Runtime (Microsoft)
- MLC-LLM (MLC Community)
- Room Database (Google)
- OkHttp (Square)

**AI Models:**
- MobileBERT (Google)
- Gemma 2B (Google)

---

**Thank you for using AVA AI!** 🎉

We hope you enjoy your privacy-first AI assistant. If you have feedback or suggestions, please let us know!

---

**Version:** 1.0 (Phase 1.0 MVP)
**Last Updated:** 2025-11-21
**License:** Proprietary (Open Source Coming Soon)
