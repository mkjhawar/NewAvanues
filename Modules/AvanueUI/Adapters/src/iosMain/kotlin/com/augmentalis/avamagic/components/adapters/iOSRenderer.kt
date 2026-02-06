package com.augmentalis.avamagic.components.adapters

import com.augmentalis.avamagic.components.basic.*
import com.augmentalis.avamagic.ui.core.data.*
import com.augmentalis.avamagic.ui.core.display.*
import com.augmentalis.avamagic.ui.core.feedback.*
import com.augmentalis.avamagic.ui.core.form.*
import com.augmentalis.avamagic.ui.core.layout.*
import com.augmentalis.avamagic.ui.core.navigation.*
import com.augmentalis.avamagic.core.*

/**
 * iOS Native Renderer
 *
 * Renders IDEAMagic Core components as native iOS SwiftUI views.
 * Follows the world-class architecture pattern:
 * Core Component → iOSRenderer → SwiftUI → Native iOS UI
 *
 * Pattern inspired by:
 * - React Native (JS → Native bridge)
 * - Flutter (Dart → Native widgets)
 * - .NET MAUI (C# → Native controls)
 *
 * Features:
 * - 100% native iOS rendering (SwiftUI)
 * - iOS Human Interface Guidelines compliant
 * - SF Symbols for icons
 * - Native animations and transitions
 * - Dark mode support
 * - Accessibility (VoiceOver, Dynamic Type)
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class iOSRenderer : Renderer {
    override val platform: Platform = Platform.iOS

    override fun render(component: Component): Any {
        return when (component) {
            // Basic components
            is ButtonComponent -> renderButton(component)
            is TextComponent -> renderText(component)
            is TextFieldComponent -> renderTextField(component)
            is IconComponent -> renderIcon(component)
            is ImageComponent -> renderImage(component)

            // Container components
            is CardComponent -> renderCard(component)
            is ChipComponent -> renderChip(component)
            is DividerComponent -> renderDivider(component)
            is BadgeComponent -> renderBadge(component)

            // Layout components
            is ColumnComponent -> renderColumn(component)
            is RowComponent -> renderRow(component)
            is ContainerComponent -> renderContainer(component)
            is ScrollViewComponent -> renderScrollView(component)

            // List components
            is ListComponent -> renderList(component)

            // Form components
            is AutocompleteComponent -> renderAutocomplete(component)
            is DateRangePickerComponent -> renderDateRangePicker(component)
            is MultiSelectComponent -> renderMultiSelect(component)
            is RangeSliderComponent -> renderRangeSlider(component)
            is TagInputComponent -> renderTagInput(component)
            is ToggleButtonGroupComponent -> renderToggleButtonGroup(component)
            is ColorPickerComponent -> renderColorPicker(component)
            is IconPickerComponent -> renderIconPicker(component)
            is CheckboxComponent -> renderCheckbox(component)
            is SwitchComponent -> renderSwitch(component)
            is SliderComponent -> renderSlider(component)
            is RadioComponent -> renderRadio(component)
            is DropdownComponent -> renderDropdown(component)
            is DatePickerComponent -> renderDatePicker(component)
            is TimePickerComponent -> renderTimePicker(component)
            is FileUploadComponent -> renderFileUpload(component)
            is SearchBarComponent -> renderSearchBar(component)
            is RatingComponent -> renderRating(component)

            // Feedback components
            is BannerComponent -> renderBanner(component)
            is SnackbarComponent -> renderSnackbar(component)
            is DialogComponent -> renderDialog(component)
            is ToastComponent -> renderToast(component)
            is NotificationCenterComponent -> renderNotificationCenter(component)
            is AlertComponent -> renderAlert(component)
            is ProgressBarComponent -> renderProgressBar(component)
            is SpinnerComponent -> renderSpinner(component)
            is TooltipComponent -> renderTooltip(component)

            // Data display components
            is AccordionComponent -> renderAccordion(component)
            is AvatarComponent -> renderAvatar(component)
            is CarouselComponent -> renderCarousel(component)
            is StatCardComponent -> renderStatCard(component)
            is DataTableComponent -> renderDataTable(component)
            is DataGridComponent -> renderDataGrid(component)
            is EmptyStateComponent -> renderEmptyState(component)
            is PaperComponent -> renderPaper(component)
            is SkeletonComponent -> renderSkeleton(component)
            is StepperComponent -> renderStepper(component)
            is TableComponent -> renderTable(component)
            is TimelineComponent -> renderTimeline(component)
            is TreeViewComponent -> renderTreeView(component)

            // Navigation components
            is AppBarComponent -> renderAppBar(component)
            is FABComponent -> renderFAB(component)
            is MasonryGridComponent -> renderMasonryGrid(component)
            is StickyHeaderComponent -> renderStickyHeader(component)
            is BottomNavComponent -> renderBottomNav(component)
            is BreadcrumbComponent -> renderBreadcrumb(component)
            is DrawerComponent -> renderDrawer(component)
            is PaginationComponent -> renderPagination(component)
            is TabsComponent -> renderTabs(component)

            else -> throw IllegalArgumentException("Unsupported component type: ${component::class.simpleName}")
        }
    }

    // ===========================
    // BASIC COMPONENTS
    // ===========================

    private fun renderButton(button: ButtonComponent): Any {
        // Map to MagicButtonView.swift
        return mapOf(
            "_type" to "MagicButtonView",
            "text" to button.text,
            "style" to mapButtonStyle(button.variant),
            "enabled" to button.enabled,
            "icon" to button.icon
        )
    }

    private fun renderText(text: TextComponent): Any {
        // Map to MagicTextView.swift
        return mapOf(
            "_type" to "MagicTextView",
            "content" to text.content,
            "variant" to text.variant.name,
            "color" to text.color,
            "align" to text.align.name,
            "bold" to text.bold,
            "italic" to text.italic
        )
    }

    private fun renderTextField(textField: TextFieldComponent): Any {
        // Map to MagicTextFieldView.swift
        return mapOf(
            "_type" to "MagicTextFieldView",
            "value" to textField.value,
            "label" to textField.label,
            "placeholder" to textField.placeholder,
            "enabled" to textField.enabled,
            "error" to textField.error
        )
    }

    private fun renderIcon(icon: IconComponent): Any {
        // Map to MagicIconView.swift (uses SF Symbols)
        return mapOf(
            "_type" to "MagicIconView",
            "name" to mapToSFSymbol(icon.name),
            "size" to icon.size.name,
            "color" to icon.color
        )
    }

    private fun renderImage(image: ImageComponent): Any {
        // Map to MagicImageView.swift
        return mapOf(
            "_type" to "MagicImageView",
            "source" to image.source,
            "alt" to image.alt,
            "fit" to image.fit.name,
            "width" to image.width,
            "height" to image.height
        )
    }

    // ===========================
    // CONTAINER COMPONENTS
    // ===========================

    private fun renderCard(card: CardComponent): Any {
        return createComponentData(
            "MagicCardView",
            "elevated" to card.elevated,
            "variant" to card.variant.name,
            "children" to mapChildren(card.children)
        )
    }

    private fun renderChip(chip: ChipComponent): Any {
        return createComponentData(
            "MagicChipView",
            "label" to chip.label,
            "variant" to chip.variant.name.lowercase(),
            "color" to chip.color.name.lowercase(),
            "size" to chip.size.name.lowercase(),
            "leadingIcon" to chip.leadingIcon,
            "trailingIcon" to chip.trailingIcon,
            "showDelete" to chip.showDelete,
            "selected" to chip.selected,
            "disabled" to chip.disabled,
            "clickable" to chip.clickable
        )
    }

    private fun renderDivider(divider: DividerComponent): Any {
        return createComponentData(
            "MagicDividerView",
            "orientation" to divider.orientation.name,
            "thickness" to divider.thickness,
            "color" to divider.color
        )
    }

    private fun renderBadge(badge: BadgeComponent): Any {
        return createComponentData(
            "MagicBadgeView",
            "content" to badge.content,
            "variant" to badge.variant.name.lowercase(),
            "color" to badge.color.name.lowercase(),
            "size" to badge.size.name.lowercase(),
            "maxCount" to badge.maxCount,
            "showZero" to badge.showZero,
            "pulse" to badge.pulse,
            "invisible" to badge.invisible
        )
    }

    // ===========================
    // LAYOUT COMPONENTS
    // ===========================

    private fun renderColumn(column: ColumnComponent): Any {
        return createComponentData(
            "MagicColumnView",
            "spacing" to mapSize(column.spacing),
            "alignment" to mapAlignment(column.alignment),
            "children" to mapChildren(column.children)
        )
    }

    private fun renderRow(row: RowComponent): Any {
        return createComponentData(
            "MagicRowView",
            "spacing" to mapSize(row.spacing),
            "alignment" to mapAlignment(row.alignment),
            "children" to mapChildren(row.children)
        )
    }

    private fun renderContainer(container: ContainerComponent): Any {
        return createComponentData(
            "MagicContainerView",
            "padding" to mapSize(container.padding),
            "children" to mapChildren(container.children)
        )
    }

    private fun renderScrollView(scrollView: ScrollViewComponent): Any {
        return createComponentData(
            "MagicScrollViewView",
            "direction" to scrollView.direction.name,
            "children" to mapChildren(scrollView.children)
        )
    }

    // ===========================
    // LIST COMPONENTS
    // ===========================

    private fun renderList(list: ListComponent): Any {
        return createComponentData(
            "MagicListView",
            "items" to mapChildren(list.items),
            "dividers" to list.showDividers
        )
    }

    // ===========================
    // FORM COMPONENTS
    // ===========================

    private fun renderCheckbox(checkbox: CheckboxComponent): Any {
        return createComponentData(
            "MagicCheckboxView",
            "checked" to checkbox.checked,
            "label" to checkbox.label,
            "enabled" to checkbox.enabled
        )
    }

    private fun renderSwitch(switch: SwitchComponent): Any {
        return createComponentData(
            "MagicSwitchView",
            "checked" to switch.checked,
            "label" to switch.label,
            "enabled" to switch.enabled
        )
    }

    private fun renderSlider(slider: SliderComponent): Any {
        return createComponentData(
            "MagicSliderView",
            "value" to slider.value,
            "min" to slider.min,
            "max" to slider.max,
            "step" to slider.step,
            "label" to slider.label
        )
    }

    private fun renderRadio(radio: RadioComponent): Any {
        return createComponentData(
            "MagicRadioView",
            "options" to radio.options,
            "selectedValue" to radio.selectedValue,
            "label" to radio.label
        )
    }

    private fun renderDropdown(dropdown: DropdownComponent): Any {
        return createComponentData(
            "MagicDropdownView",
            "options" to dropdown.options,
            "selectedValue" to dropdown.selectedValue,
            "label" to dropdown.label,
            "placeholder" to dropdown.placeholder
        )
    }

    private fun renderDatePicker(datePicker: DatePickerComponent): Any {
        return createComponentData(
            "MagicDatePickerView",
            "selectedDate" to datePicker.selectedDate,
            "label" to datePicker.label,
            "minDate" to datePicker.minDate,
            "maxDate" to datePicker.maxDate
        )
    }

    private fun renderTimePicker(timePicker: TimePickerComponent): Any {
        return createComponentData(
            "MagicTimePickerView",
            "selectedTime" to timePicker.selectedTime,
            "label" to timePicker.label,
            "format24Hour" to timePicker.format24Hour
        )
    }

    private fun renderFileUpload(fileUpload: FileUploadComponent): Any {
        return createComponentData(
            "MagicFileUploadView",
            "label" to fileUpload.label,
            "accept" to fileUpload.accept,
            "multiple" to fileUpload.multiple
        )
    }

    private fun renderSearchBar(searchBar: SearchBarComponent): Any {
        return createComponentData(
            "MagicSearchBarView",
            "value" to searchBar.value,
            "placeholder" to searchBar.placeholder,
            "showCancelButton" to searchBar.showCancelButton
        )
    }

    private fun renderRating(rating: RatingComponent): Any {
        return createComponentData(
            "MagicRatingView",
            "value" to rating.value,
            "maxRating" to rating.maxRating,
            "allowHalf" to rating.allowHalf,
            "readonly" to rating.readonly
        )
    }

    private fun renderAutocomplete(autocomplete: AutocompleteComponent): Any {
        return createComponentData(
            "MagicAutocompleteView",
            "value" to autocomplete.value,
            "suggestions" to autocomplete.suggestions,
            "placeholder" to autocomplete.placeholder,
            "label" to autocomplete.label,
            "leadingIcon" to autocomplete.leadingIcon?.let { mapToSFSymbol(it) },
            "trailingIcon" to autocomplete.trailingIcon?.let { mapToSFSymbol(it) },
            "minCharsForSuggestions" to autocomplete.minCharsForSuggestions,
            "maxSuggestions" to autocomplete.maxSuggestions,
            "filterStrategy" to autocomplete.filterStrategy.name,
            "fuzzyThreshold" to autocomplete.fuzzyThreshold,
            "isLoading" to autocomplete.isLoading,
            "emptyStateMessage" to autocomplete.emptyStateMessage,
            "highlightMatch" to autocomplete.highlightMatch,
            "enabled" to autocomplete.enabled,
            "readOnly" to autocomplete.readOnly
        )
    }

    private fun renderDateRangePicker(dateRangePicker: DateRangePickerComponent): Any {
        return createComponentData(
            "MagicDateRangePickerView",
            "startDate" to dateRangePicker.startDate,
            "endDate" to dateRangePicker.endDate,
            "label" to dateRangePicker.label,
            "placeholder" to dateRangePicker.placeholder,
            "minDate" to dateRangePicker.minDate,
            "maxDate" to dateRangePicker.maxDate,
            "presets" to dateRangePicker.presets.map { preset ->
                mapOf(
                    "label" to preset.label,
                    "daysFromToday" to preset.daysFromToday,
                    "icon" to preset.icon
                )
            },
            "dateFormat" to dateRangePicker.dateFormat,
            "displayFormat" to dateRangePicker.displayFormat,
            "singleDateMode" to dateRangePicker.singleDateMode,
            "showClearButton" to dateRangePicker.showClearButton,
            "required" to dateRangePicker.required,
            "enabled" to dateRangePicker.enabled,
            "readOnly" to dateRangePicker.readOnly
        )
    }

    private fun renderMultiSelect(multiSelect: MultiSelectComponent): Any {
        return createComponentData(
            "MagicMultiSelectView",
            "selectedValues" to multiSelect.selectedValues,
            "options" to multiSelect.options.map { option ->
                mapOf(
                    "value" to option.value,
                    "label" to option.label,
                    "group" to option.group,
                    "icon" to option.icon?.let { mapToSFSymbol(it) },
                    "description" to option.description,
                    "disabled" to option.disabled
                )
            },
            "label" to multiSelect.label,
            "placeholder" to multiSelect.placeholder,
            "displayMode" to multiSelect.displayMode.name,
            "searchable" to multiSelect.searchable,
            "searchPlaceholder" to multiSelect.searchPlaceholder,
            "showSelectAll" to multiSelect.showSelectAll,
            "maxSelections" to multiSelect.maxSelections,
            "showSelectedChips" to multiSelect.showSelectedChips,
            "enabled" to multiSelect.enabled,
            "readOnly" to multiSelect.readOnly
        )
    }

    private fun renderRangeSlider(rangeSlider: RangeSliderComponent): Any {
        return createComponentData(
            "MagicRangeSliderView",
            "startValue" to rangeSlider.startValue,
            "endValue" to rangeSlider.endValue,
            "min" to rangeSlider.min,
            "max" to rangeSlider.max,
            "step" to rangeSlider.step,
            "label" to rangeSlider.label,
            "showValues" to rangeSlider.showValues.name,
            "valuePrefix" to rangeSlider.valuePrefix,
            "valueSuffix" to rangeSlider.valueSuffix,
            "minGap" to rangeSlider.minGap,
            "enabled" to rangeSlider.enabled,
            "readOnly" to rangeSlider.readOnly
        )
    }

    private fun renderTagInput(tagInput: TagInputComponent): Any {
        return createComponentData(
            "MagicTagInputView",
            "tags" to tagInput.tags,
            "suggestions" to tagInput.suggestions,
            "label" to tagInput.label,
            "placeholder" to tagInput.placeholder,
            "maxTags" to tagInput.maxTags,
            "allowDuplicates" to tagInput.allowDuplicates,
            "caseSensitive" to tagInput.caseSensitive,
            "separators" to tagInput.separators,
            "minTagLength" to tagInput.minTagLength,
            "maxTagLength" to tagInput.maxTagLength,
            "showSuggestions" to tagInput.showSuggestions,
            "enabled" to tagInput.enabled,
            "readOnly" to tagInput.readOnly
        )
    }

    private fun renderToggleButtonGroup(group: ToggleButtonGroupComponent): Any {
        return createComponentData(
            "MagicToggleButtonGroupView",
            "selectedValues" to group.selectedValues,
            "buttons" to group.buttons.map { button ->
                mapOf(
                    "value" to button.value,
                    "label" to button.label,
                    "icon" to button.icon,
                    "disabled" to button.disabled
                )
            },
            "selectionMode" to group.selectionMode.name.lowercase(),
            "orientation" to group.orientation.name.lowercase(),
            "label" to group.label,
            "variant" to group.variant.name.lowercase(),
            "size" to group.size.name.lowercase(),
            "fullWidth" to group.fullWidth,
            "required" to group.required,
            "enabled" to group.enabled
        )
    }

    private fun renderColorPicker(colorPicker: ColorPickerComponent): Any {
        return createComponentData(
            "MagicColorPickerView",
            "value" to colorPicker.value,
            "label" to colorPicker.label,
            "mode" to colorPicker.mode.name.lowercase(),
            "showAlpha" to colorPicker.showAlpha,
            "showHexInput" to colorPicker.showHexInput,
            "showPresets" to colorPicker.showPresets,
            "showRecent" to colorPicker.showRecent,
            "presetColors" to colorPicker.presetColors,
            "recentColors" to colorPicker.recentColors,
            "placeholder" to colorPicker.placeholder,
            "helperText" to colorPicker.helperText,
            "errorText" to colorPicker.errorText,
            "enabled" to colorPicker.enabled,
            "readOnly" to colorPicker.readOnly
        )
    }

    private fun renderIconPicker(iconPicker: IconPickerComponent): Any {
        return createComponentData(
            "MagicIconPickerView",
            "value" to iconPicker.value,
            "label" to iconPicker.label,
            "library" to iconPicker.library.name.lowercase(),
            "icons" to iconPicker.icons.map { icon ->
                mapOf(
                    "name" to icon.name,
                    "label" to icon.label,
                    "category" to icon.category,
                    "tags" to icon.tags,
                    "codepoint" to icon.codepoint
                )
            },
            "categories" to iconPicker.categories,
            "showSearch" to iconPicker.showSearch,
            "showCategories" to iconPicker.showCategories,
            "showRecent" to iconPicker.showRecent,
            "recentIcons" to iconPicker.recentIcons,
            "gridColumns" to iconPicker.gridColumns,
            "iconSize" to iconPicker.iconSize.name.lowercase(),
            "placeholder" to iconPicker.placeholder,
            "helperText" to iconPicker.helperText,
            "errorText" to iconPicker.errorText,
            "enabled" to iconPicker.enabled,
            "readOnly" to iconPicker.readOnly
        )
    }

    // ===========================
    // FEEDBACK COMPONENTS
    // ===========================

    private fun renderBanner(banner: BannerComponent): Any {
        return createComponentData(
            "MagicBannerView",
            "message" to banner.message,
            "severity" to banner.severity.name.lowercase(),
            "icon" to banner.icon,
            "primaryAction" to banner.primaryAction?.let {
                mapOf("label" to it.label)
            },
            "secondaryAction" to banner.secondaryAction?.let {
                mapOf("label" to it.label)
            },
            "dismissible" to banner.dismissible,
            "sticky" to banner.sticky,
            "autoDismiss" to banner.autoDismiss,
            "visible" to banner.visible
        )
    }

    private fun renderSnackbar(snackbar: SnackbarComponent): Any {
        return createComponentData(
            "MagicSnackbarView",
            "message" to snackbar.message,
            "actionLabel" to snackbar.actionLabel,
            "duration" to snackbar.duration.name.lowercase(),
            "position" to snackbar.position.name.lowercase(),
            "severity" to snackbar.severity.name.lowercase(),
            "visible" to snackbar.visible
        )
    }

    private fun renderDialog(dialog: DialogComponent): Any {
        return createComponentData(
            "MagicDialogView",
            "title" to dialog.title,
            "message" to dialog.message,
            "showDialog" to dialog.isVisible,
            "actions" to dialog.actions.map { it.label }
        )
    }

    private fun renderToast(toast: ToastComponent): Any {
        return createComponentData(
            "MagicToastView",
            "message" to toast.message,
            "duration" to toast.duration,
            "severity" to toast.severity.name.lowercase(),
            "position" to toast.position.name.lowercase(),
            "actionLabel" to toast.action?.label
        )
    }

    private fun renderNotificationCenter(notificationCenter: NotificationCenterComponent): Any {
        return createComponentData(
            "MagicNotificationCenterView",
            "notifications" to notificationCenter.notifications.map { notification ->
                mapOf(
                    "id" to notification.id,
                    "title" to notification.title,
                    "message" to notification.message,
                    "severity" to notification.severity.name.lowercase(),
                    "timestamp" to notification.timestamp,
                    "read" to notification.read,
                    "icon" to notification.icon,
                    "actionLabel" to notification.actionLabel,
                    "priority" to notification.priority.name.lowercase(),
                    "category" to notification.category
                )
            },
            "maxVisible" to notificationCenter.maxVisible,
            "showBadge" to notificationCenter.showBadge,
            "groupByType" to notificationCenter.groupByType
        )
    }

    private fun renderAlert(alert: AlertComponent): Any {
        return createComponentData(
            "MagicAlertView",
            "title" to alert.title,
            "message" to alert.message,
            "type" to alert.type.name,
            "dismissible" to alert.dismissible
        )
    }

    private fun renderProgressBar(progressBar: ProgressBarComponent): Any {
        return createComponentData(
            "MagicProgressBarView",
            "value" to progressBar.value,
            "max" to progressBar.max,
            "indeterminate" to progressBar.indeterminate,
            "label" to progressBar.label
        )
    }

    private fun renderSpinner(spinner: SpinnerComponent): Any {
        return createComponentData(
            "MagicSpinnerView",
            "size" to spinner.size.name,
            "color" to spinner.color
        )
    }

    private fun renderTooltip(tooltip: TooltipComponent): Any {
        return createComponentData(
            "MagicTooltipView",
            "content" to tooltip.content,
            "targetContent" to render(tooltip.targetContent),
            "title" to tooltip.title,
            "placement" to tooltip.placement.name.lowercase(),
            "trigger" to tooltip.trigger.name.lowercase(),
            "showArrow" to tooltip.showArrow,
            "delay" to tooltip.delay,
            "maxWidth" to tooltip.maxWidth,
            "variant" to tooltip.variant.name.lowercase(),
            "visible" to tooltip.visible
        )
    }

    // ===========================
    // DATA DISPLAY COMPONENTS
    // ===========================

    private fun renderAccordion(accordion: AccordionComponent): Any {
        return createComponentData(
            "MagicAccordionView",
            "title" to accordion.title,
            "expanded" to accordion.expanded,
            "children" to mapChildren(accordion.children)
        )
    }

    private fun renderAvatar(avatar: AvatarComponent): Any {
        return createComponentData(
            "MagicAvatarView",
            "imageUrl" to avatar.imageUrl,
            "text" to avatar.text,
            "icon" to avatar.icon,
            "alt" to avatar.alt,
            "size" to avatar.size.name.lowercase(),
            "shape" to avatar.shape.name.lowercase(),
            "backgroundColor" to avatar.backgroundColor,
            "textColor" to avatar.textColor,
            "statusIndicator" to avatar.statusIndicator?.name?.lowercase(),
            "badgeContent" to avatar.badgeContent,
            "clickable" to avatar.clickable
        )
    }

    private fun renderStatCard(card: StatCardComponent): Any {
        return createComponentData(
            "MagicStatCardView",
            "label" to card.label,
            "value" to card.value,
            "icon" to card.icon,
            "trend" to card.trend?.name?.lowercase(),
            "changePercent" to card.changePercent,
            "changeLabel" to card.changeLabel,
            "previousValue" to card.previousValue,
            "color" to card.color.name.lowercase(),
            "variant" to card.variant.name.lowercase(),
            "loading" to card.loading,
            "clickable" to card.clickable
        )
    }

    private fun renderDataTable(table: DataTableComponent): Any {
        return createComponentData(
            "MagicDataTableView",
            "columns" to table.columns.map { col ->
                mapOf(
                    "id" to col.id,
                    "label" to col.label,
                    "sortable" to col.sortable,
                    "filterable" to col.filterable,
                    "width" to col.width,
                    "minWidth" to col.minWidth,
                    "maxWidth" to col.maxWidth,
                    "align" to col.align.name.lowercase(),
                    "type" to col.type.name.lowercase(),
                    "visible" to col.visible,
                    "resizable" to col.resizable
                )
            },
            "rows" to table.rows,
            "sortable" to table.sortable,
            "filterable" to table.filterable,
            "pagination" to table.pagination,
            "rowsPerPage" to table.rowsPerPage,
            "currentPage" to table.currentPage,
            "totalRows" to table.totalRows,
            "selectable" to table.selectable,
            "selectionMode" to table.selectionMode.name.lowercase(),
            "selectedRows" to table.selectedRows,
            "stickyHeader" to table.stickyHeader,
            "dense" to table.dense,
            "striped" to table.striped,
            "hoverable" to table.hoverable,
            "loading" to table.loading,
            "emptyMessage" to table.emptyMessage
        )
    }

    private fun renderCarousel(carousel: CarouselComponent): Any {
        return createComponentData(
            "MagicCarouselView",
            "items" to mapChildren(carousel.items),
            "autoPlay" to carousel.autoPlay,
            "interval" to carousel.interval
        )
    }

    private fun renderDataGrid(dataGrid: DataGridComponent): Any {
        return createComponentData(
            "MagicDataGridView",
            "columns" to dataGrid.columns,
            "rows" to dataGrid.rows,
            "sortable" to dataGrid.sortable
        )
    }

    private fun renderEmptyState(emptyState: EmptyStateComponent): Any {
        return createComponentData(
            "MagicEmptyStateView",
            "title" to emptyState.title,
            "message" to emptyState.message,
            "icon" to emptyState.icon?.let { mapToSFSymbol(it) }
        )
    }

    private fun renderPaper(paper: PaperComponent): Any {
        return createComponentData(
            "MagicPaperView",
            "elevation" to paper.elevation,
            "children" to mapChildren(paper.children)
        )
    }

    private fun renderSkeleton(skeleton: SkeletonComponent): Any {
        return createComponentData(
            "MagicSkeletonView",
            "variant" to skeleton.variant.name,
            "width" to skeleton.width,
            "height" to skeleton.height,
            "animated" to skeleton.animated
        )
    }

    private fun renderStepper(stepper: StepperComponent): Any {
        return createComponentData(
            "MagicStepperView",
            "steps" to stepper.steps,
            "currentStep" to stepper.currentStep,
            "orientation" to stepper.orientation.name
        )
    }

    private fun renderTable(table: TableComponent): Any {
        return createComponentData(
            "MagicTableView",
            "headers" to table.headers,
            "rows" to table.rows,
            "sortable" to table.sortable
        )
    }

    private fun renderTimeline(timeline: TimelineComponent): Any {
        return createComponentData(
            "MagicTimelineView",
            "events" to timeline.events.map { event ->
                mapOf(
                    "timestamp" to event.timestamp,
                    "title" to event.title,
                    "description" to event.description,
                    "icon" to event.icon,
                    "status" to event.status.name.lowercase(),
                    "color" to event.color?.name?.lowercase(),
                    "clickable" to event.clickable,
                    "metadata" to event.metadata
                )
            },
            "variant" to timeline.variant.name.lowercase(),
            "showConnector" to timeline.showConnector,
            "color" to timeline.color.name.lowercase(),
            "dense" to timeline.dense
        )
    }

    private fun renderTreeView(treeView: TreeViewComponent): Any {
        fun nodeToMap(node: TreeNode): Map<String, Any?> {
            return mapOf(
                "id" to node.id,
                "label" to node.label,
                "children" to node.children.map { nodeToMap(it) },
                "icon" to node.icon,
                "disabled" to node.disabled,
                "metadata" to node.metadata
            )
        }

        return createComponentData(
            "MagicTreeViewView",
            "nodes" to treeView.nodes.map { nodeToMap(it) },
            "expandedNodes" to treeView.expandedNodes.toList(),
            "selectedNodes" to treeView.selectedNodes.toList(),
            "showCheckboxes" to treeView.showCheckboxes,
            "showIcons" to treeView.showIcons,
            "showLines" to treeView.showLines,
            "selectable" to treeView.selectable,
            "multiSelect" to treeView.multiSelect,
            "expandOnClick" to treeView.expandOnClick,
            "defaultExpanded" to treeView.defaultExpanded,
            "dense" to treeView.dense
        )
    }

    // ===========================
    // NAVIGATION COMPONENTS
    // ===========================

    private fun renderAppBar(appBar: AppBarComponent): Any {
        return createComponentData(
            "MagicAppBarView",
            "title" to appBar.title,
            "leadingIcon" to appBar.leadingIcon?.let { mapToSFSymbol(it) },
            "trailingIcon" to appBar.trailingIcon?.let { mapToSFSymbol(it) },
            "elevated" to appBar.elevated
        )
    }

    private fun renderFAB(fab: FABComponent): Any {
        return createComponentData(
            "MagicFABView",
            "icon" to mapToSFSymbol(fab.icon),
            "label" to fab.label,
            "extended" to fab.extended,
            "size" to fab.size.name.lowercase(),
            "variant" to fab.variant.name.lowercase()
        )
    }

    private fun renderMasonryGrid(masonryGrid: MasonryGridComponent): Any {
        return createComponentData(
            "MagicMasonryGridView",
            "items" to masonryGrid.items.map { item ->
                mapOf(
                    "id" to item.id,
                    "aspectRatio" to item.aspectRatio,
                    "height" to item.height,
                    "content" to item.content.render(this)
                )
            },
            "columns" to masonryGrid.columns,
            "spacing" to masonryGrid.spacing,
            "horizontalArrangement" to masonryGrid.horizontalArrangement.name.lowercase()
        )
    }

    private fun renderStickyHeader(stickyHeader: StickyHeaderComponent): Any {
        return createComponentData(
            "MagicStickyHeaderView",
            "content" to stickyHeader.content.render(this),
            "elevation" to stickyHeader.elevation,
            "backgroundColor" to stickyHeader.backgroundColor,
            "showShadowOnScroll" to stickyHeader.showShadowOnScroll,
            "height" to stickyHeader.height
        )
    }

    private fun renderBottomNav(bottomNav: BottomNavComponent): Any {
        return createComponentData(
            "MagicBottomNavView",
            "items" to bottomNav.items.map { item ->
                mapOf(
                    "label" to item.label,
                    "icon" to mapToSFSymbol(item.icon),
                    "selected" to item.selected
                )
            },
            "selectedIndex" to bottomNav.selectedIndex
        )
    }

    private fun renderBreadcrumb(breadcrumb: BreadcrumbComponent): Any {
        return createComponentData(
            "MagicBreadcrumbView",
            "items" to breadcrumb.items,
            "separator" to breadcrumb.separator
        )
    }

    private fun renderDrawer(drawer: DrawerComponent): Any {
        return createComponentData(
            "MagicDrawerView",
            "isOpen" to drawer.isOpen,
            "position" to drawer.position.name,
            "children" to mapChildren(drawer.children)
        )
    }

    private fun renderPagination(pagination: PaginationComponent): Any {
        return createComponentData(
            "MagicPaginationView",
            "currentPage" to pagination.currentPage,
            "totalPages" to pagination.totalPages,
            "showFirstLast" to pagination.showFirstLast
        )
    }

    private fun renderTabs(tabs: TabsComponent): Any {
        return createComponentData(
            "MagicTabsView",
            "tabs" to tabs.tabs.map { tab ->
                mapOf(
                    "label" to tab.label,
                    "icon" to tab.icon?.let { mapToSFSymbol(it) }
                )
            },
            "selectedIndex" to tabs.selectedIndex,
            "variant" to tabs.variant.name
        )
    }
}
