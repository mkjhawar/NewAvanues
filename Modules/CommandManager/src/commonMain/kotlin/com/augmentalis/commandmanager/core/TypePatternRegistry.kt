package com.augmentalis.commandmanager

import com.augmentalis.avid.TypeCode

/**
 * Registry for UI element type patterns.
 *
 * Follows Open/Closed Principle - extend via providers, don't modify.
 * Each platform/framework can register its own patterns.
 *
 * Location: commonMain (KMP cross-platform)
 */
interface TypePatternProvider {
    /** Provider name for debugging */
    val name: String
    /** Priority - higher = checked first (Compose=90, Native=0) */
    val priority: Int
    /** Patterns mapped to type codes (3-char codes from TypeCode) */
    val patterns: Map<String, Set<String>>
}

object TypePatternRegistry {
    private val providers = mutableListOf<TypePatternProvider>()

    fun register(provider: TypePatternProvider) {
        providers.add(provider)
        providers.sortByDescending { it.priority }
    }

    fun unregister(provider: TypePatternProvider) {
        providers.remove(provider)
    }

    fun clear() {
        providers.clear()
    }

    /**
     * Get the type code for a class name.
     * Checks providers in priority order (highest first).
     * Returns 3-char TypeCode string (e.g., "BTN", "INP")
     */
    fun getTypeCode(className: String): String {
        val normalized = className.trim().lowercase()
        if (normalized.isEmpty()) return TypeCode.ELEMENT

        for (provider in providers) {
            for ((typeCode, patternSet) in provider.patterns) {
                if (patternSet.any { normalized.contains(it) }) {
                    return typeCode
                }
            }
        }
        return TypeCode.ELEMENT
    }

    fun getProviders(): List<TypePatternProvider> = providers.toList()

    /**
     * Register default providers (Compose + Native).
     * Call this at app startup.
     */
    fun registerDefaults() {
        clear()
        register(ComposePatternProvider)  // Priority 90
        register(NativePatternProvider)   // Priority 0 (fallback)
    }
}

/**
 * Native Android/iOS patterns - lowest priority fallback.
 */
object NativePatternProvider : TypePatternProvider {
    override val name = "Native"
    override val priority = 0

    override val patterns = mapOf(
        TypeCode.BUTTON to setOf(
            "button", "appcompatbutton", "materialbutton", "imagebutton",
            "floatingactionbutton", "fab", "extendedfloatingactionbutton"
        ),
        TypeCode.INPUT to setOf(
            "edittext", "textinputedittext", "autocompletetextview", "searchview",
            "textfield", "textinputlayout", "searchbar"
        ),
        TypeCode.SCROLL to setOf(
            "scrollview", "horizontalscrollview", "nestedscrollview", "recyclerview",
            "listview", "gridview", "viewpager", "viewpager2"
        ),
        TypeCode.TEXT to setOf(
            "textview", "appcompattextview", "materialtextview", "checkedtextview"
        ),
        TypeCode.CARD to setOf(
            "cardview", "materialcardview", "card"
        ),
        TypeCode.LAYOUT to setOf(
            "linearlayout", "relativelayout", "framelayout", "constraintlayout",
            "coordinatorlayout", "appbarlayout", "collapsingtoolbarlayout",
            "toolbar", "actionbar"
        ),
        TypeCode.MENU to setOf(
            "menu", "popupmenu", "contextmenu", "dropdownmenu", "navigationmenu",
            "overflowmenu", "optionsmenu", "spinner", "bottomnavigation",
            "bottomnavigationview"
        ),
        TypeCode.DIALOG to setOf(
            "dialog", "alertdialog", "bottomsheetdialog", "dialogfragment",
            "datepickerdialog", "timepickerdialog"
        ),
        TypeCode.IMAGE to setOf(
            "imageview", "appcompatimageview", "image", "icon",
            "shapeableimageview", "circleimageview"
        ),
        TypeCode.CHECKBOX to setOf(
            "checkbox", "appcompatcheckbox", "materialcheckbox"
        ),
        TypeCode.SWITCH to setOf(
            "switch", "switchcompat", "switchmaterial", "togglebutton"
        ),
        TypeCode.SLIDER to setOf(
            "slider", "seekbar", "rangeslider"
        ),
        TypeCode.TAB to setOf(
            "tablayout", "tabitem"
        ),
        TypeCode.LIST to setOf(
            "list", "itemlist", "dropdownlist"
        )
    )
}

/**
 * Jetpack Compose / Material3 patterns - high priority.
 * These take precedence over native patterns for Compose apps.
 */
object ComposePatternProvider : TypePatternProvider {
    override val name = "Compose"
    override val priority = 90

    override val patterns = mapOf(
        TypeCode.BUTTON to setOf(
            // M3 Button variants
            "iconbutton", "icontogglebutton", "filledbutton", "filledtonalbutton",
            "outlinedbutton", "elevatedbutton", "textbutton", "segmentedbutton",
            "smallfloatingactionbutton", "largefloatingactionbutton",
            // Chips (act as buttons)
            "chip", "filterchip", "inputchip", "assistchip", "suggestionchip",
            "elevatedfilterchip", "elevatedassistchip", "elevatedsuggestionchip"
        ),
        TypeCode.INPUT to setOf(
            "outlinedtextfield", "basictextfield", "decoratedtextfield",
            "outlinedsearchbar", "dockedsearchbar"
        ),
        TypeCode.SCROLL to setOf(
            "lazycolumn", "lazyrow", "lazyverticalgrid", "lazyhorizontalgrid",
            "lazystaggeredgrid"
        ),
        TypeCode.LAYOUT to setOf(
            "row", "column", "box", "surface", "scaffold",
            "topappbar", "centeralignedtopappbar", "mediumtopappbar", "largetopappbar",
            "bottomappbar"
        ),
        TypeCode.MENU to setOf(
            "navigationbar", "navigationrail", "navigationdrawer",
            "navigationbaritem", "navigationrailitem", "navigationdraweritem",
            "exposeddropdownmenu", "exposeddropdownmenubox", "dropdownmenuitem"
        ),
        TypeCode.TAB to setOf(
            "tabrow", "scrollabletabrow", "primarytabrow", "secondarytabrow",
            "tab", "leadingicontab", "texttab"
        ),
        TypeCode.DIALOG to setOf(
            "bottomsheet", "modalbottomsheet", "bottomsheetscaffold"
        ),
        TypeCode.CARD to setOf(
            "elevatedcard", "outlinedcard", "filledcard"
        ),
        TypeCode.CHECKBOX to setOf(
            "tristatecheckbox"
        ),
        TypeCode.SWITCH to setOf(
            "togglegroup"
        ),
        TypeCode.SLIDER to setOf(
            "discreteslider"
        )
    )
}
