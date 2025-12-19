package com.augmentalis.avaelements.yaml

import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.dsl.*
import kotlinx.serialization.json.*

/**
 * YAML to DSL Parser
 *
 * Converts YAML UI definitions to AvaElements DSL component tree.
 *
 * Example YAML:
 * ```yaml
 * theme: iOS26LiquidGlass
 * components:
 *   - Column:
 *       padding: 16
 *       children:
 *         - Text:
 *             text: "Hello World"
 *             font: Title
 *             color: "#007AFF"
 *         - Button:
 *             text: "Click Me"
 *             style: Primary
 *             onClick: handleClick
 * ```
 */
class YamlParser {
    /**
     * Parse YAML string to AvaUI component tree
     */
    fun parse(yaml: String): AvaUI {
        // For now, we'll use JSON as intermediate format
        // In production, use a proper YAML library like kotlinx-serialization-yaml
        val json = yamlToJson(yaml)
        return parseJson(json)
    }

    /**
     * Parse JSON representation to AvaUI
     */
    private fun parseJson(jsonString: String): AvaUI {
        val json = Json.parseToJsonElement(jsonString).jsonObject

        return AvaUI {
            // Parse theme
            val themeName = json["theme"]?.jsonPrimitive?.content
            theme = when (themeName) {
                "iOS26LiquidGlass", "iOS26" -> Themes.iOS26LiquidGlass
                "macOS26Tahoe", "macOS26" -> Themes.iOS26LiquidGlass  // TODO: Create separate macOS theme
                "Windows11Fluent2", "Windows11" -> Themes.Windows11Fluent2
                "visionOS2", "visionOS" -> Themes.visionOS2SpatialGlass
                "Material3", "Material" -> Themes.Material3Light
                else -> Themes.Material3Light
            }

            // Parse root component
            val components = json["components"]?.jsonArray
            if (components != null && components.isNotEmpty()) {
                parseComponent(this, components[0].jsonObject)
            }
        }
    }

    /**
     * Parse individual component from JSON
     */
    private fun parseComponent(scope: AvaUIScope, componentJson: JsonObject) {
        val componentType = componentJson.keys.first()
        val componentData = componentJson[componentType]?.jsonObject ?: return

        when (componentType) {
            "Column" -> parseColumn(scope, componentData)
            "Row" -> parseRow(scope, componentData)
            "Container" -> parseContainer(scope, componentData)
            "ScrollView" -> parseScrollView(scope, componentData)
            "Card" -> parseCard(scope, componentData)
            "Text" -> parseText(scope, componentData)
            "Button" -> parseButton(scope, componentData)
            "Image" -> parseImage(scope, componentData)
            "Checkbox" -> parseCheckbox(scope, componentData)
            "TextField" -> parseTextField(scope, componentData)
            "Switch" -> parseSwitch(scope, componentData)
            "Icon" -> parseIcon(scope, componentData)
            // Form components
            "Radio" -> parseRadio(scope, componentData)
            "Slider" -> parseSlider(scope, componentData)
            "Dropdown" -> parseDropdown(scope, componentData)
            "DatePicker" -> parseDatePicker(scope, componentData)
            "TimePicker" -> parseTimePicker(scope, componentData)
            "FileUpload" -> parseFileUpload(scope, componentData)
            "SearchBar" -> parseSearchBar(scope, componentData)
            "Rating" -> parseRating(scope, componentData)
            // Feedback components
            "Dialog" -> parseDialog(scope, componentData)
            "Toast" -> parseToast(scope, componentData)
            "Alert" -> parseAlert(scope, componentData)
            "ProgressBar" -> parseProgressBar(scope, componentData)
            "Spinner" -> parseSpinner(scope, componentData)
            "Badge" -> parseBadge(scope, componentData)
            "Tooltip" -> parseTooltip(scope, componentData)
            // Navigation components
            "AppBar" -> parseAppBar(scope, componentData)
            "BottomNav" -> parseBottomNav(scope, componentData)
            "Tabs" -> parseTabs(scope, componentData)
            "Drawer" -> parseDrawer(scope, componentData)
            "Breadcrumb" -> parseBreadcrumb(scope, componentData)
            "Pagination" -> parsePagination(scope, componentData)
            // Data display components
            "Table" -> parseTable(scope, componentData)
            "List" -> parseList(scope, componentData)
            "Accordion" -> parseAccordion(scope, componentData)
            "Stepper" -> parseStepper(scope, componentData)
            "Timeline" -> parseTimeline(scope, componentData)
            "TreeView" -> parseTreeView(scope, componentData)
            "Carousel" -> parseCarousel(scope, componentData)
            "Avatar" -> parseAvatar(scope, componentData)
            "Chip" -> parseChip(scope, componentData)
            "Divider" -> parseDivider(scope, componentData)
            "Paper" -> parsePaper(scope, componentData)
            "Skeleton" -> parseSkeleton(scope, componentData)
            "EmptyState" -> parseEmptyState(scope, componentData)
            "DataGrid" -> parseDataGrid(scope, componentData)
        }
    }

    // ==================== Layout Components ====================

    private fun parseColumn(scope: AvaUIScope, data: JsonObject) {
        scope.Column(
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            // Parse properties
            data["arrangement"]?.let { arrangement = parseArrangement(it.jsonPrimitive.content) }
            data["horizontalAlignment"]?.let { horizontalAlignment = parseAlignment(it.jsonPrimitive.content) }

            // Parse modifiers
            data["padding"]?.let { padding(parseFloat(it)) }
            data["background"]?.let { background(parseColor(it.jsonPrimitive.content)) }

            // Parse children
            data["children"]?.jsonArray?.forEach { child ->
                parseChildComponent(this, child.jsonObject)
            }
        }
    }

    private fun parseRow(scope: AvaUIScope, data: JsonObject) {
        scope.Row(
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["arrangement"]?.let { arrangement = parseArrangement(it.jsonPrimitive.content) }
            data["verticalAlignment"]?.let { verticalAlignment = parseAlignment(it.jsonPrimitive.content) }

            data["padding"]?.let { padding(parseFloat(it)) }
            data["background"]?.let { background(parseColor(it.jsonPrimitive.content)) }

            data["children"]?.jsonArray?.forEach { child ->
                parseChildComponent(this, child.jsonObject)
            }
        }
    }

