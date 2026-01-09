package com.augmentalis.voiceoscoreng.common

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
    /** Patterns mapped to type codes */
    val patterns: Map<VUIDTypeCode, Set<String>>
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
     * Get the VUID type code for a class name.
     * Checks providers in priority order (highest first).
     */
    fun getTypeCode(className: String): VUIDTypeCode {
        val normalized = className.trim().lowercase()
        if (normalized.isEmpty()) return VUIDTypeCode.ELEMENT

        for (provider in providers) {
            for ((typeCode, patternSet) in provider.patterns) {
                if (patternSet.any { normalized.contains(it) }) {
                    return typeCode
                }
            }
        }
        return VUIDTypeCode.ELEMENT
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
        VUIDTypeCode.BUTTON to setOf(
            "button", "appcompatbutton", "materialbutton", "imagebutton",
            "floatingactionbutton", "fab", "extendedfloatingactionbutton"
        ),
        VUIDTypeCode.INPUT to setOf(
            "edittext", "textinputedittext", "autocompletetextview", "searchview",
            "textfield", "textinputlayout", "searchbar"
        ),
        VUIDTypeCode.SCROLL to setOf(
            "scrollview", "horizontalscrollview", "nestedscrollview", "recyclerview",
            "listview", "gridview", "viewpager", "viewpager2"
        ),
        VUIDTypeCode.TEXT to setOf(
            "textview", "appcompattextview", "materialtextview", "checkedtextview"
        ),
        VUIDTypeCode.CARD to setOf(
            "cardview", "materialcardview", "card"
        ),
        VUIDTypeCode.LAYOUT to setOf(
            "linearlayout", "relativelayout", "framelayout", "constraintlayout",
            "coordinatorlayout", "appbarlayout", "collapsingtoolbarlayout",
            "toolbar", "actionbar"
        ),
        VUIDTypeCode.MENU to setOf(
            "menu", "popupmenu", "contextmenu", "dropdownmenu", "navigationmenu",
            "overflowmenu", "optionsmenu", "spinner", "bottomnavigation",
            "bottomnavigationview"
        ),
        VUIDTypeCode.DIALOG to setOf(
            "dialog", "alertdialog", "bottomsheetdialog", "dialogfragment",
            "datepickerdialog", "timepickerdialog"
        ),
        VUIDTypeCode.IMAGE to setOf(
            "imageview", "appcompatimageview", "image", "icon",
            "shapeableimageview", "circleimageview"
        ),
        VUIDTypeCode.CHECKBOX to setOf(
            "checkbox", "appcompatcheckbox", "materialcheckbox"
        ),
        VUIDTypeCode.SWITCH to setOf(
            "switch", "switchcompat", "switchmaterial", "togglebutton"
        ),
        VUIDTypeCode.SLIDER to setOf(
            "slider", "seekbar", "rangeslider"
        ),
        VUIDTypeCode.TAB to setOf(
            "tablayout", "tabitem"
        ),
        VUIDTypeCode.LIST to setOf(
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
        VUIDTypeCode.BUTTON to setOf(
            // M3 Button variants
            "iconbutton", "icontogglebutton", "filledbutton", "filledtonalbutton",
            "outlinedbutton", "elevatedbutton", "textbutton", "segmentedbutton",
            "smallfloatingactionbutton", "largefloatingactionbutton",
            // Chips (act as buttons)
            "chip", "filterchip", "inputchip", "assistchip", "suggestionchip",
            "elevatedfilterchip", "elevatedassistchip", "elevatedsuggestionchip"
        ),
        VUIDTypeCode.INPUT to setOf(
            "outlinedtextfield", "basictextfield", "decoratedtextfield",
            "outlinedsearchbar", "dockedsearchbar"
        ),
        VUIDTypeCode.SCROLL to setOf(
            "lazycolumn", "lazyrow", "lazyverticalgrid", "lazyhorizontalgrid",
            "lazystaggeredgrid"
        ),
        VUIDTypeCode.LAYOUT to setOf(
            "row", "column", "box", "surface", "scaffold",
            "topappbar", "centeralignedtopappbar", "mediumtopappbar", "largetopappbar",
            "bottomappbar"
        ),
        VUIDTypeCode.MENU to setOf(
            "navigationbar", "navigationrail", "navigationdrawer",
            "navigationbaritem", "navigationrailitem", "navigationdraweritem",
            "exposeddropdownmenu", "exposeddropdownmenubox", "dropdownmenuitem"
        ),
        VUIDTypeCode.TAB to setOf(
            "tabrow", "scrollabletabrow", "primarytabrow", "secondarytabrow",
            "tab", "leadingicontab", "texttab"
        ),
        VUIDTypeCode.DIALOG to setOf(
            "bottomsheet", "modalbottomsheet", "bottomsheetscaffold"
        ),
        VUIDTypeCode.CARD to setOf(
            "elevatedcard", "outlinedcard", "filledcard"
        ),
        VUIDTypeCode.CHECKBOX to setOf(
            "tristatecheckbox"
        ),
        VUIDTypeCode.SWITCH to setOf(
            "togglegroup"
        ),
        VUIDTypeCode.SLIDER to setOf(
            "discreteslider"
        )
    )
}
