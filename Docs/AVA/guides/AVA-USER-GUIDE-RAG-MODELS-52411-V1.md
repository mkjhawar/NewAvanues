# AVA RAG Model User Guide

**Version:** 1.0
**Last Updated:** 2025-11-23
**For:** AVA Users

---

## What are RAG Models?

**RAG (Retrieval-Augmented Generation)** allows AVA to search through your documents and answer questions based on their content.

**RAG models** convert your documents into "semantic vectors" - mathematical representations that capture the meaning of text. This enables AVA to find relevant information even when the exact words don't match.

**Example:**
- You ask: "What causes climate change?"
- AVA searches: Documents about "global warming," "greenhouse gases," "carbon emissions"
- Even though you didn't say those exact words!

---

## Available Models

### Free Tier

#### AVA Base (English) ⭐ **Recommended**
- **Size:** 90 MB
- **Languages:** English
- **Best for:** Documents, articles, knowledge bases
- **Quality:** Excellent balance of speed and accuracy

#### AVA Fast (English)
- **Size:** 61 MB
- **Languages:** English
- **Best for:** Quick searches, real-time queries
- **Quality:** Good (slightly lower than Base)

---

### Pro Tier (Requires License)

#### AVA Quality (English)
- **Size:** 420 MB
- **Languages:** English
- **Best for:** Technical documents, research papers
- **Quality:** Highest quality for English

#### AVA Multilingual ⭐ **Recommended for Non-English**
- **Size:** 470 MB
- **Languages:** 50+ languages (Spanish, French, German, Chinese, Japanese, etc.)
- **Best for:** International documents, multilingual knowledge bases
- **Quality:** Excellent across all languages

#### AVA Multilingual Quality
- **Size:** 1.1 GB
- **Languages:** 50+ languages
- **Best for:** High-quality multilingual search
- **Quality:** Best multilingual performance

#### AVA Chinese
- **Size:** 220 MB
- **Languages:** Chinese (Simplified & Traditional)
- **Best for:** Chinese-only documents
- **Quality:** Better than multilingual for Chinese

#### AVA Japanese
- **Size:** 340 MB
- **Languages:** Japanese
- **Best for:** Japanese-only documents
- **Quality:** Better than multilingual for Japanese

---

## How to Download Models

### Step 1: Open AVA Settings
1. Open AVA app
2. Tap **⚙️ Settings**
3. Scroll to **AI & Models**
4. Tap **RAG Embedding Models**

### Step 2: Choose a Model
- **For English documents:** Download **AVA Base** (recommended)
- **For multiple languages:** Download **AVA Multilingual** (requires Pro)
- **For best quality:** Download **AVA Quality** or **AVA Multilingual Quality** (requires Pro)

### Step 3: Download
1. Tap **Download Model** button
2. Wait for download to complete (1-3 minutes on WiFi)
3. See **✓ Downloaded** when ready

**Note:** First download requires WiFi. Models are stored on your device.

---

## How to Use RAG with Documents

### Step 1: Upload Documents
1. Open AVA
2. Tap **📄 Documents** tab
3. Tap **+ Add Document**
4. Select files:
   - PDF, Word (.docx), Text (.txt)
   - Markdown (.md), HTML, EPUB, RTF

### Step 2: AVA Processes Documents
- AVA automatically chunks your document into sections
- Creates semantic vectors using your downloaded model
- Stores vectors locally (secure, private)

**Processing time:**
- Small doc (10 pages): 10-30 seconds
- Medium doc (100 pages): 1-3 minutes
- Large doc (1000 pages): 10-20 minutes

### Step 3: Ask Questions
1. Open **💬 Chat** tab
2. Type question about your documents
3. AVA searches relevant sections
4. Provides answer with source citations

**Example:**
```
You: "What are the key findings in chapter 3?"

AVA: "Chapter 3 discusses three key findings:
1. Customer satisfaction increased by 25%
2. Response time decreased to under 2 minutes
3. Support costs reduced by 40%

Source: Annual Report 2024, pages 23-26"
```

---

## Managing Models

### Check Downloaded Models
1. Settings → **RAG Embedding Models**
2. See **✓ Downloaded** next to installed models

### Delete Models
1. Settings → **RAG Embedding Models**
2. Find downloaded model
3. Tap **🗑️ Delete** button
4. Confirm deletion