    private fun parseContainer(scope: AvaUIScope, data: JsonObject) {
        scope.Container(
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["alignment"]?.let { alignment = parseAlignment(it.jsonPrimitive.content) }
            data["padding"]?.let { padding(parseFloat(it)) }
            data["background"]?.let { background(parseColor(it.jsonPrimitive.content)) }

            data["child"]?.jsonObject?.let { child ->
                parseChildComponent(this, child)
            }
        }
    }

    private fun parseScrollView(scope: AvaUIScope, data: JsonObject) {
        scope.ScrollView(
            id = data["id"]?.jsonPrimitive?.content,
            orientation = data["orientation"]?.let {
                when (it.jsonPrimitive.content) {
                    "Horizontal" -> Orientation.Horizontal
                    else -> Orientation.Vertical
                }
            } ?: Orientation.Vertical,
            style = parseStyle(data["style"])
        ) {
            data["padding"]?.let { padding(parseFloat(it)) }

            data["child"]?.jsonObject?.let { child ->
                parseChildComponent(this, child)
            }
        }
    }

    private fun parseCard(scope: AvaUIScope, data: JsonObject) {
        scope.Card(
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["elevation"]?.let { elevation = it.jsonPrimitive.int }
            data["padding"]?.let { padding(parseFloat(it)) }

            data["children"]?.jsonArray?.forEach { child ->
                parseChildComponent(this, child.jsonObject)
            }
        }
    }

    // ==================== Basic Components ====================

    private fun parseText(scope: AvaUIScope, data: JsonObject) {
        scope.Text(
            text = data["text"]?.jsonPrimitive?.content ?: "",
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["font"]?.let { font = parseFont(it) }
            data["color"]?.let { color = parseColor(it.jsonPrimitive.content) }
            data["textAlign"]?.let { textAlign = parseTextAlign(it.jsonPrimitive.content) }
            data["maxLines"]?.let { maxLines = it.jsonPrimitive.int }
        }
    }

    private fun parseButton(scope: AvaUIScope, data: JsonObject) {
        scope.Button(
            text = data["text"]?.jsonPrimitive?.content ?: "",
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["style"]?.let { buttonStyle = parseButtonStyle(it.jsonPrimitive.content) }
            data["enabled"]?.let { enabled = it.jsonPrimitive.boolean }
            data["leadingIcon"]?.let { leadingIcon = it.jsonPrimitive.content }
            data["trailingIcon"]?.let { trailingIcon = it.jsonPrimitive.content }
            // onClick handler would be registered separately in application code
        }
    }

    private fun parseImage(scope: AvaUIScope, data: JsonObject) {
        scope.Image(
            source = data["source"]?.jsonPrimitive?.content ?: "",
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["contentDescription"]?.let { contentDescription = it.jsonPrimitive.content }
            data["contentScale"]?.let { contentScale = parseContentScale(it.jsonPrimitive.content) }
        }
    }

    private fun parseCheckbox(scope: AvaUIScope, data: JsonObject) {
        scope.Checkbox(
            label = data["label"]?.jsonPrimitive?.content ?: "",
            checked = data["checked"]?.jsonPrimitive?.boolean ?: false,
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["enabled"]?.let { enabled = it.jsonPrimitive.boolean }
        }
    }

    private fun parseTextField(scope: AvaUIScope, data: JsonObject) {
        scope.TextField(
            value = data["value"]?.jsonPrimitive?.content ?: "",
            placeholder = data["placeholder"]?.jsonPrimitive?.content ?: "",
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["label"]?.let { label = it.jsonPrimitive.content }
            data["enabled"]?.let { enabled = it.jsonPrimitive.boolean }
            data["readOnly"]?.let { readOnly = it.jsonPrimitive.boolean }
            data["isError"]?.let { isError = it.jsonPrimitive.boolean }
            data["errorMessage"]?.let { errorMessage = it.jsonPrimitive.content }
            data["leadingIcon"]?.let { leadingIcon = it.jsonPrimitive.content }
            data["trailingIcon"]?.let { trailingIcon = it.jsonPrimitive.content }
            data["maxLength"]?.let { maxLength = it.jsonPrimitive.int }
        }
    }

    private fun parseSwitch(scope: AvaUIScope, data: JsonObject) {
        scope.Switch(
            checked = data["checked"]?.jsonPrimitive?.boolean ?: false,
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["enabled"]?.let { enabled = it.jsonPrimitive.boolean }
        }
    }

    private fun parseIcon(scope: AvaUIScope, data: JsonObject) {
        scope.Icon(
            name = data["name"]?.jsonPrimitive?.content ?: "",
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["tint"]?.let { tint = parseColor(it.jsonPrimitive.content) }
            data["contentDescription"]?.let { contentDescription = it.jsonPrimitive.content }
        }
    }

    // ==================== Child Component Parsing ====================

    private fun parseChildComponent(scope: ColumnScope, componentJson: JsonObject) {
        val componentType = componentJson.keys.first()
        val componentData = componentJson[componentType]?.jsonObject ?: return

        when (componentType) {
            "Text" -> scope.Text(componentData["text"]?.jsonPrimitive?.content ?: "") {
                componentData["font"]?.let { font = parseFont(it) }
                componentData["color"]?.let { color = parseColor(it.jsonPrimitive.content) }
            }
            "Button" -> scope.Button(componentData["text"]?.jsonPrimitive?.content ?: "") {
                componentData["style"]?.let { buttonStyle = parseButtonStyle(it.jsonPrimitive.content) }
            }
            "Row" -> scope.Row {
                componentData["children"]?.jsonArray?.forEach { child ->
                    parseChildComponent(this, child.jsonObject)
                }
            }
            "Column" -> scope.Column {
                componentData["children"]?.jsonArray?.forEach { child ->
                    parseChildComponent(this, child.jsonObject)
                }
            }
        }
    }

