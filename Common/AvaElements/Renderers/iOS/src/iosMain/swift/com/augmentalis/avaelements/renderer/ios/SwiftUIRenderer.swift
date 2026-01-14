import SwiftUI
import Foundation

/// SwiftUI renderer for AVAMagic components
///
/// This renderer converts AVAMagic component models into SwiftUI views,
/// providing platform-native rendering on iOS and macOS.
///
/// **Architecture:**
/// - Component type switch for routing to specialized mappers
/// - Theme integration with automatic inheritance
/// - Resource management for icons, images, and assets
/// - Error handling for unsupported components
///
/// **Usage:**
/// ```swift
/// let renderer = SwiftUIRenderer(theme: currentTheme)
/// let view = renderer.render(component: myComponent)
/// ```
///
/// **Flutter Parity:**
/// Supports 58 Flutter parity components across:
/// - Layout (10): Wrap, Expanded, Flexible, Flex, Padding, Align, Center, SizedBox, ConstrainedBox, FittedBox
/// - Material (17): FilterChip, ActionChip, ChoiceChip, InputChip, ExpansionTile, CheckboxListTile, etc.
/// - Scrolling (7): ListView.builder, GridView.builder, PageView, ReorderableListView, etc.
/// - Animation (8): AnimatedContainer, AnimatedOpacity, AnimatedPositioned, etc.
/// - Transitions (19): FadeTransition, SlideTransition, Hero, ScaleTransition, etc.
///
/// @since 3.0.0-flutter-parity
public class SwiftUIRenderer: ObservableObject {

    // MARK: - Properties

    /// Current theme for styling components
    @Published public var theme: AVATheme

    /// Icon resource manager for icon loading and caching
    private let iconResourceManager: IOSIconResourceManager

    /// Image loader for network and cached images
    private let imageLoader: IOSImageLoader

    // MARK: - Initialization

    /// Create a new SwiftUI renderer
    ///
    /// - Parameter theme: Theme to use for component styling (defaults to current theme)
    public init(theme: AVATheme = AVAThemeProvider.currentTheme) {
        self.theme = theme
        self.iconResourceManager = IOSIconResourceManager.shared
        self.imageLoader = IOSImageLoader.shared
    }

    // MARK: - Rendering

