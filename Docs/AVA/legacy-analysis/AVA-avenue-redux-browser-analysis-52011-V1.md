# Avenue Redux Browser - Comprehensive Codebase Analysis

**Analysis Date:** 2025-11-20
**Analyzed Project:** `/tmp/avenue-redux-browser/avenue-redux-browser/`
**Project Type:** Voice-First Android Web Browser
**Architecture:** Modular MVVM with Hilt DI

---

## Project Architecture Summary

### Overview
Avenue Redux Browser is an Android-based web browser application built with Kotlin, designed for voice-first and accessibility-focused interaction. The app uses a modular architecture with Hilt for dependency injection, Realm for local storage, and follows MVVM pattern.

### Module Breakdown
The project consists of 9 modules:

1. **app** - Main application module with browser screens and navigation
2. **augmentalis_theme** - Custom theming system with containers, buttons, and styled components
3. **bottom-command-bar** - Bottom command bar UI component for voice commands
4. **voiceos-common** - Shared utilities, models, and browser components
5. **voiceos-storage** - Database models and storage utilities (Realm)
6. **voiceos-logger** - Logging utilities
7. **voiceos-resources** - Shared resources
8. **app-preferences** - SharedPreferences utilities and encryption
9. **color_picker** - Color picker component

### Architectural Pattern
- **MVVM (Model-View-ViewModel)** with LiveData
- **Dependency Injection**: Dagger Hilt
- **Database**: Realm
- **Navigation**: Android Navigation Component
- **UI**: Mix of XML layouts and custom views

### Key Technologies
- **Language**: Kotlin
- **Build System**: Gradle (Kotlin DSL)
- **Min SDK**: 26, Target SDK: 34
- **DI**: Hilt
- **Database**: Realm
- **Image Loading**: Coil with SVG support
- **Async**: Coroutines
- **Cloud Services**: Firebase, Dropbox SDK, OneDrive, Box integration

---

## 1. Complete Class Inventory

### App Module (`com.augmentalis.dev.voiceos_browser`)

| Class Name | Package | Purpose | Public Functions | Key Dependencies |
|------------|---------|---------|------------------|------------------|
| **App** | `com.augmentalis.dev.voiceos_browser` | Main application class | `onCreate()`, `newImageLoader()` | HiltAndroidApp, ImageLoaderFactory, Coil |
| **WebBrowserActivity** | `com.augmentalis.dev.voiceos_browser` | Main host activity | `onCreate()`, `openBrowser()`, `openSavedPages()`, `openUrl()`, `changeThemeStyle()`, `handleDropboxResponse()` | AppCompatActivity, NavController, BroadcastReceiver |
| **WebViewFragment** | `.screens` | Main browser fragment | `dashboard()`, `onViewCreated()`, `openUrl()`, `performGoBack()`, `updateDesktopMode()`, `dismissDialog()`, `startScan()`, `selectFirst()`, `selectLast()`, `onlySideScroll()`, `loadWebViewInitialCommands()`, `loadWebViewNavigationCommands()`, `loadWebViewWebSpecificCommands()`, `loadScrollCommands()`, `loadWebViewCursorCommands()`, `loadWebViewZoomCommands()`, `loadWebViewZoomLevelCommands()`, `loadWebViewTouchCommands()` | WebViewBaseFragment, UiController, WebViewModel, FragmentWebviewBinding |
| **WebViewModel** | `.screens` | ViewModel for browser | `loadNavCommands()`, `toggleFreezeFrame()`, `scrollRight/Left/Top/Bottom/Up/Down()`, `toggleDrag()`, `pinchClose/Open()`, `clearCookies()`, `setWebView()`, `addView()`, `deleteFrame()`, `nextView/previousView()`, `favoriteWebPage()`, `changeDesktopMode()`, `isDesktopMode()`, `reload()`, `nextPage/previousPage()`, `saveBrowser()`, `loadBrowser()`, `setZoomLevel()`, `zoomIn/Out()`, `executeAvaDynamicCommand()` | ViewModel, Repository, LiveData |
| **WebViewBaseFragment** | `.screens` | Base fragment utilities | `dialogIsInit()`, `createDialog()`, `createAlertDialog()`, `isDialogInitialized()`, `getHelpCommands()`, `showHelpDialog()`, `dismissHelpDialogListener()`, `startDownload()`, `hideActionBar()` | Fragment, Dialog, AlertDialog, DownloadManager |
| **AddVidCallAccountFragment** | `.screens` | Video call account setup | Fragment for adding video call accounts | Fragment |
| **WebViewDialogFragment** | `.screens` | Authentication dialog | Authentication dialog for web login | DialogFragment |
| **WebViewAuthError** | `.screens` | Auth error handling | Error handling for authentication | - |
| **AppModule** | `.di` | Dependency injection | `provideSharedPref()`, `provideSharedPrefUtil()`, `provideContext()`, `provideRealm()`, `provideZipManager()` | Hilt Module, Singleton providers |
| **CustomAnnotations** | `.di` | Custom DI annotations | Custom qualifier annotations | Hilt |
| **CoroutineHiltModules** | `.di` | Coroutine scopes | Provides coroutine dispatchers | Hilt, Coroutines |

### VoiceOS Common Module (`com.augmentalis.dev.voiceos_common`)

#### Model Classes