    private fun parseChildComponent(scope: RowScope, componentJson: JsonObject) {
        val componentType = componentJson.keys.first()
        val componentData = componentJson[componentType]?.jsonObject ?: return

        when (componentType) {
            "Text" -> scope.Text(componentData["text"]?.jsonPrimitive?.content ?: "") {
                componentData["font"]?.let { font = parseFont(it) }
                componentData["color"]?.let { color = parseColor(it.jsonPrimitive.content) }
            }
            "Button" -> scope.Button(componentData["text"]?.jsonPrimitive?.content ?: "") {
                componentData["style"]?.let { buttonStyle = parseButtonStyle(it.jsonPrimitive.content) }
            }
            "Icon" -> scope.Icon(componentData["name"]?.jsonPrimitive?.content ?: "") {
                componentData["tint"]?.let { tint = parseColor(it.jsonPrimitive.content) }
            }
        }
    }

    private fun parseChildComponent(scope: ContainerScope, componentJson: JsonObject) {
        val componentType = componentJson.keys.first()
        val componentData = componentJson[componentType]?.jsonObject ?: return

        when (componentType) {
            "Text" -> scope.Text(componentData["text"]?.jsonPrimitive?.content ?: "")
            "Column" -> scope.Column {}
            "Row" -> scope.Row {}
        }
    }

    private fun parseChildComponent(scope: ScrollViewScope, componentJson: JsonObject) {
        val componentType = componentJson.keys.first()

        when (componentType) {
            "Column" -> scope.Column {}
            "Row" -> scope.Row {}
        }
    }

    private fun parseChildComponent(scope: CardScope, componentJson: JsonObject) {
        val componentType = componentJson.keys.first()
        val componentData = componentJson[componentType]?.jsonObject ?: return

        when (componentType) {
            "Text" -> scope.Text(componentData["text"]?.jsonPrimitive?.content ?: "")
            "Column" -> scope.Column {}
            "Row" -> scope.Row {}
        }
    }

    // ==================== Parsing Utilities ====================

    private fun parseStyle(data: JsonElement?): ComponentStyle? {
        if (data == null || data is JsonNull) return null
        val obj = data.jsonObject

        return ComponentStyle(
            width = obj["width"]?.let { parseSize(it) },
            height = obj["height"]?.let { parseSize(it) },
            padding = obj["padding"]?.let { parseSpacing(it) } ?: Spacing.Zero,
            margin = obj["margin"]?.let { parseSpacing(it) } ?: Spacing.Zero,
            backgroundColor = obj["backgroundColor"]?.let { parseColor(it.jsonPrimitive.content) },
            opacity = obj["opacity"]?.jsonPrimitive?.float ?: 1.0f
        )
    }

    private fun parseColor(colorString: String): Color {
        return if (colorString.startsWith("#")) {
            Color.hex(colorString)
        } else {
            // Named colors
            when (colorString.lowercase()) {
                "black" -> Color.Black
                "white" -> Color.White
                "red" -> Color.Red
                "green" -> Color.Green
                "blue" -> Color.Blue
                "transparent" -> Color.Transparent
                else -> Color.Black
            }
        }
    }

    private fun parseSize(element: JsonElement): Size {
        return when {
            element is JsonPrimitive && element.isString -> {
                when (element.content) {
                    "Auto" -> Size.Auto
                    "Fill" -> Size.Fill
                    else -> Size.Fixed(element.content.toFloatOrNull() ?: 0f)
                }
            }
            element is JsonPrimitive -> Size.Fixed(element.float)
            else -> Size.Auto
        }
    }

    private fun parseSpacing(element: JsonElement): Spacing {
        return when (element) {
            is JsonPrimitive -> Spacing.all(element.float)
            is JsonObject -> {
                Spacing(
                    top = element["top"]?.jsonPrimitive?.float ?: 0f,
                    right = element["right"]?.jsonPrimitive?.float ?: 0f,
                    bottom = element["bottom"]?.jsonPrimitive?.float ?: 0f,
                    left = element["left"]?.jsonPrimitive?.float ?: 0f
                )
            }
            else -> Spacing.Zero
        }
    }

    private fun parseFont(element: JsonElement): Font {
        return when {
            element is JsonPrimitive && element.isString -> {
                when (element.content) {
                    "Title" -> Font.Title
                    "Heading" -> Font.Heading
                    "Body" -> Font.Body
                    "Caption" -> Font.Caption
                    else -> Font.Body
                }
            }
            element is JsonObject -> {
                Font(
                    family = element["family"]?.jsonPrimitive?.content ?: "System",
                    size = element["size"]?.jsonPrimitive?.float ?: 16f,
                    weight = parseFontWeight(element["weight"]?.jsonPrimitive?.content),
                    style = parseFontStyle(element["style"]?.jsonPrimitive?.content)
                )
            }
            else -> Font.Body
        }
    }

    private fun parseFontWeight(weight: String?): Font.Weight {
        return when (weight) {
            "Thin" -> Font.Weight.Thin
            "ExtraLight" -> Font.Weight.ExtraLight
            "Light" -> Font.Weight.Light
            "Regular" -> Font.Weight.Regular
            "Medium" -> Font.Weight.Medium
            "SemiBold" -> Font.Weight.SemiBold
            "Bold" -> Font.Weight.Bold
            "ExtraBold" -> Font.Weight.ExtraBold
            "Black" -> Font.Weight.Black
            else -> Font.Weight.Regular
        }
    }

    private fun parseFontStyle(style: String?): Font.Style {
        return when (style) {
            "Italic" -> Font.Style.Italic
            "Oblique" -> Font.Style.Oblique
            else -> Font.Style.Normal
        }
    }

    private fun parseArrangement(value: String): Arrangement {
        return when (value) {
            "Start" -> Arrangement.Start
            "Center" -> Arrangement.Center
            "End" -> Arrangement.End
            "SpaceBetween" -> Arrangement.SpaceBetween
            "SpaceAround" -> Arrangement.SpaceAround
            "SpaceEvenly" -> Arrangement.SpaceEvenly
            else -> Arrangement.Start
        }
    }