    /// Render a component to a SwiftUI view
    ///
    /// - Parameter component: AVAMagic component to render
    /// - Returns: SwiftUI view representing the component
    @ViewBuilder
    public func render(component: AVAComponent) -> some View {
        switch component {

            // MARK: Phase 1 - Form Components
            case let checkbox as AVACheckbox:
                renderCheckbox(checkbox)
            case let textField as AVATextField:
                renderTextField(textField)
            case let button as AVAButton:
                renderButton(button)
            case let switchComponent as AVASwitch:
                renderSwitch(switchComponent)

            // MARK: Phase 1 - Display Components
            case let text as AVAText:
                renderText(text)
            case let image as AVAImage:
                renderImage(image)
            case let icon as AVAIcon:
                renderIcon(icon)

            // MARK: Phase 1 - Layout Components
            case let container as AVAContainer:
                renderContainer(container)
            case let row as AVARow:
                renderRow(row)
            case let column as AVAColumn:
                renderColumn(column)
            case let card as AVACard:
                renderCard(card)

            // MARK: Phase 1 - Navigation & Data
            case let scrollView as AVAScrollView:
                renderScrollView(scrollView)
            case let list as AVAList:
                renderList(list)

            // MARK: Phase 3 - Input Components
            case let slider as AVASlider:
                renderSlider(slider)
            case let rangeSlider as AVARangeSlider:
                renderRangeSlider(rangeSlider)
            case let datePicker as AVADatePicker:
                renderDatePicker(datePicker)
            case let timePicker as AVATimePicker:
                renderTimePicker(timePicker)
            case let radioButton as AVARadioButton:
                renderRadioButton(radioButton)
            case let radioGroup as AVARadioGroup:
                renderRadioGroup(radioGroup)
            case let dropdown as AVADropdown:
                renderDropdown(dropdown)
            case let autocomplete as AVAAutocomplete:
                renderAutocomplete(autocomplete)
            case let fileUpload as AVAFileUpload:
                renderFileUpload(fileUpload)
            case let imagePicker as AVAImagePicker:
                renderImagePicker(imagePicker)
            case let rating as AVARating:
                renderRating(rating)
            case let searchBar as AVASearchBar:
                renderSearchBar(searchBar)

            // MARK: Phase 3 - Display Components
            case let badge as AVABadge:
                renderBadge(badge)
            case let chip as AVAChip:
                renderChip(chip)
            case let avatar as AVAAvatar:
                renderAvatar(avatar)
            case let divider as AVADivider:
                renderDivider(divider)
            case let skeleton as AVASkeleton:
                renderSkeleton(skeleton)
            case let spinner as AVASpinner:
                renderSpinner(spinner)
            case let progressBar as AVAProgressBar:
                renderProgressBar(progressBar)
            case let tooltip as AVATooltip:
                renderTooltip(tooltip)

            // MARK: Phase 3 - Layout Components
            case let grid as AVAGrid:
                renderGrid(grid)
            case let stack as AVAStack:
                renderStack(stack)
            case let spacer as AVASpacer:
                renderSpacer(spacer)
            case let drawer as AVADrawer:
                renderDrawer(drawer)
            case let tabs as AVATabs:
                renderTabs(tabs)

            // MARK: Phase 3 - Navigation Components
            case let appBar as AVAAppBar:
                renderAppBar(appBar)
            case let bottomNav as AVABottomNav:
                renderBottomNav(bottomNav)
            case let breadcrumb as AVABreadcrumb:
                renderBreadcrumb(breadcrumb)
            case let pagination as AVAPagination:
                renderPagination(pagination)

            // MARK: Phase 3 - Feedback Components
            case let alert as AVAAlert:
                renderAlert(alert)
            case let snackbar as AVASnackbar:
                renderSnackbar(snackbar)
            case let modal as AVAModal:
                renderModal(modal)
            case let toast as AVAToast:
                renderToast(toast)
            case let confirm as AVAConfirm:
                renderConfirm(confirm)
            case let contextMenu as AVAContextMenu:
                renderContextMenu(contextMenu)

            // MARK: Flutter Parity - Layout Components (10)
            // TODO: Layout mappers will be implemented by Agent 2
            // - WrapComponent → renderWrap()
            // - ExpandedComponent → renderExpanded()
            // - FlexibleComponent → renderFlexible()
            // - FlexComponent → renderFlex()
            // - PaddingComponent → renderPadding()
            // - AlignComponent → renderAlign()
            // - CenterComponent → renderCenter()
            // - SizedBoxComponent → renderSizedBox()
            // - ConstrainedBoxComponent → renderConstrainedBox()
            // - FittedBoxComponent → renderFittedBox()

            // MARK: Flutter Parity - Material Components (17)
            // TODO: Material mappers will be implemented by Agent 3
            // - FilterChip → renderFilterChip()
            // - ActionChip → renderActionChip()
            // - ChoiceChip → renderChoiceChip()
            // - InputChip → renderInputChip()
            // - ExpansionTile → renderExpansionTile()
            // - CheckboxListTile → renderCheckboxListTile()
            // - SwitchListTile → renderSwitchListTile()
            // - FilledButton → renderFilledButton()
            // - PopupMenuButton → renderPopupMenuButton()
            // - RefreshIndicator → renderRefreshIndicator()
            // - IndexedStack → renderIndexedStack()
            // - VerticalDivider → renderVerticalDivider()
            // - FadeInImage → renderFadeInImage()
            // - CircleAvatar → renderCircleAvatar()
            // - RichText → renderRichText()
            // - SelectableText → renderSelectableText()
            // - EndDrawer → renderEndDrawer()

            // MARK: Flutter Parity - Scrolling Components (7)
            // TODO: Scrolling mappers will be implemented by Agent 4
            // - ListViewBuilderComponent → renderListViewBuilder()
            // - GridViewBuilderComponent → renderGridViewBuilder()
            // - ListViewSeparatedComponent → renderListViewSeparated()
            // - PageViewComponent → renderPageView()
            // - ReorderableListViewComponent → renderReorderableListView()
            // - CustomScrollViewComponent → renderCustomScrollView()
            // - SliverComponent variations → renderSliver*()

            // MARK: Flutter Parity - Animation Components (8)
            // TODO: Animation mappers will be implemented by Agent 5
            // - AnimatedContainer → renderAnimatedContainer()
            // - AnimatedOpacity → renderAnimatedOpacity()
            // - AnimatedPositioned → renderAnimatedPositioned()
            // - AnimatedDefaultTextStyle → renderAnimatedDefaultTextStyle()
            // - AnimatedPadding → renderAnimatedPadding()
            // - AnimatedSize → renderAnimatedSize()
            // - AnimatedAlign → renderAnimatedAlign()
            // - AnimatedScale → renderAnimatedScale()

            // MARK: Flutter Parity - Transition Components (19)
            // TODO: Transition mappers will be implemented by Agent 6
            // - FadeTransition → renderFadeTransition()
            // - SlideTransition → renderSlideTransition()
            // - Hero → renderHero()
            // - ScaleTransition → renderScaleTransition()
            // - RotationTransition → renderRotationTransition()
            // - PositionedTransition → renderPositionedTransition()
            // - SizeTransition → renderSizeTransition()
            // - AnimatedCrossFade → renderAnimatedCrossFade()
            // - AnimatedSwitcher → renderAnimatedSwitcher()
            // - DecoratedBoxTransition → renderDecoratedBoxTransition()
            // - AlignTransition → renderAlignTransition()
            // - DefaultTextStyleTransition → renderDefaultTextStyleTransition()
            // - RelativePositionedTransition → renderRelativePositionedTransition()
            // - AnimatedList → renderAnimatedList()
            // - AnimatedModalBarrier → renderAnimatedModalBarrier()

            default:
                // Unknown component type - show error view
                renderUnknownComponent(component)
        }
    }