| Class Name | Package | Purpose | Public Functions | Key Dependencies |
|------------|---------|---------|------------------|------------------|
| **AiView** | `.model` | Universal view container | `drawView()`, `refreshView()`, `executeAvaDynamicCommand()`, `zoomIn/Out()`, `scrollUp/Down/Left/Right()`, `pinchOpen/Close()`, `drillStart/Down()`, `desktopMode()`, `reload()`, `nextPage/previousPage()`, `singleClick/doubleClick()`, `toDatabase()`, `setZoomLevel()`, `showMetaData()`, `hideMetaData()`, `getContentHeight()`, `getScrollX/Y()` | FrameLayout, supports WebView/PDF/Image/Video/Camera/Note/Workflow views |
| **WebViewContainer** | `.model` | WebView wrapper | `loadCurrentUrl()`, `loadUrl()`, `setDesktopMode()`, `webZoomIn/Out()`, `executeAvaDynamicCommand()`, `drillDown()`, `smoothScrollUp/Down()`, `clearText()`, `checkInputType()`, `pageLeft/Right()`, `setZoomLevel()`, `exitPage()`, `logout()` | WebView, WebChromeClient, AuthWebViewClient, UiController |
| **UiController** | `.model` | UI callback interface | `proceedBasicAuth()`, `onWebViewLoaded()`, `getNewTabWebView()`, `updateCommandBar()`, `workflowTitleClick()`, `updateKeyboardCommandBar()`, `singleClick()`, `isPlayerPaused/Muted()`, `exitPage()`, `logoutPage()`, `updateFrame()` | Interface for fragment callbacks |
| **AiViewUI** | `.model` | View UI interface | Interface for UI views | - |
| **NoteViewContainer** | `.model` | Note editor container | Note editing view container | FrameLayout |
| **ImageViewContainer** | `.model` | Image viewer | Image viewing with zoom/pan | FrameLayout |
| **PdfViewContainer** | `.model` | PDF viewer | PDF viewing with zoom/scroll | FrameLayout |
| **CameraViewContainer** | `.model` | Camera view | Camera preview and controls | FrameLayout |
| **VideoPlayerContainerWithThumbnail** | `.model` | Video player | Video playback controls | FrameLayout |
| **WorkflowContainer** | `.model` | Workflow steps | Workflow UI container | FrameLayout |
| **AppUser** | `.model` | User model | User data model | - |
| **RegisterUser** | `.model` | Registration model | User registration data | - |
| **PhoneCode** | `.model` | Phone codes | Country phone code data | - |
| **DialogItems** | `.model` | Dialog data | Dialog item models | - |
| **GalleryItem** | `.model` | Gallery item | Gallery data model | - |
| **SelectedContentInfo** | `.model` | Content selection | Selected content data | - |

#### Browser Models

| Class Name | Package | Purpose | Public Functions | Key Dependencies |
|------------|---------|---------|------------------|------------------|
| **WebViewPage** | `.model.browser` | Browser page manager | `addNewTab()`, `removeSelectedFrame()`, `nextWebView()`, `favoriteWebPage()`, `setCurrentDesktop()`, `isDesktopModeOn()`, `reload()`, `nextPage/previousPage()`, `toggleFreeze()`, `scrollUp/Down/Left/Right()`, `setZoomLevel()`, `zoomIn/Out()`, `executeAvaDynamicCommand()`, `clearCookies()`, `loadPagesFromDatabase()` | Manages tabs, favorites, and web views |
| **WebViewTabs** | `.model.browser` | Tab UI component | `drawTab()`, `updateDesktopMode()`, `drillStart/Down()`, `pinchOpen/Close()` | Tab bar item |
| **WebViewFavorites** | `.model.browser` | Favorites bar | Manages favorite links | - |
| **AuthWebViewClient** | `.model.browser` | Auth WebView client | `onPageFinished()`, `onPageStarted()`, `shouldOverrideUrlLoading()`, `onReceivedError()`, `onReceivedHttpAuthRequest()` | WebViewClient, handles auth flows |
| **AuthDatabase** | `.model.browser` | Auth credentials DB | `setUsernamePassword()`, `getUsernamePassword()`, `deleteUsernamePassword()` | Singleton, in-memory auth cache |
| **WebLogins** | `.model.browser` | Login types enum | Enum: GOOGLE, OFFICE, VIDCALL | Enum |
| **WebViewInterface** | `.model.browser` | JavaScript interface | JavaScript bridge interface | - |

#### Repository

| Class Name | Package | Purpose | Public Functions | Key Dependencies |
|------------|---------|---------|------------------|------------------|
| **Repository** | `.repository` | Data repository | `updateWebBrowser()`, `loadWebBrowser()`, `getSetting()` | Realm database access |
| **RepositoryInterface** | `.repository` | Repository interface | Interface for repository | - |

#### Utilities

| Class Name | Package | Purpose | Public Functions | Key Dependencies |
|------------|---------|---------|------------------|------------------|
| **JsCommands** | `.utils` | JavaScript commands | JavaScript injection utilities | - |
| **BrowserUtils** | `.utils` | Browser utilities | `checkIsYoutubeVideoView()`, URL utilities | - |
| **ConnectivityService** | `.utils` | Network connectivity | Network state monitoring | Service |
| **LocationService** | `.utils` | Location services | GPS location services | Service |
| **MotionInputManager** | `.utils` | Touch simulation | `drag()`, touch event simulation | - |
| **SystemProperties** | `.utils` | System props | Device detection (Vuzix, RealWear, etc.) | - |
| **ZipManager** | `.utils` | File compression | Zip/unzip utilities | - |
| **GpsParser** | `.utils` | GPS parsing | GPS data parsing | - |
| **DisplayMessage** | `.utils` | Toast/alerts | Toast message utilities | - |
| **JsonUtils** | `.utils` | JSON parsing | JSON utilities | - |
| **CommExtension** | `.utils` | Extensions | Kotlin extension functions | - |

### Bottom Command Bar Module (`com.augmentalis.dev.bottom_command_bar`)

| Class Name | Package | Purpose | Public Functions | Key Dependencies |
|------------|---------|---------|------------------|------------------|
| **BottomCommandBar** | - | Command bar container | `setButtons()`, `focusFirstButton/LastButton()`, `focusButton()`, `selectCurrentCommand()` | ConstraintLayout, custom view |
| **CommandBarButton** | - | Command button | Static factory methods: `back()`, `home()`, `select()`, `cancel()`, `submit()`, `scrollUp/Down()`, `editNote()`, `addNote()`, `deleteNote()`, `renameNote()`, `clear()`, `qr()`, `copy()`, `import/export()`, `metadata()`, `showHelp()`. Instance methods: `setSize()`, `replaceButtonIcon()`, `replaceCommandText()` | MaterialButton, custom styled button |
| **CommandModel** | - | Command data model | Data class for commands | - |
| **VoiceCommands** | - | Voice command dialogs | `showHelpMenuDialog()` | Dialog utilities |
| **CommandListAdapter** | - | Command list adapter | RecyclerView adapter for commands | RecyclerView.Adapter |

