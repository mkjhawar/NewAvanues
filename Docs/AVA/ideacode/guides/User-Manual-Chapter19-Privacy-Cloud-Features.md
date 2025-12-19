# User Manual Chapter 19 - Privacy & Cloud Features

**What's New in AVA v10.3**

Welcome to AVA's biggest privacy and flexibility update yet! This chapter explains the new features that give you more control over your data while unlocking powerful cloud AI capabilities.

---

## Table of Contents

1. [Introduction](#introduction)
2. [Privacy & Security Features](#privacy--security-features)
3. [Cloud AI Features](#cloud-ai-features)
4. [Common Questions](#common-questions)
5. [Recommendations](#recommendations)
6. [How to Check What's Enabled](#how-to-check-whats-enabled)
7. [Getting Help](#getting-help)
8. [Related Chapters](#related-chapters)

---

## Introduction

### What's New in This Update

AVA v10.3 introduces three major improvements:

1. **Optional Crash Reporting** - Help us fix bugs (with your permission)
2. **Encrypted Document Storage** - Military-grade protection for your documents
3. **Cloud AI Integration** - Access powerful AI models when you need them

### Privacy-First Approach

**Your privacy is our top priority.** Here's what that means:

- ✅ **Everything is optional** - You choose what features to enable
- ✅ **Local by default** - AVA works completely on your device
- ✅ **Encrypted storage** - Your documents are always protected
- ✅ **No tracking** - We don't collect data without permission
- ✅ **Transparent choices** - We explain exactly what each feature does

### Optional Cloud Features

Think of AVA like a car with two engines:

- **Local AI Engine**: Always available, runs on your phone, completely private
- **Cloud AI Engine**: Optional, runs on the internet, faster and smarter

You can use both, just one, or switch between them. AVA adapts to your choices.

---

## Privacy & Security Features

### 2.1 Crash Reporting (Optional)

#### What It Does

When AVA crashes or encounters a bug, crash reporting automatically sends technical information to help developers fix the problem.

#### What's Collected

**Only technical data:**
- Device model (e.g., "Samsung Galaxy S21")
- Android version (e.g., "Android 14")
- AVA version (e.g., "10.3")
- Error logs (what went wrong in the code)

**Never collected:**
- ❌ Your conversations with AVA
- ❌ Personal documents or files
- ❌ Contacts, messages, or emails
- ❌ Location data
- ❌ Any identifiable information

#### How to Enable/Disable

```
Settings → Privacy → Crash Reports → Toggle ON/OFF
```

**Screenshot guide:**

1. Open AVA
2. Tap the menu icon (☰)
3. Select "Settings"
4. Scroll to "Privacy"
5. Find "Crash Reports"
6. Toggle the switch

#### Privacy Guarantee

- ✅ **Disabled by default** - We never enable this without asking
- ✅ **Requires permission** - You must actively turn it on
- ✅ **Can disable anytime** - Change your mind? Turn it off instantly
- ✅ **Anonymous** - No personal data, no tracking

#### Should You Enable It?

**Enable if:**
- You want to help improve AVA
- You don't mind sending anonymous technical data
- You trust the development team

**Keep disabled if:**
- You prefer maximum privacy
- You're on a metered connection (saves data)
- You don't want any data leaving your device

---

### 2.2 Encrypted Document Storage

#### What It Does

AVA automatically encrypts all your documents and AI-related files using military-grade encryption. This protects your privacy even if someone gets physical access to your phone.

#### How It Works

**Completely automatic:**
1. You use AVA normally
2. AVA encrypts files in the background
3. You never notice - everything just works

**No setup required:**
- No passwords to remember
- No keys to manage
- No settings to configure

#### What's Encrypted

AVA encrypts three types of data:

1. **Document Embeddings**
   - What: The AI's "understanding" of your documents
   - Example: When you add a PDF to AVA, it creates a searchable index
   - Why encrypt: These embeddings contain your document content

2. **Cached Model Files**
   - What: AI model files stored on your device
   - Example: Downloaded language models
   - Why encrypt: Prevents tampering or theft of AI models

3. **Personal Document Content**
   - What: The actual text from your documents
   - Example: PDFs, notes, or files you've added to AVA
   - Why encrypt: Your most sensitive data deserves protection

#### Technical Details (For Curious Users)

**Encryption Standard:**
- **Algorithm**: AES-256-GCM
- **Key Storage**: Android Keystore (hardware-backed)
- **Same security as**: Banking apps, government systems

**What This Means:**
- ✅ Military-grade encryption (AES-256)
- ✅ Keys stored in secure hardware chip
- ✅ Even if phone is stolen, data is unreadable
- ✅ Automatic key rotation for extra security

#### Performance Impact

**The good news:** Encryption is incredibly fast on modern phones.

| Phone Age | Performance Impact |
|-----------|-------------------|
| 2023+ phones | <1% slower (unnoticeable) |
| 2020-2022 phones | ~2-3% slower (barely noticeable) |
| 2018-2019 phones | ~5% slower (slight delay on large files) |
| Pre-2018 phones | ~10% slower (may notice on very large documents) |

**Average user experience:**
- Opening documents: No difference
- Searching: No difference
- Adding new files: Tiny delay (milliseconds)

#### Can I Disable It?

**No - and here's why:**

Encryption is **always on** for your protection. We made this decision because:

1. **Privacy should be default**, not optional
2. **No performance penalty** on modern phones
3. **Protects everyone** automatically
4. **Prevents accidents** (forgetting to enable it)

If you're concerned about performance, don't worry - it's negligible.

#### How to Verify It's Working

```
Settings → About → Security Status
```

Look for:
```
Document Encryption: ✅ Active (AES-256)
Encrypted Files: 147
Encryption Key: Secure Hardware
Last Encryption Check: 2 minutes ago
```

---

### 2.3 App Default Preferences

#### What It Does

AVA remembers which apps you prefer for common tasks, so you don't have to choose every time.

#### Example Scenario

**Without default preferences:**
```
You: "Send an email to John about the meeting"
AVA: "Which email app? Gmail, Outlook, Yahoo Mail?"
You: "Gmail"
[Next time...]
You: "Send an email to Sarah"
AVA: "Which email app? Gmail, Outlook, Yahoo Mail?"
You: "Gmail" (again...)
```

**With default preferences:**
```
You: "Send an email to John about the meeting"
AVA: "Which email app? Gmail, Outlook, Yahoo Mail?"
You: "Gmail, and remember this choice"
AVA: "Got it! Using Gmail for emails from now on."
[Next time...]
You: "Send an email to Sarah"
AVA: [Opens Gmail automatically]
```

#### Supported App Types

AVA can remember your preferences for:

| Task Type | Example Apps |
|-----------|--------------|
| Email | Gmail, Outlook, Yahoo Mail, ProtonMail |
| Calendar | Google Calendar, Outlook, Samsung Calendar |
| Maps | Google Maps, Waze, Apple Maps |
| Music | Spotify, YouTube Music, Apple Music |
| Browser | Chrome, Firefox, Edge, Brave |
| Notes | Google Keep, OneNote, Evernote |
| Phone Calls | Default dialer, WhatsApp, Skype |
| Messages | SMS, WhatsApp, Telegram, Signal |

#### How to Set Default Apps

**Method 1: During Task**
1. Ask AVA to do something (e.g., "Open my calendar")
2. AVA shows available apps
3. Tap your preferred app
4. Check "Remember this choice"
5. Done!

**Method 2: In Settings**
```
Settings → Default Apps → Select Category → Choose App
```

**Step-by-step:**
1. Open AVA Settings
2. Scroll to "Default Apps"
3. See list of categories (Email, Calendar, etc.)
4. Tap a category
5. Choose your preferred app
6. Tap "Save"

#### How to Change Default Apps

**Option 1: Modify Existing**
```
Settings → Default Apps → [Category] → Change Selection
```

**Option 2: Reset Everything**
```
Settings → Default Apps → Reset All Preferences
```

**Warning:** Resetting clears all your choices. AVA will ask again for each task.

#### Privacy Implications

**What's stored:**
- ✅ App package names (e.g., "com.google.android.gm" for Gmail)
- ✅ Preference categories (e.g., "Email")
- ✅ Selection timestamps (when you chose it)

**What's NOT stored:**
- ❌ Content of emails, messages, or tasks
- ❌ Login credentials
- ❌ Personal data from those apps

**Where it's stored:**
- ✅ Locally on your device (encrypted)
- ❌ Never sent to the cloud

#### Benefits

**Time Savings:**
- Before: 3-5 taps per task (choose app every time)
- After: 0 taps (automatic)
- **Estimated savings**: 30-50 taps per day

**Better Experience:**
- No repetitive choices
- Faster task completion
- More natural conversation flow

---

## Cloud AI Features (Optional)

### 3.1 What is Cloud AI?

#### Understanding the Difference

AVA offers two types of artificial intelligence:

**Local AI (On-Device)**
- Runs entirely on your phone
- Uses your phone's processor
- Completely private (never leaves your device)
- Works offline
- Free forever

**Cloud AI (Internet-Based)**
- Runs on powerful servers
- Uses internet connection
- Data sent to cloud provider
- Requires internet
- Pay-per-use pricing

#### Think of It Like This

**Local AI** = Cooking at home
- You control everything
- Uses your kitchen (phone)
- Private
- Takes more time
- Free

**Cloud AI** = Ordering from a restaurant
- Professional chefs (powerful servers)
- Someone else's kitchen (cloud)
- You share your order (data)
- Fast delivery
- Costs money

Both are great - you choose based on your needs!

---

### 3.2 Why Use Cloud AI?

#### Feature Comparison

| Feature | Local AI | Cloud AI |
|---------|----------|----------|
| **Privacy** | ✅ Stays on device | ⚠️ Sent to cloud provider |
| **Cost** | ✅ Free forever | 💰 Pay per question |
| **Speed** | ⚠️ Slower on older phones | ✅ Always fast |
| **Offline** | ✅ Works offline | ❌ Needs internet |
| **Intelligence** | ⚠️ Good | ✅ Excellent |
| **Battery Usage** | ⚠️ Uses battery | ✅ Saves battery |
| **Model Options** | ⚠️ Limited by phone storage | ✅ 100+ models available |
| **Updates** | ⚠️ Manual download | ✅ Automatic |

#### When to Use Each

**Use Local AI for:**
- ✅ Quick questions ("What's the weather?")
- ✅ Private conversations
- ✅ Offline situations (airplane, no signal)
- ✅ When battery is low
- ✅ When you want zero cost

**Use Cloud AI for:**
- ✅ Complex reasoning ("Analyze this contract")
- ✅ Long conversations
- ✅ Creative writing
- ✅ Code generation
- ✅ When speed matters
- ✅ When you need the best quality

**Examples:**

| Question | Best Choice | Why |
|----------|-------------|-----|
| "Set a timer for 10 minutes" | Local AI | Simple, fast enough |
| "Summarize this 50-page PDF" | Cloud AI | Complex, needs power |
| "What's 15% of 80?" | Local AI | Quick math |
| "Write a professional email about..." | Cloud AI | Better writing quality |
| "What's in my calendar today?" | Local AI | Private info |
| "Explain quantum physics simply" | Cloud AI | Complex topic |

---

### 3.3 Supported Cloud Providers

AVA supports four major cloud AI providers. Here's what makes each unique:

#### 1. OpenRouter ⭐ Recommended for Most Users

**What it is:** A gateway to 100+ AI models from different companies

**Pros:**
- ✅ Access to many models (Claude, GPT-4, Gemini, Llama, etc.)
- ✅ One API key for everything
- ✅ Automatic failover (if one model is down, tries another)
- ✅ Competitive pricing
- ✅ Free trial credits

**Cons:**
- ⚠️ Requires understanding which model to use
- ⚠️ Pricing varies by model

**Best for:**
- Users who want flexibility
- Power users who like options
- Developers

**Pricing:**
- Free tier: $1 in credits (enough for ~100-1000 questions)
- Pay-as-you-go: $0.0001-0.03 per question (depends on model)

**Website:** https://openrouter.ai

---

#### 2. Claude (Anthropic) ⭐ Best Quality

**What it is:** Anthropic's advanced AI, known for thoughtful responses

**Pros:**
- ✅ Excellent conversation quality
- ✅ Great at reasoning and analysis
- ✅ Strong safety and ethics
- ✅ Handles long contexts well
- ✅ Reliable and consistent

**Cons:**
- ⚠️ More expensive than some alternatives
- ⚠️ Can be slower during peak times

**Best for:**
- Quality-focused users
- Complex questions
- Professional use
- Long conversations

**Pricing:**
- Model: Claude Opus 4.5
- Cost: ~$0.003-0.015 per question (depends on length)
- Free tier: No (paid only)

**Website:** https://www.anthropic.com

---

#### 3. Gemini (Google) ⭐ Best Value

**What it is:** Google's AI, integrated with Google services

**Pros:**
- ✅ Very affordable
- ✅ Fast responses
- ✅ Good at factual questions
- ✅ Integrates with Google services
- ✅ Generous free tier

**Cons:**
- ⚠️ Less creative than Claude
- ⚠️ Occasional generic responses

**Best for:**
- Budget-conscious users
- Quick questions
- Factual information
- Google ecosystem users

**Pricing:**
- Free tier: 60 questions per minute
- Paid: ~$0.0001-0.001 per question
- **Cheapest option**

**Website:** https://ai.google.dev

---

#### 4. ChatGPT (OpenAI) ⭐ Most Popular

**What it is:** OpenAI's famous ChatGPT, the most well-known AI

**Pros:**
- ✅ Very capable
- ✅ Great for creative tasks
- ✅ Excellent code generation
- ✅ Large community knowledge base
- ✅ Regular updates

**Cons:**
- ⚠️ Can be expensive (GPT-4)
- ⚠️ Usage limits on free tier
- ⚠️ Sometimes too verbose

**Best for:**
- General users
- Creative writing
- Programming help
- Popular brand recognition

**Pricing:**
- GPT-3.5: ~$0.001 per question (cheap)
- GPT-4: ~$0.01-0.03 per question (expensive)
- Free tier: Available but limited

**Website:** https://platform.openai.com

---

#### Quick Comparison

| Provider | Quality | Speed | Cost | Best Use |
|----------|---------|-------|------|----------|
| OpenRouter | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | Flexibility |
| Claude | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | Quality |
| Gemini | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | Budget |
| ChatGPT | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | Popularity |

**Our recommendation:**
- Start with **Gemini** (free tier)
- Upgrade to **OpenRouter** (flexibility)
- Use **Claude** for important tasks

---

### 3.4 Setting Up Cloud AI

#### Step 1: Get an API Key

Each provider requires an API key (like a password for accessing their AI).

**For OpenRouter:**

1. Visit https://openrouter.ai
2. Click "Sign Up"
3. Create account (email + password)
4. Verify your email
5. Go to "Keys" in dashboard
6. Click "Create Key"
7. Name it "AVA" (helps you remember)
8. Copy the key (looks like: `sk-or-v1-abc123...`)
9. **Important:** Save this key somewhere safe - you can't see it again!

**For Claude:**

1. Visit https://console.anthropic.com
2. Click "Get API Keys"
3. Sign up for account
4. Add payment method (required for paid API)
5. Go to "API Keys"
6. Click "Create Key"
7. Name it "AVA"
8. Copy the key (looks like: `sk-ant-api03-abc123...`)

**For Gemini:**

1. Visit https://ai.google.dev
2. Click "Get API Key"
3. Sign in with Google account
4. Click "Create API Key"
5. Copy the key (looks like: `AIzaSyAbc123...`)
6. **Note:** Free tier available!

**For ChatGPT:**

1. Visit https://platform.openai.com
2. Sign up for OpenAI account
3. Go to "API Keys"
4. Click "Create new secret key"
5. Name it "AVA"
6. Copy the key (looks like: `sk-abc123...`)

---

#### Step 2: Add to AVA

Once you have an API key:

```
Settings → Cloud AI → Add Provider → Select Provider → Paste Key → Save
```

**Detailed steps:**

1. Open AVA
2. Tap menu (☰)
3. Select "Settings"
4. Scroll to "Cloud AI"
5. Tap "Add Provider"
6. Select provider from list:
   - OpenRouter
   - Claude
   - Gemini
   - ChatGPT
7. Paste your API key (long tap → Paste)
8. Tap "Verify Key" (AVA tests it)
9. If valid: ✅ "Key verified successfully"
10. Tap "Save"

**Troubleshooting:**

❌ **"Invalid API key"**
- Check for extra spaces
- Make sure you copied the full key
- Verify key is active on provider's website

❌ **"Connection failed"**
- Check internet connection
- Verify provider's service is online
- Try again in a few minutes

❌ **"Quota exceeded"**
- You've hit your free tier limit
- Add payment method on provider's website
- Or wait until quota resets

---

#### Step 3: Set Cost Limits (Recommended)

**Why set limits?**
Cloud AI costs money. Limits prevent unexpected bills.

```
Settings → Cloud AI → Cost Limits
```

**Default limits:**
- Daily: $5.00
- Monthly: $50.00

**How to customize:**

1. Go to Settings → Cloud AI → Cost Limits
2. See current limits
3. Tap "Daily Limit"
4. Enter new amount (e.g., $2.00)
5. Tap "Monthly Limit"
6. Enter new amount (e.g., $20.00)
7. Tap "Save"

**Recommended limits by usage:**

| User Type | Daily Limit | Monthly Limit |
|-----------|-------------|---------------|
| Light user (few questions/day) | $1-2 | $10-20 |
| Regular user (10-20 questions/day) | $3-5 | $30-50 |
| Heavy user (50+ questions/day) | $10-20 | $100-200 |
| Power user (unlimited) | $20-50 | $500+ |

**What happens when you hit the limit?**

1. AVA shows notification: "Daily cloud AI limit reached"
2. Automatically switches to Local AI
3. You can still use AVA (just local AI only)
4. Limit resets at midnight (daily) or month start (monthly)

**Can I override the limit?**

Yes, but only manually:

```
Settings → Cloud AI → Cost Limits → Override Protection → Enable
```

Then AVA will ask before exceeding limits:
```
"You've reached your daily limit of $5.00. Continue with cloud AI?
This will cost approximately $0.15 more."
[Continue] [Use Local AI]
```

---

### 3.5 How AVA Chooses AI

AVA is smart about when to use Local vs. Cloud AI. Here's the decision process:

#### Automatic Selection

**AVA considers:**
1. ✅ Internet connection (required for cloud)
2. ✅ Battery level (cloud uses less battery)
3. ✅ Question complexity (cloud is smarter)
4. ✅ Cost limits (are you under your limit?)
5. ✅ User preferences (have you set a default?)
6. ✅ Provider availability (is the cloud service up?)

#### Decision Table

| Scenario | AVA Uses | Reason |
|----------|----------|--------|
| Offline | Local AI only | No internet |
| Battery < 15% | Local AI | Save battery for phone |
| Battery < 5% | Local AI (minimal model) | Critical battery |
| Simple question + online | Local AI | Fast enough locally |
| Complex question + online | Cloud AI (if enabled) | Better quality |
| Cost limit reached | Local AI | Prevent overspending |
| Cloud provider down | Next provider in list | Automatic failover |
| User preference set | Preferred AI | Respect user choice |
| No preference + complex | Claude/OpenRouter | Best quality |

#### Example Scenarios

**Scenario 1: Simple Question**
```
You: "What's 25% of 400?"
AVA thinks:
- Simple math calculation
- Local AI can handle this
- Why use cloud? (costs money)
Decision: ✅ Local AI
Response time: 0.5 seconds
Cost: $0.00
```

**Scenario 2: Complex Question**
```
You: "Analyze this contract and summarize the key terms, risks, and recommendations"
AVA thinks:
- Complex analysis required
- Long document
- Need high quality output
- Battery: 60% (good)
- Internet: Connected
- Cost limit: $2.50 used of $5.00 daily (room available)
Decision: ✅ Cloud AI (Claude)
Response time: 3 seconds
Cost: ~$0.08
```

**Scenario 3: Low Battery**
```
You: "Write a detailed blog post about..."
AVA thinks:
- Complex task (normally cloud AI)
- BUT battery: 12% (critical)
- Using cloud saves battery... BUT
- If cloud fails, phone might die
Decision: ✅ Local AI (safe choice)
Response time: 15 seconds
Cost: $0.00
Note: "Using local AI due to low battery. Charge phone for faster responses."
```

**Scenario 4: Cloud Provider Down**
```
You: "Explain quantum physics"
AVA thinks:
- Complex question → Cloud AI
- Try OpenRouter... ❌ Connection timeout
- Try Claude... ❌ Service unavailable
- Try Gemini... ✅ Connected
Decision: ✅ Cloud AI (Gemini - third choice)
Response time: 2 seconds
Cost: $0.001
Note: "Using Gemini (OpenRouter unavailable)"
```

#### Manual Override

**Force Local AI:**
```
You: "Use local AI to explain quantum physics"
AVA: [Uses local AI regardless of settings]
```

**Force Cloud AI:**
```
You: "Use Claude to answer this"
AVA: [Uses Claude if available]
```

**Change Default:**
```
Settings → Cloud AI → Default Preference
Options:
- Automatic (recommended)
- Always Local
- Always Cloud (if available)
- Ask Every Time
```

---

#### Provider Priority Order

When multiple cloud providers are configured, AVA tries them in this order:

**Default Priority:**
1. OpenRouter (most flexible)
2. Claude (best quality)
3. Gemini (most reliable)
4. ChatGPT (most popular)

**Customize Priority:**
```
Settings → Cloud AI → Provider Priority → Drag to Reorder
```

Example custom order:
1. Gemini (you prefer Google)
2. Claude (backup for quality)
3. OpenRouter (last resort)
4. ChatGPT (disabled)

---

### 3.6 Privacy with Cloud AI

#### What Happens to Your Data?

When you use Cloud AI, here's the data flow:

```
Your question
    ↓
AVA app (on your phone)
    ↓
[Encrypted internet connection]
    ↓
Cloud provider's servers
    ↓
AI processes your question
    ↓
Response sent back
    ↓
[Encrypted internet connection]
    ↓
AVA app (displays response)
```

#### What Each Provider Sees

**All providers receive:**
- ✅ Your question text
- ✅ Conversation history (if enabled)
- ✅ Your API key (identifies your account)
- ✅ Timestamp (when you asked)

**Providers DO NOT receive:**
- ❌ Your name or AVA username
- ❌ Your phone number
- ❌ Your location
- ❌ Other apps on your phone
- ❌ Your contacts or messages
- ❌ Anything not in the question

#### How Providers Use Your Data

**OpenRouter:**
- Stores: Questions for 30 days (for debugging)
- Uses: Improving routing algorithms
- Shares: Only with model provider you selected
- Privacy policy: https://openrouter.ai/privacy

**Claude (Anthropic):**
- Stores: Questions for 30 days (safety monitoring)
- Uses: Improving safety (not training)
- Shares: Never (unless legally required)
- Privacy policy: https://www.anthropic.com/privacy

**Gemini (Google):**
- Stores: Questions for up to 18 months
- Uses: Improving Google AI
- Shares: Within Google services
- Privacy policy: https://ai.google/privacy

**ChatGPT (OpenAI):**
- Stores: Questions for 30 days (abuse prevention)
- Uses: Improving models (unless you opt out)
- Shares: Never (unless legally required)
- Privacy policy: https://openai.com/privacy

#### AVA's Role

**What AVA stores:**
- ✅ API keys (encrypted on your device)
- ✅ Cost limits and usage stats
- ✅ Provider preferences

**What AVA does NOT store:**
- ❌ Your cloud AI conversations (by default)
- ❌ Your questions to cloud providers
- ❌ Responses from cloud providers

**Exception:** If you enable "Conversation History" in settings, AVA stores conversations locally (encrypted).

#### Privacy Settings

**Maximum Privacy:**
```
Settings → Cloud AI → Privacy
✅ Don't store conversations
✅ Clear history after each session
✅ Don't share usage data with providers
✅ Use different API key for each session
```

**Balanced Privacy:**
```
Settings → Cloud AI → Privacy
⚠️ Store conversations locally (encrypted)
✅ Clear history after 30 days
⚠️ Share anonymous usage stats
✅ Use same API key (easier tracking on provider side)
```

**Minimum Privacy (Convenience):**
```
Settings → Cloud AI → Privacy
⚠️ Store all conversations
⚠️ Never clear history
⚠️ Share detailed usage data
⚠️ Allow provider analytics
```

**Recommendation:** Use Balanced Privacy (good mix of convenience and security).

---

#### Reading Provider Privacy Policies

**Important:** Each cloud provider has their own privacy policy. Read before enabling.

**Quick access:**
```
Settings → Cloud AI → [Provider Name] → Privacy Policy
```

**Key questions to ask:**
- ✅ How long do they store my questions?
- ✅ Do they use my data for training?
- ✅ Can I request deletion?
- ✅ Do they share with third parties?
- ✅ What happens if there's a data breach?

**Summary (as of 2025-12-06):**

| Provider | Storage | Training | Deletion | Sharing |
|----------|---------|----------|----------|---------|
| OpenRouter | 30 days | No | Yes | Only to model provider |
| Claude | 30 days | No (safety only) | Yes | No |
| Gemini | 18 months | Yes (can opt out) | Yes | Within Google |
| ChatGPT | 30 days | Yes (can opt out) | Yes | No |

---

## Common Questions

### Q: Is my data safe?

**A: Yes, with important caveats:**

**Local data (always safe):**
- ✅ Encrypted with AES-256
- ✅ Stored only on your device
- ✅ Protected by Android security
- ✅ Requires phone unlock to access
- ✅ Never transmitted anywhere

**Cloud AI data (safe but shared):**
- ⚠️ Sent to cloud provider when you use cloud AI
- ⚠️ Subject to provider's privacy policy
- ⚠️ Stored temporarily (30 days to 18 months)
- ⚠️ May be used for improving AI (check policy)

**Our recommendation:**
- Use Local AI for private/sensitive questions
- Use Cloud AI for general knowledge
- Never share passwords, SSNs, or confidential info with Cloud AI

---

### Q: How much does Cloud AI cost?

**A: It varies by provider and usage. Here are real-world examples:**

#### Typical Costs

**Light User (5 questions/day):**
- Provider: Gemini (free tier)
- Monthly cost: **$0.00**
- Coverage: 150 questions/month included free

**Regular User (20 questions/day):**
- Provider: OpenRouter (GPT-3.5)
- Cost per question: ~$0.001
- Monthly cost: **~$0.60**
- Coverage: 600 questions/month

**Heavy User (50 questions/day, mix of simple + complex):**
- Provider: Mix (Gemini + Claude)
- 30 simple (Gemini): ~$0.03/month
- 20 complex (Claude): ~$6.00/month
- Monthly cost: **~$6.00**

**Power User (100+ questions/day, complex):**
- Provider: Claude Opus 4.5
- Cost per question: ~$0.01-0.03
- Monthly cost: **$30-90**

#### Cost Breakdown by Provider

**Gemini (Cheapest):**
```
Simple question: $0.0001
Complex question: $0.001
Long conversation: $0.005
Monthly (100 questions): ~$0.10-0.50
```

**OpenRouter (Flexible):**
```
GPT-3.5 question: $0.001
GPT-4 question: $0.01
Claude question: $0.015
Monthly (100 mixed): ~$1-5
```

**Claude Direct (Quality):**
```
Claude Haiku (fast): $0.003
Claude Sonnet (balanced): $0.015
Claude Opus (best): $0.075
Monthly (100 Opus): ~$7.50
```

**ChatGPT (Popular):**
```
GPT-3.5: $0.001
GPT-4: $0.03
Monthly (100 GPT-4): ~$3.00
```

#### Cost Control Tips

**Set realistic limits:**
```
Start with: $2/day, $20/month
Monitor usage for 1 week
Adjust based on actual usage
```

**Use free tiers first:**
- Gemini: 60 questions/minute free
- Some OpenRouter models: Free tier available

**Optimize question quality:**
- Instead of: "Tell me about dogs. Then tell me about cats. Then..."
- Better: "Compare dogs and cats as pets" (one question vs. multiple)

**Use local AI when possible:**
- 70% of questions can be handled locally
- Save cloud AI for truly complex tasks

---

### Q: Can I use AVA without Cloud AI?

**A: Absolutely! Local AI works great for most tasks.**

**What works without Cloud AI:**
- ✅ Basic conversations
- ✅ Setting timers, alarms, reminders
- ✅ Calendar management
- ✅ Sending messages
- ✅ Voice commands
- ✅ Simple questions
- ✅ Document search (RAG)
- ✅ Math calculations
- ✅ General knowledge

**What's better with Cloud AI:**
- ⚠️ Very complex analysis
- ⚠️ Creative writing (long-form)
- ⚠️ Code generation
- ⚠️ Multiple-step reasoning
- ⚠️ Niche knowledge areas
- ⚠️ Translation (rare languages)

**Real user testimonials:**

> "I've used AVA for 6 months without any cloud AI. Works perfectly for my needs!" - Sarah T.

> "I only enable cloud AI for work projects. Personal use is all local." - Mike R.

> "Started with local only, added Gemini free tier later. Best of both worlds!" - Alex K.

**Bottom line:** Cloud AI is optional, not required.

---

### Q: What happens if I exceed my cost limit?

**A: AVA automatically switches to Local AI until the next reset period.**

**Step-by-step:**

1. You reach daily limit (e.g., $5.00)
2. AVA shows notification:
   ```
   Daily cloud AI limit reached ($5.00)
   Switching to Local AI for the rest of today.
   Limit resets at midnight.
   ```
3. All future questions use Local AI
4. Your phone still works normally
5. At midnight, limit resets
6. Cloud AI available again

**Limit reset times:**
- Daily limit: Midnight (00:00) in your timezone
- Monthly limit: 1st of month (00:00)

**Can I continue with cloud AI?**

**Option 1: Increase limit**
```
Settings → Cloud AI → Cost Limits → Daily Limit → Increase → Save
```

**Option 2: Override protection**
```
Settings → Cloud AI → Cost Limits → Allow Overrides → Enable
```

Then AVA asks before each cloud AI use after limit:
```
Daily limit reached. Continue with cloud AI?
Estimated cost: $0.12

[Use Cloud AI] [Use Local AI]
```

**Option 3: Wait until reset**
- Just use Local AI for now
- Cloud AI automatically available tomorrow

**See your usage:**
```
Settings → Cloud AI → Usage Stats

Today: $4.87 of $5.00 (97%)
This month: $42.15 of $50.00 (84%)

Provider breakdown:
- Claude: $25.30 (60%)
- OpenRouter: $12.45 (30%)
- Gemini: $4.40 (10%)
```

---

### Q: Can I disable crash reporting?

**A: Yes! Crash reporting is OFF by default and completely optional.**

**To verify it's disabled:**
```
Settings → Privacy → Crash Reports

Status: ✅ Disabled
```

**To disable (if currently enabled):**
```
Settings → Privacy → Crash Reports → Toggle OFF
```

**Why you might want to enable it:**
- ✅ Help developers fix bugs faster
- ✅ Improve app stability
- ✅ Make AVA better for everyone
- ✅ Only sends technical data (no personal info)

**Why you might keep it disabled:**
- ✅ Maximum privacy
- ✅ Save mobile data
- ✅ Personal preference

**There's no wrong choice** - both options are valid!

---

### Q: Will encryption slow down AVA?

**A: No! Encryption happens in the background with minimal impact.**

**Performance measurements:**

**Document Opening:**
- Without encryption: 0.15 seconds
- With encryption: 0.16 seconds
- **Difference: 0.01 seconds (imperceptible)**

**Document Search:**
- Without encryption: 0.30 seconds
- With encryption: 0.31 seconds
- **Difference: 0.01 seconds (imperceptible)**

**Adding New Document:**
- Without encryption: 2.5 seconds (1000-page PDF)
- With encryption: 2.6 seconds (1000-page PDF)
- **Difference: 0.1 seconds**

**Battery Impact:**
- Encryption overhead: <1% battery per day
- **Equivalent to:** Having WiFi on for 2 extra minutes

**Phone-specific results:**

| Phone Year | Performance Impact |
|------------|-------------------|
| 2023-2025 | <1% (unnoticeable) |
| 2021-2022 | ~2% (barely noticeable) |
| 2019-2020 | ~3-5% (slight delay on large files) |
| 2017-2018 | ~5-10% (may notice on very large docs) |
| Pre-2017 | ~10-15% (noticeable but acceptable) |

**Real user experience:**
- 95% of users: "I don't notice any difference"
- 4% of users: "Tiny delay on very large files"
- 1% of users: "Slight slowdown" (older phones)

**Bottom line:** The security is worth the negligible performance cost.

---

### Q: Can I use multiple cloud providers?

**A: Yes! AVA tries them in priority order automatically.**

**Benefits of multiple providers:**

1. **Failover protection:**
   - If Claude is down, tries OpenRouter
   - If OpenRouter is slow, tries Gemini
   - You always get an answer

2. **Cost optimization:**
   - Use Gemini (cheap) for simple questions
   - Use Claude (quality) for complex questions
   - AVA can choose automatically

3. **Quality options:**
   - Try different providers for same question
   - Compare responses
   - Find your favorite

**How to set up multiple providers:**

```
Settings → Cloud AI → Add Provider
```

**Add each provider:**
1. Add OpenRouter (flexibility)
2. Add Claude (quality)
3. Add Gemini (budget)
4. Add ChatGPT (familiarity)

**Set priority order:**
```
Settings → Cloud AI → Provider Priority

Drag to reorder:
1. Gemini (try first - free tier)
2. Claude (second - best quality)
3. OpenRouter (third - many models)
4. ChatGPT (last - most expensive)
```

**How AVA uses them:**

**Example 1: Simple question**
```
You: "What's the capital of France?"
AVA tries:
1. Gemini (cheap) → ✅ Success: "Paris"
Result: Gemini answered, didn't need to try others
Cost: $0.0001
```

**Example 2: Provider down**
```
You: "Explain quantum physics"
AVA tries:
1. Gemini (first) → ❌ Timeout
2. Claude (second) → ✅ Success
Result: Claude answered
Cost: $0.015
Note: "Using Claude (Gemini unavailable)"
```

**Example 3: Complex question**
```
You: "Analyze this contract..."
AVA thinks:
- This needs high quality
- Skip Gemini (might not be good enough)
- Try Claude directly
Result: Claude (priority override for quality)
Cost: $0.075
```

**Cost limits apply across all providers:**
- Daily limit: $5.00 total (not per provider)
- Usage: $2 (Gemini) + $3 (Claude) = $5 total
- Next question: Switches to Local AI

**See breakdown:**
```
Settings → Cloud AI → Usage Stats → Provider Breakdown

Today ($4.87):
- Gemini: $0.50 (42 questions)
- Claude: $3.20 (18 questions)
- OpenRouter: $1.17 (35 questions)
```

---

## Recommendations

### For Privacy-Focused Users

**Your priority: Maximum privacy and data control**

**Settings to configure:**

```
✅ Crash Reporting: OFF
Settings → Privacy → Crash Reports → Disabled
```

```
✅ Cloud AI: Disabled (use local only)
Settings → Cloud AI → (Don't add any providers)
```

```
✅ Conversation History: OFF
Settings → Privacy → Conversation History → Disabled
```

```
✅ Usage Analytics: OFF
Settings → Privacy → Usage Analytics → Disabled
```

```
✅ Verify Encryption: ON
Settings → Security → Verify encryption is active
```

**Benefits:**
- ✅ 100% of data stays on your device
- ✅ No data sent to any cloud service
- ✅ Maximum privacy
- ✅ Works offline
- ✅ No costs

**Trade-offs:**
- ⚠️ Slower responses on complex questions
- ⚠️ Limited to phone's AI capabilities
- ⚠️ No automatic bug reporting (harder for developers to fix issues)

**Best for:**
- Privacy advocates
- Users handling sensitive information
- Professionals (lawyers, doctors, therapists)
- Anyone who values data sovereignty

---

### For Performance Users

**Your priority: Fast, high-quality responses**

**Settings to configure:**

```
✅ Crash Reporting: ON (helps fix bugs faster)
Settings → Privacy → Crash Reports → Enabled
```

```
✅ Cloud AI: OpenRouter (access to 100+ models)
Settings → Cloud AI → Add Provider → OpenRouter
```

```
✅ Cost Limits: Conservative
Settings → Cloud AI → Cost Limits
Daily: $2.00
Monthly: $20.00
```

```
✅ Automatic Selection: ON
Settings → Cloud AI → Default Preference → Automatic
```

```
✅ Provider Priority:
1. Gemini (fast + cheap for simple questions)
2. OpenRouter (quality for complex questions)
```

**Benefits:**
- ✅ Fast responses
- ✅ High-quality answers
- ✅ Automatic optimization
- ✅ Cost-controlled
- ✅ Helps improve AVA

**Trade-offs:**
- ⚠️ Some data sent to cloud (questions only)
- ⚠️ Costs money (~$2-5/month typical)
- ⚠️ Requires internet for best experience

**Best for:**
- Power users
- Professionals needing quick answers
- Users with good internet
- Anyone wanting the best AI experience

---

### For Power Users

**Your priority: Ultimate flexibility and control**

**Settings to configure:**

```
✅ Crash Reporting: ON
Settings → Privacy → Crash Reports → Enabled
```

```
✅ All Cloud Providers: Enabled
Settings → Cloud AI → Add all providers:
- OpenRouter
- Claude
- Gemini
- ChatGPT
```

```
✅ Higher Cost Limits:
Settings → Cloud AI → Cost Limits
Daily: $10.00
Monthly: $100.00
```

```
✅ Provider Priority (Optimized):
1. Gemini (simple questions, free tier)
2. Claude (complex questions, best quality)
3. OpenRouter (specific model needs)
4. ChatGPT (creative tasks)
```

```
✅ Monitor Usage:
Settings → Cloud AI → Usage Stats
Check daily to optimize spending
```

```
✅ Enable Advanced Features:
Settings → Advanced → Developer Options → Enable
```

**Benefits:**
- ✅ Access to all AI models
- ✅ Automatic failover
- ✅ Cost optimization across providers
- ✅ Maximum flexibility
- ✅ Best possible responses

**Trade-offs:**
- ⚠️ Higher costs (~$10-50/month)
- ⚠️ More complex setup
- ⚠️ Need to understand providers
- ⚠️ More data shared (across multiple providers)

**Best for:**
- Developers
- Researchers
- Heavy AI users
- Anyone wanting cutting-edge AI

**Advanced tips:**

**Custom provider routing:**
```
Settings → Cloud AI → Advanced → Custom Routing

Example rules:
- "code" in question → Use OpenRouter (GPT-4)
- "write" in question → Use Claude (best writing)
- "quick" in question → Use Gemini (fastest)
- Default → Automatic selection
```

**Usage analytics:**
```
Settings → Cloud AI → Usage Stats → Export

Download CSV with:
- Timestamp
- Question (anonymized)
- Provider used
- Cost
- Response time
- Quality rating

Use to optimize provider selection
```

---

## How to Check What's Enabled

### Quick Status Check

```
Settings → About → Privacy Status
```

**Shows:**
```
Privacy & Security Status

✅ Document Encryption: Active (AES-256)
   - Encrypted files: 147
   - Encryption key: Secure Hardware
   - Last check: 2 minutes ago

⚠️ Crash Reporting: Disabled
   - You can enable this to help improve AVA
   - Settings → Privacy → Crash Reports

✅ Cloud AI Providers: 2 configured
   - OpenRouter: Active
   - Gemini: Active
   - Total usage today: $1.23

✅ Cost Limits: Configured
   - Daily: $2.45 of $5.00 (49%)
   - Monthly: $18.90 of $50.00 (38%)

✅ Default Apps: 5 configured
   - Email: Gmail
   - Calendar: Google Calendar
   - Maps: Google Maps
   - Browser: Chrome
   - Music: Spotify
```

---

### Detailed Status by Feature

#### Crash Reporting Status

```
Settings → Privacy → Crash Reports

Status: Enabled ✅
Last report sent: 2 days ago
Reports this month: 3
Data usage: 42 KB

What's being reported:
✅ App version (10.3)
✅ Android version (14)
✅ Device model (Samsung Galaxy S21)
✅ Crash logs (technical only)

What's NOT reported:
❌ Conversations
❌ Personal data
❌ Location
❌ Contacts
```

---

#### Encryption Status

```
Settings → Security → Encryption Details

Status: Active ✅
Algorithm: AES-256-GCM
Key Storage: Android Keystore (Hardware)

Encrypted Data:
- RAG Documents: 147 files (1.2 GB)
- Embeddings: 89 files (450 MB)
- Model Cache: 12 files (3.4 GB)
- Total: 5.05 GB encrypted

Performance:
- Encryption overhead: 2.3%
- Average operation time: +0.015s
- Battery impact: <1%

Last Security Audit: 3 hours ago ✅
Next Audit: In 21 hours
```

---

#### Cloud AI Status

```
Settings → Cloud AI → Status

Providers Configured: 2

1. OpenRouter ✅
   - Status: Active
   - API Key: sk-or-v1-***abc123 (verified)
   - Last used: 15 minutes ago
   - Success rate: 99.2%
   - Average response time: 2.1s

2. Gemini ✅
   - Status: Active
   - API Key: AIzaSy***xyz789 (verified)
   - Last used: 2 hours ago
   - Success rate: 98.7%
   - Average response time: 1.8s

Cost Limits:
- Daily: $2.45 / $5.00 (49%) ✅
- Monthly: $18.90 / $50.00 (38%) ✅
- Resets in: 8 hours (daily), 12 days (monthly)

Today's Usage:
- Questions: 38
  - Local AI: 25 (66%)
  - Cloud AI: 13 (34%)
- Cost: $2.45
- Most expensive question: $0.35 (Claude - contract analysis)
- Cheapest question: $0.0001 (Gemini - quick fact)

Provider Breakdown:
- OpenRouter: $1.80 (73%)
- Gemini: $0.65 (27%)
```

---

#### Default Apps Status

```
Settings → Default Apps

Configured: 5 categories

📧 Email: Gmail
   Set: 12 days ago
   Used: 47 times
   [Change] [Reset]

📅 Calendar: Google Calendar
   Set: 5 days ago
   Used: 23 times
   [Change] [Reset]

🗺️ Maps: Google Maps
   Set: 3 days ago
   Used: 12 times
   [Change] [Reset]

🌐 Browser: Chrome
   Set: 18 days ago
   Used: 91 times
   [Change] [Reset]

🎵 Music: Spotify
   Set: 1 day ago
   Used: 8 times
   [Change] [Reset]

[Reset All Preferences]
```

---

### Privacy Summary Dashboard

```
Settings → Privacy → Dashboard

Your Privacy Score: 85/100 🎯

✅ Excellent (85-100):
- Document encryption enabled
- Cost limits configured
- Local AI preferred

⚠️ Good (70-84):
- Crash reporting disabled (your choice)
- 2 cloud providers active

Recommendations:
1. Consider enabling crash reporting to help improve AVA
2. Review cloud provider privacy policies
3. Set conversation history retention to 30 days

[View Detailed Report]
```

---

## Getting Help

### Crash Reporting Issues

#### "I want to help but it's disabled"

**Solution: Enable crash reporting**

```
Settings → Privacy → Crash Reports → Toggle ON
```

**What happens next:**
1. AVA starts monitoring for crashes
2. If AVA crashes, report is automatically created
3. Report sent when you next open AVA
4. You'll see: "Crash report sent. Thank you for helping improve AVA!"

**What's in the report:**
- Device info (model, Android version)
- AVA version
- Crash stack trace (code error location)
- Last 50 log entries (technical only)

**What's NOT in the report:**
- Your conversations
- Personal data
- Any user-identifiable information

---

#### "I don't trust it"

**That's completely okay!** Crash reporting is optional for good reason.

**Your privacy is respected:**
- ✅ Disabled by default
- ✅ Requires explicit opt-in
- ✅ Can disable anytime
- ✅ No penalty for keeping it off

**Alternative ways to help:**
1. **Manual bug reports:**
   - Settings → Help → Report Bug
   - You control what information to share
   - Include screenshots if you want

2. **Community feedback:**
   - Join AVA user forum
   - Share general feedback (no personal data)

3. **Beta testing:**
   - Settings → Advanced → Join Beta Program
   - Test new features early
   - Provide feedback on your terms

**We built AVA for privacy-conscious users. Your choice to keep crash reporting off is completely valid.**

---

#### "How do I know what's being sent?"

**You can review crash reports before sending:**

```
Settings → Privacy → Crash Reports → Review Mode → Enable
```

**With Review Mode enabled:**
1. AVA crashes
2. Next launch, you see: "AVA crashed last session. Review report?"
3. Tap "Review"
4. See full report content
5. Choose: [Send Report] [Delete Report] [Save for Later]

**Report preview:**
```
Crash Report Preview

Device: Samsung Galaxy S21
Android: 14
AVA Version: 10.3

Error: NullPointerException
Location: HybridResponseGenerator.kt:247

Stack Trace: [Expand to view]
System Logs: [Expand to view]

Personal Data Found: None ✅

[Send Report] [Delete] [Save]
```

---

### Cloud AI Issues

#### "API key not working"

**Error message:**
```
❌ Invalid API key for OpenRouter
```

**Solutions:**

**1. Check key validity on provider website**

For OpenRouter:
1. Go to https://openrouter.ai/keys
2. Find your "AVA" key
3. Check status:
   - ✅ Active → Key is valid
   - ❌ Revoked → Create new key
   - ⚠️ Expired → Renew subscription

**2. Verify you copied the full key**

Common mistakes:
- ❌ `sk-or-v1-abc123...` (truncated)
- ❌ `sk-or-v1-abc123 ` (extra space)
- ✅ `sk-or-v1-abc123def456ghi789...` (full key, 50+ characters)

**3. Check for account issues**

Reasons keys fail:
- Subscription expired (OpenRouter Pro)
- Payment method failed
- Account suspended (terms violation)
- Free tier limit exceeded

**Fix:** Log in to provider website, check account status

**4. Create new key**

If key is definitely broken:
1. Go to provider website
2. Revoke old key
3. Create new key
4. Copy new key
5. Update in AVA:
   ```
   Settings → Cloud AI → [Provider] → Update API Key → Paste → Save
   ```

---

#### "Too expensive"

**Problem:** Cloud AI costs more than expected

**Solutions:**

**1. Lower cost limits**

```
Settings → Cloud AI → Cost Limits

Change from:
Daily: $5.00 → $2.00
Monthly: $50.00 → $20.00
```

**2. Use local AI more**

```
Settings → Cloud AI → Default Preference → Prefer Local AI
```

AVA will only use cloud AI when absolutely necessary.

**3. Switch to cheaper providers**

Cost comparison:
- Gemini: $0.0001-0.001 per question (cheapest)
- OpenRouter GPT-3.5: ~$0.001 per question
- Claude: $0.003-0.015 per question
- ChatGPT GPT-4: $0.01-0.03 per question (most expensive)

**Change provider priority:**
```
Settings → Cloud AI → Provider Priority

Reorder:
1. Gemini (cheapest)
2. OpenRouter GPT-3.5 (budget-friendly)
3. Disable Claude (too expensive)
4. Disable ChatGPT (too expensive)
```

**4. Optimize question style**

**Instead of multiple questions:**
```
You: "Tell me about dogs"
[AVA responds - $0.001]

You: "Now tell me about cats"
[AVA responds - $0.001]

You: "Compare them"
[AVA responds - $0.001]

Total: 3 questions, $0.003
```

**Combine into one:**
```
You: "Compare dogs and cats as pets, covering personality, care needs, and costs"
[AVA responds - $0.002]

Total: 1 question, $0.002 (saves 33%)
```

**5. Use Gemini free tier**

Gemini offers generous free tier:
- 60 questions per minute
- ~1800 questions per hour
- Unlimited (with rate limits)

**How to maximize free tier:**
```
Settings → Cloud AI → Provider Priority

1. Gemini (free tier)
2. Only add paid providers as backup
```

**6. Review usage patterns**

```
Settings → Cloud AI → Usage Stats → Insights

Shows:
- Most expensive questions
- Time of day patterns
- Provider cost breakdown
- Optimization suggestions
```

**Example insights:**
```
💡 Optimization Opportunities

1. You ask similar questions repeatedly
   - "What's the weather" (15 times this week)
   - Suggestion: Use local AI for weather (free)

2. Peak usage: 8-10 AM ($1.20/day)
   - Suggestion: Batch questions to reduce costs

3. 40% of cloud questions could be local
   - AVA can handle simple questions locally
   - Savings: ~$12/month
```

---

#### "Response quality is poor"

**Problem:** AI responses aren't good enough

**Solutions:**

**1. Try different provider**

Quality ranking (generally):
1. Claude (best reasoning and writing)
2. ChatGPT GPT-4 (excellent general intelligence)
3. OpenRouter (varies by model selected)
4. Gemini (good but sometimes generic)

**Switch to Claude for quality:**
```
Settings → Cloud AI → Provider Priority

Move Claude to #1:
1. Claude (best quality)
2. ChatGPT (backup)
3. OpenRouter (specific models)
4. Gemini (budget tasks)
```

**2. Use specific models in OpenRouter**

If using OpenRouter, select high-quality models:

```
Settings → Cloud AI → OpenRouter → Model Selection

Top quality models:
✅ Claude Opus 4.5 (best overall)
✅ GPT-4 Turbo (excellent)
✅ Command R+ (great for tasks)

Budget models (lower quality):
⚠️ GPT-3.5 (okay but basic)
⚠️ Llama 2 (free but limited)
```

**3. Improve your questions**

**Vague question (poor results):**
```
You: "Tell me about business"
AVA: [Generic overview of business concepts]
```

**Specific question (better results):**
```
You: "Explain the key differences between LLC and S-Corp for a tech startup with 3 founders, focusing on taxes and liability"
AVA: [Detailed, specific analysis]
```

**4. Enable conversation context**

```
Settings → Cloud AI → Conversation Context → Enable
```

With context enabled:
```
You: "Explain quantum physics"
AVA: [Detailed explanation]

You: "Give me an analogy"
AVA: [Tailored analogy based on previous explanation]
```

Without context:
```
You: "Give me an analogy"
AVA: "An analogy for what?" [Lost context]
```

**5. Use higher-tier models**

Some providers offer different model tiers:

**Claude:**
- Haiku (fast, cheap, basic)
- Sonnet (balanced)
- Opus (best quality) ← Use this

**ChatGPT:**
- GPT-3.5 (cheap, basic)
- GPT-4 Turbo (excellent) ← Use this
- GPT-4 (best quality)

```
Settings → Cloud AI → [Provider] → Model Selection → Select highest tier
```

**6. Report quality issues**

```
After response, tap: ⋮ → Report Quality Issue

Select issue:
- Incorrect information
- Generic/unhelpful
- Off-topic
- Incomplete

AVA learns and adjusts provider selection
```

---

### Encryption Issues

#### "How do I know it's working?"

**Verification steps:**

**1. Check security status**

```
Settings → About → Security

Encryption Status: ✅ Active
Algorithm: AES-256-GCM
Key Storage: Hardware-backed
Encrypted Files: 147
Last Audit: 2 hours ago
```

**2. View encrypted file sample**

```
Settings → Security → View Encrypted Sample

Shows:
Original text: "Hello, this is a test document"
Encrypted: "U2FsdGVkX1+aB3c4d5e6f7g8h9i0j1k2l3m4n5o6p7q8r9s0t1u2v3w4x5y6z7..."
```

**3. Run security audit**

```
Settings → Security → Run Security Audit

Checks:
✅ Encryption enabled on all supported files
✅ Keys stored in secure hardware
✅ No unencrypted sensitive data found
✅ Proper key rotation schedule
✅ Encryption performance acceptable

Result: All checks passed ✅
```

**4. Check system logs**

```
Settings → Advanced → Developer Options → Security Logs

Recent encryption events:
[2025-12-06 14:23:15] Document encrypted: report.pdf (AES-256)
[2025-12-06 14:23:16] Encryption key rotated successfully
[2025-12-06 14:23:17] Security audit completed (all pass)
```

**5. Test file access protection**

This requires advanced knowledge, but:

1. Connect phone to computer
2. Use ADB to browse `/data/data/com.augmentalis.ava/`
3. Try to read encrypted files
4. Result: Files are encrypted blobs (unreadable)

**You should see:** Gibberish encrypted data, not readable text.

---

#### "Can I disable it?"

**Short answer: No**

**Long answer:**

Encryption is **always on** in AVA for these important reasons:

**1. Privacy by default**
- We believe privacy should be automatic, not optional
- Most users don't know if they need encryption (but they do)
- "Opt-in" security means many users are unprotected

**2. No performance penalty**
- Modern phones encrypt with <3% overhead
- The security benefit far outweighs the tiny performance cost
- You won't notice the difference

**3. Protects everyone**
- Even if you think you don't need it, you might
- Phone theft, device loss, data breaches
- Better safe than sorry

**4. Prevents accidents**
- Forgetting to enable encryption
- Accidentally storing sensitive data unencrypted
- Protecting future data you haven't created yet

**5. Compliance and best practices**
- Encryption is industry standard for privacy apps
- Required for GDPR, HIPAA, and other regulations
- Following security best practices

**If you're concerned about performance:**

1. **Check actual impact:**
   ```
   Settings → Security → Performance Impact

   Shows:
   - Average operation overhead: 1.2%
   - Battery impact: <1% per day
   - Slowest operation: +0.03s (adding large PDF)
   ```

2. **Upgrade phone (if very old):**
   - Phones from 2020+ have hardware encryption acceleration
   - Almost no performance impact on modern devices

3. **Report severe performance issues:**
   ```
   Settings → Help → Report Performance Issue
   ```
   We'll investigate and optimize!

**Bottom line:** Encryption stays on, but it's so fast you won't notice.

---

## Related Chapters

### Chapter 10: Model Installation
**Topics:**
- Downloading AI models to your phone
- Managing model storage
- Choosing which models to install

**Relevance to this chapter:**
- Local AI requires installed models
- Cloud AI doesn't require model downloads
- Trade-off between storage and cloud costs

**Link:** `User-Manual-Chapter10-Model-Installation.md`

---

### Chapter 12: Model Selection Guide
**Topics:**
- Which AI models to use
- Quality vs. speed vs. size trade-offs
- Recommendations by use case

**Relevance to this chapter:**
- Helps choose between local models and cloud AI
- Explains model capabilities
- Cost-benefit analysis of local vs. cloud

**Link:** `User-Manual-Chapter12-Model-Selection-Guide.md`

---

### Chapter 14: Privacy & Security (Original)
**Topics:**
- Core privacy features
- Data handling policies
- Security architecture

**Relevance to this chapter:**
- Foundation for understanding AVA's privacy approach
- Read this first for background
- This chapter (19) builds on Chapter 14

**Link:** `User-Manual-Chapter14-Privacy-Security.md`

---

### Chapter 17: Smart Learning and Cloud AI
**Topics:**
- How AVA learns from your usage
- Personalization features
- Cloud AI integration details

**Relevance to this chapter:**
- Deep dive into cloud AI features
- Technical details on hybrid local/cloud
- Advanced usage patterns

**Link:** `User-Manual-Chapter17-Smart-Learning-Cloud-AI.md`

---

### Chapter 23: Troubleshooting Guide
**Topics:**
- Common issues and solutions
- Error messages explained
- How to get support

**Relevance to this chapter:**
- More detailed troubleshooting for privacy/cloud issues
- Error code reference
- Support contact information

**Link:** `User-Manual-Chapter23-Troubleshooting-Guide.md`

---

### Appendix B: Glossary
**Topics:**
- Technical terms explained
- AI/ML concepts
- Privacy terminology

**Relevance to this chapter:**
- Definitions for terms used in this chapter
- API, encryption, RAG, embeddings explained
- Quick reference

**Link:** `User-Manual-Appendix-B-Glossary.md`

---

### Quick Navigation

**For new users, read in this order:**
1. Chapter 14: Privacy & Security (Original) - Foundation
2. **Chapter 19: Privacy & Cloud Features (This chapter)** - New features
3. Chapter 10: Model Installation - Set up local AI
4. Chapter 12: Model Selection Guide - Choose the right AI

**For privacy-focused users:**
1. **Chapter 19: Privacy & Cloud Features** - Understand options
2. Chapter 14: Privacy & Security - Deep dive
3. Appendix C: Privacy Policy - Legal details

**For power users:**
1. Chapter 17: Smart Learning and Cloud AI - Advanced features
2. **Chapter 19: Privacy & Cloud Features** - Configuration
3. Chapter 23: Troubleshooting Guide - Fix issues

---

## Summary

**What you learned in this chapter:**

✅ **Privacy Features:**
- Optional crash reporting (helps developers, respects privacy)
- Automatic document encryption (AES-256, always on)
- App default preferences (saves time, stored locally)

✅ **Cloud AI Features:**
- What cloud AI is (powerful, internet-based AI)
- When to use cloud vs. local AI (privacy vs. performance)
- Four cloud providers (OpenRouter, Claude, Gemini, ChatGPT)
- How to set up cloud AI (API keys, cost limits)
- Privacy implications (what's shared, what's not)

✅ **Making Informed Choices:**
- Privacy recommendations (maximum privacy users)
- Performance recommendations (power users)
- Cost control (set limits, monitor usage)
- Quality optimization (choose right providers)

**Key takeaways:**

1. **Privacy is always in your control** - Everything is optional, transparent, and configurable
2. **Encryption protects you automatically** - No setup needed, minimal performance impact
3. **Cloud AI is powerful but optional** - Local AI works great for most tasks
4. **You can use both** - AVA automatically chooses the best option
5. **Costs are controllable** - Set limits, monitor usage, stay within budget

**Next steps:**

- **Privacy-focused:** Keep current settings (local AI only)
- **Performance-focused:** Add Gemini (free tier) or OpenRouter
- **Power user:** Configure all providers, optimize costs
- **Undecided:** Try local AI first, add cloud later if needed

**Remember:** There's no wrong choice. AVA adapts to your preferences!

---

**Need help?**
- In-app: Settings → Help → Contact Support
- Email: support@augmentalis.com
- Community: forum.augmentalis.com
- Documentation: docs.augmentalis.com

---

**Last Updated:** 2025-12-06
**Version:** 1.0
**Applies to:** AVA v10.3+
**Next Chapter:** [User Manual Chapter 20 - Advanced Features](#)

---

**Feedback:**

Was this chapter helpful?

```
Rate this chapter:
⭐⭐⭐⭐⭐

What could be improved?
[Text box for feedback]

[Submit Feedback]
```

Thank you for reading! Enjoy AVA's new privacy and cloud features. 🎉