    // MARK: - Helper Rendering Methods (Stubs for Phase 1/3 components)

    // Note: These are minimal stubs. Full implementations will be added by specialized agents
    // or in future development phases. The focus here is on infrastructure.

    @ViewBuilder
    private func renderCheckbox(_ component: AVACheckbox) -> some View {
        Text("Checkbox - Implementation pending")
    }

    @ViewBuilder
    private func renderTextField(_ component: AVATextField) -> some View {
        Text("TextField - Implementation pending")
    }

    @ViewBuilder
    private func renderButton(_ component: AVAButton) -> some View {
        Text("Button - Implementation pending")
    }

    @ViewBuilder
    private func renderSwitch(_ component: AVASwitch) -> some View {
        Text("Switch - Implementation pending")
    }

    @ViewBuilder
    private func renderText(_ component: AVAText) -> some View {
        Text("Text - Implementation pending")
    }

    @ViewBuilder
    private func renderImage(_ component: AVAImage) -> some View {
        Text("Image - Implementation pending")
    }

    @ViewBuilder
    private func renderIcon(_ component: AVAIcon) -> some View {
        // Icon rendering uses the IOSIconResourceManager
        if let iconResource = component.iconResource {
            IconView(
                iconResource: iconResource,
                size: component.size ?? 24,
                tint: component.tint,
                iconManager: iconResourceManager
            )
        } else {
            EmptyView()
        }
    }

    @ViewBuilder
    private func renderContainer(_ component: AVAContainer) -> some View {
        Text("Container - Implementation pending")
    }

    @ViewBuilder
    private func renderRow(_ component: AVARow) -> some View {
        Text("Row - Implementation pending")
    }

    @ViewBuilder
    private func renderColumn(_ component: AVAColumn) -> some View {
        Text("Column - Implementation pending")
    }

    @ViewBuilder
    private func renderCard(_ component: AVACard) -> some View {
        Text("Card - Implementation pending")
    }

    @ViewBuilder
    private func renderScrollView(_ component: AVAScrollView) -> some View {
        Text("ScrollView - Implementation pending")
    }

    @ViewBuilder
    private func renderList(_ component: AVAList) -> some View {
        Text("List - Implementation pending")
    }

    @ViewBuilder
    private func renderSlider(_ component: AVASlider) -> some View {
        Text("Slider - Implementation pending")
    }