### Augmentalis Theme Module (`com.augmentalis.theme`)

#### Containers

| Class Name | Package | Purpose | Public Functions | Key Dependencies |
|------------|---------|---------|------------------|------------------|
| **Container** | `.container` | Base container | Base themed container | ConstraintLayout |
| **LeftContainer** | `.container` | Left panel | Left sidebar container | Container |
| **RightContainer** | `.container` | Right panel | Right sidebar container | Container |
| **DashboardContainer** | `.container` | Dashboard panel | Dashboard container | Container |
| **NoteViewContainer** | `.container` | Note container | Note editing container | Container |
| **DeviceSettingsContainer** | `.container` | Settings panel | Settings container | Container |

#### Buttons

| Class Name | Package | Purpose | Public Functions | Key Dependencies |
|------------|---------|---------|------------------|------------------|
| **ActionButton** | `.buttons.action_button` | Base action button | Themed action button | MaterialButton |
| **PositiveButton** | `.buttons.action_button` | Confirm button | Positive action button | ActionButton |
| **NegativeButton** | `.buttons.action_button` | Cancel button | Negative action button | ActionButton |
| **NeutralButton** | `.buttons.action_button` | Neutral button | Neutral action button | ActionButton |
| **DashboardButton** | `.buttons` | Dashboard button | Dashboard menu button | MaterialButton |
| **MenuButton** | `.buttons` | Menu button | Menu item button | MaterialButton |
| **RightContainerMenuButton** | `.buttons` | Right menu button | Right panel menu button | MenuButton |

#### Text Views

| Class Name | Package | Purpose | Public Functions | Key Dependencies |
|------------|---------|---------|------------------|------------------|
| **CustomTextView** | `.textview` | Base text view | Themed text view | AppCompatTextView |
| **CustomLeftTextView** | `.textview` | Left text view | Left panel text | CustomTextView |
| **CustomRightTextView** | `.textview` | Right text view | Right panel text | CustomTextView |
| **CustomRightSwitchButton** | `.textview` | Switch button | Toggle switch view | CustomTextView |
| **PopUpTextView** | `.textview` | Popup text | Popup text view | CustomTextView |

#### Headers

| Class Name | Package | Purpose | Public Functions | Key Dependencies |
|------------|---------|---------|------------------|------------------|
| **LeftContainerHeader** | `.headers` | Left header | Left panel header | ConstraintLayout |
| **RightContainerHeader** | `.headers` | Right header | Right panel header | ConstraintLayout |
| **PopupHeader** | `.headers` | Popup header | Popup dialog header | ConstraintLayout |
| **PopUpHeaderHighPriority** | `.headers` | High priority header | High priority popup header | PopupHeader |

#### Custom Views

| Class Name | Package | Purpose | Public Functions | Key Dependencies |
|------------|---------|---------|------------------|------------------|
| **CustomView** | `.custom` | Base custom view | Base themed view | View |
| **Image** | `.custom` | Custom image view | Themed image view | AppCompatImageView |
| **ScrollView** | `.views` | Custom scroll view | Themed scroll view | ScrollView |
| **CustomHorizontalScrollView** | `.views` | Horizontal scroll | Themed horizontal scroll | HorizontalScrollView |
| **CustomSwitchButton** | `.layouts` | Switch layout | Switch button layout | ConstraintLayout |
| **CursorEditText** | `.utils` | Cursor edit text | EditText with custom cursor | AppCompatEditText |

#### Theme Models

| Class Name | Package | Purpose | Public Functions | Key Dependencies |
|------------|---------|---------|------------------|------------------|
| **Theme** | `.model` | Main theme model | Complete theme configuration | Data class |
| **TitleBarModel** | `.model` | Title bar theme | Title bar styling | Data class |
| **CommandBarModel** | `.model` | Command bar theme | Command bar styling | Data class |
| **CommandBarButtonModel** | `.model` | Button theme | Button styling | Data class |
| **ActionButtonModel** | `.model` | Action button theme | Action button styling | Data class |
| **MenuButtonModel** | `.model` | Menu button theme | Menu button styling | Data class |
| **DashboardContainerModel** | `.model` | Dashboard theme | Dashboard styling | Data class |
| **SettingsContainerModel** | `.model` | Settings theme | Settings styling | Data class |
| **DeviceSettingsContainerModel** | `.model` | Device settings theme | Device settings styling | Data class |
| **NoteViewContainerModel** | `.model` | Note view theme | Note view styling | Data class |
| **PopupModel** | `.model` | Popup theme | Popup styling | Data class |
| **TaskFrameModel** | `.model` | Task frame theme | Task frame styling | Data class |
| **WorkflowThemeModel** | `.model` | Workflow theme | Workflow styling | Data class |
| **RadiusModel** | `.model` | Corner radius | Border radius values | Data class |
| **StrokeModel** | `.model` | Border stroke | Stroke width/color | Data class |
| **ShadowModel** | `.model` | Shadow styling | Shadow configuration | Data class |
| **GradientModel** | `.model` | Gradient config | Gradient colors/direction | Data class |
| **CursorModel** | `.model` | Cursor styling | Cursor configuration | Data class |
| **ColorItem** | `.model` | Color data | Color value holder | Data class |

#### Theme Utilities

| Class Name | Package | Purpose | Public Functions | Key Dependencies |
|------------|---------|---------|------------------|------------------|
| **AugmentalisTheme** | `.utils` | Theme manager | `getSavedTheme()`, theme constants | Object |
| **DefaultTheme** | `.utils` | Default light theme | Default theme configuration | Object |
| **DefaultNightTheme** | `.utils` | Default dark theme | Dark theme configuration | Object |
| **ContainerType** | `.utils` | Container types | Container type enum | Enum |
| **CursorSize** | `.utils` | Cursor sizes | Cursor size enum | Enum |
| **Extension** | `.utils` | Extension functions | Theme extension functions | - |
| **Common** | `.utils` | Common utilities | Common theme utilities | - |