    private fun parseAlignment(value: String): Alignment {
        return when (value) {
            "TopStart" -> Alignment.TopStart
            "TopCenter" -> Alignment.TopCenter
            "TopEnd" -> Alignment.TopEnd
            "CenterStart" -> Alignment.CenterStart
            "Center" -> Alignment.Center
            "CenterEnd" -> Alignment.CenterEnd
            "BottomStart" -> Alignment.BottomStart
            "BottomCenter" -> Alignment.BottomCenter
            "BottomEnd" -> Alignment.BottomEnd
            "Start" -> Alignment.Start
            "End" -> Alignment.End
            else -> Alignment.TopStart
        }
    }

    private fun parseTextAlign(value: String): TextScope.TextAlign {
        return when (value) {
            "Start" -> TextScope.TextAlign.Start
            "Center" -> TextScope.TextAlign.Center
            "End" -> TextScope.TextAlign.End
            "Justify" -> TextScope.TextAlign.Justify
            else -> TextScope.TextAlign.Start
        }
    }

    private fun parseButtonStyle(value: String): ButtonScope.ButtonStyle {
        return when (value) {
            "Primary" -> ButtonScope.ButtonStyle.Primary
            "Secondary" -> ButtonScope.ButtonStyle.Secondary
            "Tertiary" -> ButtonScope.ButtonStyle.Tertiary
            "Text" -> ButtonScope.ButtonStyle.Text
            "Outlined" -> ButtonScope.ButtonStyle.Outlined
            else -> ButtonScope.ButtonStyle.Primary
        }
    }

    private fun parseContentScale(value: String): ImageScope.ContentScale {
        return when (value) {
            "Fit" -> ImageScope.ContentScale.Fit
            "Fill" -> ImageScope.ContentScale.Fill
            "Crop" -> ImageScope.ContentScale.Crop
            "None" -> ImageScope.ContentScale.None
            else -> ImageScope.ContentScale.Fit
        }
    }

    private fun parseFloat(element: JsonElement): Float {
        return when (element) {
            is JsonPrimitive -> element.float
            else -> 0f
        }
    }

    // ==================== Form Component Parsers ====================

    private fun parseRadio(scope: AvaUIScope, data: JsonObject) {
        val optionsArray = data["options"]?.jsonArray ?: return
        val options = optionsArray.map { optionElement ->
            val optionObj = optionElement.jsonObject
            RadioOption(
                value = optionObj["value"]?.jsonPrimitive?.content ?: "",
                label = optionObj["label"]?.jsonPrimitive?.content ?: "",
                enabled = optionObj["enabled"]?.jsonPrimitive?.boolean ?: true
            )
        }

        scope.Radio(
            options = options,
            selectedValue = data["selectedValue"]?.jsonPrimitive?.content,
            groupName = data["groupName"]?.jsonPrimitive?.content ?: "radioGroup",
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["orientation"]?.let {
                orientation = when (it.jsonPrimitive.content) {
                    "Horizontal" -> Orientation.Horizontal
                    else -> Orientation.Vertical
                }
            }
        }
    }

    private fun parseSlider(scope: AvaUIScope, data: JsonObject) {
        scope.Slider(
            value = data["value"]?.jsonPrimitive?.float ?: 0f,
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["valueRange"]?.jsonObject?.let { range ->
                val min = range["min"]?.jsonPrimitive?.float ?: 0f
                val max = range["max"]?.jsonPrimitive?.float ?: 1f
                valueRange = min..max
            }
            data["steps"]?.let { steps = it.jsonPrimitive.int }
            data["showLabel"]?.let { showLabel = it.jsonPrimitive.boolean }
        }
    }

    private fun parseDropdown(scope: AvaUIScope, data: JsonObject) {
        val optionsArray = data["options"]?.jsonArray ?: return
        val options = optionsArray.map { optionElement ->
            val optionObj = optionElement.jsonObject
            DropdownOption(
                value = optionObj["value"]?.jsonPrimitive?.content ?: "",
                label = optionObj["label"]?.jsonPrimitive?.content ?: "",
                icon = optionObj["icon"]?.jsonPrimitive?.content,
                disabled = optionObj["disabled"]?.jsonPrimitive?.boolean ?: false
            )
        }

        scope.Dropdown(
            options = options,
            selectedValue = data["selectedValue"]?.jsonPrimitive?.content,
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["placeholder"]?.let { placeholder = it.jsonPrimitive.content }
            data["searchable"]?.let { searchable = it.jsonPrimitive.boolean }
        }
    }

    private fun parseDatePicker(scope: AvaUIScope, data: JsonObject) {
        scope.DatePicker(
            selectedDate = data["selectedDate"]?.jsonPrimitive?.long,
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["minDate"]?.let { minDate = it.jsonPrimitive.long }
            data["maxDate"]?.let { maxDate = it.jsonPrimitive.long }
            data["dateFormat"]?.let { dateFormat = it.jsonPrimitive.content }
        }
    }

    private fun parseTimePicker(scope: AvaUIScope, data: JsonObject) {
        scope.TimePicker(
            hour = data["hour"]?.jsonPrimitive?.int ?: 0,
            minute = data["minute"]?.jsonPrimitive?.int ?: 0,
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["is24Hour"]?.let { is24Hour = it.jsonPrimitive.boolean }
        }
    }

    private fun parseFileUpload(scope: AvaUIScope, data: JsonObject) {
        scope.FileUpload(
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["accept"]?.jsonArray?.let {
                accept = it.map { elem -> elem.jsonPrimitive.content }
            }
            data["multiple"]?.let { multiple = it.jsonPrimitive.boolean }
            data["maxSize"]?.let { maxSize = it.jsonPrimitive.long }
            data["placeholder"]?.let { placeholder = it.jsonPrimitive.content }
        }
    }

    private fun parseSearchBar(scope: AvaUIScope, data: JsonObject) {
        scope.SearchBar(
            value = data["value"]?.jsonPrimitive?.content ?: "",
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["placeholder"]?.let { placeholder = it.jsonPrimitive.content }
            data["showClearButton"]?.let { showClearButton = it.jsonPrimitive.boolean }
            data["suggestions"]?.jsonArray?.let {
                suggestions = it.map { elem -> elem.jsonPrimitive.content }
            }
        }
    }