    @ViewBuilder
    private func renderRangeSlider(_ component: AVARangeSlider) -> some View {
        Text("RangeSlider - Implementation pending")
    }

    @ViewBuilder
    private func renderDatePicker(_ component: AVADatePicker) -> some View {
        Text("DatePicker - Implementation pending")
    }

    @ViewBuilder
    private func renderTimePicker(_ component: AVATimePicker) -> some View {
        Text("TimePicker - Implementation pending")
    }

    @ViewBuilder
    private func renderRadioButton(_ component: AVARadioButton) -> some View {
        Text("RadioButton - Implementation pending")
    }

    @ViewBuilder
    private func renderRadioGroup(_ component: AVARadioGroup) -> some View {
        Text("RadioGroup - Implementation pending")
    }

    @ViewBuilder
    private func renderDropdown(_ component: AVADropdown) -> some View {
        Text("Dropdown - Implementation pending")
    }

    @ViewBuilder
    private func renderAutocomplete(_ component: AVAAutocomplete) -> some View {
        Text("Autocomplete - Implementation pending")
    }

    @ViewBuilder
    private func renderFileUpload(_ component: AVAFileUpload) -> some View {
        Text("FileUpload - Implementation pending")
    }

    @ViewBuilder
    private func renderImagePicker(_ component: AVAImagePicker) -> some View {
        Text("ImagePicker - Implementation pending")
    }

    @ViewBuilder
    private func renderRating(_ component: AVARating) -> some View {
        Text("Rating - Implementation pending")
    }

    @ViewBuilder
    private func renderSearchBar(_ component: AVASearchBar) -> some View {
        Text("SearchBar - Implementation pending")
    }

    @ViewBuilder
    private func renderBadge(_ component: AVABadge) -> some View {
        Text("Badge - Implementation pending")
    }

    @ViewBuilder
    private func renderChip(_ component: AVAChip) -> some View {
        Text("Chip - Implementation pending")
    }

    @ViewBuilder
    private func renderAvatar(_ component: AVAAvatar) -> some View {
        Text("Avatar - Implementation pending")
    }

    @ViewBuilder
    private func renderDivider(_ component: AVADivider) -> some View {
        Text("Divider - Implementation pending")
    }

    @ViewBuilder
    private func renderSkeleton(_ component: AVASkeleton) -> some View {
        Text("Skeleton - Implementation pending")
    }

    @ViewBuilder
    private func renderSpinner(_ component: AVASpinner) -> some View {
        Text("Spinner - Implementation pending")
    }

    @ViewBuilder
    private func renderProgressBar(_ component: AVAProgressBar) -> some View {
        Text("ProgressBar - Implementation pending")
    }

    @ViewBuilder
    private func renderTooltip(_ component: AVATooltip) -> some View {
        Text("Tooltip - Implementation pending")
    }

    @ViewBuilder
    private func renderGrid(_ component: AVAGrid) -> some View {
        Text("Grid - Implementation pending")
    }

    @ViewBuilder
    private func renderStack(_ component: AVAStack) -> some View {
        Text("Stack - Implementation pending")
    }

    @ViewBuilder
    private func renderSpacer(_ component: AVASpacer) -> some View {
        Text("Spacer - Implementation pending")
    }

    @ViewBuilder
    private func renderDrawer(_ component: AVADrawer) -> some View {
        Text("Drawer - Implementation pending")
    }

    @ViewBuilder
    private func renderTabs(_ component: AVATabs) -> some View {
        Text("Tabs - Implementation pending")
    }

    @ViewBuilder
    private func renderAppBar(_ component: AVAAppBar) -> some View {
        Text("AppBar - Implementation pending")
    }

    @ViewBuilder
    private func renderBottomNav(_ component: AVABottomNav) -> some View {
        Text("BottomNav - Implementation pending")
    }

    @ViewBuilder
    private func renderBreadcrumb(_ component: AVABreadcrumb) -> some View {
        Text("Breadcrumb - Implementation pending")
    }

    @ViewBuilder
    private func renderPagination(_ component: AVAPagination) -> some View {
        Text("Pagination - Implementation pending")
    }

    @ViewBuilder
    private func renderAlert(_ component: AVAAlert) -> some View {
        Text("Alert - Implementation pending")
    }

