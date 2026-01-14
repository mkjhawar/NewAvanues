# AVA AI - User Manual: Chapter 12 - Document Q&A (RAG Features)

**Version**: 1.1.0
**Last Updated**: November 27, 2025
**For AVA AI App Version**: 1.1+

---

## Welcome to AVA Document Q&A!

AVA can now answer questions about your documents! Upload PDFs, manuals, research papers, or web pages, and ask AVA anything about them. This feature is called RAG (Retrieval-Augmented Generation), but you can just think of it as "Document Q&A."

## 12.1. Understanding RAG (Retrieval Augmented Generation)

AVA uses **Retrieval Augmented Generation (RAG)** to give the AI access to your personal documents and data. This essentially gives the AI a "Long-Term Memory".

### New in v2.0: Enhanced Short-Term Memory
We have also upgraded AVA's **Short-Term Memory** (or "Working Memory"). 
*   **Context Continuity**: AVA now remembers the details of your conversation for the last few turns.
*   **Seamless Flow**: You can ask follow-up questions like "What was that again?" or "Tell me more about it" without repeating context. Since this runs entirely on-device, your privacy is preserved.
**What You'll Learn:**
- How to upload and manage documents
- Asking questions about your documents
- Creating knowledge bases for specific topics
- Managing storage and privacy
- Tips for best results

