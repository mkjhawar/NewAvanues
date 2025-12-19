package net.ideahq.avamagic.codegen.ast

import kotlin.random.Random

/**
 * Multiplatform UUID utilities for AVAMagic CodeGen
 *
 * Generates unique identifiers that work across Android, iOS, Web, and Desktop.
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
object UuidUtils {

    private val HEX_CHARS = "0123456789abcdef"

    /**
     * Generate a UUID v4 compatible identifier
     *
     * Format: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
     * where y is one of 8, 9, a, or b
     */
    fun generateUuid(): String {
        val random = Random.Default

        val uuid = StringBuilder(36)

        for (i in 0..35) {
            when (i) {
                8, 13, 18, 23 -> uuid.append('-')
                14 -> uuid.append('4') // Version 4
                19 -> {
                    // Variant (8, 9, a, or b)
                    val variant = random.nextInt(4)
                    uuid.append(HEX_CHARS[8 + variant])
                }
                else -> {
                    val randomHex = random.nextInt(16)
                    uuid.append(HEX_CHARS[randomHex])
                }
            }
        }

        return uuid.toString()
    }

    /**
     * Generate a short UUID (8 characters)
     *
     * Useful for component IDs where full UUID is not necessary
     */
    fun generateShortId(): String {
        val random = Random.Default
        return buildString {
            repeat(8) {
                append(HEX_CHARS[random.nextInt(16)])
            }
        }
    }

    /**
     * Generate a prefixed ID
     *
     * @param prefix The prefix to use (e.g., "btn", "txt", "card")
     * @return Prefixed short ID (e.g., "btn_a3f2c891")
     */
    fun generatePrefixedId(prefix: String): String {
        return "${prefix}_${generateShortId()}"
    }

    /**
     * Generate a component ID based on component type
     *
     * @param componentType The component type
     * @return Prefixed ID (e.g., "button_a3f2c891")
     */
    fun generateComponentId(componentType: ComponentType): String {
        val prefix = when (componentType) {
            ComponentType.BUTTON -> "btn"
            ComponentType.TEXT -> "txt"
            ComponentType.TEXT_FIELD -> "input"
            ComponentType.CARD -> "card"
            ComponentType.CHECKBOX -> "chk"
            ComponentType.IMAGE -> "img"
            ComponentType.ICON -> "icon"
            ComponentType.DIVIDER -> "div"
            ComponentType.CHIP -> "chip"
            ComponentType.LIST_ITEM -> "item"
            ComponentType.CONTAINER -> "cont"
            ComponentType.ROW -> "row"
            ComponentType.COLUMN -> "col"
            ComponentType.SPACER -> "spc"
            ComponentType.SWITCH -> "swt"
            ComponentType.SLIDER -> "sld"
            ComponentType.PROGRESS_BAR -> "prog"
            ComponentType.SPINNER -> "spin"
            ComponentType.ALERT -> "alert"
            ComponentType.DIALOG -> "dlg"
            ComponentType.TOAST -> "toast"
            ComponentType.TOOLTIP -> "tip"
            ComponentType.DROPDOWN -> "drop"
            ComponentType.DATE_PICKER -> "date"
            ComponentType.TIME_PICKER -> "time"
            ComponentType.SEARCH_BAR -> "srch"
            ComponentType.RATING -> "rate"
            ComponentType.BADGE -> "badge"
            ComponentType.APP_BAR -> "app"
            ComponentType.BOTTOM_NAV -> "nav"
            ComponentType.TABS -> "tabs"
            ComponentType.DRAWER -> "drawer"
            ComponentType.GRID -> "grid"
            ComponentType.STACK -> "stack"
            ComponentType.SCROLL_VIEW -> "scroll"
            ComponentType.RADIO -> "radio"
            ComponentType.FILE_UPLOAD -> "file"
            ComponentType.PAGINATION -> "page"
            ComponentType.BREADCRUMB -> "bread"
            ComponentType.ACCORDION -> "accord"
            ComponentType.LABEL -> "lbl"
            ComponentType.COLOR_PICKER -> "color"
            ComponentType.ICON_PICKER -> "iconp"
            ComponentType.CUSTOM -> "cust"
        }
        return generatePrefixedId(prefix)
    }

    /**
     * Validate UUID format
     */
    fun isValidUuid(uuid: String): Boolean {
        val regex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
        return regex.matches(uuid.lowercase())
    }

    /**
     * Validate short ID format
     */
    fun isValidShortId(id: String): Boolean {
        return id.length == 8 && id.all { it in '0'..'9' || it in 'a'..'f' }
    }
}

/**
 * Extension to generate UUID for any object
 */
fun Any.generateUuid(): String = UuidUtils.generateUuid()

/**
 * Extension to generate component ID based on type
 */
fun ComponentType.generateId(): String = UuidUtils.generateComponentId(this)
