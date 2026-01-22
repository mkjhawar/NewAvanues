/**
 * TypeCode.kt - Type codes for semantic AVID categorization
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-01-13
 */
package com.augmentalis.avid

/**
 * 3-character type codes for AVID categorization
 *
 * Used for semantic grouping and filtering of AVIDs by entity type.
 */
object TypeCode {
    // UI Elements
    const val BUTTON = "BTN"
    const val INPUT = "INP"
    const val TEXT = "TXT"
    const val IMAGE = "IMG"
    const val SCROLL = "SCR"
    const val CARD = "CRD"
    const val LIST = "LST"
    const val ITEM = "ITM"
    const val MENU = "MNU"
    const val DIALOG = "DLG"
    const val CHECKBOX = "CHK"
    const val SWITCH = "SWT"
    const val SLIDER = "SLD"
    const val TAB = "TAB"
    const val LAYOUT = "LAY"

    // AVA Entities
    const val MESSAGE = "MSG"
    const val CONVERSATION = "CNV"
    const val DOCUMENT = "DOC"
    const val CHUNK = "CHU"
    const val MEMORY = "MEM"
    const val DECISION = "DEC"
    const val LEARNING = "LRN"
    const val INTENT = "INT"
    const val CLUSTER = "CLS"
    const val BOOKMARK = "BMK"
    const val ANNOTATION = "ANN"
    const val FILTER = "FLT"
    const val UTTERANCE = "UTT"

    // WebAvanue Entities
    const val FAVORITE = "FAV"
    const val DOWNLOAD = "DWN"
    const val HISTORY = "HST"
    const val SESSION = "SES"
    const val GROUP = "GRP"

    // System Entities
    const val REQUEST = "REQ"
    const val WINDOW = "WIN"
    const val STREAM = "STR"
    const val PRESET = "PRE"
    const val DEVICE = "DEV"
    const val SYNC = "SYN"
    const val ELEMENT = "ELM"
    const val COMMAND = "CMD"
    const val SCREEN = "SCN"
    const val APP = "APP"

    /**
     * Infer type code from class name or type string
     */
    fun fromTypeName(typeName: String): String {
        return when (typeName.lowercase()) {
            "button", "imagebutton", "floatingactionbutton" -> BUTTON
            "input", "edittext", "textfield", "textinput" -> INPUT
            "text", "textview", "label" -> TEXT
            "image", "imageview", "icon" -> IMAGE
            "scroll", "scrollview", "recyclerview", "listview" -> SCROLL
            "card", "cardview" -> CARD
            "list", "recycler" -> LIST
            "item", "listitem", "viewholder" -> ITEM
            "menu", "menuitem", "popupmenu" -> MENU
            "dialog", "modal", "alertdialog", "bottomsheet" -> DIALOG
            "checkbox", "checkboxpreference" -> CHECKBOX
            "switch", "toggle", "switchcompat" -> SWITCH
            "slider", "seekbar" -> SLIDER
            "tab", "browsertab" -> TAB
            "layout", "container", "view", "viewgroup" -> LAYOUT
            "message", "chatmessage" -> MESSAGE
            "conversation", "chat", "thread" -> CONVERSATION
            "document", "file" -> DOCUMENT
            "chunk", "segment" -> CHUNK
            "memory", "context" -> MEMORY
            "decision" -> DECISION
            "learning", "trained" -> LEARNING
            "intent", "action" -> INTENT
            "cluster" -> CLUSTER
            "bookmark" -> BOOKMARK
            "annotation" -> ANNOTATION
            "filter", "filterpreset" -> FILTER
            "utterance" -> UTTERANCE
            "favorite" -> FAVORITE
            "download" -> DOWNLOAD
            "history", "historyentry" -> HISTORY
            "session", "browsersession" -> SESSION
            "group", "tabgroup" -> GROUP
            "request", "requestid" -> REQUEST
            "window", "windowid" -> WINDOW
            "stream", "streamid" -> STREAM
            "preset", "layoutpreset" -> PRESET
            "device", "deviceid" -> DEVICE
            "sync", "syncrequest" -> SYNC
            "command" -> COMMAND
            "screen" -> SCREEN
            "app", "application" -> APP
            else -> ELEMENT
        }
    }
}
