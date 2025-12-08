# User Manual: Chapter 17 - Smart Learning & Cloud AI Backup

**Version**: 1.0
**Date**: 2025-12-04

---

## How AVA Learns

AVA is designed to get smarter over time. The more you use her, the better she understands you.

### Automatic Learning

AVA automatically learns from your conversations:

1. **When you ask something new**, AVA remembers how you phrased it
2. **Next time you ask the same thing**, AVA responds instantly
3. **You don't need to do anything** - learning happens automatically

**Example:**
- First time: "Play some smooth jazz" (AVA thinks for a moment)
- Next time: "Play some smooth jazz" (AVA responds instantly)

### What AVA Learns

| Type | Examples |
|------|----------|
| Music commands | "play jazz", "put on some blues", "music please" |
| Smart home | "lights on", "dim the bedroom", "turn off everything" |
| Reminders | "remind me tomorrow", "don't let me forget" |
| Weather | "what's it like outside", "do I need an umbrella" |
| And more... | AVA learns YOUR way of speaking |

### When Learning Happens (NEW - Dec 2025)

AVA learns in the background to avoid slowing you down:

**Immediate Learning:**
- When battery is above 50%
- When you're actively chatting
- AVA remembers your phrase right away

**Background Learning:**
- When battery is below 50%
- When you're not actively using AVA
- Learning queued for later (won't drain battery)

**Smart Scheduling:**
- AVA waits for good battery levels
- Learning happens during charging
- Retries automatically if interrupted

**You'll never notice** - AVA handles all of this automatically!

---

## Battery-Smart AI

AVA is designed to preserve your phone's battery while still giving you great responses.

### How It Works

| Your Battery | What AVA Does |
|--------------|---------------|
| 50%+ | Uses your phone's AI (private, no internet needed) |
| 30-50% | Uses your phone's AI, but more efficiently |
| Below 30% | Uses Cloud AI backup (if configured) |
| Below 15% | Uses Cloud AI only (saves battery) |

### When Your Phone Gets Warm

If your phone feels warm, AVA automatically:
1. Switches to Cloud AI (if available)
2. Waits a moment for your phone to cool down
3. Shows you a gentle message

**You'll see**: "Device is warm. Using cloud backup..."

---

## Setting Up Cloud AI Backup (Optional)

Adding a Cloud AI backup ensures AVA always responds quickly, even when:
- Your battery is low
- Your phone is warm
- You want faster responses

**NEW (2025-12-05):** AVA now supports **4 major cloud providers** with automatic fallback!

### Step 1: Open AVA Settings

1. Open AVA
2. Tap the menu icon (three lines)
3. Tap **Settings**
4. Tap **Cloud AI Backup**

### Step 2: Choose a Provider

| Provider | Cost | Speed | Best For |
|----------|------|-------|----------|
| **OpenRouter** | Varies ($0.50-$15/1M tokens) | Very fast | Access to 100+ models |
| **Anthropic** | ~$3-$15/1M tokens | Fast | Claude 3.5 (most capable) |
| **Google AI** | ~$1.25-$7/1M tokens | Very fast | Gemini 1.5 (longest context) |
| **OpenAI** | ~$10-$60/1M tokens | Fast | GPT-4 Turbo |

**Recommendation:** Start with **OpenRouter** - it gives you access to 100+ models including Claude, GPT-4, and Gemini through a single API key.

### Step 3: Get Your API Key

#### For OpenRouter (Recommended):
1. Go to [openrouter.ai](https://openrouter.ai)
2. Sign up for account
3. Click **API Keys**
4. Click **Create Key**
5. Copy the key (starts with `sk-or-`)

**Cost:** Pay-as-you-go, most models $0.50-$5 per million tokens (very affordable!)

#### For Anthropic (Claude 3.5):
1. Go to [console.anthropic.com](https://console.anthropic.com)
2. Sign up for account
3. Click **API Keys**
4. Click **Create Key**
5. Copy the key (starts with `sk-ant-`)

**Cost:** ~$3-$15 per million tokens

#### For Google AI (Gemini):
1. Go to [ai.google.dev](https://ai.google.dev)
2. Click **Get API Key**
3. Create new key
4. Copy the key (starts with `AIza`)

**Cost:** Free tier available, then ~$1.25-$7 per million tokens

#### For OpenAI (GPT-4):
1. Go to [platform.openai.com](https://platform.openai.com)
2. Sign up and add payment method
3. Click **API Keys**
4. Click **Create new secret key**
5. Copy the key (starts with `sk-`)

**Cost:** ~$10-$60 per million tokens

### Step 4: Add Key to AVA

1. In AVA Settings > Cloud AI Backup
2. Select your provider (e.g., "Groq")
3. Paste your API key
4. Tap **Save**
5. Tap **Test Connection**

You should see: "Connected to Cloud AI"

---

## Cloud AI Status Indicators

AVA shows you when Cloud AI is being used:

| Indicator | Meaning |
|-----------|---------|
| No indicator | Using your phone's AI (normal, most private) |
| Cloud icon | Using Cloud AI backup |
| Warning icon | Cloud AI not available (check settings) |

### Automatic Fallback (NEW!)

AVA now tries multiple cloud providers automatically if one fails:

1. **Try OpenRouter** (if configured)
   - If fails → Try Anthropic
2. **Try Anthropic** (if configured)
   - If fails → Try Google AI
3. **Try Google AI** (if configured)
   - If fails → Try OpenAI
4. **Try OpenAI** (if configured)
   - If all fail → Use built-in templates (always works)

**You don't need to configure all providers** - AVA will use whichever ones you've set up!

---

## Privacy & Security

### What Stays on Your Phone

| Data | Location |
|------|----------|
| Your learned phrases | On your phone only |
| Conversation history | On your phone only |
| Settings | On your phone only |

### When Cloud AI is Used

| Data | What Happens |
|------|--------------|
| Your message | Sent securely (encrypted) |
| Response | Returned and not stored |
| Learning data | Stays on your phone |

**Important**: AVA learns from ALL your conversations, but Cloud AI providers don't store your data.

---

## Teaching AVA Manually

While AVA learns automatically, you can also teach her directly:

### Method 1: Long Press

1. Long-press any AVA response
2. Tap **"This was wrong"**
3. Tell AVA what you meant
4. AVA learns immediately

### Method 2: Say "Teach AVA"

1. Say: "Teach AVA"
2. AVA asks: "What would you like me to learn?"
3. Give an example: "When I say 'jazz time', play jazz music"
4. AVA confirms: "Got it! 'jazz time' means play music"

### Method 3: Settings

1. Open AVA Settings
2. Tap **Learning & Training**
3. Tap **Add New Phrase**
4. Enter the phrase and what it means

---

## Viewing What AVA Has Learned

To see what AVA has learned:

1. Open AVA Settings
2. Tap **Learning & Training**
3. Tap **View Learned Phrases**

You'll see:
- Phrases AVA learned automatically
- Phrases you taught manually
- How often each phrase is used

### Managing Learned Phrases

| Action | How |
|--------|-----|
| Delete a phrase | Swipe left, tap Delete |
| Edit a phrase | Tap the phrase, make changes |
| Clear all | Tap "Clear All Learning" (in Advanced) |

---

## Gesture & Cursor Commands

Some advanced commands require special permissions to work:

### Commands That Need Accessibility

| Command Type | Examples | Permission Required |
|--------------|----------|---------------------|
| Gestures | "swipe up", "swipe left" | Accessibility Service |
| Cursor | "move cursor up", "click" | Accessibility Service |
| Scrolling | "scroll down", "scroll to top" | Accessibility Service |

### Enabling Accessibility

When you try a gesture or cursor command without the permission enabled, AVA will show:

> "To use gesture and cursor commands, please enable AVA Accessibility Service in Settings."

**To enable:**
1. Open your phone's **Settings**
2. Tap **Accessibility**
3. Find **AVA Accessibility Service**
4. Toggle it **ON**
5. Tap **Allow** to confirm

### Basic Commands (No Special Permission)

These commands work immediately without any setup:

| Type | Examples |
|------|----------|
| WiFi | "turn on wifi", "wifi off" |
| Bluetooth | "bluetooth on", "turn off bluetooth" |
| Volume | "volume up", "mute", "max volume" |
| Media | "play music", "pause", "next song" |
| Apps | "open camera", "open settings" |
| Time/Weather | "what time is it", "weather today" |
| Reminders | "remind me at 5pm", "set alarm" |

---

## Troubleshooting

### "AVA doesn't understand me"

Try these:
1. Speak more clearly
2. Use simpler phrases
3. Teach AVA: say "Teach AVA" and give an example

### "Responses are slow"

Check:
1. Your battery level (low battery = slower)
2. Phone temperature (warm = slower)
3. Set up Cloud AI backup for faster responses

### "Cloud AI not working"

Check:
1. Internet connection
2. API key is correct
3. Provider account has credits

### "Gesture/cursor commands not working"

If you see: "Please enable AVA Accessibility Service in Settings"

1. Open phone **Settings**
2. Tap **Accessibility**
3. Find **AVA Accessibility Service**
4. Toggle it **ON**
5. Return to AVA and try again

### "AVA forgot what I taught her"

This is rare, but can happen if:
- App data was cleared
- App was reinstalled

To prevent: Enable **Backup Learning Data** in Settings

---

## Battery Tips

To maximize battery life while using AVA:

| Tip | Impact |
|-----|--------|
| Set up Cloud AI backup | Uses less battery when low |
| Charge regularly | AVA works best at 50%+ |
| Avoid direct sunlight | Prevents overheating |
| Close unused apps | More resources for AVA |

---

## FAQ

### Does AVA listen all the time?
No. AVA only processes audio when you activate her (tap or wake word).

### Is my data sent to the cloud?
Only when using Cloud AI backup, and only your current message.

### Can I use AVA offline?
Yes! AVA works completely offline. Cloud AI is optional.

### How much data does Cloud AI use?
About 1-2 KB per message (very small).

### Can I delete all my data?
Yes. Settings > Privacy > Clear All Data.

### Why does AVA get faster over time?
Because she learns your phrases and responds instantly to things she knows.

---

## Summary

| Feature | What It Does |
|---------|--------------|
| **Automatic Learning** | AVA learns your phrases automatically |
| **Battery-Smart** | Switches to cloud when battery low |
| **Cloud AI Backup** | Optional backup for faster responses |
| **Manual Teaching** | Teach AVA specific phrases |
| **Privacy First** | Learning stays on your phone |
| **Gesture Commands** | Swipe, scroll, cursor (needs Accessibility) |

---

**Need Help?**
- Say: "Help me with settings"
- Or visit: support.augmentalis.com

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.1 | 2025-12-06 | Updated with actual CloudLLMProvider implementation (4 providers, automatic fallback) |
| 1.0 | 2025-12-04 | Initial release |

---

**Related:**
- [Chapter 38 - LLM Model Management (Developer)](../Developer-Manual-Chapter38-LLM-Model-Management.md)
- [Chapter 73 - Production Readiness & Security (Developer)](../Developer-Manual-Chapter73-Production-Readiness-Security.md#cloudllmprovider-multi-backend-system)

---

**Updated:** 2025-12-06 (added multi-provider support with OpenRouter, Anthropic, Google AI, OpenAI)

---

**Created by Intelligent Devices LLC**
