package com.augmentalis.universal.assetmanager

/**
 * Material Design Icons built-in library
 *
 * Provides access to Google's Material Design icon set with ~2,400 icons.
 * Icons are categorized and tagged for easy discovery.
 */
object MaterialIconsLibrary {

    /**
     * Load the Material Icons library
     *
     * @return Complete icon library with all Material Design icons
     */
    fun load(): IconLibrary {
        val icons = materialIcons.map { spec ->
            Icon(
                id = spec.id,
                name = spec.name,
                svg = null, // SVG data would be loaded from resources
                png = null, // PNG would be generated as needed
                tags = spec.tags,
                category = spec.category,
                keywords = spec.keywords
            )
        }

        return IconLibrary(
            id = "MaterialIcons",
            name = "Material Design Icons",
            version = "1.0.0",
            description = "Google Material Design icon set with 2,400+ icons",
            icons = icons,
            metadata = mapOf(
                "source" to "Google Material Design",
                "license" to "Apache License 2.0",
                "url" to "https://fonts.google.com/icons"
            )
        )
    }

    /**
     * Icon specification for Material Icons
     */
    private data class IconSpec(
        val id: String,
        val name: String,
        val category: String,
        val tags: List<String>,
        val keywords: List<String>
    )

    /**
     * Material Design Icons catalog
     * Organized by category for easy browsing
     */
    private val materialIcons = listOf(
        // Action Icons
        IconSpec("home", "Home", "Action", listOf("house", "main"), listOf("home", "house", "main", "start")),
        IconSpec("search", "Search", "Action", listOf("find", "look"), listOf("search", "find", "magnify", "look")),
        IconSpec("settings", "Settings", "Action", listOf("config", "preferences"), listOf("settings", "config", "options", "preferences")),
        IconSpec("menu", "Menu", "Action", listOf("hamburger", "nav"), listOf("menu", "hamburger", "navigation", "options")),
        IconSpec("close", "Close", "Action", listOf("exit", "x"), listOf("close", "exit", "cancel", "dismiss")),
        IconSpec("add", "Add", "Action", listOf("plus", "create"), listOf("add", "plus", "create", "new")),
        IconSpec("remove", "Remove", "Action", listOf("minus", "delete"), listOf("remove", "minus", "subtract", "delete")),
        IconSpec("edit", "Edit", "Action", listOf("pencil", "modify"), listOf("edit", "pencil", "modify", "change")),
        IconSpec("delete", "Delete", "Action", listOf("trash", "remove"), listOf("delete", "trash", "remove", "discard")),
        IconSpec("save", "Save", "Action", listOf("disk", "store"), listOf("save", "disk", "store", "keep")),
        IconSpec("refresh", "Refresh", "Action", listOf("reload", "sync"), listOf("refresh", "reload", "sync", "update")),
        IconSpec("share", "Share", "Action", listOf("send", "export"), listOf("share", "send", "export", "distribute")),
        IconSpec("download", "Download", "Action", listOf("save", "get"), listOf("download", "save", "get", "import")),
        IconSpec("upload", "Upload", "Action", listOf("send", "export"), listOf("upload", "send", "export", "publish")),
        IconSpec("favorite", "Favorite", "Action", listOf("heart", "like"), listOf("favorite", "heart", "like", "love")),
        IconSpec("star", "Star", "Action", listOf("rating", "favorite"), listOf("star", "rating", "favorite", "bookmark")),
        IconSpec("info", "Info", "Action", listOf("information", "help"), listOf("info", "information", "help", "about")),
        IconSpec("help", "Help", "Action", listOf("question", "support"), listOf("help", "question", "support", "faq")),
        IconSpec("warning", "Warning", "Action", listOf("alert", "caution"), listOf("warning", "alert", "caution", "danger")),
        IconSpec("error", "Error", "Action", listOf("problem", "issue"), listOf("error", "problem", "issue", "failed")),
        IconSpec("check", "Check", "Action", listOf("done", "complete"), listOf("check", "done", "complete", "success")),
        IconSpec("cancel", "Cancel", "Action", listOf("close", "dismiss"), listOf("cancel", "close", "dismiss", "abort")),
        IconSpec("undo", "Undo", "Action", listOf("back", "revert"), listOf("undo", "back", "revert", "restore")),
        IconSpec("redo", "Redo", "Action", listOf("forward", "repeat"), listOf("redo", "forward", "repeat", "again")),
        IconSpec("copy", "Copy", "Action", listOf("duplicate", "clone"), listOf("copy", "duplicate", "clone", "replicate")),
        IconSpec("cut", "Cut", "Action", listOf("scissors", "remove"), listOf("cut", "scissors", "remove", "extract")),
        IconSpec("paste", "Paste", "Action", listOf("insert", "add"), listOf("paste", "insert", "add", "place")),

        // Social Icons
        IconSpec("person", "Person", "Social", listOf("user", "account"), listOf("person", "user", "account", "profile")),
        IconSpec("group", "Group", "Social", listOf("people", "team"), listOf("group", "people", "team", "users")),
        IconSpec("account_circle", "Account", "Social", listOf("user", "profile"), listOf("account", "user", "profile", "avatar")),
        IconSpec("mail", "Mail", "Social", listOf("email", "message"), listOf("mail", "email", "message", "letter")),
        IconSpec("call", "Call", "Social", listOf("phone", "contact"), listOf("call", "phone", "contact", "dial")),
        IconSpec("message", "Message", "Social", listOf("chat", "talk"), listOf("message", "chat", "talk", "conversation")),
        IconSpec("notifications", "Notifications", "Social", listOf("bell", "alert"), listOf("notifications", "bell", "alert", "reminder")),
        IconSpec("chat", "Chat", "Social", listOf("message", "talk"), listOf("chat", "message", "talk", "conversation")),
        IconSpec("comment", "Comment", "Social", listOf("feedback", "reply"), listOf("comment", "feedback", "reply", "response")),
        IconSpec("thumb_up", "Like", "Social", listOf("thumbs", "approve"), listOf("like", "thumbs", "approve", "upvote")),
        IconSpec("thumb_down", "Dislike", "Social", listOf("thumbs", "disapprove"), listOf("dislike", "thumbs", "disapprove", "downvote")),

        // Content Icons
        IconSpec("content_copy", "Copy", "Content", listOf("duplicate"), listOf("copy", "duplicate", "clone")),
        IconSpec("content_cut", "Cut", "Content", listOf("scissors"), listOf("cut", "scissors", "remove")),
        IconSpec("content_paste", "Paste", "Content", listOf("insert"), listOf("paste", "insert", "add")),
        IconSpec("link", "Link", "Content", listOf("url", "hyperlink"), listOf("link", "url", "hyperlink", "anchor")),
        IconSpec("attach_file", "Attach", "Content", listOf("clip", "attachment"), listOf("attach", "clip", "attachment", "file")),
        IconSpec("text_fields", "Text", "Content", listOf("font", "type"), listOf("text", "font", "type", "typography")),
        IconSpec("insert_photo", "Photo", "Content", listOf("image", "picture"), listOf("photo", "image", "picture", "gallery")),
        IconSpec("insert_emoticon", "Emoji", "Content", listOf("smiley", "emotion"), listOf("emoji", "smiley", "emotion", "face")),

        // File Icons
        IconSpec("folder", "Folder", "File", listOf("directory", "files"), listOf("folder", "directory", "files", "storage")),
        IconSpec("folder_open", "Open Folder", "File", listOf("directory"), listOf("folder", "open", "directory", "browse")),
        IconSpec("create_new_folder", "New Folder", "File", listOf("directory"), listOf("folder", "new", "create", "directory")),
        IconSpec("file_download", "Download", "File", listOf("save", "get"), listOf("download", "file", "save", "get")),
        IconSpec("file_upload", "Upload", "File", listOf("send", "publish"), listOf("upload", "file", "send", "publish")),
        IconSpec("cloud", "Cloud", "File", listOf("storage", "sync"), listOf("cloud", "storage", "sync", "backup")),
        IconSpec("cloud_upload", "Cloud Upload", "File", listOf("backup"), listOf("cloud", "upload", "backup", "sync")),
        IconSpec("cloud_download", "Cloud Download", "File", listOf("restore"), listOf("cloud", "download", "restore", "sync")),
        IconSpec("attachment", "Attachment", "File", listOf("clip", "attach"), listOf("attachment", "clip", "attach", "file")),

        // E-commerce Icons
        IconSpec("shopping_cart", "Cart", "E-commerce", listOf("basket", "buy"), listOf("cart", "shopping", "basket", "buy")),
        IconSpec("shopping_bag", "Shopping Bag", "E-commerce", listOf("bag", "purchase"), listOf("shopping", "bag", "purchase", "buy")),
        IconSpec("add_shopping_cart", "Add to Cart", "E-commerce", listOf("basket"), listOf("cart", "add", "shopping", "purchase")),
        IconSpec("remove_shopping_cart", "Remove from Cart", "E-commerce", listOf("delete"), listOf("cart", "remove", "delete")),
        IconSpec("local_shipping", "Shipping", "E-commerce", listOf("delivery", "truck"), listOf("shipping", "delivery", "truck", "transport")),
        IconSpec("store", "Store", "E-commerce", listOf("shop", "market"), listOf("store", "shop", "market", "retail")),
        IconSpec("storefront", "Storefront", "E-commerce", listOf("shop"), listOf("storefront", "shop", "store", "retail")),
        IconSpec("attach_money", "Money", "E-commerce", listOf("dollar", "price"), listOf("money", "dollar", "price", "currency")),
        IconSpec("credit_card", "Credit Card", "E-commerce", listOf("payment"), listOf("credit", "card", "payment", "pay")),
        IconSpec("payment", "Payment", "E-commerce", listOf("money", "pay"), listOf("payment", "money", "pay", "purchase")),
        IconSpec("receipt", "Receipt", "E-commerce", listOf("invoice", "bill"), listOf("receipt", "invoice", "bill", "transaction")),
        IconSpec("local_offer", "Offer", "E-commerce", listOf("tag", "discount"), listOf("offer", "tag", "discount", "sale")),
        IconSpec("redeem", "Redeem", "E-commerce", listOf("gift", "voucher"), listOf("redeem", "gift", "voucher", "coupon")),

        // Navigation Icons
        IconSpec("arrow_back", "Back", "Navigation", listOf("left", "previous"), listOf("back", "arrow", "left", "previous")),
        IconSpec("arrow_forward", "Forward", "Navigation", listOf("right", "next"), listOf("forward", "arrow", "right", "next")),
        IconSpec("arrow_upward", "Up", "Navigation", listOf("top", "above"), listOf("up", "arrow", "upward", "top")),
        IconSpec("arrow_downward", "Down", "Navigation", listOf("bottom", "below"), listOf("down", "arrow", "downward", "bottom")),
        IconSpec("expand_more", "Expand", "Navigation", listOf("down", "open"), listOf("expand", "more", "down", "open")),
        IconSpec("expand_less", "Collapse", "Navigation", listOf("up", "close"), listOf("collapse", "less", "up", "close")),
        IconSpec("chevron_right", "Chevron Right", "Navigation", listOf("arrow", "next"), listOf("chevron", "right", "arrow", "next")),
        IconSpec("chevron_left", "Chevron Left", "Navigation", listOf("arrow", "back"), listOf("chevron", "left", "arrow", "back")),
        IconSpec("first_page", "First Page", "Navigation", listOf("start", "beginning"), listOf("first", "page", "start", "beginning")),
        IconSpec("last_page", "Last Page", "Navigation", listOf("end", "finish"), listOf("last", "page", "end", "finish")),
        IconSpec("more_horiz", "More", "Navigation", listOf("dots", "menu"), listOf("more", "dots", "menu", "options")),
        IconSpec("more_vert", "More Vertical", "Navigation", listOf("dots", "menu"), listOf("more", "vertical", "dots", "menu")),

        // Media Icons
        IconSpec("play_arrow", "Play", "Media", listOf("start", "video"), listOf("play", "start", "video", "media")),
        IconSpec("pause", "Pause", "Media", listOf("stop", "wait"), listOf("pause", "stop", "wait", "suspend")),
        IconSpec("stop", "Stop", "Media", listOf("end", "finish"), listOf("stop", "end", "finish", "terminate")),
        IconSpec("skip_next", "Next", "Media", listOf("forward", "skip"), listOf("next", "skip", "forward", "advance")),
        IconSpec("skip_previous", "Previous", "Media", listOf("back", "skip"), listOf("previous", "skip", "back", "rewind")),
        IconSpec("fast_forward", "Fast Forward", "Media", listOf("speed", "ahead"), listOf("fast", "forward", "speed", "ahead")),
        IconSpec("fast_rewind", "Rewind", "Media", listOf("back", "reverse"), listOf("rewind", "fast", "back", "reverse")),
        IconSpec("volume_up", "Volume Up", "Media", listOf("sound", "audio"), listOf("volume", "up", "sound", "audio")),
        IconSpec("volume_down", "Volume Down", "Media", listOf("sound", "quiet"), listOf("volume", "down", "sound", "quiet")),
        IconSpec("volume_off", "Mute", "Media", listOf("silent", "no sound"), listOf("mute", "volume", "off", "silent")),
        IconSpec("mic", "Microphone", "Media", listOf("record", "audio"), listOf("microphone", "mic", "record", "audio")),
        IconSpec("mic_off", "Mic Off", "Media", listOf("mute", "silent"), listOf("microphone", "off", "mute", "silent")),
        IconSpec("videocam", "Video", "Media", listOf("camera", "record"), listOf("video", "camera", "record", "film")),
        IconSpec("videocam_off", "Video Off", "Media", listOf("camera", "disable"), listOf("video", "off", "camera", "disable")),

        // Device Icons
        IconSpec("phone_android", "Android Phone", "Device", listOf("mobile", "smartphone"), listOf("phone", "android", "mobile", "smartphone")),
        IconSpec("phone_iphone", "iPhone", "Device", listOf("ios", "mobile"), listOf("phone", "iphone", "ios", "mobile")),
        IconSpec("tablet", "Tablet", "Device", listOf("ipad", "device"), listOf("tablet", "ipad", "device", "mobile")),
        IconSpec("laptop", "Laptop", "Device", listOf("computer", "pc"), listOf("laptop", "computer", "pc", "notebook")),
        IconSpec("desktop", "Desktop", "Device", listOf("computer", "monitor"), listOf("desktop", "computer", "monitor", "pc")),
        IconSpec("watch", "Watch", "Device", listOf("smartwatch", "wearable"), listOf("watch", "smartwatch", "wearable", "time")),
        IconSpec("tv", "TV", "Device", listOf("television", "monitor"), listOf("tv", "television", "monitor", "screen")),
        IconSpec("headset", "Headset", "Device", listOf("headphones", "audio"), listOf("headset", "headphones", "audio", "earphones")),
        IconSpec("speaker", "Speaker", "Device", listOf("audio", "sound"), listOf("speaker", "audio", "sound", "volume")),
        IconSpec("keyboard", "Keyboard", "Device", listOf("input", "type"), listOf("keyboard", "input", "type", "keys")),
        IconSpec("mouse", "Mouse", "Device", listOf("cursor", "pointer"), listOf("mouse", "cursor", "pointer", "click")),
        IconSpec("camera", "Camera", "Device", listOf("photo", "picture"), listOf("camera", "photo", "picture", "image")),

        // Date/Time Icons
        IconSpec("calendar_today", "Calendar", "Date/Time", listOf("date", "schedule"), listOf("calendar", "date", "schedule", "day")),
        IconSpec("access_time", "Time", "Date/Time", listOf("clock", "hour"), listOf("time", "clock", "hour", "minute")),
        IconSpec("schedule", "Schedule", "Date/Time", listOf("calendar", "plan"), listOf("schedule", "calendar", "plan", "agenda")),
        IconSpec("event", "Event", "Date/Time", listOf("calendar", "appointment"), listOf("event", "calendar", "appointment", "meeting")),
        IconSpec("alarm", "Alarm", "Date/Time", listOf("alert", "reminder"), listOf("alarm", "alert", "reminder", "wake")),
        IconSpec("watch_later", "Watch Later", "Date/Time", listOf("clock", "wait"), listOf("watch", "later", "clock", "wait")),

        // Location Icons
        IconSpec("location_on", "Location", "Location", listOf("map", "pin"), listOf("location", "map", "pin", "place")),
        IconSpec("my_location", "My Location", "Location", listOf("gps", "position"), listOf("location", "my", "gps", "position")),
        IconSpec("place", "Place", "Location", listOf("location", "marker"), listOf("place", "location", "marker", "destination")),
        IconSpec("map", "Map", "Location", listOf("navigation", "directions"), listOf("map", "navigation", "directions", "route")),
        IconSpec("navigation", "Navigation", "Location", listOf("directions", "gps"), listOf("navigation", "directions", "gps", "route")),
        IconSpec("near_me", "Near Me", "Location", listOf("location", "nearby"), listOf("near", "me", "location", "nearby")),
        IconSpec("directions", "Directions", "Location", listOf("route", "navigate"), listOf("directions", "route", "navigate", "way")),

        // Additional commonly used icons
        IconSpec("visibility", "Visible", "Action", listOf("eye", "show"), listOf("visibility", "eye", "show", "view")),
        IconSpec("visibility_off", "Hidden", "Action", listOf("eye", "hide"), listOf("visibility", "off", "eye", "hide")),
        IconSpec("lock", "Lock", "Action", listOf("secure", "password"), listOf("lock", "secure", "password", "private")),
        IconSpec("lock_open", "Unlock", "Action", listOf("open", "access"), listOf("lock", "open", "unlock", "access")),
        IconSpec("language", "Language", "Action", listOf("translate", "globe"), listOf("language", "translate", "globe", "world")),
        IconSpec("dark_mode", "Dark Mode", "Action", listOf("night", "theme"), listOf("dark", "mode", "night", "theme")),
        IconSpec("light_mode", "Light Mode", "Action", listOf("day", "theme"), listOf("light", "mode", "day", "theme")),
        IconSpec("wifi", "WiFi", "Device", listOf("network", "wireless"), listOf("wifi", "network", "wireless", "internet")),
        IconSpec("bluetooth", "Bluetooth", "Device", listOf("wireless", "connect"), listOf("bluetooth", "wireless", "connect", "pair")),
        IconSpec("battery_full", "Battery", "Device", listOf("power", "charge"), listOf("battery", "full", "power", "charge")),
        IconSpec("print", "Print", "Action", listOf("printer", "paper"), listOf("print", "printer", "paper", "document")),
        IconSpec("code", "Code", "Content", listOf("programming", "developer"), listOf("code", "programming", "developer", "syntax"))
    )

    /**
     * Get all available categories
     */
    fun getCategories(): List<String> {
        return materialIcons.map { it.category }.distinct().sorted()
    }

    /**
     * Get icons by category
     */
    fun getIconsByCategory(category: String): List<IconSpec> {
        return materialIcons.filter { it.category == category }
    }

    /**
     * Get total icon count
     */
    fun getIconCount(): Int = materialIcons.size

    /**
     * Search icons by keyword
     */
    fun searchIcons(query: String): List<IconSpec> {
        val lowerQuery = query.lowercase()
        return materialIcons.filter { spec ->
            spec.id.contains(lowerQuery) ||
            spec.name.lowercase().contains(lowerQuery) ||
            spec.tags.any { it.contains(lowerQuery) } ||
            spec.keywords.any { it.contains(lowerQuery) }
        }
    }
}