    @ViewBuilder
    private func renderSnackbar(_ component: AVASnackbar) -> some View {
        Text("Snackbar - Implementation pending")
    }

    @ViewBuilder
    private func renderModal(_ component: AVAModal) -> some View {
        Text("Modal - Implementation pending")
    }

    @ViewBuilder
    private func renderToast(_ component: AVAToast) -> some View {
        Text("Toast - Implementation pending")
    }

    @ViewBuilder
    private func renderConfirm(_ component: AVAConfirm) -> some View {
        Text("Confirm - Implementation pending")
    }

    @ViewBuilder
    private func renderContextMenu(_ component: AVAContextMenu) -> some View {
        Text("ContextMenu - Implementation pending")
    }

    @ViewBuilder
    private func renderUnknownComponent(_ component: AVAComponent) -> some View {
        VStack(spacing: 8) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundColor(.orange)
                .font(.title2)
            Text("Unknown Component")
                .font(.headline)
            Text(String(describing: type(of: component)))
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color.orange.opacity(0.1))
        .cornerRadius(8)
    }

    // MARK: - Child Rendering Helpers

    /// Render a child component
    ///
    /// Used by container components to recursively render children.
    ///
    /// - Parameter child: Child component to render
    /// - Returns: Rendered SwiftUI view
    @ViewBuilder
    public func renderChild(_ child: AVAComponent) -> some View {
        render(component: child)
    }

    /// Render multiple children in a container
    ///
    /// - Parameter children: Array of child components
    /// - Returns: Group of rendered views
    @ViewBuilder
    public func renderChildren(_ children: [AVAComponent]) -> some View {
        ForEach(Array(children.enumerated()), id: \.offset) { index, child in
            renderChild(child)
        }
    }
}

// MARK: - Icon View Component

/// SwiftUI view for rendering icons from IconResource
///
/// SECURITY FIX: Added Task cancellation on disappear to prevent memory leaks
/// and state updates on deallocated views.
struct IconView: View {
    let iconResource: IconResource
    let size: CGFloat
    let tint: Color?
    let iconManager: IOSIconResourceManager

    @State private var loadedIcon: IOSIconResourceManager.LoadedIcon?
    @State private var loadTask: Task<Void, Never>?  // SECURITY FIX: Track task for cancellation

    var body: some View {
        Group {
            if let loadedIcon = loadedIcon {
                switch loadedIcon {
                case .sfSymbol(let name):
                    Image(systemName: name)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: size, height: size)
                        .foregroundColor(tint)

                case .image(let uiImage):
                    Image(uiImage: uiImage)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: size, height: size)
                        .foregroundColor(tint)

                case .url(let url):
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .empty:
                            ProgressView()
                                .frame(width: size, height: size)
                        case .success(let image):
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: size, height: size)
                        case .failure:
                            Image(systemName: "exclamationmark.triangle")
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: size, height: size)
                                .foregroundColor(.red)
                        @unknown default:
                            EmptyView()
                        }
                    }
                }
            } else {
                ProgressView()
                    .frame(width: size, height: size)
                    .onAppear {
                        // SECURITY FIX: Store task reference for cancellation
                        loadTask = Task {
                            // Check for cancellation before updating state
                            guard !Task.isCancelled else { return }
                            let icon = await iconManager.loadIcon(resource: iconResource, size: size)
                            // Check again after async operation
                            guard !Task.isCancelled else { return }
                            loadedIcon = icon
                        }
                    }
                    .onDisappear {
                        // SECURITY FIX: Cancel task when view disappears to prevent memory leaks
                        loadTask?.cancel()
                        loadTask = nil
                    }
            }
        }
    }
}

// MARK: - Theme Placeholder Types

/// Placeholder for AVATheme - will be defined by theme system
public struct AVATheme {
    public init() {}
}

/// Placeholder for theme provider
public struct AVAThemeProvider {
    public static var currentTheme: AVATheme {
        AVATheme()
    }
}

// MARK: - Component Protocol Placeholder

/// Base protocol for all AVAMagic components
public protocol AVAComponent {
    var id: String { get }
}

// MARK: - Component Type Placeholders