**When to delete:**
- Free up storage space (models are 90 MB - 1.1 GB each)
- Switching to different model
- No longer using RAG features

### Switch Models
1. Delete old model (optional - can keep both)
2. Download new model
3. AVA automatically uses the new model

**Note:** You can have multiple models installed. AVA uses the best available model for your language.

---

## Troubleshooting

### "Download Failed" Error

**Solutions:**
1. Check WiFi connection (models are large)
2. Ensure enough storage space (up to 1.1 GB)
3. Try again (may be temporary server issue)

### "Model Not Found" Error

**Solutions:**
1. Re-download model from Settings
2. Check `/sdcard/ava-ai-models/embeddings/` folder
3. Restart AVA app

### "Processing Document Failed" Error

**Solutions:**
1. Ensure model is downloaded (check Settings)
2. Check document format (PDF, DOCX, TXT supported)
3. Check document size (max 100 MB per document)
4. Try splitting large documents

### RAG Search Not Working

**Solutions:**
1. Verify model downloaded: Settings → RAG Models
2. Verify document processed: Documents tab → check status
3. Try different phrasing in your question
4. Ensure question relates to document content

---

## Privacy & Security

### Your Data is Private
- ✅ All processing happens **on your device**
- ✅ Documents never leave your phone
- ✅ No cloud uploads
- ✅ No data collection

### Model Security
- ✅ Models are **digitally signed** by AVA
- ✅ Only authorized AVA apps can load models
- ✅ Tamper detection prevents modification
- ✅ Package verification ensures authenticity

### Storage Locations
**Models:**
- `/sdcard/ava-ai-models/embeddings/` (shared across AVA apps)
- `/sdcard/Android/data/com.augmentalis.ava/files/models/` (app-specific)

**Documents & Vectors:**
- `/sdcard/Android/data/com.augmentalis.ava/files/rag/` (app-specific, private)

---

## Tips for Best Results

### Document Preparation
- ✅ **Use clear headings**: Helps AVA understand structure
- ✅ **Include table of contents**: Improves navigation
- ✅ **Keep formatting simple**: Plain text works best
- ⚠️ **Avoid scanned PDFs**: OCR quality affects results

### Asking Questions
- ✅ **Be specific**: "What was Q3 revenue?" vs "Tell me about money"
- ✅ **Use document language**: Ask in same language as document
- ✅ **Reference sections**: "According to chapter 2..." helps AVA focus
- ✅ **Ask follow-ups**: AVA maintains conversation context

### Performance
- ⚠️ **First search is slower**: AVA loads model (5-10 seconds)
- ✅ **Subsequent searches are fast**: Model stays in memory
- 💡 **Batch similar questions**: Ask multiple questions at once
- 💡 **Use smaller models**: AVA Fast for speed, AVA Quality for accuracy

---

## FAQs

### Do I need internet to use RAG?
**No.** After downloading the model, everything runs offline on your device.

### Can I use RAG without downloading models?
**No.** RAG requires at least one embedding model. Download **AVA Base** (free, 90 MB) to start.

### Which model should I download first?
**AVA Base (English)** - It's free, works great for most English documents, and only 90 MB.

### Can I use multiple models?
**Yes.** You can download multiple models. AVA automatically chooses the best one for your language.

### Does RAG work with scanned documents?
**Partially.** AVA can process scanned PDFs if they have OCR (text layer). Quality depends on OCR accuracy.

### How much storage do models need?
- **AVA Base:** 90 MB
- **AVA Fast:** 61 MB
- **AVA Multilingual:** 470 MB
- **AVA Quality:** 420 MB
- **AVA Multilingual Quality:** 1.1 GB

### Are models updated automatically?
**No.** You must manually download updates from Settings when new versions are released.

### Can I share models between AVA apps?
**Yes.** Models in `/sdcard/ava-ai-models/embeddings/` are shared across:
- AVA Standalone
- AVA Connect
- VoiceOS

---

## Support

**Need help?**
- 📧 Email: support@augmentalis.com
- 🌐 Website: https://augmentalis.com/ava/support
- 📖 Documentation: https://docs.augmentalis.com/ava/rag

**Report issues:**
- Settings → Help & Feedback → Report Bug

---

**Last Updated:** 2025-11-23
**AVA Version:** 3.0+
**Platform:** Android 8.0+
