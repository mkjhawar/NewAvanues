# AVA AI - Interactive Demo Suite

**Production-Ready HTML Demos** showcasing AVA's privacy-first AI assistant features.

## 🎯 Overview

This demo suite provides interactive, single-file HTML demonstrations of AVA AI's core features. Each demo runs standalone in any modern browser with **zero dependencies** and **no build step required**.

## 📁 Demos Available

### 1. **index.html** - Demo Suite Landing Page
- **Purpose**: Central hub for all demos
- **Features**:
  - Overview of AVA AI capabilities
  - Quick stats (95%+ local processing, <50ms NLU, 25.5MB model)
  - Navigation to all feature demos
- **Open**: `file:///Volumes/M-Drive/Coding/ava/demos/index.html`

---

### 2. **chat.html** - Chat Interface Demo 💬
- **Purpose**: Simulate AVA's conversational UI with NLU classification
- **Features**:
  - Real-time intent classification (<50ms simulated)
  - Confidence badges (High/Medium/Low)
  - Auto-trigger Teach-AVA for low confidence (<70%)
  - Message history with user/assistant bubbles
  - Live statistics (message count, avg latency, avg confidence)
  - Recent intents tracking
  - Example prompts (weather, translate, reminder, search, query)
- **Tech**: Material 3 design, gradient UI, responsive phone frame
- **Try**: Type messages or click example prompts

---

### 3. **teach.html** - Teach-AVA Training Demo 🎓
- **Purpose**: User-driven training interface for custom intents
- **Features**:
  - Add/Edit/Delete training examples
  - MD5 hash deduplication (prevents duplicates)
  - Filter by intent (search, translate, reminder, query, general)
  - Multi-locale support (English, Spanish, French)
  - Stats tracking (total examples, intents, locales)
  - FAB (Floating Action Button) for quick add
- **Tech**: Modal dialogs, CRUD operations, animated cards
- **Try**: Click "+" to add examples, filter intents, edit/delete cards

---

### 4. **overlay.html** - Smart Glasses Overlay Demo 👓
- **Purpose**: Context-aware voice orb for 8+ smart glasses devices
- **Features**:
  - **Draggable voice orb** (64dp glassmorphic design)
  - **5 states**: Docked, Listening, Processing, Responding, Error
  - **9 app categories**: Browser, Messaging, Email, Social, Productivity, Maps, Shopping, Media
  - **Context-aware suggestions**: Different chips per app category
  - **App detection**: Simulated <100ms context detection
  - **Supported devices**: Meta Ray-Ban, Vuzix Blade 2, Rokid Air, XREAL Air 2, etc.
- **Tech**: Backdrop filters, CSS animations, drag-and-drop
- **Try**: Drag the orb, change states, switch active apps to see different suggestions

---

### 5. **nlu.html** - NLU Classification Demo 🧠
- **Purpose**: ONNX Runtime with MobileBERT INT8 intent classification
- **Features**:
  - **Real-time classification**: Top 3 intents with confidence scores
  - **Performance metrics**: Tokenization (<5ms), Inference (<50ms), Total (<60ms)
  - **Token count**: WordPiece tokenization simulation
  - **Intent distribution**: Live bar charts for 6 intents
  - **Technical specs**: Model size, vocab, runtime, acceleration details
  - **Example prompts**: 5 pre-built test cases
- **Tech**: Progress bars, confidence color coding, analytics dashboard
- **Try**: Type custom text or click examples to see classification results

---

## 🚀 Quick Start

### Option 1: Open Directly (Recommended)
```bash
# From the demos directory
open index.html   # macOS
```

Or simply **double-click** `index.html` in Finder.

### Option 2: Local Web Server (Optional)
```bash
# From the demos directory
python3 -m http.server 8000

# Open browser to:
# http://localhost:8000/
```

### Option 3: VS Code Live Server
1. Install "Live Server" extension
2. Right-click `index.html`
3. Select "Open with Live Server"

---

## 📊 Technical Details

### Design System
- **Colors**: Blues & Greys (Professional palette - see [`THEME.md`](./THEME.md))
- **Primary Gradient**: `#2563eb → #1e40af` (Blue 600 → Blue 800)
- **Typography**: -apple-system (San Francisco on macOS)
- **Effects**: Glassmorphism, backdrop-filter blur
- **Animations**: Smooth 300ms transitions
- **Layout**: CSS Grid, Flexbox

### Phone Frame Simulation
- **Dimensions**: 400px × 711px (iPhone 13 Pro ratio)
- **Border Radius**: 40px device, 30px screen
- **Shadow**: 0 10px 40px rgba(0,0,0,0.3)
- **States**: Portrait/Landscape toggle (where applicable)

### Performance Budgets (Simulated)
- **NLU Tokenization**: 2-5ms
- **NLU Inference**: 25-55ms
- **Total NLU**: <60ms
- **Context Detection**: <100ms
- **Chat Latency**: 20-60ms

---

## 🎨 Customization

### Changing Colors

**See full theme documentation**: [`THEME.md`](./THEME.md)

Current color scheme (Blues & Greys):