    private fun parseRating(scope: AvaUIScope, data: JsonObject) {
        scope.Rating(
            value = data["value"]?.jsonPrimitive?.float ?: 0f,
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["maxRating"]?.let { maxRating = it.jsonPrimitive.int }
            data["allowHalf"]?.let { allowHalf = it.jsonPrimitive.boolean }
            data["readonly"]?.let { readonly = it.jsonPrimitive.boolean }
            data["icon"]?.let { icon = it.jsonPrimitive.content }
        }
    }

    // ==================== Feedback Component Parsers ====================

    private fun parseDialog(scope: AvaUIScope, data: JsonObject) {
        scope.Dialog(
            isOpen = data["isOpen"]?.jsonPrimitive?.boolean ?: false,
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["title"]?.let { title = it.jsonPrimitive.content }
            data["dismissible"]?.let { dismissible = it.jsonPrimitive.boolean }
            data["actions"]?.jsonArray?.let { actionsArray ->
                actions = actionsArray.map { actionElement ->
                    val actionObj = actionElement.jsonObject
                    DialogAction(
                        label = actionObj["label"]?.jsonPrimitive?.content ?: "",
                        style = when (actionObj["style"]?.jsonPrimitive?.content) {
                            "Primary" -> DialogActionStyle.Primary
                            "Secondary" -> DialogActionStyle.Secondary
                            "Text" -> DialogActionStyle.Text
                            "Outlined" -> DialogActionStyle.Outlined
                            else -> DialogActionStyle.Primary
                        },
                        onClick = {}
                    )
                }
            }
        }
    }

    private fun parseToast(scope: AvaUIScope, data: JsonObject) {
        scope.Toast(
            message = data["message"]?.jsonPrimitive?.content ?: "",
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["duration"]?.let { duration = it.jsonPrimitive.long }
            data["severity"]?.let {
                severity = when (it.jsonPrimitive.content) {
                    "Success" -> ToastSeverity.Success
                    "Warning" -> ToastSeverity.Warning
                    "Error" -> ToastSeverity.Error
                    else -> ToastSeverity.Info
                }
            }
            data["position"]?.let {
                position = when (it.jsonPrimitive.content) {
                    "TopLeft" -> ToastPosition.TopLeft
                    "TopCenter" -> ToastPosition.TopCenter
                    "TopRight" -> ToastPosition.TopRight
                    "BottomLeft" -> ToastPosition.BottomLeft
                    "BottomRight" -> ToastPosition.BottomRight
                    else -> ToastPosition.BottomCenter
                }
            }
        }
    }

    private fun parseAlert(scope: AvaUIScope, data: JsonObject) {
        scope.Alert(
            title = data["title"]?.jsonPrimitive?.content ?: "",
            message = data["message"]?.jsonPrimitive?.content ?: "",
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["severity"]?.let {
                severity = when (it.jsonPrimitive.content) {
                    "Success" -> AlertSeverity.Success
                    "Warning" -> AlertSeverity.Warning
                    "Error" -> AlertSeverity.Error
                    else -> AlertSeverity.Info
                }
            }
            data["dismissible"]?.let { dismissible = it.jsonPrimitive.boolean }
            data["icon"]?.let { icon = it.jsonPrimitive.content }
        }
    }

    private fun parseProgressBar(scope: AvaUIScope, data: JsonObject) {
        scope.ProgressBar(
            value = data["value"]?.jsonPrimitive?.float ?: 0f,
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["showLabel"]?.let { showLabel = it.jsonPrimitive.boolean }
            data["indeterminate"]?.let { indeterminate = it.jsonPrimitive.boolean }
        }
    }

    private fun parseSpinner(scope: AvaUIScope, data: JsonObject) {
        scope.Spinner(
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["size"]?.let {
                size = when (it.jsonPrimitive.content) {
                    "Small" -> SpinnerSize.Small
                    "Large" -> SpinnerSize.Large
                    else -> SpinnerSize.Medium
                }
            }
            data["label"]?.let { label = it.jsonPrimitive.content }
        }
    }

    private fun parseBadge(scope: AvaUIScope, data: JsonObject) {
        scope.Badge(
            content = data["content"]?.jsonPrimitive?.content ?: "",
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["variant"]?.let {
                variant = when (it.jsonPrimitive.content) {
                    "Primary" -> BadgeVariant.Primary
                    "Secondary" -> BadgeVariant.Secondary
                    "Success" -> BadgeVariant.Success
                    "Warning" -> BadgeVariant.Warning
                    "Error" -> BadgeVariant.Error
                    else -> BadgeVariant.Default
                }
            }
            data["size"]?.let {
                size = when (it.jsonPrimitive.content) {
                    "Small" -> BadgeSize.Small
                    "Large" -> BadgeSize.Large
                    else -> BadgeSize.Medium
                }
            }
        }
    }

    private fun parseTooltip(scope: AvaUIScope, data: JsonObject) {
        scope.Tooltip(
            content = data["content"]?.jsonPrimitive?.content ?: "",
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["position"]?.let {
                position = when (it.jsonPrimitive.content) {
                    "Bottom" -> TooltipPosition.Bottom
                    "Left" -> TooltipPosition.Left
                    "Right" -> TooltipPosition.Right
                    else -> TooltipPosition.Top
                }
            }
            // Parse child component
            data["child"]?.jsonObject?.let { childData ->
                val childType = childData.keys.first()
                val childComponentData = childData[childType]?.jsonObject
                when (childType) {
                    "Text" -> Text(childComponentData?.get("text")?.jsonPrimitive?.content ?: "")
                    "Button" -> Button(childComponentData?.get("text")?.jsonPrimitive?.content ?: "")
                    "Icon" -> Icon(childComponentData?.get("name")?.jsonPrimitive?.content ?: "")
                }
            }
        }
    }

    // ==================== Navigation Component Parsers ====================

    private fun parseAppBar(scope: AvaUIScope, data: JsonObject) {
        scope.AppBar(
            title = data["title"]?.jsonPrimitive?.content ?: "",
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["navigationIcon"]?.let { navigationIcon = it.jsonPrimitive.content }
            data["elevation"]?.let { elevation = it.jsonPrimitive.int }
            data["actions"]?.jsonArray?.let { actionsArray ->
                actions = actionsArray.map { actionElement ->
                    val actionObj = actionElement.jsonObject
                    com.augmentalis.avaelements.components.navigation.AppBarAction(
                        icon = actionObj["icon"]?.jsonPrimitive?.content ?: "",
                        label = actionObj["label"]?.jsonPrimitive?.content,
                        onClick = {}
                    )
                }
            }
        }
    }