### VoiceOS Storage Module (`com.augmentalis.dev.voiceos_storage`)

#### Database Models (Realm)

| Class Name | Package | Purpose | Public Functions | Key Dependencies |
|------------|---------|---------|------------------|------------------|
| **WebBrowserDB** | `.database` | Browser state DB | `id`, `listOfAllViews`, `pOnFocus` | RealmObject |
| **WebPageDB** | `.database` | Web page DB | `name`, `scrollYPosition`, `scrollXPosition`, `uri`, `desktopMode` | RealmObject |
| **TaskDB** | `.database` | Task database | Task storage | RealmObject |
| **SettingDB** | `.database` | Settings DB | App settings storage | RealmObject |
| **AiViewDB** | `.database` | AiView DB | View state storage | RealmObject |
| **CommandDB** | `.database` | Command DB | Command storage | RealmObject |
| **AvawCommandDB** | `.database` | AVAW command DB | AVAW command storage | RealmObject |
| **FavAppsDB** | `.database` | Favorite apps DB | Favorite apps storage | RealmObject |
| **WorkFlowStepDB** | `.database` | Workflow step DB | Workflow step storage | RealmObject |

#### Utilities

| Class Name | Package | Purpose | Public Functions | Key Dependencies |
|------------|---------|---------|------------------|------------------|
| **ViewType** | `.utils` | View type enum | WEB_VIEW, PDF_VIEW, IMAGE_VIEW, VIDEO_PLAYER, CAMERA_VIEW, NOTE_VIEW, WORKFLOW_VIEW, FRAME_VIEW | Enum |
| **ViewMode** | `.utils` | View mode enum | ROW, COLUMN, GRID | Enum |
| **WorkflowDirection** | `.utils` | Workflow direction | Direction enum | Enum |
| **Utils** | `.utils` | Storage utilities | Storage utility functions | - |
| **DashBoardItem** | `.model` | Dashboard item | Dashboard item model | Data class |

### App Preferences Module (`com.augmentalis.dev.preferences`)

| Class Name | Package | Purpose | Public Functions | Key Dependencies |
|------------|---------|---------|------------------|------------------|
| **SharedPreferenceUtils** | - | Preferences manager | `getThemeStyle()`, `setThemeStyle()`, `getNewCustomTheme()`, `getHeaderLogo()`, `getBackgroundImage()`, `getKeepBackgroundImage()`, `getBrowserScale()`, `isGazeEnabled()`, `setGoogleLogedIN/OfficeLogedIN/VidcallLogedIN()`, plus many more getters/setters | Singleton preference utility |
| **TrippleDes** | - | Encryption utility | Triple DES encryption for preferences | Encryption |
| **AppExtension** | - | Extension functions | Extension functions for preferences | - |

### VoiceOS Logger Module (`com.augmentalis.voiceoslogger`)

| Class Name | Package | Purpose | Public Functions | Key Dependencies |
|------------|---------|---------|------------------|------------------|
| **VoiceOsLogger** | - | Logging utility | `d()`, `e()`, `i()`, `w()`, `v()` (debug, error, info, warning, verbose) | Object, Android Log wrapper |

---

## 2. UI Elements Inventory

| Component Name | Type | Purpose | Layout File | Parent Screen/Container | Key UI Elements |
|----------------|------|---------|-------------|------------------------|-----------------|
| **WebBrowserActivity** | Activity | Main host activity | `activity_web_browser.xml` | Root | NavHostFragment |
| **WebViewFragment** | Fragment | Main browser screen | `fragment_webview.xml` | WebBrowserActivity | Toolbar, title, desktop mode icon, tabs container, favorites scroll, webView container, BottomCommandBar |
| **AddVidCallAccountFragment** | Fragment | Video call account setup | `fragment_add_vidcall_account.xml` | WebBrowserActivity | Video call account form |
| **WebViewDialogFragment** | DialogFragment | Authentication dialog | `dialog_authenticate.xml` | WebViewFragment | Username/password inputs, login button |
| **BottomCommandBar** | Custom View | Command bar | `bottom_command_bar.xml` | WebViewFragment | Button container, title text, command buttons |
| **CommandBarButton** | MaterialButton | Command button | - | BottomCommandBar | Icon, text (as contentDescription) |
| **DashboardButton** | MaterialButton | Dashboard button | `button_dashboard.xml` | Various containers | Icon, text |
| **CustomSwitchButton** | ConstraintLayout | Switch toggle | `custom_switch_button.xml` | Settings screens | Switch, label |
| **LeftContainer** | ConstraintLayout | Left panel | - | Various screens | Themed left sidebar |
| **RightContainer** | ConstraintLayout | Right panel | - | Various screens | Themed right sidebar |
| **DashboardContainer** | ConstraintLayout | Dashboard panel | - | Dashboard screen | Themed dashboard |
| **NoteViewContainer** | FrameLayout | Note editor | - | WebViewFragment (as AiView) | EditText for notes |
| **DeviceSettingsContainer** | ConstraintLayout | Settings panel | - | Settings screen | Settings options |
| **LeftContainerHeader** | ConstraintLayout | Left header | - | LeftContainer | Logo, title |
| **RightContainerHeader** | ConstraintLayout | Right header | - | RightContainer | Title, close button |
| **PopupHeader** | ConstraintLayout | Popup header | - | Dialogs | Title, close button |
| **PopUpHeaderHighPriority** | ConstraintLayout | High priority header | - | Important dialogs | Title, close button |
| **WebViewContainer** | WebView | Web browser view | - | WebViewFragment (via AiView) | WebView with custom client |
| **ImageViewContainer** | FrameLayout | Image viewer | - | WebViewFragment (as AiView) | ImageView with zoom/pan |
| **PdfViewContainer** | FrameLayout | PDF viewer | - | WebViewFragment (as AiView) | PDF rendering view |
| **CameraViewContainer** | FrameLayout | Camera view | `activity_barcode_scanner.xml` | WebViewFragment (as AiView) | Camera preview, controls |
| **VideoPlayerContainerWithThumbnail** | FrameLayout | Video player | `layout_video_container.xml` | WebViewFragment (as AiView) | Video player, controls, thumbnail |
| **WorkflowContainer** | FrameLayout | Workflow steps | - | WebViewFragment (as AiView) | Workflow step list |
| **WebViewTabs** | Custom View | Tab bar item | - | WebViewFragment (linear_tabs) | Tab icon, title |
| **WebViewFavorites** | Custom View | Favorite link | - | WebViewFragment (linear_favorites) | Favorite icon, title |
| **AddTaskDialog** | Dialog | Add web page dialog | `dialog_add_task.xml` | WebViewFragment | Toolbar, header logo, title, URL input, confirm/cancel buttons |
| **HelpDialog** | Dialog | Help menu | `dialog_help_menu.xml` | WebViewFragment | Command list with icons |
| **ProgressDialog** | Dialog | Progress indicator | `progress_dialog.xml` | Various screens | Progress bar, message |
| **LicenseActivity** | Activity | License screen | `activity_license.xml` | Separate flow | License info |
| **BarcodeScannerActivity** | Activity | QR/barcode scanner | `activity_barcode_scanner.xml` | Launched from WebViewFragment | Camera scanner view |
| **CustomEditText** | EditText | Custom input | `edittext_layout.xml` | Various forms | Styled EditText |
| **PinView** | Custom View | PIN input | `view_pin.xml` | Auth screens | PIN digit inputs |
| **ToastLayout** | FrameLayout | Custom toast | `layout_toast.xml` | App-wide | Toast message view |
| **ToolbarFrame** | FrameLayout | Toolbar frame | `frame_toolbar.xml` | Various screens | Toolbar container |
| **TabTitleFrame** | FrameLayout | Tab title frame | `frame_tab_title.xml` | Tab bar | Tab title container |
| **ItemCommandView** | ConstraintLayout | Command item | `item_command.xml` | Help dialog | Command icon, text |

