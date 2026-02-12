# iOS Voice Control Capabilities - Comprehensive Analysis
**VoiceOSCore-Analysis-iOSVoiceControlCapabilities-260212-V1**

Author: Research Analysis
Date: 2026-02-12
Scope: iOS voice recognition, accessibility services, web control, and KMP feasibility

---

## Executive Summary

This document provides a comprehensive analysis of iOS capabilities for building voice-controlled browser/web apps, comparing them to Android's AccessibilityService model used in VoiceOSCore. The analysis covers speech recognition APIs, accessibility services, web control capabilities, platform limitations, and Kotlin Multiplatform (KMP) viability for iOS.

**Key Findings:**
- ✅ iOS has excellent on-device speech recognition (SFSpeechRecognizer, new SpeechAnalyzer in iOS 26)
- ✅ WKWebView supports JavaScript injection and DOM scraping
- ✅ KMP for iOS is production-ready as of 2025 (Kotlin stable, Compose Multiplatform 1.8.0 stable)
- ❌ **iOS does NOT allow third-party apps to read UI elements from other apps** (critical limitation vs Android)
- ❌ No overlay permissions for third-party apps (unlike Android's TYPE_ACCESSIBILITY_OVERLAY)
- ❌ Background microphone access severely restricted (no always-listening capability)
- ⚠️ App Store review guidelines require careful compliance for voice/accessibility features

---

## 1. iOS Voice Recognition APIs

### 1.1 Speech Framework (SFSpeechRecognizer)

**Official Documentation:** [Apple Developer - Speech](https://developer.apple.com/documentation/speech)

**Capabilities:**
- **On-device vs Cloud:** `supportsOnDeviceRecognition` property determines mode
  - `true`: Processes locally (private, faster, works offline)
  - `false`: Uses Apple servers (requires network)
- **Languages:** Supports 50+ languages with `SFSpeechRecognizer.supportedLocales()`
- **Streaming vs Batch:** Both supported
  - Streaming: `SFSpeechAudioBufferRecognitionRequest` for live audio
  - Batch: `SFSpeechURLRecognitionRequest` for pre-recorded files
- **Always-listening:** ❌ **NOT SUPPORTED** - See section 4 on background limitations

**Code Example:**
```swift
let recognizer = SFSpeechRecognizer(locale: Locale(identifier: "en-US"))
if recognizer?.supportsOnDeviceRecognition == true {
    // On-device processing available
}
```

**References:**
- [Recognizing speech in live audio](https://developer.apple.com/documentation/Speech/recognizing-speech-in-live-audio)
- [iOS Speech Recognition (SFSpeechRecognizer)](https://theptrk.com/2025/08/24/ios-speech-recognition-sfspeechrecognizer/)

### 1.2 SpeechAnalyzer (NEW in iOS 26 - 2026)

**Official Announcement:** [WWDC25 - SpeechAnalyzer](https://developer.apple.com/videos/play/wwdc2025/277/)

**Major Improvements over SFSpeechRecognizer:**
- **Faster performance** with on-device processing
- **Long-form audio support** (lectures, meetings, conversations)
- **Distant audio support** (not just close-proximity microphone)
- **System-managed models** - doesn't increase app size or memory footprint
- **Three transcriber engines:**
  1. **DictationTranscriber** - Natural, punctuation-aware dictation
  2. **SpeechTranscriber** - Clean speech-to-text ideal for **voice commands**
  3. **SpeechDetector** - Detects speech presence/timing without full transcription

**VoiceOSCore Relevance:**
SpeechTranscriber is purpose-built for command recognition and would be ideal for VoiceAvanue on iOS.

**References:**
- [iOS 26: SpeechAnalyzer Guide](https://antongubarenko.substack.com/p/ios-26-speechanalyzer-guide)
- [On-Device Speech Transcription with Apple SpeechAnalyzer](https://www.callstack.com/blog/on-device-speech-transcription-with-apple-speechanalyzer)
- [Apple SpeechAnalyzer and Argmax WhisperKit](https://www.argmaxinc.com/blog/apple-and-argmax)

### 1.3 SiriKit / App Intents

**Official Documentation:** [Apple Developer - SiriKit](https://developer.apple.com/documentation/sirikit/)

**Capabilities:**
- Register **custom voice commands** via Intent Definition Files
- Create Intents Extension target in Xcode
- Support Siri Shortcuts for frequently-performed actions
- Register custom vocabulary for app-specific terminology

**Limitations:**
- ❌ Commands are **Siri-triggered only** (user must say "Hey Siri, [command]")
- ❌ Cannot intercept arbitrary voice input like Android AccessibilityService
- ❌ Requires user to learn specific Siri phrases
- ⚠️ Intent domains are limited to predefined categories (messaging, payments, workouts, etc.)

**Custom Intents (iOS 12+):**
- Can define custom intents outside predefined domains
- Still requires Siri invocation
- Useful for app-specific shortcuts, NOT system-wide voice control

**Code Example:**
```swift
// Define custom intent in .intentdefinition file
// Implement handler:
class IntentHandler: INExtension, MyCustomIntentHandling {
    func handle(intent: MyCustomIntent, completion: @escaping (MyCustomIntentResponse) -> Void) {
        // Perform action
        completion(MyCustomIntentResponse(code: .success, userActivity: nil))
    }
}
```

**References:**
- [Streamlining iOS Voice-Activated Apps with SiriKit](https://povio.com/blog/streamlining-ios-voice-activated-apps-with-sirikit)
- [Swift and SiriKit: Adding Voice Commands to Your App](https://commitstudiogs.medium.com/swift-and-sirikit-adding-voice-commands-to-your-app-d78604210bf9)
- [Registering Custom Vocabulary with SiriKit](https://developer.apple.com/documentation/sirikit/registering-custom-vocabulary-with-sirikit)

### 1.4 Voice Control (iOS 13+ Accessibility Feature)

**Official Documentation:** [Use Voice Control on iPhone](https://support.apple.com/en-us/111778)

**What It Is:**
- System-level accessibility feature (Settings > Accessibility > Voice Control)
- Allows hands-free operation of iPhone/iPad
- Commands like "Tap [element name]", "Scroll down", "Open [app]"
- Overlay grid system for precise targeting ("Show grid", "Tap number 5")

**Third-Party App Integration:**
- ✅ Apps using **Apple's accessibility API automatically supported**
- ✅ Proper `accessibilityLabel`, `accessibilityHint` labels enable Voice Control
- ❌ **Third-party apps CANNOT hook into or extend Voice Control**
- ❌ **Third-party apps CANNOT create custom Voice Control overlays**

**Comparison to Android:**
| Feature | iOS Voice Control | Android VoiceOSCore |
|---------|------------------|---------------------|
| System-level voice commands | ✅ Built-in | ✅ Custom via AccessibilityService |
| Third-party extensibility | ❌ No | ✅ Yes |
| Custom overlays | ❌ Apple-only | ✅ Yes (TYPE_ACCESSIBILITY_OVERLAY) |
| Read other apps' UI | ❌ No | ✅ Yes (AccessibilityEvent) |
| Custom command generation | ❌ No | ✅ Yes (scraping-based) |

**References:**
- [Apple's Voice Control improves accessibility OS-wide](https://techcrunch.com/2019/06/03/apples-voice-control-improves-accessibility-os-wide-on-all-its-devices/)
- [New in iOS 13 Accessibility - Voice Control and More](https://www.deque.com/blog/new-in-ios-13-accessibility-voice-control-and-more/)
- [Hands on with Apple's new voice control accessibility feature](https://appleinsider.com/articles/19/06/07/hands-on-with-apples-new-voice-control-accessibility-feature)

### 1.5 Natural Language Framework (On-Device NLU)

**Official Documentation:** [Apple Developer - Natural Language](https://developer.apple.com/documentation/naturallanguage)

**Capabilities:**
- **On-device NLU processing** (no cloud required)
- **Language detection:** Identify language from text
- **Tokenization:** Split text into words/sentences
- **Part-of-speech tagging:** Identify nouns, verbs, adjectives
- **Lemmatization:** Get root form of words
- **Named Entity Recognition (NER):** Extract person names, places, organizations
- **Sentiment analysis:** Determine positive/negative/neutral tone
- **Custom ML models:** Train models via CreateML for intent classification

**Intent Classification:**
- ✅ Can build custom intent classifiers with CreateML
- ✅ 50% faster response time vs server-side (on-device)
- ✅ Privacy-preserving (no data sent to cloud)
- ⚠️ Requires training data and model creation

**VoiceOSCore Relevance:**
Could parse voice commands locally to classify user intent (e.g., "open settings" → AppControl intent).

**References:**
- [Natural Language on iOS](https://stefanblos.com/posts/natural-language-on-ios/)
- [Enhancing iOS Apps with Natural Language Processing and Core ML](https://moldstud.com/articles/p-enhancing-ios-apps-with-natural-language-processing-and-core-ml-a-comprehensive-guide)
- [Offline Natural Language Understanding Engine on iOS](http://hongchaozhang.github.io/blog/2019/05/22/offline-natural-language-understanding-engine-on-ios/)

---

## 2. iOS Accessibility Services Equivalent

### 2.1 UIAccessibility Protocol

**Official Documentation:** [UIAccessibility - Apple Developer](https://developer.apple.com/documentation/uikit/uiaccessibility-protocol)

**What It Is:**
- Protocol providing accessibility information about UI elements
- Used by VoiceOver, Voice Control, and other assistive technologies
- Apps implement `isAccessibilityElement`, `accessibilityLabel`, `accessibilityHint`, etc.

**Accessibility Tree (AUI):**
- iOS creates an **Accessibility UI (AUI)** from the visual interface
- VoiceOver and Voice Control navigate this tree
- Standard UIKit controls automatically implement UIAccessibility

**CRITICAL LIMITATION for VoiceOSCore:**
❌ **Third-party apps CANNOT read the accessibility tree of OTHER apps**
- UIAccessibility exposes **only the app's own UI** to assistive technologies
- Only **Apple's system services** (VoiceOver, Voice Control) can read other apps' accessibility trees
- **iOS app sandboxing prevents cross-app UI inspection**

**Comparison to Android AccessibilityService:**
| Capability | Android AccessibilityService | iOS UIAccessibility |
|------------|----------------------------|---------------------|
| Read own app's UI | ✅ Yes | ✅ Yes |
| Read OTHER apps' UI | ✅ Yes (`AccessibilityEvent.getSource()`) | ❌ **NO - Sandboxed** |
| Draw overlays | ✅ Yes (`TYPE_ACCESSIBILITY_OVERLAY`) | ❌ **NO - Apple-only** |
| Perform actions on other apps | ✅ Yes (`AccessibilityNodeInfo.performAction()`) | ❌ **NO** |
| Generate commands from scraped UI | ✅ Yes (VoiceOSCore model) | ❌ **IMPOSSIBLE** |

**References:**
- [UIAccessibility - NSHipster](https://nshipster.com/uiaccessibility/)
- [Accessibility element - iOS development](https://medium.com/short-swift-stories/accessibility-element-2d55cefdf9d7)
- [Understanding the Accessible User Interface](https://www.createwithswift.com/understanding-the-accessible-user-interface/)

### 2.2 Third-Party Screen Readers: NOT ALLOWED

**Official Forum Response:** [Third party screen reader for iOS](https://developer.apple.com/forums/thread/737069)

Apple explicitly states:
> "iOS does not allow developers to implement third-party screen readers with custom features for blind people through standard accessibility APIs."

**Why This Matters:**
- The entire VoiceOSCore Android architecture relies on **scraping other apps' UI elements**
- This is **fundamentally impossible** on iOS due to sandboxing
- Third-party apps cannot access the accessibility tree of Safari, Chrome, system apps, or other third-party apps

**References:**
- [iOS vs. Android Accessibility](https://pauljadam.com/iosvsandroida11y/)
- [Android Vs. iOS: Accessibility Features Compared](https://www.iamhable.com/en-am/blogs/article/android-vs-ios-accessibility-features-compared)

### 2.3 Overlay Permissions: RESTRICTED

**Finding:** iOS does NOT provide overlay permissions for third-party apps.

**Forum Discussion:** [Overlay and Accessibility Permissions for iOS Apps](https://discussions.apple.com/thread/256116103)

**Developer Approach:**
- Apps can request **overlay permissions** (with user prompt)
- Apps can request **accessibility settings** access
- ⚠️ **App Store review may reject** apps requiring these permissions without clear justification

**Comparison to Android:**
- Android: `WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY` allows overlays system-wide
- iOS: No equivalent - overlays restricted to within-app views (e.g., floating buttons on app's own UI)

**VoiceOSCore Impact:**
- Cannot draw voice command overlays on Safari or other apps
- Can ONLY overlay on WKWebView **within your own app**

**References:**
- [Overlay Attacks: Top Techniques and How to Counter Them](https://www.appsealing.com/overlay-attacks/)

---

## 3. iOS WebView Voice Control

### 3.1 WKWebView JavaScript Injection

**Official Documentation:** [WKUserScript - Apple Developer](https://developer.apple.com/documentation/webkit/wkuserscript)

**Capabilities:**
✅ **Full JavaScript injection and DOM scraping** within WKWebView

**Injection Timing:**
- **`atDocumentStart`**: Runs immediately after `document` element created (before parsing)
- **`atDocumentEnd`**: Runs after parsing, before subresources load (DOMContentLoaded)

**Code Example:**
```swift
let webView = WKWebView()
let userContentController = WKUserContentController()

// Inject JavaScript at document start
let script = WKUserScript(
    source: "console.log('Injected!'); document.body.style.background = 'blue';",
    injectionTime: .atDocumentStart,
    forMainFrameOnly: true
)
userContentController.addUserScript(script)

// Configuration
let config = WKWebViewConfiguration()
config.userContentController = userContentController
webView = WKWebView(frame: .zero, configuration: config)
```

**DOM Scraping:**
```swift
// Evaluate JavaScript to scrape DOM
webView.evaluateJavaScript("document.getElementsByTagName('*').length") { (result, error) in
    if let count = result as? Int {
        print("Total elements: \(count)")
    }
}
```

**Security (iOS 14+):**
- **`evaluateJavaScript:inFrame:inContentWorld:completionHandler:`** - Runs in sandboxed environment
- Mitigates collision attacks with untrusted web JavaScript

**VoiceOSCore Integration:**
✅ **YES - Can inject DOMScraperBridge JavaScript into WKWebView**
✅ **YES - Can scrape all DOM elements** (same as Android WebAvanue)
✅ **YES - Can overlay voice targets** on WKWebView

⚠️ **LIMITATION:** Only works within **your own app's WKWebView**, NOT Safari or other browsers

**References:**
- [JavaScript Manipulation on iOS Using WebKit](https://medium.com/capital-one-tech/javascript-manipulation-on-ios-using-webkit-2b1115e7e405)
- [Injecting JavaScript Into Web View In iOS](https://swiftsenpai.com/development/web-view-javascript-injection/)
- [IOS/Swift: WebView: Javascript Injection, Pop ups, New Tabs](https://medium.com/@itsuki.enjoy/ios-swift-webview-javascript-injection-pop-ups-new-tabs-user-agent-and-cookies-1e46d04262b0)

### 3.2 Safari Web Extensions

**Official Documentation:** [Safari Extensions - Apple Developer](https://developer.apple.com/safari/extensions/)

**Capabilities:**
- ✅ **Content scripts** - Inject JavaScript into web pages
- ✅ **DOM manipulation** via content scripts
- ✅ **Background scripts** - Run extension logic
- ⚠️ Content scripts have **limited extension API access** (can call `browser.runtime.sendMessage`, `browser.storage`)
- ⚠️ Background scripts have **no direct DOM access** (must go through content scripts)

**2025 Updates:**
- Support for `WKWebExtension`, `WKWebExtensionContext`, `WKWebExtensionController` Swift/Objective-C classes
- Integration into WebKit-based browsers (Mac, iPhone, iPad)

**Voice Overlay Feasibility:**
- ✅ Can inject voice command overlay into Safari web pages
- ✅ Can scrape DOM elements
- ❌ **Cannot overlay on Safari's native UI** (address bar, tabs, system chrome)
- ❌ **Cannot extend to other apps**

**References:**
- [Implement a Safari iOS Extension with React Step-By-Step](https://medium.com/@gabrieIa/implement-a-safari-ios-extension-with-react-step-by-step-268665c1c4dd)
- [Injecting a script into a webpage](https://developer.apple.com/documentation/safariservices/safari_app_extensions/injecting_a_script_into_a_webpage)
- [Safari by Apple - Release Notes - December 2025](https://releasebot.io/updates/apple/safari)

### 3.3 iOS Voice Control + Web Content

**How It Works:**
- iOS Voice Control reads `aria-label`, `accessibilityLabel` from web elements
- Users can say "Tap [label]" to interact with web buttons, links
- Grid overlay allows precise targeting ("Show grid", "Tap 5")

**Third-Party Integration:**
- ❌ Cannot programmatically trigger Voice Control
- ❌ Cannot extend Voice Control commands
- ✅ Can improve compatibility by adding proper ARIA labels to web elements

---

## 4. iOS Background Audio / Always Listening

### 4.1 Background Microphone Restrictions

**Finding:** iOS **severely restricts** background microphone access.

**Restrictions:**
- ❌ Apps **cannot listen for voice commands while backgrounded** by default
- ❌ Microphone access **suspended when app backgrounded**
- ⚠️ Limited exceptions for **VoIP apps** (requires PushKit, strict review)
- ⚠️ iOS 13+ **tightened VoIP restrictions** - only actual internet calls allowed

**Comparison to Android:**
| Feature | Android VoiceOSCore | iOS |
|---------|---------------------|-----|
| Foreground service | ✅ Yes (persistent notification) | ❌ No equivalent |
| Always-listening | ✅ Yes (AccessibilityService) | ❌ **NO** |
| Background mic access | ✅ Yes (with permission) | ❌ **Suspended** |
| VoIP exception | N/A | ⚠️ VoIP only (strict review) |

**iOS Approach:**
- **"Hey Siri"** - System-level always-listening (Apple-only)
- **Push Notifications** - Wake app on-demand
- **Shortcuts** - User-triggered automation

**VoiceOSCore Impact:**
❌ **Cannot implement always-listening voice control** like Android foreground service

**References:**
- [Microphone background service - Apple Developer Forums](https://developer.apple.com/forums/thread/106415)
- [Microphone privacy on iOS and Android](https://grokipedia.com/page/Microphone_privacy_on_iOS_and_Android)

### 4.2 Audio Session Categories

**Official Documentation:** [AVAudioSession - Apple Developer](https://developer.apple.com/documentation/avfaudio/avaudiosession)

**Relevant Categories:**
- **`AVAudioSessionCategoryRecord`** - For recording audio
- **`AVAudioSessionCategoryPlayAndRecord`** - For VoIP apps
- **Background mode:** `audio` (for playing audio) or `voip` (for VoIP)

**Code Example:**
```swift
// Configure audio session for recording
let audioSession = AVAudioSession.sharedInstance()
try? audioSession.setCategory(.record, mode: .default)
try? audioSession.setActive(true)
```

**Background Modes:**
```xml
<!-- Info.plist -->
<key>UIBackgroundModes</key>
<array>
    <string>audio</string>  <!-- For continuous audio playback -->
    <string>voip</string>   <!-- For VoIP apps (strict review) -->
</array>
```

**Reality Check:**
- `audio` mode requires **continuous audio playback** (not mic listening)
- `voip` mode requires **actual internet calling functionality**
- Apple **will reject** apps abusing background modes for always-listening

**References:**
- [Do I Need Special Permissions For Voice Features In My App?](https://thisisglance.com/learning-centre/do-i-need-special-permissions-for-voice-features-in-my-app)

### 4.3 Privacy Restrictions

**Microphone Indicator (iOS 14+):**
- 🟠 **Orange dot** appears when microphone is active
- Control Center shows which app recently used microphone
- Users can easily detect background mic usage

**User Control:**
- Explicit user opt-in required for microphone access
- Settings > Privacy > Microphone - per-app toggles
- Default denial (no mic access without affirmative permission)

**"Hey Siri" vs Third-Party:**
- "Hey Siri" uses **dedicated low-power chip** (no app access)
- Only activates after trigger phrase
- Third-party apps **cannot replicate** this system-level integration

**References:**
- [3 Ways to See What Apps are Using the Microphone](https://www.hollyland.com/blog/tips/see-what-apps-are-using-the-microphone-of-iphone)
- [Control access to hardware features on iPhone](https://support.apple.com/guide/iphone/control-access-to-hardware-features-iph168c4bbd5/ios)

---

## 5. Platform Limitations (iOS vs Android)

### 5.1 What iOS CANNOT Do (That Android Can)

| Capability | Android VoiceOSCore | iOS | Impact |
|------------|---------------------|-----|--------|
| **Read other apps' UI elements** | ✅ AccessibilityService | ❌ **SANDBOXED** | **CRITICAL - Core feature impossible** |
| **System-wide overlays** | ✅ TYPE_ACCESSIBILITY_OVERLAY | ❌ **NO** | Cannot overlay voice targets on Safari/apps |
| **Background mic listening** | ✅ Foreground service | ❌ **NO** | Cannot implement always-listening |
| **Third-party accessibility services** | ✅ Yes | ❌ **NO** | Cannot extend system accessibility |
| **Screen scraping** | ✅ AccessibilityNodeInfo tree | ❌ **NO** | Cannot generate commands from UI |
| **Custom voice control overlay** | ✅ Yes | ❌ **Apple-only** | Cannot build VoiceOSCore equivalent |

### 5.2 What iOS CAN Do

| Capability | iOS Implementation | Notes |
|------------|-------------------|-------|
| **Speech recognition** | ✅ SFSpeechRecognizer, SpeechAnalyzer | Excellent on-device support |
| **On-device NLU** | ✅ Natural Language framework | Intent classification possible |
| **WKWebView JS injection** | ✅ WKUserScript, evaluateJavaScript | **Works for own app's WebView** |
| **Safari extension** | ✅ WKWebExtension | Can inject content scripts into Safari |
| **Voice within app** | ✅ Fully supported | Can build voice-controlled browser app |
| **SiriKit integration** | ✅ App Intents | User-triggered shortcuts |

### 5.3 App Store Review Guidelines

**Official Guidelines:** [App Store Review Guidelines](https://developer.apple.com/app-store/review/guidelines/)

**Accessibility Requirements:**
- ✅ Apps **must be accessible** to VoiceOver, Voice Control, Switch Control
- ✅ Proper `accessibilityLabel`, `accessibilityHint` required
- ⚠️ Accessibility errors (low contrast, small text, missing labels) = rejection

**Common Rejections:**
- App crashes or broken flows (2.1 Performance)
- Misleading metadata (2.3 Accurate Metadata)
- Paywall/IAP issues (3.1.1 In-App Purchase)
- **Accessibility errors** (text too small, low contrast, no screen reader support)

**Voice Control Apps:**
- ❌ Apps abusing **background modes** will be rejected
- ❌ Apps requiring **overlay permissions** without clear justification may be rejected
- ✅ Apps implementing **within-app voice control** are acceptable
- ✅ **Safari extensions** for voice control are acceptable

**VoIP Background Mode:**
- ⚠️ Requires **actual internet calling** functionality
- ⚠️ Apple **strictly reviews** VoIP entitlements
- ❌ **Will reject** if used for voice command listening instead of calls

**References:**
- [App Store Review Guidelines (2025): Checklist + Top Rejection Reasons](https://nextnative.dev/blog/app-store-review-guidelines)
- [iOS Accessibility Guidelines: Best Practices for 2025](https://medium.com/@david-auerbach/ios-accessibility-guidelines-best-practices-for-2025-6ed0d256200e)
- [iOS App Store Review Guidelines 2026: Best Practices](https://crustlab.com/blog/ios-app-store-review-guidelines/)

---

## 6. Real-World Examples

### 6.1 Voice Control Browser Apps

**LipSurf - Voice Control for the Browser:**
- **URL:** [LipSurf](https://www.lipsurf.com/)
- **Platform:** Chrome/Edge extension (not iOS-specific)
- **Approach:** Browser extension with content scripts
- **Capabilities:** Dictation, navigation, custom commands
- **iOS Status:** Not available on iOS (extensions only support limited scenarios)

**Voqal Browser:**
- **Discussion:** [Voqal Browser - Voice controlled web browser](https://community.openai.com/t/voqal-browser-voice-controlled-web-browser/984490)
- **Platform:** Built on OpenAI's Realtime API
- **iOS Status:** Not mentioned (likely web-based)

### 6.2 Open-Source Voice Frameworks

**SEPIA Framework:**
- **URL:** [SEPIA Framework](https://sepia-framework.github.io/)
- **Platform:** Cross-platform (iOS, Android, desktop browsers)
- **Approach:** DIY AI assistant framework
- **Capabilities:** Custom commands, voice-controlled smart services
- **iOS Implementation:** Uses iOS app with voice recognition

**OpenVoiceOS:**
- **GitHub:** [OpenVoiceOS](https://github.com/openVoiceOS)
- **Platform:** Open-source voice AI platform
- **Focus:** Privacy-focused, community-driven
- **iOS Status:** Primarily Linux/embedded systems, not iOS-native

**React Native Voice Recognition:**
- **GitHub:** [react-native-voice/voice](https://github.com/react-native-voice/voice)
- **Platform:** React Native (iOS + Android)
- **Capabilities:** Online and offline speech recognition
- **iOS Implementation:** Wraps SFSpeechRecognizer

### 6.3 iOS Voice Control Feature (Built-In)

**How iOS Voice Control Works:**
- System-level feature (Settings > Accessibility > Voice Control)
- Uses **Siri's speech recognition engine**
- Reads accessibility labels from apps and web content
- Overlay grid for precise targeting
- Commands: "Tap [element]", "Scroll down", "Show numbers", "Show grid"

**Third-Party Support:**
- Apps using **UIAccessibility** automatically supported
- Apps with proper **ARIA labels** in web content work with Voice Control
- ❌ Third-party apps **cannot extend or customize** Voice Control

**References:**
- [How to Set Up and Use Voice Control in iOS 13](https://beebom.com/set-up-use-voice-control-ios-13/)
- [Using Voice Control in iOS 13 to Operate an iPhone Hands-Free](https://www.macrumors.com/guide/voice-control/)

---

## 7. KMP (Kotlin Multiplatform) for iOS

### 7.1 KMP iOS Support in 2025-2026

**Official Roadmap:** [What's Next for Kotlin Multiplatform - August 2025](https://blog.jetbrains.com/kotlin/2025/08/kmp-roadmap-aug-2025/)

**Production Readiness:**
✅ **Kotlin Multiplatform is STABLE** (since November 2023)
✅ **Compose Multiplatform for iOS is STABLE** (v1.8.0, May 2025)

**Release Announcement:** [Compose Multiplatform 1.8.0 Released](https://blog.jetbrains.com/kotlin/2025/05/compose-multiplatform-1-8-0-released-compose-multiplatform-for-ios-is-stable-and-production-ready/)

**Key Milestones:**
- **May 6, 2025:** Compose Multiplatform for iOS reached **stable status**
- **2026 Target:** Stable Swift interoperability (Swift Export)

**Production Usage:**
- Companies using KMP: Netflix, McDonald's, Cash App
- Survey result: **96% of teams** using Compose Multiplatform on iOS report **no major performance concerns**

**Features (Compose Multiplatform 1.8.0):**
- ✅ Native-like scrolling
- ✅ iOS-native text selection
- ✅ Drag-and-drop functionality
- ✅ Variable font support
- ✅ Natural gestures
- ✅ Startup time comparable to native apps
- ✅ Scrolling performance on par with SwiftUI (even on high-refresh-rate devices)

**References:**
- [Is Kotlin Multiplatform production ready in 2026?](https://www.kmpship.app/blog/is-kotlin-multiplatform-production-ready-2026)
- [Compose Multiplatform for iOS Stable in 2025](https://www.kmpship.app/blog/compose-multiplatform-ios-stable-2025)

### 7.2 Compose Multiplatform for iOS

**Capabilities:**
- ✅ Share **UI code** between Android and iOS
- ✅ Use Compose for both platforms
- ✅ Interop with SwiftUI and UIKit
- ✅ Embed Compose in existing iOS apps
- ✅ Incorporate native iOS views into Compose screens

**Integration Scenarios:**
1. **Full Compose app** - Entire iOS app built with Compose
2. **Hybrid app** - Mix Compose and SwiftUI/UIKit
3. **Gradual migration** - Add Compose screens to existing app

**Code Example (Compose in SwiftUI):**
```swift
import SwiftUI
import shared // KMP shared module

struct ContentView: View {
    var body: some View {
        ComposeView()
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

**References:**
- [Integration with the SwiftUI framework](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-swiftui-integration.html)
- [Kotlin Multiplatform: Mixing SwiftUI and Jetpack Compose](https://cazimirroman.medium.com/mixing-swiftui-and-jetpack-compose-in-a-kotlin-multiplatform-project-for-ios-2f49a47085e7)

### 7.3 Swift Interop (Kotlin 2.2.20+)

**Swift Export Feature:**
- Enables **native Swift interop** for KMP
- Kotlin code appears as idiomatic Swift
- **Status:** Experimental (Kotlin 2.2.20)
- **Stable release target:** 2026

**SKIE (Swift Kotlin Interface Enhancer):**
- **URL:** [SKIE - Swift Kotlin Interface Enhancer](https://skie.touchlab.co/)
- **Purpose:** Improves Kotlin-Swift interop
- **Features:**
  - Bridge Kotlin Flows to Swift AsyncSequence
  - Seamless interop between Kotlin suspend functions and Swift async/await
  - Better enum/sealed class handling

**Code Example (SKIE):**
```kotlin
// Kotlin
class Repository {
    suspend fun fetchData(): Result<Data> { ... }
    fun observeUpdates(): Flow<Update> { ... }
}

// Swift (with SKIE)
let repository = Repository()

// Suspend function → async/await
let data = try await repository.fetchData()

// Flow → AsyncSequence
for await update in repository.observeUpdates() {
    print(update)
}
```

**References:**
- [Kotlin to Swift Export: Native iOS Integration Guide 2025](https://www.kmpship.app/blog/kotlin-swift-export-ios-integration-2025)

### 7.4 Shared Business Logic for VoiceOSCore

**What Can Be Shared:**
✅ **VOS command parsing** (`.vos` file format)
✅ **StaticCommandRegistry** logic
✅ **CommandLoader** implementation
✅ **Intent classification** (NLU logic)
✅ **Multi-locale support** (locale-specific command loading)
✅ **Database models** (SQLDelight for cross-platform DB)
✅ **Command matching algorithms**

**What CANNOT Be Shared:**
❌ **Platform-specific scraping** (Android AccessibilityService ≠ iOS sandbox)
❌ **Overlay rendering** (different APIs)
❌ **Microphone access** (different background models)
❌ **System integration** (AccessibilityService vs limited iOS APIs)

**Recommended Architecture:**
```
KMP Shared Module (commonMain)
├── VOS command parser (.vos → Command objects)
├── StaticCommandRegistry (locale-aware lookup)
├── CommandLoader (load from .vos files)
├── Intent classifier (NLU matching)
└── Database (SQLDelight)

Android-Specific (androidMain)
├── VoiceOSAccessibilityService (scraping)
├── AndroidScreenExtractor (UI tree traversal)
├── OverlayRenderer (TYPE_ACCESSIBILITY_OVERLAY)
└── AndroidCursorHandler

iOS-Specific (iosMain)
├── SpeechRecognizerWrapper (SFSpeechRecognizer/SpeechAnalyzer)
├── WKWebViewScraperBridge (JS injection for web)
├── VoiceCommandHandler (within-app commands only)
└── SafariExtensionBridge (if building Safari extension)
```

**VoiceOSCore iOS Strategy:**
1. **Share command parsing/matching logic** via KMP
2. **Build iOS app with WKWebView** (own voice-controlled browser)
3. **Optionally build Safari extension** (voice overlay for Safari web pages)
4. **Accept limitations:** No system-wide voice control, no always-listening

**References:**
- [Unlocking Kotlin Multiplatform: Integrating shared KMP code into an iOS project](https://medium.com/@mobileatexxeta/unlocking-kotlin-multiplatform-integrating-shared-kmp-code-into-an-ios-project-e12813097a2c)
- [FAQ | Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform/faq.html)

---

## 8. Feasibility Assessment for VoiceOSCore on iOS

### 8.1 Core VoiceOSCore Features - iOS Mapping

| VoiceOSCore Feature | Android Implementation | iOS Feasibility | Notes |
|---------------------|----------------------|-----------------|-------|
| **Voice recognition** | SpeechRecognizer | ✅ **YES** | SFSpeechRecognizer / SpeechAnalyzer |
| **Always-listening** | Foreground service | ❌ **NO** | Background mic restricted |
| **Screen scraping (apps)** | AccessibilityService | ❌ **NO** | Sandboxing prevents cross-app access |
| **Screen scraping (web)** | DOMScraperBridge | ✅ **YES** | WKWebView JS injection (own app only) |
| **Voice overlays (apps)** | TYPE_ACCESSIBILITY_OVERLAY | ❌ **NO** | No system-wide overlays |
| **Voice overlays (web)** | DOM injection | ✅ **YES** | WKWebView / Safari extension |
| **Command parsing** | StaticCommandRegistry | ✅ **YES** | Shareable via KMP |
| **Multi-locale** | DataStore + .vos files | ✅ **YES** | Shareable via KMP |
| **VOS distribution** | SFTP sync | ✅ **YES** | Network APIs available |
| **4-Tier voice** | AVID + scraping + VOS | ⚠️ **PARTIAL** | Tier 1,2,4 yes; Tier 3 (scraping) no |
| **Custom commands** | User-generated .vos | ✅ **YES** | File system access available |

### 8.2 Architecture Options for iOS

#### Option A: Voice-Controlled Browser App (WKWebView)
**Description:** Build iOS app with embedded WKWebView, inject voice control overlay.

**Pros:**
- ✅ Full control over WebView
- ✅ JavaScript injection for DOM scraping
- ✅ Voice overlay on web content
- ✅ Can share VOS parsing logic via KMP
- ✅ No App Store restrictions (within-app feature)

**Cons:**
- ❌ Only works within the app (not Safari or other browsers)
- ❌ Users must use **your app** instead of Safari
- ❌ No access to Safari bookmarks, autofill, etc.
- ❌ No always-listening (must open app first)

**Implementation:**
```swift
// WKWebView with voice overlay
class VoiceWebViewController: UIViewController {
    let webView = WKWebView()
    let speechRecognizer = SFSpeechRecognizer()
    let voiceOverlayView = VoiceOverlayView()

    override func viewDidLoad() {
        super.viewDidLoad()

        // Inject DOMScraperBridge
        let script = WKUserScript(
            source: DOMScraperBridge.js,
            injectionTime: .atDocumentEnd,
            forMainFrameOnly: true
        )
        webView.configuration.userContentController.addUserScript(script)

        // Start voice recognition
        startVoiceRecognition()
    }

    func startVoiceRecognition() {
        speechRecognizer?.recognitionTask(...) { result, error in
            if let command = result?.bestTranscription.formattedString {
                self.handleVoiceCommand(command)
            }
        }
    }

    func handleVoiceCommand(_ command: String) {
        // Parse command via KMP StaticCommandRegistry
        let intent = CommandParser.parse(command)

        // Execute action (e.g., click element)
        webView.evaluateJavaScript("document.getElementById('\(intent.targetId)').click()")
    }
}
```

**VoiceOSCore Modules Usable:**
- ✅ VOS command parser (KMP shared)
- ✅ StaticCommandRegistry (KMP shared)
- ✅ DOMScraperBridge (JS, shareable)
- ❌ VoiceOSAccessibilityService (Android-only)
- ❌ AndroidScreenExtractor (Android-only)

#### Option B: Safari Web Extension
**Description:** Build Safari extension that injects voice control into Safari web pages.

**Pros:**
- ✅ Works in **Safari** (user's default browser)
- ✅ Content scripts can scrape DOM
- ✅ Can inject voice overlay on web pages
- ✅ Integrates with user's existing Safari setup

**Cons:**
- ❌ **No microphone access in content scripts** (must use background script)
- ❌ Background script cannot directly access DOM
- ❌ Complex messaging between background script (mic) and content script (overlay)
- ❌ Only works in Safari (not Chrome, Edge, Firefox on iOS)
- ❌ No always-listening (extension must be activated)

**Implementation:**
```javascript
// Content script (runs on web page)
browser.runtime.onMessage.addListener((message) => {
    if (message.type === 'VOICE_COMMAND') {
        const command = message.command;
        const element = findElementByVoiceTarget(command);
        if (element) element.click();
    }
});

// Background script (has extension APIs, but no DOM access)
browser.runtime.onMessage.addListener((message) => {
    if (message.type === 'START_LISTENING') {
        // Cannot directly access microphone
        // Would need to use native messaging to native app
    }
});
```

**Challenges:**
- Safari extensions **cannot directly access microphone** from JavaScript
- Would need **native messaging** to native iOS app for speech recognition
- Complex architecture: Extension → Native App → Speech Recognition → Extension

**References:**
- [Implement a Safari iOS Extension with React Step-By-Step](https://medium.com/@gabrieIa/implement-a-safari-ios-extension-with-react-step-by-step-268665c1c4dd)

#### Option C: Hybrid (App + Extension)
**Description:** Combine Option A and Option B - build both an app and a Safari extension.

**Pros:**
- ✅ App for standalone voice-controlled browser
- ✅ Extension for Safari integration
- ✅ Share KMP logic between both
- ✅ Covers both use cases

**Cons:**
- ❌ Double development effort
- ❌ Complexity of maintaining two codebases (even with KMP sharing)
- ❌ Still no always-listening or system-wide voice control

#### Option D: iOS App with SiriKit Shortcuts
**Description:** Build app with custom Siri shortcuts for common web tasks.

**Pros:**
- ✅ Leverages iOS native voice assistant (Siri)
- ✅ User can create custom shortcuts
- ✅ Works system-wide (Siri integration)

**Cons:**
- ❌ Requires "Hey Siri, [shortcut name]" (not natural conversation)
- ❌ User must pre-configure shortcuts
- ❌ Not real-time voice control (discrete commands only)
- ❌ Cannot dynamically generate commands from scraped UI

### 8.3 Recommended Approach

**My Recommendation:** **Option A - Voice-Controlled Browser App (WKWebView)**

**Reasoning:**
1. **Achievable with current iOS APIs** - No reliance on restricted features
2. **Shareable KMP logic** - Reuse VOS parsing, command registry, multi-locale
3. **Full feature parity for web control** - Same DOMScraperBridge as Android
4. **App Store compliant** - No accessibility/overlay abuse
5. **Clear user value** - Hands-free web browsing for accessibility users

**Because:**
- Android VoiceOSCore's **strength is web scraping**, not app scraping
- iOS **allows full WKWebView control** within your app
- Users who want voice control are **willing to use a dedicated app**
- Accessibility users **need this functionality** (legitimate use case)

**Risk if ignored:**
- Attempting Option B (Safari extension) leads to **complex native messaging** with limited benefit
- Attempting system-wide voice control (like Android) leads to **App Store rejection**
- Not building iOS version means **missing iOS accessibility market**

**Trade-offs Accepted:**
- ❌ No always-listening (user must open app)
- ❌ No system-wide voice control (only within app)
- ❌ No app scraping (only web scraping)

**Features Retained:**
- ✅ Web voice control (same as WebAvanue on Android)
- ✅ DOM scraping + voice overlay
- ✅ VOS command system
- ✅ Multi-locale support
- ✅ 4-Tier voice (Tier 1, 2, 4; no Tier 3 app scraping)

---

## 9. Technical Implementation Plan (Option A)

### 9.1 KMP Shared Module

**Module Structure:**
```
shared/
├── commonMain/
│   ├── VosParser.kt              // Parse .vos files
│   ├── StaticCommandRegistry.kt   // Locale-aware command lookup
│   ├── CommandLoader.kt           // Load commands from .vos files
│   ├── IntentClassifier.kt        // NLU matching
│   ├── MultiLocaleSupport.kt      // Locale switching
│   └── database/                  // SQLDelight schemas
│       ├── Command.sq
│       └── VosFile.sq
├── androidMain/
│   ├── AndroidPlatform.kt
│   └── ... (existing VoiceOSCore Android code)
└── iosMain/
    ├── IOSPlatform.kt
    ├── SpeechRecognizerWrapper.kt
    ├── WKWebViewScraperBridge.kt
    └── VoiceCommandHandler.kt
```

**Shared Code (Kotlin):**
```kotlin
// commonMain/VosParser.kt
object VosParser {
    fun parse(vosContent: String): List<Command> {
        // Parse .vos format
        // Same logic as Android
    }
}

// commonMain/StaticCommandRegistry.kt
object StaticCommandRegistry {
    private val commands = mutableMapOf<String, Command>()

    fun load(locale: String) {
        val vosFile = getVosFile(locale) // Platform-specific
        commands.putAll(VosParser.parse(vosFile))
    }

    fun match(utterance: String): Command? {
        // Fuzzy matching, synonym expansion, etc.
    }
}
```

**iOS-Specific Code (Swift):**
```swift
// iosMain/SpeechRecognizerWrapper.kt (Kotlin, exposed to Swift)
class SpeechRecognizerWrapper {
    fun startRecognition(onResult: (String) -> Unit) {
        // Platform-specific Swift bridge
    }
}

// Swift (app code)
import shared

class VoiceControlManager {
    let registry = StaticCommandRegistry()

    init() {
        registry.load(locale: "en-US")
    }

    func handleVoiceInput(_ utterance: String) {
        if let command = registry.match(utterance: utterance) {
            executeCommand(command)
        }
    }
}
```

### 9.2 iOS App Architecture

**Tech Stack:**
- SwiftUI for native iOS UI (navigation, settings)
- WKWebView for web content
- SFSpeechRecognizer / SpeechAnalyzer for voice input
- KMP shared module for command parsing
- Combine for reactive state management

**View Structure:**
```swift
VoiceWebBrowserApp
├── ContentView (SwiftUI)
│   ├── NavigationView
│   │   ├── VoiceWebView (WKWebView wrapper)
│   │   ├── VoiceOverlay (overlay UI)
│   │   └── AddressBar
│   ├── SettingsView
│   │   ├── LocaleSelector
│   │   ├── VoiceSettings
│   │   └── VosFileManager
│   └── VoiceFeedback (visual/audio feedback)
└── VoiceControlManager (shared KMP logic)
```

**WKWebView Integration:**
```swift
// VoiceWebView.swift
import WebKit
import shared

struct VoiceWebView: UIViewRepresentable {
    @Binding var url: URL
    let voiceManager: VoiceControlManager

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        let userContentController = WKUserContentController()

        // Inject DOMScraperBridge
        let domScript = WKUserScript(
            source: loadDOMScraperBridge(),
            injectionTime: .atDocumentEnd,
            forMainFrameOnly: true
        )
        userContentController.addUserScript(domScript)

        // Message handler for scraped elements
        userContentController.add(context.coordinator, name: "voiceElements")

        config.userContentController = userContentController
        let webView = WKWebView(frame: .zero, configuration: config)
        webView.navigationDelegate = context.coordinator

        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        webView.load(URLRequest(url: url))
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, WKNavigationDelegate, WKScriptMessageHandler {
        var parent: VoiceWebView

        init(_ parent: VoiceWebView) {
            self.parent = parent
        }

        func userContentController(_ userContentController: WKUserContentController,
                                  didReceive message: WKScriptMessage) {
            if message.name == "voiceElements" {
                if let elements = message.body as? [[String: Any]] {
                    // Process scraped elements
                    parent.voiceManager.updateElements(elements)
                }
            }
        }
    }
}
```

**Speech Recognition:**
```swift
// SpeechManager.swift
import Speech

class SpeechManager: ObservableObject {
    private let speechRecognizer = SFSpeechRecognizer(locale: Locale(identifier: "en-US"))
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private let audioEngine = AVAudioEngine()

    @Published var isListening = false
    @Published var transcription = ""

    func startRecognition(onCommand: @escaping (String) -> Void) {
        guard let recognizer = speechRecognizer, recognizer.isAvailable else {
            print("Speech recognition not available")
            return
        }

        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        guard let recognitionRequest = recognitionRequest else { return }

        recognitionRequest.shouldReportPartialResults = true

        recognitionTask = recognizer.recognitionTask(with: recognitionRequest) { result, error in
            if let result = result {
                let utterance = result.bestTranscription.formattedString
                self.transcription = utterance

                if result.isFinal {
                    onCommand(utterance)
                }
            }
        }

        let recordingFormat = audioEngine.inputNode.outputFormat(forBus: 0)
        audioEngine.inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { buffer, _ in
            recognitionRequest.append(buffer)
        }

        audioEngine.prepare()
        try? audioEngine.start()
        isListening = true
    }

    func stopRecognition() {
        audioEngine.stop()
        audioEngine.inputNode.removeTap(onBus: 0)
        recognitionRequest?.endAudio()
        recognitionTask?.cancel()
        isListening = false
    }
}
```

**Voice Command Execution:**
```swift
// VoiceCommandHandler.swift
import shared

class VoiceCommandHandler {
    let registry = StaticCommandRegistry()
    weak var webView: WKWebView?

    init() {
        // Load default locale
        registry.load(locale: "en-US")
    }

    func handleCommand(_ utterance: String) {
        guard let command = registry.match(utterance: utterance) else {
            print("No matching command for: \(utterance)")
            return
        }

        switch command.type {
        case .click:
            clickElement(avid: command.targetAvid)
        case .navigate:
            navigateToUrl(command.url)
        case .scroll:
            scroll(direction: command.direction)
        // ... other command types
        default:
            break
        }
    }

    func clickElement(avid: String) {
        let js = "document.querySelector('[data-avid=\"\(avid)\"]')?.click()"
        webView?.evaluateJavaScript(js)
    }

    func navigateToUrl(_ url: String) {
        if let url = URL(string: url) {
            webView?.load(URLRequest(url: url))
        }
    }

    func scroll(direction: String) {
        let js = "window.scrollBy(0, \(direction == "down" ? 300 : -300))"
        webView?.evaluateJavaScript(js)
    }
}
```

### 9.3 DOMScraperBridge (Reusable from Android)

**JavaScript (same as Android WebAvanue):**
```javascript
// DOMScraperBridge.js
(function() {
    function scrapeElements() {
        const elements = [];
        const allElements = document.querySelectorAll('a, button, input, select, textarea, [onclick]');

        allElements.forEach((el, index) => {
            const avid = `avid_${index}`;
            el.setAttribute('data-avid', avid);

            const rect = el.getBoundingClientRect();
            elements.push({
                avid: avid,
                tagName: el.tagName,
                text: el.innerText || el.value || el.placeholder,
                ariaLabel: el.getAttribute('aria-label'),
                x: rect.left,
                y: rect.top,
                width: rect.width,
                height: rect.height
            });
        });

        return elements;
    }

    // Send scraped elements to Swift
    function sendElementsToNative() {
        const elements = scrapeElements();
        window.webkit.messageHandlers.voiceElements.postMessage(elements);
    }

    // Scrape on page load
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', sendElementsToNative);
    } else {
        sendElementsToNative();
    }

    // Re-scrape on mutations
    const observer = new MutationObserver(sendElementsToNative);
    observer.observe(document.body, { childList: true, subtree: true });
})();
```

**This JavaScript is identical to Android's DOMScraperBridge and can be shared across platforms.**

### 9.4 VOS File Distribution

**iOS File System:**
```swift
// VosFileManager.swift
import Foundation

class VosFileManager {
    static let vosDirectory: URL = {
        let documentsPath = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        return documentsPath.appendingPathComponent("vos_files")
    }()

    static func loadVosFile(locale: String, type: String) -> String? {
        let filename = "\(locale).\(type).vos"
        let fileURL = vosDirectory.appendingPathComponent(filename)
        return try? String(contentsOf: fileURL)
    }

    static func saveVosFile(locale: String, type: String, content: String) {
        let filename = "\(locale).\(type).vos"
        let fileURL = vosDirectory.appendingPathComponent(filename)
        try? FileManager.default.createDirectory(at: vosDirectory, withIntermediateDirectories: true)
        try? content.write(to: fileURL, atomically: true, encoding: .utf8)
    }

    static func syncViaNetwork() {
        // Implement SFTP sync (same protocol as Android)
        // Use third-party SSH library (e.g., NMSSH)
    }
}
```

**Network Sync (Reusable Architecture):**
- iOS can use same SFTP protocol as Android VOS sync
- Third-party libraries available: [NMSSH](https://github.com/NMSSH/NMSSH) (Objective-C SSH/SFTP)
- Manifest-based delta sync logic shareable via KMP

---

## 10. Conclusion and Recommendations

### 10.1 Summary of Findings

**iOS Capabilities:**
- ✅ **Excellent voice recognition** (SFSpeechRecognizer, SpeechAnalyzer)
- ✅ **On-device NLU** (Natural Language framework)
- ✅ **WKWebView JS injection** (full DOM scraping within own app)
- ✅ **Safari extensions** (content scripts for web pages)
- ✅ **KMP production-ready** (share business logic with Android)

**iOS Limitations:**
- ❌ **No cross-app UI scraping** (sandboxing prevents AccessibilityService equivalent)
- ❌ **No system-wide overlays** (cannot overlay on Safari/other apps)
- ❌ **No always-listening** (background microphone severely restricted)
- ❌ **No third-party accessibility extensions** (Apple-only)

**VoiceOSCore Feasibility:**
- ✅ **Voice-controlled browser app (WKWebView)** - FULLY FEASIBLE
- ⚠️ **Safari extension** - POSSIBLE but complex (native messaging required)
- ❌ **System-wide voice control** - IMPOSSIBLE (iOS restrictions)

### 10.2 Recommended Strategy

**Phase 1: iOS Voice Browser App (WKWebView)**
- Build standalone iOS app with embedded WKWebView
- Inject DOMScraperBridge for web element scraping
- Implement voice overlay on web content
- Share VOS parsing/command logic via KMP
- Support multi-locale (.vos file distribution)
- Target: iOS 15+ (SFSpeechRecognizer stable)

**Phase 2: Safari Extension (Optional)**
- Build Safari extension with content scripts
- Native messaging to iOS app for speech recognition
- Voice overlay on Safari web pages
- More complex architecture, lower priority

**Phase 3: iOS 26 SpeechAnalyzer (2026)**
- Migrate to SpeechAnalyzer API when iOS 26 releases
- Use SpeechTranscriber for optimized command recognition
- Better performance, long-form audio support

### 10.3 Architecture Recommendation

**KMP Module Structure:**
```
NewAvanues/Modules/
├── VoiceOSCore/                    # Shared KMP module
│   ├── src/
│   │   ├── commonMain/
│   │   │   ├── VosParser.kt
│   │   │   ├── StaticCommandRegistry.kt
│   │   │   ├── CommandLoader.kt
│   │   │   ├── IntentClassifier.kt
│   │   │   └── database/
│   │   ├── androidMain/
│   │   │   ├── VoiceOSAccessibilityService.kt
│   │   │   ├── AndroidScreenExtractor.kt
│   │   │   └── AndroidCursorHandler.kt
│   │   └── iosMain/
│   │       ├── SpeechRecognizerWrapper.kt
│   │       ├── WKWebViewScraperBridge.kt
│   │       └── VoiceCommandHandler.kt
└── WebAvanue/                      # Existing Android web module
    └── ... (DOMScraperBridge.js shareable)

Apps/
├── Android/
│   └── VoiceAvanue/                # Existing Android app
└── iOS/
    └── VoiceAvanue/                # NEW iOS app
        ├── VoiceWebBrowserApp.swift
        ├── VoiceWebView.swift
        ├── SpeechManager.swift
        ├── VoiceCommandHandler.swift
        └── DOMScraperBridge.js     # Symlink/copy from WebAvanue
```

### 10.4 Trade-Offs Accepted

**What iOS Version Will NOT Have:**
- ❌ Always-listening voice control (no background mic)
- ❌ System-wide voice overlays (no AccessibilityService equivalent)
- ❌ App scraping (only web scraping via WKWebView)
- ❌ Cross-app voice control (only within VoiceAvanue app)

**What iOS Version WILL Have:**
- ✅ Voice-controlled web browsing (WKWebView)
- ✅ DOM element scraping + voice overlay
- ✅ VOS command system (shareable with Android)
- ✅ Multi-locale support
- ✅ 4-Tier voice (Tier 1, 2, 4; no Tier 3)
- ✅ SFTP VOS sync
- ✅ Custom .vos file loading

### 10.5 Final Recommendation

**Build iOS app with WKWebView-based voice control.**

**Reasoning:**
1. **Achievable** with current iOS APIs (no reliance on restricted features)
2. **Valuable** for accessibility users (hands-free web browsing)
3. **App Store compliant** (legitimate accessibility use case)
4. **Shareable logic** with Android via KMP (60%+ code reuse)
5. **Unique offering** (no direct competitors for voice-controlled web browser on iOS)

**Because:**
- iOS market is **43% of US smartphone users** (source: Statcounter 2025)
- Accessibility features are **App Store priority** (WCAG compliance)
- VoiceOSCore's **web scraping is its strength** (not app scraping)
- iOS **allows full WKWebView control** (same capability as Android WebView)

**Risk if ignored:**
- **Missing half the mobile market** (iOS users)
- **Accessibility users on iOS** have no equivalent solution
- **Competitors** could build similar iOS app first

**Next Steps:**
1. Create `Apps/iOS/VoiceAvanue/` project in Xcode
2. Add KMP shared module dependency
3. Implement WKWebView wrapper with DOMScraperBridge
4. Port SpeechRecognition to iOS (SFSpeechRecognizer)
5. Test with VOS files from Android distribution
6. Submit to App Store with accessibility focus

---

## Sources

### Speech Recognition & NLU
- [Bring advanced speech-to-text to your app with SpeechAnalyzer - WWDC25](https://developer.apple.com/videos/play/wwdc2025/277/)
- [Apple SpeechAnalyzer and Argmax WhisperKit](https://www.argmaxinc.com/blog/apple-and-argmax)
- [SFSpeechRecognizer - Apple Developer Documentation](https://developer.apple.com/documentation/speech/sfspeechrecognizer)
- [On-Device Speech Transcription with Apple SpeechAnalyzer and AI SDK](https://www.callstack.com/blog/on-device-speech-transcription-with-apple-speechanalyzer)
- [iOS 26: SpeechAnalyzer Guide](https://antongubarenko.substack.com/p/ios-26-speechanalyzer-guide)
- [Speech - Apple Developer Documentation](https://developer.apple.com/documentation/speech)
- [Recognizing speech in live audio](https://developer.apple.com/documentation/Speech/recognizing-speech-in-live-audio)
- [iOS Speech Recognition (SFSpeechRecognizer)](https://theptrk.com/2025/08/24/ios-speech-recognition-sfspeechrecognizer/)
- [Natural Language - Apple Developer Documentation](https://developer.apple.com/documentation/naturallanguage)
- [Natural Language on iOS](https://stefanblos.com/posts/natural-language-on-ios/)
- [Offline Natural Language Understanding Engine on iOS](http://hongchaozhang.github.io/blog/2019/05/22/offline-natural-language-understanding-engine-on-ios/)

### SiriKit & App Intents
- [Streamlining iOS Voice-Activated Apps with SiriKit](https://povio.com/blog/streamlining-ios-voice-activated-apps-with-sirikit)
- [SiriKit - Apple Developer Documentation](https://developer.apple.com/documentation/sirikit/)
- [How does Siri App Integration work? A Complete Guide in 2025](https://www.excellentwebworld.com/siri-third-party-app-integration/)
- [Swift and SiriKit: Adding Voice Commands to Your App](https://commitstudiogs.medium.com/swift-and-sirikit-adding-voice-commands-to-your-app-d78604210bf9)
- [Registering Custom Vocabulary with SiriKit](https://developer.apple.com/documentation/sirikit/registering-custom-vocabulary-with-sirikit)

### iOS Accessibility & Voice Control
- [Use Voice Control on your iPhone, iPad, or iPod touch](https://support.apple.com/en-us/111778)
- [Apple's Voice Control improves accessibility OS-wide](https://techcrunch.com/2019/06/03/apples-voice-control-improves-accessibility-os-wide-on-all-its-devices/)
- [New in iOS 13 Accessibility - Voice Control and More](https://www.deque.com/blog/new-in-ios-13-accessibility-voice-control-and-more/)
- [Hands on with Apple's new voice control accessibility feature](https://appleinsider.com/articles/19/06/07/hands-on-with-apples-new-voice-control-accessibility-feature)
- [UIAccessibility - Apple Developer Documentation](https://developer.apple.com/documentation/uikit/uiaccessibility-protocol)
- [UIAccessibility - NSHipster](https://nshipster.com/uiaccessibility/)
- [Accessibility element - iOS development](https://medium.com/short-swift-stories/accessibility-element-2d55cefdf9d7)
- [Third party screen reader for iOS](https://developer.apple.com/forums/thread/737069)
- [iOS vs. Android Accessibility](https://pauljadam.com/iosvsandroida11y/)
- [Android Vs. iOS: Accessibility Features Compared](https://www.iamhable.com/en-am/blogs/article/android-vs-ios-accessibility-features-compared)

### Overlay & Sandbox Restrictions
- [Overlay and Accessibility Permissions for iOS Apps](https://discussions.apple.com/thread/256116103)
- [Protecting user data with App Sandbox](https://developer.apple.com/documentation/security/protecting-user-data-with-app-sandbox)
- [Security of runtime process in iOS, iPadOS, and visionOS](https://support.apple.com/guide/security/security-of-runtime-process-sec15bfe098e/web)

### WKWebView & Safari Extensions
- [JavaScript Manipulation on iOS Using WebKit](https://medium.com/capital-one-tech/javascript-manipulation-on-ios-using-webkit-2b1115e7e405)
- [Injecting JavaScript Into Web View In iOS](https://swiftsenpai.com/development/web-view-javascript-injection/)
- [WKUserScript - Apple Developer Documentation](https://developer.apple.com/documentation/webkit/wkuserscript)
- [IOS/Swift: WebView: Javascript Injection](https://medium.com/@itsuki.enjoy/ios-swift-webview-javascript-injection-pop-ups-new-tabs-user-agent-and-cookies-1e46d04262b0)
- [Implement a Safari iOS Extension with React Step-By-Step](https://medium.com/@gabrieIa/implement-a-safari-ios-extension-with-react-step-by-step-268665c1c4dd)
- [Injecting a script into a webpage](https://developer.apple.com/documentation/safariservices/safari_app_extensions/injecting_a_script_into_a_webpage)
- [Safari Extensions - Apple Developer](https://developer.apple.com/safari/extensions/)

### Background Audio & Privacy
- [Microphone background service](https://developer.apple.com/forums/thread/106415)
- [Microphone privacy on iOS and Android](https://grokipedia.com/page/Microphone_privacy_on_iOS_and_Android)
- [3 Ways to See What Apps are Using the Microphone](https://www.hollyland.com/blog/tips/see-what-apps-are-using-the-microphone-of-iphone)
- [Control access to hardware features on iPhone](https://support.apple.com/guide/iphone/control-access-to-hardware-features-iph168c4bbd5/ios)
- [Do I Need Special Permissions For Voice Features In My App?](https://thisisglance.com/learning-centre/do-i-need-special-permissions-for-voice-features-in-my-app)

### App Store Review Guidelines
- [App Store Review Guidelines](https://developer.apple.com/app-store/review/guidelines/)
- [App Store Review Guidelines (2025): Checklist + Top Rejection Reasons](https://nextnative.dev/blog/app-store-review-guidelines)
- [iOS Accessibility Guidelines: Best Practices for 2025](https://medium.com/@david-auerbach/ios-accessibility-guidelines-best-practices-for-2025-6ed0d256200e)
- [iOS App Store Review Guidelines 2026: Best Practices](https://crustlab.com/blog/ios-app-store-review-guidelines/)

### Kotlin Multiplatform & Compose
- [Compose Multiplatform 1.8.0 Released](https://blog.jetbrains.com/kotlin/2025/05/compose-multiplatform-1-8-0-released-compose-multiplatform-for-ios-is-stable-and-production-ready/)
- [Is Kotlin Multiplatform production ready in 2026?](https://www.kmpship.app/blog/is-kotlin-multiplatform-production-ready-2026)
- [Compose Multiplatform for iOS Stable in 2025](https://www.kmpship.app/blog/compose-multiplatform-ios-stable-2025)
- [What's Next for Kotlin Multiplatform - August 2025](https://blog.jetbrains.com/kotlin/2025/08/kmp-roadmap-aug-2025/)
- [Kotlin to Swift Export: Native iOS Integration Guide 2025](https://www.kmpship.app/blog/kotlin-swift-export-ios-integration-2025)
- [Kotlin Multiplatform: Mixing SwiftUI and Jetpack Compose](https://cazimirroman.medium.com/mixing-swiftui-and-jetpack-compose-in-a-kotlin-multiplatform-project-for-ios-2f49a47085e7)
- [Integration with the SwiftUI framework](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-swiftui-integration.html)
- [SKIE - Swift Kotlin Interface Enhancer](https://skie.touchlab.co/)
- [Unlocking Kotlin Multiplatform](https://medium.com/@mobileatexxeta/unlocking-kotlin-multiplatform-integrating-shared-kmp-code-into-an-ios-project-e12813097a2c)
- [FAQ | Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform/faq.html)

### Voice Control Examples
- [SEPIA Framework](https://sepia-framework.github.io/)
- [OpenVoiceOS](https://github.com/openVoiceOS)
- [Voice Control for the Browser - LipSurf](https://www.lipsurf.com/)
- [Voqal Browser - Voice controlled web browser](https://community.openai.com/t/voqal-browser-voice-controlled-web-browser/984490)
- [React Native Voice Recognition](https://github.com/react-native-voice/voice)

### Android Accessibility (Comparison Reference)
- [Developing an Accessibility Service for Android](https://codelabs.developers.google.com/codelabs/developing-android-a11y-service)
- [Impact of Accessibility Permission in Android Apps](https://www.browserstack.com/guide/accessibility-permission-in-android)
- [Create your own accessibility service](https://developer.android.com/guide/topics/ui/accessibility/service)
- [The Risk of Accessibility Permissions in Android Devices](https://blog.zonealarm.com/2020/12/the-risk-of-accessibility-permissions-in-android-devices/)

---

**End of Analysis**