    private fun parseBottomNav(scope: AvaUIScope, data: JsonObject) {
        val itemsArray = data["items"]?.jsonArray ?: return
        val items = itemsArray.map { itemElement ->
            val itemObj = itemElement.jsonObject
            com.augmentalis.avaelements.components.navigation.BottomNavItem(
                icon = itemObj["icon"]?.jsonPrimitive?.content ?: "",
                label = itemObj["label"]?.jsonPrimitive?.content ?: "",
                badge = itemObj["badge"]?.jsonPrimitive?.content
            )
        }

        scope.BottomNav(
            items = items,
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["selectedIndex"]?.let { selectedIndex = it.jsonPrimitive.int }
        }
    }

    private fun parseTabs(scope: AvaUIScope, data: JsonObject) {
        val tabsArray = data["tabs"]?.jsonArray ?: return
        val tabs = tabsArray.map { tabElement ->
            val tabObj = tabElement.jsonObject
            com.augmentalis.avaelements.components.navigation.Tab(
                label = tabObj["label"]?.jsonPrimitive?.content ?: "",
                icon = tabObj["icon"]?.jsonPrimitive?.content
            )
        }

        scope.Tabs(
            tabs = tabs,
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["selectedIndex"]?.let { selectedIndex = it.jsonPrimitive.int }
        }
    }

    private fun parseDrawer(scope: AvaUIScope, data: JsonObject) {
        scope.Drawer(
            isOpen = data["isOpen"]?.jsonPrimitive?.boolean ?: false,
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["position"]?.let {
                position = when (it.jsonPrimitive.content) {
                    "Right" -> com.augmentalis.avaelements.components.navigation.DrawerPosition.Right
                    else -> com.augmentalis.avaelements.components.navigation.DrawerPosition.Left
                }
            }
            data["items"]?.jsonArray?.let { itemsArray ->
                items = itemsArray.map { itemElement ->
                    val itemObj = itemElement.jsonObject
                    com.augmentalis.avaelements.components.navigation.DrawerItem(
                        id = itemObj["id"]?.jsonPrimitive?.content ?: "",
                        icon = itemObj["icon"]?.jsonPrimitive?.content,
                        label = itemObj["label"]?.jsonPrimitive?.content ?: "",
                        badge = itemObj["badge"]?.jsonPrimitive?.content
                    )
                }
            }
        }
    }

    private fun parseBreadcrumb(scope: AvaUIScope, data: JsonObject) {
        val itemsArray = data["items"]?.jsonArray ?: return
        val items = itemsArray.map { itemElement ->
            val itemObj = itemElement.jsonObject
            com.augmentalis.avaelements.components.navigation.BreadcrumbItem(
                label = itemObj["label"]?.jsonPrimitive?.content ?: "",
                href = itemObj["href"]?.jsonPrimitive?.content
            )
        }

        scope.Breadcrumb(
            items = items,
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["separator"]?.let { separator = it.jsonPrimitive.content }
        }
    }

    private fun parsePagination(scope: AvaUIScope, data: JsonObject) {
        scope.Pagination(
            totalPages = data["totalPages"]?.jsonPrimitive?.int ?: 1,
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["currentPage"]?.let { currentPage = it.jsonPrimitive.int }
            data["showFirstLast"]?.let { showFirstLast = it.jsonPrimitive.boolean }
            data["showPrevNext"]?.let { showPrevNext = it.jsonPrimitive.boolean }
            data["maxVisible"]?.let { maxVisible = it.jsonPrimitive.int }
        }
    }

    // ==================== Data Display Component Parsers ====================

    private fun parseTable(scope: AvaUIScope, data: JsonObject) {
        val columnsArray = data["columns"]?.jsonArray ?: return
        val columns = columnsArray.map { columnElement ->
            val columnObj = columnElement.jsonObject
            com.augmentalis.avaelements.components.data.TableColumn(
                id = columnObj["id"]?.jsonPrimitive?.content ?: "",
                label = columnObj["label"]?.jsonPrimitive?.content ?: "",
                sortable = columnObj["sortable"]?.jsonPrimitive?.boolean ?: false
            )
        }

        val rowsArray = data["rows"]?.jsonArray ?: emptyList()
        val rows = rowsArray.map { rowElement ->
            val rowObj = rowElement.jsonObject
            val cells = rowObj.entries.filter { it.key != "id" }.map { (_, value) ->
                com.augmentalis.avaelements.components.data.TableCell(
                    content = value.jsonPrimitive.content
                )
            }
            com.augmentalis.avaelements.components.data.TableRow(
                id = rowObj["id"]?.jsonPrimitive?.content ?: "",
                cells = cells
            )
        }

        scope.Table(
            columns = columns,
            rows = rows,
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["sortable"]?.let { sortable = it.jsonPrimitive.boolean }
            data["hoverable"]?.let { hoverable = it.jsonPrimitive.boolean }
            data["striped"]?.let { striped = it.jsonPrimitive.boolean }
        }
    }

    private fun parseList(scope: AvaUIScope, data: JsonObject) {
        val itemsArray = data["items"]?.jsonArray ?: return
        val items = itemsArray.map { itemElement ->
            val itemObj = itemElement.jsonObject
            com.augmentalis.avaelements.components.data.ListItem(
                id = itemObj["id"]?.jsonPrimitive?.content ?: "",
                primary = itemObj["primary"]?.jsonPrimitive?.content ?: "",
                secondary = itemObj["secondary"]?.jsonPrimitive?.content,
                icon = itemObj["icon"]?.jsonPrimitive?.content,
                avatar = itemObj["avatar"]?.jsonPrimitive?.content
            )
        }

        scope.List(
            items = items,
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["selectable"]?.let { selectable = it.jsonPrimitive.boolean }
        }
    }