---

## 3. Screen Flow Documentation

### Main Screens

1. **WebBrowserActivity (Entry Point)**
   - Host activity containing NavHostFragment
   - Handles theme switching
   - Manages Dropbox OAuth broadcasts
   - No direct UI - container only

2. **WebViewFragment (Main Screen)**
   - Primary browser interface
   - Contains toolbar with title and desktop mode indicator
   - Tab bar for multiple web pages
   - Favorites bar for quick access
   - Main content area (webView_container) for AiView instances
   - Bottom command bar for voice/touch commands

3. **AddVidCallAccountFragment**
   - Video call account setup screen
   - Accessed from WebViewFragment when adding video call accounts

4. **AddTaskDialog (Dialog)**
   - URL input dialog for adding new web pages
   - Appears when "Add Page" command is triggered
   - Shows when browser has no open pages

5. **WebViewDialogFragment (Dialog)**
   - HTTP Basic Auth dialog
   - Shows username/password inputs
   - QR code scan option for credentials

6. **HelpDialog (Dialog)**
   - Shows available voice commands
   - Lists all current command bar buttons with icons and descriptions

### Navigation Flow

```
App Launch
    ↓
WebBrowserActivity (onCreate)
    ↓
NavController loads WebViewFragment
    ↓
WebViewFragment (onViewCreated)
    ↓
Check for URL parameter:
    - Has URL? → Load URL directly
    - Has saved URL? → Load saved pages
    - No params? → Load from database or show AddTaskDialog
    ↓
User Interactions:
    ├─ Add Page → AddTaskDialog → Enter URL → Load page
    ├─ Select Tab → Switch to different WebView
    ├─ Select Favorite → Load favorite URL
    ├─ Command Bar → Navigate to different command modes
    ├─ Basic Auth → WebViewDialogFragment → Enter credentials
    ├─ Help Button → HelpDialog → View commands
    └─ Back Button → Close app or dialog
```

### Entry Points

1. **Normal Launch**
   - `WebBrowserActivity.openBrowser(context)` → Empty browser or loads database

2. **Open with URL**
   - `WebBrowserActivity.openUrl(context, url)` → Loads specific URL

3. **Open Saved Pages**
   - `WebBrowserActivity.openSavedPages(context, savedUrlName)` → Loads named saved state

4. **External Link Intent**
   - Intent filter in AndroidManifest → Handles http/https URLs

### Screen Transitions

- **Fragment Transitions**: Handled by Navigation Component
- **Dialog Presentations**: Modal dialogs overlay current screen
- **No Fragment Back Stack**: Single fragment architecture with dialog overlays
- **Activity Finish**: Back button closes activity when no dialog is showing

---

## 4. Command System Documentation

### Command Architecture

The application uses a hierarchical command system with multiple modes, designed for voice-first interaction.

### Command Modes

#### 1. **INITIAL_COMMANDS** (Default Mode)
Primary command set when fragment loads.

| Command | Icon | Function | Description |
|---------|------|----------|-------------|
| Go Back | ic_baseline_arrow_back_24 | `goBack()` | Navigate back or close app |
| Home | ic_home_icon | `goHome()` | Close app/return to dashboard |
| Add Page | ic_baseline_add_24 | `createWebDialog()` | Open URL input dialog |
| Navigate Commands | ic_open_scroll_command_icon | `loadWebViewNavigationCommands()` | Switch to navigation mode |
| Web Media Commands | ic_open_browser | `loadWebViewWebSpecificCommands()` | Switch to web-specific mode |
| Previous Frame | ic_baseline_navigate_before_24 | `previousView()` | Switch to previous tab (if multiple tabs) |
| Next Frame | ic_baseline_navigate_next_24 | `nextView()` | Switch to next tab (if multiple tabs) |

#### 2. **NAVIGATION_COMMANDS** (Navigation Mode)
Commands for page navigation and interaction modes.

| Command | Icon | Function | Description |
|---------|------|----------|-------------|
| Go Back | ic_baseline_arrow_back_24 | `loadWebViewInitialCommands()` | Return to initial mode |
| Home | ic_home_icon | `goHome()` | Close app |
| Open Scroll | ic_scroll_icon | `loadScrollCommands()` | Switch to scroll mode |
| Open Cursor | ic_baseline_ads_click_24 | `loadWebViewCursorCommands()` | Switch to cursor mode |
| Zoom Commands | ic_baseline_zoom_in_24 | `loadWebViewZoomCommands()` | Switch to zoom mode |