**Important:** All document processing happens on your device. Your documents never leave your phone.

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Uploading Documents](#uploading-documents)
3. [Asking Questions](#asking-questions)
4. [Managing Documents](#managing-documents)
5. [Knowledge Bases](#knowledge-bases)
6. [Language Support](#language-support)
7. [Privacy & Storage](#privacy--storage)
8. [Tips for Best Results](#tips-for-best-results)
9. [Troubleshooting](#troubleshooting)

---

## Getting Started

### What Can You Do?

**Document Q&A:**
- Upload a user manual → Ask "How do I reset my device?"
- Upload a recipe book → Ask "What ingredients do I need for lasagna?"
- Upload a research paper → Ask "What were the key findings?"

**Knowledge Bases:**
- Medical documents → Build your personal health knowledge base
- Legal contracts → Quick contract reference
- Technical manuals → Easy equipment troubleshooting

**Examples:**

```
You: [Upload product manual PDF]
AVA: Document processed: "Product Manual v2.1" (45 pages, 120 sections)
     Ready to answer questions!

You: What's the warranty period?
AVA: According to page 8 of the Product Manual, the warranty period
     is 2 years from the date of purchase for manufacturing defects.

     📄 Source: Product Manual v2.1, Page 8
```

---

## Uploading Documents

### Supported Formats

AVA can read:
- ✅ **PDF files** (.pdf) - Most common
- ✅ **Web pages** (URLs) - Articles, documentation
- ✅ **Images** (.jpg, .png) - Screenshots with text (OCR)
- 🔮 **Word documents** (.docx) - Coming soon
- 🔮 **eBooks** (.epub) - Coming soon

---

### How to Upload a PDF

**Step 1: Open RAG Manager**
1. Tap the **Menu** icon (☰) in AVA
2. Select **"Document Library"**
3. Tap the **"+"** button (bottom right)

**Step 2: Choose Document**
1. Select **"Upload PDF"**
2. Browse to your PDF file
3. Tap the file to select it

**Step 3: Wait for Processing**
- AVA will show a progress bar
- Processing time: ~5 seconds per 100 pages
- You'll see: "Processing complete: 45 pages, 120 sections"

**Step 4: Start Asking Questions!**
- Go back to chat
- Ask anything about the document
- AVA will cite which page the answer came from

---

### How to Add a Web Page

**Step 1: Copy the URL**
- Open your browser
- Navigate to the page you want to save
- Copy the URL (long-press → Copy)

**Step 2: Add to AVA**
1. In AVA, tap **Menu** → **Document Library**
2. Tap **"+"** → **"Add Web Page"**
3. Paste the URL
4. Tap **"Add"**

**Step 3: Wait for Processing**
- AVA fetches and processes the page
- Takes about 3-5 seconds
- You'll see: "Web page added: [Title]"

**Example:**
```
URL: https://docs.example.com/getting-started
AVA: ✅ Added: "Getting Started Guide"
     2,500 words processed, ready for questions!
```

---

### How to Upload Images (Text Recognition)

**Step 1: Take or Select Photo**
1. In Document Library, tap **"+"** → **"Upload Image"**
2. Either:
   - Take a photo of a page or sign
   - Select existing photo from gallery

**Step 2: Wait for OCR**
- AVA uses OCR (Optical Character Recognition)
- Extracts text from the image
- Takes 5-10 seconds per image

**Step 3: Review Accuracy**
- AVA shows confidence: "Text recognized: 95% confident"
- If low confidence (<80%), try retaking photo with better lighting

**Tips for Best OCR:**
- Good lighting (no shadows)
- Hold phone steady
- Flat surface (no wrinkles or bends)
- Clear, printed text (handwriting less accurate)

---

## Asking Questions

### Natural Language Questions

Ask AVA questions just like you would ask a person:

**✅ Good Questions:**
```
"What is the return policy?"
"How do I reset my password?"
"What were the main findings in this paper?"
"Summarize chapter 3"
"What ingredients are needed for recipe X?"
```

**❌ Avoid:**
```
"return policy" (too vague, not a question)
"tell me everything" (too broad)
```

---

### How AVA Answers

**Step 1: Understanding Your Question**
- AVA reads your question
- Identifies key concepts

**Step 2: Searching Documents**
- Searches all uploaded documents
- Finds the 5 most relevant sections
- Takes less than 0.5 seconds

**Step 3: Generating Answer**
- Reads the relevant sections
- Composes a clear answer
- Cites the source

**Step 4: Showing Sources**
- AVA tells you which document
- Shows page number (for PDFs)
- Gives confidence indicator (🟢 High, 🟡 Medium, 🔴 Low)

**Example:**
```
You: What's the cancellation policy?

AVA: You can cancel your subscription at any time without penalty.
     Refunds are issued within 7-14 business days to your original
     payment method. Partial month refunds are not available.

     📄 Source: Terms of Service, Section 8.2
     🟢 95% Confidence
```

---

### Multiple Documents

If you have multiple documents uploaded, AVA searches **all of them** and tells you which one has the answer.

**Example:**
```
You: What's the warranty?

AVA: The warranty is 2 years for electronic components and 1 year
     for accessories. See the warranty card for complete details.

     📄 Source: Product Manual v2.1, Page 8
     🟢 92%
```

---

## Managing Documents

### Viewing Your Documents

**Step 1: Open Document Library**
- Tap **Menu** (☰) → **Document Library**

**Step 2: Browse Documents**
- See all uploaded documents
- Shows: Title, Pages, Size, Date added
- Tap any document for details

**Document Details:**
- Title: "Product Manual v2.1"
- Pages: 45
- Sections: 120
- Size: 3.2 MB
- Added: Nov 27, 2025
- Language: English

---

### Deleting Documents

**Step 1: Select Document**
- In Document Library, long-press a document
- Or tap document → Tap trash icon (🗑️)

**Step 2: Confirm Deletion**
- AVA asks: "Delete 'Product Manual'? This cannot be undone."
- Tap **"Delete"** to confirm

**What Happens:**
- Document removed from library
- All associated data deleted
- Storage space freed up
- Cannot ask questions about it anymore

---

### Storage Usage

**Checking Storage:**
1. Tap **Menu** → **Settings** → **Document Storage**
2. See:
   - Total documents: 5
   - Storage used: 42 MB / 10 GB
   - Available: 9.96 GB

**Storage Limits:**
- Default limit: 10 GB
- Average PDF: ~5-10 MB
- You can store ~1,000 average PDFs
- Change limit in Settings

**What Uses Storage:**
- Original documents (PDFs, images)
- Processed text chunks
- Search indexes
- Cached data

---

## Knowledge Bases

### What Are Knowledge Bases?

Knowledge bases are collections of related documents organized by topic. Think of them as folders for your documents.

**Examples:**
- **Medical KB**: Health records, research papers, medication guides
- **Legal KB**: Contracts, agreements, policy documents
- **Technical KB**: Product manuals, repair guides, specifications
- **Cooking KB**: Recipe books, cooking guides

---

### Creating a Knowledge Base

**Step 1: Create New KB**
1. Tap **Menu** → **Knowledge Bases**
2. Tap **"+ New Knowledge Base"**
3. Enter name: "Medical Documents"
4. Choose category: Medical
5. Tap **"Create"**

**Step 2: Add Documents**
1. Open the knowledge base
2. Tap **"+ Add Documents"**
3. Select documents from library
4. Tap **"Add Selected"**

**Step 3: Activate Knowledge Base**
- Toggle the knowledge base **ON**
- AVA now searches only these documents
- Faster, more focused answers

**Example:**
```
[Medical KB Active]

You: What are the side effects of aspirin?

AVA: [Searches only medical documents]
     Common side effects include stomach upset, heartburn,
     and increased bleeding tendency...

     📄 Source: Medication Guide - Aspirin, Page 3
     🟢 94%
```

---

### Switching Knowledge Bases

**Quick Switch:**
1. In chat, tap **KB** icon (top right)
2. See list of knowledge bases:
   - 📚 All Documents (Default)
   - 🏥 Medical Documents
   - ⚖️ Legal Documents
   - 🔧 Technical Manuals
3. Tap the one you want to use

**What Changes:**
- Active KB shown at top: [🏥 Medical KB]
- AVA searches only that KB
- Faster search (fewer documents)
- More relevant answers

---

## Language Support

### Multilingual Documents

AVA supports **100+ languages** for document Q&A!

**How It Works:**
1. Upload document in any language
2. AVA auto-detects language
3. Ask questions in **your** preferred language
4. AVA translates and answers

**Example (Spanish document, English question):**
```
Document: "Manual de Usuario" (Spanish)

You: What's the reset procedure?

AVA: Press and hold the power button for 10 seconds until
     the LED flashes blue, then release.

     📄 Source: Manual de Usuario, Página 12
     🟢 88%
```

---

### Changing Language Mode

**Step 1: Open Settings**
1. Tap **Menu** → **Settings** → **Language**

**Step 2: Choose Mode**
- **English-only**: Faster, uses less storage
- **Multilingual**: 100+ languages, needs download

**Step 3: Download Model (if needed)**
- Multilingual mode needs a 30 MB model
- One-time download
- Works offline after download

**Language Mode Affects:**
- ✅ Document Q&A (RAG)
- ✅ Voice commands (NLU)
- ✅ Chat (LLM)
- All features use the same language setting

---

## Privacy & Storage

### Privacy First

**Your Data Stays on Your Device:**
- ✅ Documents stored locally
- ✅ Processing happens on your phone
- ✅ No cloud upload (unless you use cloud LLM)
- ✅ You control when to delete

**When Using Cloud LLM:**
- Document content **is** sent to cloud for answers
- Only the relevant sections (not entire document)
- See Settings → Privacy → Cloud LLM for details

**When Using Local LLM:**
- 100% private
- Nothing leaves your device
- Fully offline

---

### Storage Locations

**Where Documents Are Stored:**

**Internal Storage:**
- Path: `/storage/emulated/0/Android/data/com.augmentalis.ava/files/rag/`
- Only AVA can access
- Deleted when you uninstall AVA

**External Storage (Optional):**
- Path: `/sdcard/.AVAVoiceAvanues/rag/`
- Hidden folder (dot prefix)
- Survives app reinstall
- Enable in Settings → Storage → Use External Storage

---

### Clearing Data

**Clear All Documents:**
1. Settings → Document Storage → **"Clear All Documents"**
2. Confirm: "This will delete all documents and cannot be undone."
3. Tap **"Clear"**

**What's Deleted:**
- All uploaded documents
- All processed chunks
- All search indexes
- Storage freed immediately

**What's NOT Deleted:**
- App settings
- Chat history (unless you clear that too)
- Models (NLU, LLM, RAG embedding model)

---

## Tips for Best Results

### 1. Use Clear, Specific Questions

**✅ Good:**
```
"What is the warranty period for electronic components?"
"How do I perform a factory reset?"
"What were the key findings in the 2023 study?"
```

**❌ Too Vague:**
```
"Warranty?"
"Reset?"
"Findings?"
```

---

### 2. Upload Quality Documents

**Best Results:**
- Clear, readable PDFs
- Well-structured text
- Good quality scans (if scanned PDF)
- Modern fonts (avoid handwriting for OCR)

**Lower Quality:**
- Scanned images with poor lighting
- Handwritten notes (OCR less accurate)
- Complex multi-column layouts
- Very old PDFs with encoding issues

---

### 3. Organize with Knowledge Bases

Instead of uploading 50 documents and searching all of them:

**Better Approach:**
- Create topic-specific KBs
- Activate the relevant KB before asking
- Faster search, better answers

---

### 4. Check Source Citations

Always look at the source AVA cites:

```
📄 Source: Product Manual v2.1, Page 8
```

- Verify it's the right document
- Check the page if you need more context
- If wrong source, rephrase your question

---

### 5. Use Follow-Up Questions

AVA remembers context:

```
You: What's the warranty?
AVA: 2 years for electronic components...

You: What about accessories?
AVA: Accessories have a 1-year warranty...

You: How do I claim a warranty?
AVA: To claim a warranty, contact customer support at...
```

---

## Troubleshooting

### Issue: "Document processing failed"

**Possible Causes:**
- PDF is corrupted
- File too large (>100 MB)
- Not enough storage
- Unsupported PDF format

**Solutions:**
1. Check file size: Settings → Document Storage → Free space
2. Try a different PDF viewer to verify file works
3. Free up storage: Delete old documents
4. Try re-downloading the PDF

---

### Issue: "No answer found"

**Symptoms:**
- AVA says: "I don't have enough information to answer that."
- Or: "I couldn't find relevant information in the documents."

**Possible Causes:**
- Answer is not in the uploaded documents
- Question phrased too differently from document text
- Wrong knowledge base active

**Solutions:**
1. Rephrase your question
2. Check if correct document is uploaded
3. Switch to "All Documents" KB (search everything)
4. Verify document content has the info

---

### Issue: Answers are inaccurate

**Symptoms:**
- AVA gives wrong page number
- Answer doesn't match source
- Confidence low (🔴 <60%)

**Possible Causes:**
- Document had poor OCR (if scanned)
- Question ambiguous
- Similar topics in document confused AVA

**Solutions:**
1. Check the cited page manually
2. Rephrase question to be more specific
3. If scanned PDF, try re-scanning with better quality
4. Use exact terminology from document

---

### Issue: Slow processing

**Symptoms:**
- Upload takes >30 seconds for small PDF
- Questions take >5 seconds to answer

**Possible Causes:**
- Device is low-end
- Many documents uploaded (>100)
- Language mode: Multilingual (slower than English-only)
- Local LLM enabled (slower than cloud)

**Solutions:**
1. Use English-only mode: Settings → Language → English-only
2. Use Cloud LLM: Settings → LLM Provider → Anthropic/OpenAI
3. Delete old documents: Document Library → Delete unused
4. Restart AVA

---

### Issue: "Model not found"

**Error:**
```
❌ RAG model not found. Please download the model.
```

**Cause:**
- Multilingual RAG model not downloaded
- Model deleted accidentally

**Solution:**
1. Go to Settings → Language → Multilingual
2. Tap **"Download Model"** (30 MB)
3. Wait for download to complete
4. Restart AVA

**Or Use English-Only:**
1. Settings → Language → English-only
2. No download needed (included in app)

---

## What's Next?

Now that you know how to use Document Q&A, try these:

1. **Upload your first document**: Start with a simple PDF
2. **Ask 5 questions**: Get familiar with how AVA answers
3. **Create a knowledge base**: Organize related documents
4. **Explore multilingual**: Upload documents in other languages
5. **Try voice commands**: See Chapter 11 for voice control

---

## Related Chapters

- **Chapter 10**: [Model Installation](User-Manual-Chapter10-Model-Installation.md) - Download multilingual models
- **Chapter 11**: [Voice Commands](User-Manual-Chapter11-Voice-Commands.md) - Use voice to ask questions
- **Chapter 13**: Memory & Context (Coming Soon) - How AVA remembers conversations

---

## Need Help?

**In-App Support:**
- Tap Menu → Help → Document Q&A

**Community:**
- Forum: https://community.augmentalis.ai
- Discord: https://discord.gg/ava-ai

**Report Issues:**
- Email: support@augmentalis.ai
- Include: App version, device model, error message

---

**Version**: 1.1.0
**Last Updated**: November 27, 2025
**For AVA AI App Version**: 1.1+
**Maintained By**: AVA AI Team