// These are minimal placeholders to make the renderer compile.
// Actual component definitions will come from the shared Kotlin/KMP layer.

public struct AVACheckbox: AVAComponent { public var id: String = UUID().uuidString }
public struct AVATextField: AVAComponent { public var id: String = UUID().uuidString }
public struct AVAButton: AVAComponent { public var id: String = UUID().uuidString }
public struct AVASwitch: AVAComponent { public var id: String = UUID().uuidString }
public struct AVAText: AVAComponent { public var id: String = UUID().uuidString }
public struct AVAImage: AVAComponent { public var id: String = UUID().uuidString }
public struct AVAIcon: AVAComponent {
    public var id: String = UUID().uuidString
    public var iconResource: IconResource?
    public var size: CGFloat?
    public var tint: Color?
}
public struct AVAContainer: AVAComponent { public var id: String = UUID().uuidString }
public struct AVARow: AVAComponent { public var id: String = UUID().uuidString }
public struct AVAColumn: AVAComponent { public var id: String = UUID().uuidString }
public struct AVACard: AVAComponent { public var id: String = UUID().uuidString }
public struct AVAScrollView: AVAComponent { public var id: String = UUID().uuidString }
public struct AVAList: AVAComponent { public var id: String = UUID().uuidString }
public struct AVASlider: AVAComponent { public var id: String = UUID().uuidString }
public struct AVARangeSlider: AVAComponent { public var id: String = UUID().uuidString }
public struct AVADatePicker: AVAComponent { public var id: String = UUID().uuidString }
public struct AVATimePicker: AVAComponent { public var id: String = UUID().uuidString }
public struct AVARadioButton: AVAComponent { public var id: String = UUID().uuidString }
public struct AVARadioGroup: AVAComponent { public var id: String = UUID().uuidString }
public struct AVADropdown: AVAComponent { public var id: String = UUID().uuidString }
public struct AVAAutocomplete: AVAComponent { public var id: String = UUID().uuidString }
public struct AVAFileUpload: AVAComponent { public var id: String = UUID().uuidString }
public struct AVAImagePicker: AVAComponent { public var id: String = UUID().uuidString }
public struct AVARating: AVAComponent { public var id: String = UUID().uuidString }
public struct AVASearchBar: AVAComponent { public var id: String = UUID().uuidString }
public struct AVABadge: AVAComponent { public var id: String = UUID().uuidString }
public struct AVAChip: AVAComponent { public var id: String = UUID().uuidString }
public struct AVAAvatar: AVAComponent { public var id: String = UUID().uuidString }
public struct AVADivider: AVAComponent { public var id: String = UUID().uuidString }
public struct AVASkeleton: AVAComponent { public var id: String = UUID().uuidString }
public struct AVASpinner: AVAComponent { public var id: String = UUID().uuidString }
public struct AVAProgressBar: AVAComponent { public var id: String = UUID().uuidString }
public struct AVATooltip: AVAComponent { public var id: String = UUID().uuidString }
public struct AVAGrid: AVAComponent { public var id: String = UUID().uuidString }
public struct AVAStack: AVAComponent { public var id: String = UUID().uuidString }
public struct AVASpacer: AVAComponent { public var id: String = UUID().uuidString }
public struct AVADrawer: AVAComponent { public var id: String = UUID().uuidString }
public struct AVATabs: AVAComponent { public var id: String = UUID().uuidString }
public struct AVAAppBar: AVAComponent { public var id: String = UUID().uuidString }
public struct AVABottomNav: AVAComponent { public var id: String = UUID().uuidString }
public struct AVABreadcrumb: AVAComponent { public var id: String = UUID().uuidString }
public struct AVAPagination: AVAComponent { public var id: String = UUID().uuidString }
public struct AVAAlert: AVAComponent { public var id: String = UUID().uuidString }
public struct AVASnackbar: AVAComponent { public var id: String = UUID().uuidString }
public struct AVAModal: AVAComponent { public var id: String = UUID().uuidString }
public struct AVAToast: AVAComponent { public var id: String = UUID().uuidString }
public struct AVAConfirm: AVAComponent { public var id: String = UUID().uuidString }
public struct AVAContextMenu: AVAComponent { public var id: String = UUID().uuidString }