#### 3. **SCROLL_COMMANDS** (Scroll Mode)
Commands for scrolling and page positioning.

| Command | Icon | Function | Description |
|---------|------|----------|-------------|
| Go Back | ic_baseline_arrow_back_24 | `loadWebViewNavigationCommands()` | Return to navigation mode |
| Home | ic_home_icon | `goHome()` | Close app |
| Scroll Up | ic_scroll_up | `scrollUp()` | Scroll page up |
| Scroll Down | ic_scroll_down | `scrollDown()` | Scroll page down |
| Scroll Left | ic_baseline_navigate_before_24 | `scrollLeft()` | Scroll page left |
| Scroll Right | ic_baseline_navigate_next_24 | `scrollRight()` | Scroll page right |
| Page Up | ic_baseline_vertical_align_top_24 | `scrollTop()` | Scroll to top |
| Page Down | ic_baseline_vertical_align_bottom_24 | `scrollBottom()` | Scroll to bottom |
| Freeze/Unfreeze Page | ic_freeze_svg / ic_unfreeze_svg | `toggleFreezeFrame()` | Toggle page freeze (prevents scrolling) |

#### 4. **CURSOR_COMMANDS** (Cursor Mode)
Commands for cursor interaction (accessibility features).

| Command | Icon | Function | Description |
|---------|------|----------|-------------|
| Go Back | ic_baseline_arrow_back_24 | `loadWebViewNavigationCommands()` | Return to navigation mode |
| Home | ic_home_icon | `goHome()` | Close app |
| Select | ic_noun_one_click | `singleClick()` | Simulate single click at cursor |
| Double Click | ic_noun_double_click | `doubleClick()` | Simulate double click at cursor |

#### 5. **ZOOM_COMMANDS** (Zoom Mode)
Commands for zoom control.

| Command | Icon | Function | Description |
|---------|------|----------|-------------|
| Go Back | ic_baseline_arrow_back_24 | `loadWebViewNavigationCommands()` | Return to navigation mode |
| Home | ic_home_icon | `goHome()` | Close app |
| Zoom In | ic_baseline_zoom_in_24 | `zoomIn()` | Increase zoom level |
| Zoom Out | ic_baseline_zoom_out_24 | `zoomOut()` | Decrease zoom level |
| Zoom Level | ic_zoom_level | `loadWebViewZoomLevelCommands()` | Switch to zoom level mode |

#### 6. **ZOOM_LEVEL_COMMANDS** (Zoom Level Mode)
Commands for setting specific zoom levels.

| Command | Icon | Function | Description |
|---------|------|----------|-------------|
| Go Back | ic_baseline_arrow_back_24 | `loadWebViewZoomCommands()` | Return to zoom mode |
| Home | ic_home_icon | `goHome()` | Close app |
| Zoom Level +1 | ic_level_1 | `setZoomLevel(1)` | Set zoom to level 1 |
| Zoom Level +2 | ic_level_2 | `setZoomLevel(2)` | Set zoom to level 2 |
| Zoom Level +3 | ic_level_3 | `setZoomLevel(3)` | Set zoom to level 3 |
| Zoom Level +4 | ic_level_4 | `setZoomLevel(4)` | Set zoom to level 4 |
| Zoom Level +5 | ic_level_5 | `setZoomLevel(5)` | Set zoom to level 5 |

#### 7. **WEB_SPECIFIC_COMMANDS** (Web-Specific Mode)
Commands for web page specific actions.

| Command | Icon | Function | Description |
|---------|------|----------|-------------|
| Go Back | ic_baseline_arrow_back_24 | `loadWebViewInitialCommands()` | Return to initial mode |
| Home | ic_home_icon | `goHome()` | Close app |
| Previous Page | arrow_back_white | `previousPage()` | Browser back navigation |
| Next Page | arrow_forward_white | `nextPage()` | Browser forward navigation |
| Reload | ic_baseline_refresh | `reload()` | Reload current page |
| Desktop Mode On/Off | ic_baseline_desktop_windows_24 / ic_baseline_desktop_access_disabled_24 | `changeDesktopMode()` | Toggle desktop/mobile user agent |
| Touch Commands | ic_touch_commands | `loadWebViewTouchCommands()` | Switch to touch mode |
| Favorite Page | ic_baseline_favorite_24 | `favoriteWebPage()` | Add current page to favorites |
| Clear Cookies | ic_clear_cookies_24 | `clearCookies()` | Clear browser cookies |

#### 8. **TOUCH_COMMANDS** (Touch Mode)
Commands for touch gesture simulation.

| Command | Icon | Function | Description |
|---------|------|----------|-------------|
| Go Back | ic_baseline_arrow_back_24 | `loadWebViewWebSpecificCommands()` | Return to web-specific mode |
| Home | ic_home_icon | `goHome()` | Close app |
| Drag Start/Stop | ic_drag | `toggleDrag()` | Toggle drag mode |
| Rotate Image | ic_rotate_view | `rotateView()` | Rotate current view |
| Pinch Open | ic_baseline_pinch_open | `pinchOpen()` | Simulate pinch out (zoom in) |
| Pinch Close | ic_pinch_close | `pinchClose()` | Simulate pinch in (zoom out) |

### How Commands Work

1. **Command Definition**
   - Commands are defined in `WebViewCommands.kt` as extension functions on `Context`
   - Each function returns `ArrayList<CommandBarButton>`
   - Commands contain: text (contentDescription), icon, onClick/onButtonClick lambda

2. **Command Display**
   - Commands are set on `BottomCommandBar` via `setButtons(commands)`
   - Command bar displays buttons horizontally with scrolling
   - Current command title shown in command bar text area
   - Active button highlighted with different theme