    private fun parseAccordion(scope: AvaUIScope, data: JsonObject) {
        val itemsArray = data["items"]?.jsonArray ?: return
        val items = itemsArray.map { itemElement ->
            val itemObj = itemElement.jsonObject
            com.augmentalis.avaelements.components.data.AccordionItem(
                id = itemObj["id"]?.jsonPrimitive?.content ?: "",
                title = itemObj["title"]?.jsonPrimitive?.content ?: "",
                content = TextComponent(
                    text = itemObj["content"]?.jsonPrimitive?.content ?: "",
                    id = null,
                    style = null,
                    modifiers = emptyList(),
                    font = Font.Body,
                    color = Color.Black,
                    textAlign = TextScope.TextAlign.Start,
                    maxLines = null,
                    overflow = TextScope.TextOverflow.Clip
                )
            )
        }

        scope.Accordion(
            items = items,
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["allowMultiple"]?.let { allowMultiple = it.jsonPrimitive.boolean }
        }
    }

    private fun parseStepper(scope: AvaUIScope, data: JsonObject) {
        val stepsArray = data["steps"]?.jsonArray ?: return
        val steps = stepsArray.map { stepElement ->
            val stepObj = stepElement.jsonObject
            com.augmentalis.avaelements.components.data.Step(
                label = stepObj["label"]?.jsonPrimitive?.content ?: "",
                description = stepObj["description"]?.jsonPrimitive?.content,
                status = when (stepObj["status"]?.jsonPrimitive?.content) {
                    "Active" -> com.augmentalis.avaelements.components.data.StepStatus.Active
                    "Complete" -> com.augmentalis.avaelements.components.data.StepStatus.Complete
                    "Error" -> com.augmentalis.avaelements.components.data.StepStatus.Error
                    else -> com.augmentalis.avaelements.components.data.StepStatus.Pending
                }
            )
        }

        scope.Stepper(
            steps = steps,
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["currentStep"]?.let { currentStep = it.jsonPrimitive.int }
            data["orientation"]?.let {
                orientation = when (it.jsonPrimitive.content) {
                    "Horizontal" -> Orientation.Horizontal
                    else -> Orientation.Vertical
                }
            }
        }
    }

    private fun parseTimeline(scope: AvaUIScope, data: JsonObject) {
        val itemsArray = data["items"]?.jsonArray ?: return
        val items = itemsArray.map { itemElement ->
            val itemObj = itemElement.jsonObject
            com.augmentalis.avaelements.components.data.TimelineItem(
                id = itemObj["id"]?.jsonPrimitive?.content ?: "",
                timestamp = itemObj["timestamp"]?.jsonPrimitive?.content ?: "",
                title = itemObj["title"]?.jsonPrimitive?.content ?: "",
                description = itemObj["description"]?.jsonPrimitive?.content,
                icon = itemObj["icon"]?.jsonPrimitive?.content,
                color = itemObj["color"]?.let { parseColor(it.jsonPrimitive.content) }
            )
        }

        scope.Timeline(
            items = items,
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["orientation"]?.let {
                orientation = when (it.jsonPrimitive.content) {
                    "Horizontal" -> Orientation.Horizontal
                    else -> Orientation.Vertical
                }
            }
        }
    }

    private fun parseTreeView(scope: AvaUIScope, data: JsonObject) {
        fun parseNode(nodeObj: JsonObject): com.augmentalis.avaelements.components.data.TreeNode {
            val children = nodeObj["children"]?.jsonArray?.map { parseNode(it.jsonObject) } ?: emptyList()
            return com.augmentalis.avaelements.components.data.TreeNode(
                id = nodeObj["id"]?.jsonPrimitive?.content ?: "",
                label = nodeObj["label"]?.jsonPrimitive?.content ?: "",
                icon = nodeObj["icon"]?.jsonPrimitive?.content,
                children = children
            )
        }

        val nodesArray = data["nodes"]?.jsonArray ?: return
        val nodes = nodesArray.map { parseNode(it.jsonObject) }

        scope.TreeView(
            nodes = nodes,
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        )
    }

    private fun parseCarousel(scope: AvaUIScope, data: JsonObject) {
        val itemsArray = data["items"]?.jsonArray ?: return
        val items = itemsArray.map { itemElement ->
            // Simplified: assuming items are images
            val itemObj = itemElement.jsonObject
            val itemType = itemObj.keys.first()
            val itemData = itemObj[itemType]?.jsonObject ?: return@map null

            when (itemType) {
                "Image" -> ImageComponent(
                    source = itemData["source"]?.jsonPrimitive?.content ?: "",
                    id = null,
                    style = null,
                    modifiers = emptyList(),
                    contentDescription = null,
                    contentScale = ImageScope.ContentScale.Fit
                )
                else -> null
            }
        }.filterNotNull()

        scope.Carousel(
            items = items,
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["autoPlay"]?.let { autoPlay = it.jsonPrimitive.boolean }
            data["interval"]?.let { interval = it.jsonPrimitive.long }
            data["showIndicators"]?.let { showIndicators = it.jsonPrimitive.boolean }
            data["showControls"]?.let { showControls = it.jsonPrimitive.boolean }
        }
    }

    private fun parseAvatar(scope: AvaUIScope, data: JsonObject) {
        scope.Avatar(
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["source"]?.let { source = it.jsonPrimitive.content }
            data["text"]?.let { text = it.jsonPrimitive.content }
            data["size"]?.let {
                size = when (it.jsonPrimitive.content) {
                    "Small" -> com.augmentalis.avaelements.components.data.AvatarSize.Small
                    "Large" -> com.augmentalis.avaelements.components.data.AvatarSize.Large
                    else -> com.augmentalis.avaelements.components.data.AvatarSize.Medium
                }
            }
            data["shape"]?.let {
                shape = when (it.jsonPrimitive.content) {
                    "Square" -> com.augmentalis.avaelements.components.data.AvatarShape.Square
                    "Rounded" -> com.augmentalis.avaelements.components.data.AvatarShape.Rounded
                    else -> com.augmentalis.avaelements.components.data.AvatarShape.Circle
                }
            }
        }
    }