```css
/* Primary Gradient */
background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%);

/* Background */
background: #f1f5f9;

/* Text */
color: #0f172a; /* Primary */
color: #64748b; /* Secondary */
```

To customize, replace these values throughout the HTML files or use the CSS variables documented in `THEME.md`.

### Modifying Intents
Edit the intent templates in each demo's `<script>` section:

```javascript
// chat.html
const intentTemplates = {
    search: { response: "Custom response", confidence: 0.88 },
    // Add your custom intents...
};
```

### Adding Examples
Insert new training examples in `teach.html`:

```html
<div class="training-card" data-intent="your_intent">
    <div class="card-header">
        <span class="intent-badge">your_intent</span>
        <!-- ... -->
    </div>
    <div class="utterance-text">Your example text</div>
    <!-- ... -->
</div>
```

---

## 🧪 Testing Scenarios

### Chat Demo
1. **High Confidence**: "What's the weather today?" → weather (90%+)
2. **Medium Confidence**: General queries (50-70%)
3. **Low Confidence**: Unknown input → Triggers Teach-AVA suggestion
4. **Performance**: Check stats (latency, confidence, intents)

### Teach-AVA Demo
1. **Add Example**: Click "+", enter text, select intent/locale
2. **Filter**: Click intent buttons to filter cards
3. **Edit/Delete**: Click pencil/trash icons on cards
4. **Stats**: Watch total/intent/locale counts update

### Overlay Demo
1. **Drag Orb**: Click and drag the voice orb anywhere
2. **Change State**: Click state buttons (Docked/Listening/Processing/Responding/Error)
3. **Switch Apps**: Select different apps to see context-aware suggestions
4. **Suggestions**: Click suggestion chips to simulate actions

### NLU Demo
1. **Custom Input**: Type text and click "Classify Intent"
2. **Examples**: Click pre-built examples for instant results
3. **Analytics**: Watch intent distribution bars grow
4. **Performance**: Verify <60ms total time consistently

---

## 📱 Mobile Responsiveness

All demos are optimized for desktop viewing (1400px+ recommended). For mobile:
- Phone frame is fixed at 400px width
- Use landscape orientation for better experience
- Scroll to see full content

---

## 🔗 Integration with AVA AI

These demos simulate the following AVA modules:

| Demo | AVA Module | Source Files |
|------|-----------|--------------|
| **chat.html** | `features:chat` | `ChatScreen.kt`, `ChatViewModel.kt`, `MessageBubble.kt` |
| **teach.html** | `features:teachava` | `TeachAvaScreen.kt`, `TeachAvaViewModel.kt`, `TrainingExampleCard.kt` |
| **overlay.html** | `features:overlay` | `OverlayController.kt`, `ContextEngine.kt`, `VoiceOrb.kt` |
| **nlu.html** | `features:nlu` | `IntentClassifier.kt`, `BertTokenizer.kt`, `ModelManager.kt` |

---

## 🚨 Known Limitations

1. **No Real NLU**: Demos use rule-based classification (not ONNX Runtime)
2. **No Database**: All data is in-memory (refresh resets state)
3. **No Voice**: Voice input is simulated via text
4. **No Persistence**: Training examples are not saved
5. **Simplified UI**: Production app has more features (history, settings, etc.)

---

## 📄 File Structure

```
demos/
├── index.html           # Landing page (main entry)
├── chat.html           # Chat interface demo
├── teach.html          # Teach-AVA training demo
├── overlay.html        # Smart glasses overlay demo
├── nlu.html            # NLU classification demo
├── architecture.html   # Architecture viewer demo
├── performance.html    # Performance dashboard demo
├── README.md           # This file
└── THEME.md            # Complete theme documentation
```

**Total**: 7 HTML files, 2 documentation files (zero dependencies)

---

## 🎓 Educational Use

These demos are designed for:
- **User Testing**: Validate UX/UI designs
- **Stakeholder Demos**: Showcase AVA features without full build
- **Developer Onboarding**: Understand feature behavior before coding
- **Documentation**: Visual supplements to technical docs
- **Presentations**: Live demos for pitches/demos

---

## 🛠️ Maintenance

### Updating Demos
1. Edit HTML files directly (all CSS/JS is inline)
2. Test in Chrome/Safari/Firefox
3. Ensure <100KB file size per demo (for fast loading)
4. Maintain zero dependencies (no external libraries)

### Version Control
- Demos are tracked in Git (`/demos` directory)
- Commit message format: `docs(demos): Add/Update [demo-name]`
- Tag releases: `v1.0.0-demos` (separate from app releases)

---

## 📞 Support

**Questions or Issues?**
- Review AVA Developer Manual: `/docs/Developer-Manual-Complete.md`
- Check source code: `/Universal/AVA/Features/[module]`
- Contact: Manoj Jhawar, manoj@ideahq.net

---

**Created by**: Manoj Jhawar, manoj@ideahq.net
**Last Updated**: 2025-11-02
**AVA Version**: Phase 1 MVP (Week 6)
**Demo Suite Version**: 1.0.0

---

*These demos are part of the AVA AI project, a privacy-first, user-trainable AI assistant for Android and smart glasses.*