3. **Command Invocation**

   **Touch/Click:**
   - User taps command button
   - Button becomes active (focused)
   - Lambda function executes
   - Command bar updates to show command name

   **Voice (Accessibility - TODO):**
   - Voice recognition would trigger command by name
   - Accessibility service broadcasts click events
   - Commands respond to broadcast intents
   - (Currently has TODO comments for accessibility integration)

4. **Command Navigation**
   - Commands form a hierarchical tree structure
   - Each mode has "Go Back" to return to parent mode
   - "Home" command always returns to app exit
   - Mode transitions update command bar with new button set

5. **Command State**
   - Some commands toggle state (e.g., Desktop Mode, Freeze, Drag)
   - Toggle commands update their icon and text on click
   - State changes persist in ViewModel and database

### Command-to-Screen Interactions

| Command Mode | Affects | Updates |
|-------------|---------|---------|
| Initial Commands | Fragment navigation, Tab switching | WebViewPage, tabs UI |
| Navigation Commands | Command mode only | Command bar |
| Scroll Commands | WebView scroll position | WebView scrollY/X, database |
| Cursor Commands | Accessibility cursor | Broadcasts to accessibility service |
| Zoom Commands | WebView zoom level | WebView scale, database |
| Web-Specific Commands | WebView state, favorites | WebView settings, favorites bar, cookies |
| Touch Commands | WebView touch simulation | WebView gestures |

### Command Extensions

Commands can be extended in `WebViewCommands.kt`:

```kotlin
fun Context.getCustomCommands(
    goBack: () -> Unit,
    customAction: () -> Unit
) = arrayListOf(
    CommandBarButton.back(this) { goBack.invoke() },
    CommandBarButton(
        context = this,
        text = getString(R.string.custom_command),
        iconResource = R.drawable.ic_custom,
        onClick = { customAction.invoke() }
    )
)
```

### Help System

- Help button on command bar (when collapsed)
- Opens `HelpDialog` showing all current commands
- Displays command icon and name in list format
- User can see available voice commands at any time

---

## 5. Project Architecture Summary (Detailed)

### Module Dependencies

```
app
├── augmentalis_theme
├── bottom-command-bar
│   └── voiceos-resources
├── voiceos-common
│   ├── voiceos-storage
│   ├── voiceos-logger
│   └── voiceos-resources
├── voiceos-storage
├── voiceos-logger
├── app-preferences
└── voiceos-resources
```

### Architectural Patterns

#### MVVM (Model-View-ViewModel)

**View Layer:**
- `WebViewFragment` - UI layer, observes ViewModel LiveData
- XML layouts with ViewBinding
- Custom views from augmentalis_theme

**ViewModel Layer:**
- `WebViewModel` - Business logic, LiveData publishers
- Manages WebViewPage state
- Handles browser operations (scroll, zoom, tabs)
- Communicates with Repository

**Model Layer:**
- Data classes: `AiView`, `WebViewContainer`, `WebViewPage`
- Database models: `WebBrowserDB`, `WebPageDB` (Realm)
- Repository pattern for data access

#### Dependency Injection (Hilt)

**Modules:**
- `AppModule` - Provides app-wide singletons
  - SharedPreferences
  - SharedPreferenceUtils
  - Realm database
  - ZipManager
  - Application context

**Annotations:**
- `@HiltAndroidApp` on `App` class
- `@AndroidEntryPoint` on `WebBrowserActivity`, `WebViewFragment`
- `@HiltViewModel` on `WebViewModel`
- `@Inject` on constructor parameters

#### Repository Pattern

**Repository:**
- `Repository` class with `RepositoryInterface`
- Abstracts Realm database operations
- Methods:
  - `updateWebBrowser(webViewPage)` - Save browser state
  - `loadWebBrowser()` - Load browser state
  - `getSetting(settingType)` - Get app settings

**Data Flow:**
```
ViewModel → Repository → Realm Database
    ↓           ↓             ↓
LiveData ← Data Model ← RealmObject
    ↓
Fragment (Observer)
```

#### Navigation

**Android Navigation Component:**
- `nav_graph_web_browser.xml` defines navigation graph
- Destinations:
  - WebViewFragment (start destination)
  - AddVidCallAccountFragment
- Arguments passed via Safe Args plugin

### Key Technologies & Libraries

#### Core Technologies
- **Language**: Kotlin (100%)
- **Build**: Gradle 8.x with Kotlin DSL
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34

#### Android Jetpack
- **AppCompat** - Backward compatibility
- **ConstraintLayout** - Layout system
- **Navigation Component** - Fragment navigation with Safe Args
- **Lifecycle Components** - LiveData, ViewModel
- **ViewBinding** - Type-safe view access
- **DataBinding** - Data-driven UI (enabled but limited use)
- **Hilt** - Dependency injection

#### UI Libraries
- **Material Components** - Material Design widgets
- **Coil** - Image loading with SVG support
- **Custom Theme System** (augmentalis_theme) - Complete theming framework
- **SDP/SSP** - Scalable size units

#### Database
- **Realm** - Mobile database
  - Schema version: 1
  - Database name: "Workstation"
  - Allows queries/writes on UI thread
  - Auto-migration with deleteRealmIfMigrationNeeded()

#### Cloud & Sync
- **Firebase** - Analytics, Crashlytics
- **Google Services** - Google Drive integration
- **Dropbox SDK** - Dropbox file integration
- **Microsoft ADAL** - OneDrive/Office 365 auth
- **Box SDK** - Box cloud storage

#### Networking
- No explicit networking library (uses WebView for web content)
- WebView with custom WebViewClient for authentication

#### Utilities
- **Kotlin Coroutines** - Async operations
- **ExifInterface** - Image metadata reading
- **ZXing** - Barcode/QR code scanning
- **Triple DES** - Preference encryption

#### Security
- **DexGuard** - Code obfuscation (commented out in current build)
- **ProGuard** - Minification disabled in debug/release
- **Triple DES** encryption for sensitive preferences

### Data Persistence

#### Realm Database

**Schema:**

```kotlin
WebBrowserDB {
    id: String (PK) = "browser"
    listOfAllViews: RealmList<WebPageDB>
    pOnFocus: WebPageDB
}

WebPageDB {
    name: String (PK) = UUID
    scrollYPosition: Int
    scrollXPosition: Int
    uri: String
    desktopMode: Boolean
}
```