    private fun parseChip(scope: AvaUIScope, data: JsonObject) {
        scope.Chip(
            label = data["label"]?.jsonPrimitive?.content ?: "",
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["icon"]?.let { icon = it.jsonPrimitive.content }
            data["deletable"]?.let { deletable = it.jsonPrimitive.boolean }
            data["selected"]?.let { selected = it.jsonPrimitive.boolean }
        }
    }

    private fun parseDivider(scope: AvaUIScope, data: JsonObject) {
        scope.Divider(
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["orientation"]?.let {
                orientation = when (it.jsonPrimitive.content) {
                    "Horizontal" -> Orientation.Horizontal
                    else -> Orientation.Vertical
                }
            }
            data["thickness"]?.let { thickness = it.jsonPrimitive.float }
            data["text"]?.let { text = it.jsonPrimitive.content }
        }
    }

    private fun parsePaper(scope: AvaUIScope, data: JsonObject) {
        scope.Paper(
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["elevation"]?.let { elevation = it.jsonPrimitive.int }
            // Parse children if needed
        }
    }

    private fun parseSkeleton(scope: AvaUIScope, data: JsonObject) {
        scope.Skeleton(
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["variant"]?.let {
                variant = when (it.jsonPrimitive.content) {
                    "Rectangular" -> com.augmentalis.avaelements.components.data.SkeletonVariant.Rectangular
                    "Circular" -> com.augmentalis.avaelements.components.data.SkeletonVariant.Circular
                    else -> com.augmentalis.avaelements.components.data.SkeletonVariant.Text
                }
            }
            data["width"]?.let { width = parseSize(it) }
            data["height"]?.let { height = parseSize(it) }
            data["animation"]?.let {
                animation = when (it.jsonPrimitive.content) {
                    "Wave" -> com.augmentalis.avaelements.components.data.SkeletonAnimation.Wave
                    "None" -> com.augmentalis.avaelements.components.data.SkeletonAnimation.None
                    else -> com.augmentalis.avaelements.components.data.SkeletonAnimation.Pulse
                }
            }
        }
    }

    private fun parseEmptyState(scope: AvaUIScope, data: JsonObject) {
        scope.EmptyState(
            title = data["title"]?.jsonPrimitive?.content ?: "",
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["icon"]?.let { icon = it.jsonPrimitive.content }
            data["description"]?.let { description = it.jsonPrimitive.content }
        }
    }

    private fun parseDataGrid(scope: AvaUIScope, data: JsonObject) {
        val columnsArray = data["columns"]?.jsonArray ?: return
        val columns = columnsArray.map { columnElement ->
            val columnObj = columnElement.jsonObject
            com.augmentalis.avaelements.components.data.DataGridColumn(
                id = columnObj["id"]?.jsonPrimitive?.content ?: "",
                label = columnObj["label"]?.jsonPrimitive?.content ?: "",
                sortable = columnObj["sortable"]?.jsonPrimitive?.boolean ?: true,
                align = when (columnObj["align"]?.jsonPrimitive?.content) {
                    "Center" -> com.augmentalis.avaelements.components.data.TextAlign.Center
                    "End" -> com.augmentalis.avaelements.components.data.TextAlign.End
                    else -> com.augmentalis.avaelements.components.data.TextAlign.Start
                }
            )
        }

        val rowsArray = data["rows"]?.jsonArray ?: emptyList()
        val rows = rowsArray.map { rowElement ->
            val rowObj = rowElement.jsonObject
            val id = rowObj["id"]?.jsonPrimitive?.content ?: ""
            val cells = rowObj.entries.filter { it.key != "id" }
                .associate { (key, value) ->
                    key to (value.jsonPrimitive.content as Any)
                }
            com.augmentalis.avaelements.components.data.DataGridRow(id = id, cells = cells)
        }

        scope.DataGrid(
            columns = columns,
            rows = rows,
            id = data["id"]?.jsonPrimitive?.content,
            style = parseStyle(data["style"])
        ) {
            data["pageSize"]?.let { pageSize = it.jsonPrimitive.int }
            data["currentPage"]?.let { currentPage = it.jsonPrimitive.int }
            data["sortBy"]?.let { sortBy = it.jsonPrimitive.content }
            data["selectable"]?.let { selectable = it.jsonPrimitive.boolean }
        }
    }

    /**
     * Simple YAML to JSON converter
     * In production, use a proper YAML library
     */
    private fun yamlToJson(yaml: String): String {
        // This is a simplified converter for demonstration
        // In production, use kotlinx-serialization-yaml or similar library

        // For now, assume the YAML is simple enough to convert manually
        // This is a placeholder implementation
        return """
        {
            "theme": "Material3",
            "components": []
        }
        """.trimIndent()
    }
}

/**
 * DSL to YAML Converter
 */
class YamlGenerator {
    fun generate(ui: AvaUI): String {
        val builder = StringBuilder()

        // Generate theme
        builder.appendLine("theme: ${ui.theme.platform.name}")
        builder.appendLine()

        // Generate components
        builder.appendLine("components:")
        ui.root?.let { generateComponent(builder, it, indent = 1) }

        return builder.toString()
    }

    private fun generateComponent(builder: StringBuilder, component: Component, indent: Int) {
        val indentStr = "  ".repeat(indent)

        when (component) {
            is ColumnComponent -> {
                builder.appendLine("$indentStr- Column:")
                component.id?.let { builder.appendLine("$indentStr    id: $it") }
                builder.appendLine("$indentStr    arrangement: ${component.arrangement}")
                if (component.children.isNotEmpty()) {
                    builder.appendLine("$indentStr    children:")
                    component.children.forEach { generateComponent(builder, it, indent + 2) }
                }
            }
            is TextComponent -> {
                builder.appendLine("$indentStr- Text:")
                builder.appendLine("$indentStr    text: \"${component.text}\"")
                builder.appendLine("$indentStr    color: ${component.color.toHex()}")
            }
            is ButtonComponent -> {
                builder.appendLine("$indentStr- Button:")
                builder.appendLine("$indentStr    text: \"${component.text}\"")
                builder.appendLine("$indentStr    style: ${component.buttonStyle}")
            }
            // Add other component types as needed
        }
    }
}