**Additional Tables:**
- TaskDB
- SettingDB
- AiViewDB
- CommandDB
- AvawCommandDB
- FavAppsDB
- WorkFlowStepDB

#### SharedPreferences

**Stored Data:**
- Theme configuration (custom themes, colors)
- User login states (Google, Office, VidCall)
- App settings (gaze enabled, browser scale)
- Header logo path
- Background image settings
- Device-specific settings

**Encryption:**
- Triple DES encryption via `TrippleDes` class
- Applied to sensitive preferences

#### In-Memory Storage

**AuthDatabase (Singleton):**
- Username/password cache for HTTP Basic Auth
- URL-based storage
- No disk persistence (security)

### WebView Architecture

#### AiView System

**Purpose:** Universal container for different view types

**Supported Types:**
- WEB_VIEW - Web browsing
- PDF_VIEW - PDF documents
- IMAGE_VIEW - Images with zoom/pan
- VIDEO_PLAYER - Video playback
- CAMERA_VIEW - Camera preview
- NOTE_VIEW - Note taking
- WORKFLOW_VIEW - Step workflows
- FRAME_VIEW - Empty frame

**Architecture:**
```
AiView (FrameLayout)
├── WebViewContainer (for WEB_VIEW)
├── PdfViewContainer (for PDF_VIEW)
├── ImageViewContainer (for IMAGE_VIEW)
├── VideoPlayerContainer (for VIDEO_PLAYER)
├── CameraViewContainer (for CAMERA_VIEW)
├── NoteViewContainer (for NOTE_VIEW)
└── WorkflowContainer (for WORKFLOW_VIEW)
```

**Multi-Tab Support:**
- WebViewPage manages multiple AiView instances
- Each AiView has corresponding WebViewTabs UI
- Tab switching updates focus and command bar

#### WebViewContainer Features

**Desktop Mode:**
- Custom User-Agent switching
- Desktop: Full desktop UA string
- Mobile: Default Android WebView UA
- Per-tab desktop mode setting

**Authentication:**
- HTTP Basic Auth via WebViewDialogFragment
- AuthDatabase for credential caching
- OAuth flows (Dropbox, Google, Office)
- Login detection for Google/Office/VidCall

**JavaScript Interface:**
- `JavascriptInterface` injected as "webview"
- JavaScript-to-native communication
- Dynamic command execution

**Download Handling:**
- DownloadListener for file downloads
- DownloadManager integration
- Downloads saved to /Augmentalis/<type>/ in Downloads

**Cookie Management:**
- Cookie persistence enabled
- Third-party cookies enabled
- Clear cookies command available

### Theme System

#### Theme Architecture

**Components:**
- 23 theme model classes
- 6 container types
- 4 button types
- 5 text view types
- 4 header types
- Custom view system

**Theme Storage:**
- Saved in SharedPreferences
- Runtime theme switching
- Day/night mode support
- Custom gradient support

**Theme Models:**
- Stroke (border width/color)
- Radius (corner radius)
- Gradient (colors, direction)
- Shadow (elevation, color)
- Color (ARGB values)

**Theming Approach:**
- All UI components themeable
- Consistent design language
- Accessibility-friendly colors
- Custom cursor support

### Voice-First Design

**Command Bar:**
- Always visible at bottom
- Voice command naming convention
- Help dialog for command discovery
- Keyboard-free operation support

**Accessibility Features:**
- Custom cursor system (TODO: integration)
- Gaze control support (TODO: integration)
- Motion input simulation
- Device-specific adaptations (Vuzix, RealWear)

**Device Support:**
- Vuzix smart glasses
- RealWear HMT (Head-Mounted Tablet)
- Standard Android devices
- Custom scale adjustments per device

### Build Configuration

**Flavors:** None (single flavor)

**Build Types:**
- Debug: No minification, no shrinking
- Release: No minification, no shrinking (DexGuard commented out)

**ABI Filters:**
- arm64-v8a
- armeabi-v7a
- x86
- x86_64

**Multi-Language Support:**
- 41 language configurations
- Includes regional variants (en-rAU, en-rGB, es-rMX, etc.)

**ProGuard/DexGuard:**
- Currently disabled
- Configuration files present for future use
- Hilt-specific rules included

### API Keys & Secrets

**Build Config Fields:**
- LICENSE_KEY
- DROPBOX_CLIENT_ID, DROPBOX_CLIENT_KEY
- ONEDRIVE_CLIENT_ID, ONEDRIVE_CLIENT_KEY
- BOX_CLIENT_ID, BOX_CLIENT_KEY
- PLAYSTORE_LICENSE_KEY
- MERCHANT_ID
- GOOGLE_DRIVE_CLIENT_ID
- And more...

**Source:** `keystore.properties` file (not in repository)

---

## Code Statistics

- **Total Kotlin Files**: 151
- **Total XML Layouts**: ~20
- **Modules**: 9
- **Main Application Package**: `com.augmentalis.dev`

---

## Naming Conventions

- **Kotlin files**: PascalCase
- **Layouts**: snake_case with prefixes (activity_, fragment_, dialog_, item_, layout_)
- **Resources**: snake_case with type prefixes (ic_, btn_, txt_)
- **View IDs**: camelCase with type prefixes (ivMode, txtTitle, btnCancel)

---

## Code Quality

- **Documentation**: Comprehensive KDoc comments on key classes
- **Architecture**: Clean separation of concerns
- **Modularity**: Well-organized module structure
- **Testability**: Dependency injection enables testing
- **Test Coverage**: Minimal (example tests only)

---

## Future Integration Points (TODOs in code)

1. Accessibility Service integration for voice commands
2. Cursor magnification system
3. Gaze control features
4. Barcode scanning activity
5. Rotation view broadcasts
6. Extended accessibility features

---

## Security Considerations

- Triple DES encryption for preferences
- No hardcoded credentials
- OAuth flows for cloud services
- HTTP Basic Auth with secure dialogs
- Cookie management
- Download security via DownloadManager

---

**End of Analysis**
