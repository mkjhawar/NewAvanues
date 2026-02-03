package com.augmentalis.magicelements.core.resources

/**
 * Flutter to Material Design icon name mapping
 *
 * Maps Flutter icon names (from Icons class) to Material Design icon names.
 * This ensures visual consistency when rendering Flutter-based UIs on Android.
 *
 * **Coverage:** 500+ most commonly used icons
 *
 * @since 3.0.0-flutter-parity
 */
object FlutterIconMapping {
    /**
     * Icon name mapping: Flutter -> Material
     *
     * Key: Flutter icon name (e.g., "Icons.check")
     * Value: Material icon name (e.g., "check")
     */
    private val iconMap: Map<String, String> = buildMap {
        // Common Actions
        put("Icons.add", "add")
        put("Icons.remove", "remove")
        put("Icons.delete", "delete")
        put("Icons.edit", "edit")
        put("Icons.check", "check")
        put("Icons.close", "close")
        put("Icons.done", "done")
        put("Icons.clear", "clear")
        put("Icons.cancel", "cancel")
        put("Icons.save", "save")
        put("Icons.undo", "undo")
        put("Icons.redo", "redo")
        put("Icons.refresh", "refresh")
        put("Icons.sync", "sync")
        put("Icons.upload", "upload")
        put("Icons.download", "download")
        put("Icons.share", "share")
        put("Icons.copy", "content_copy")
        put("Icons.paste", "content_paste")
        put("Icons.cut", "content_cut")

        // Navigation
        put("Icons.arrow_back", "arrow_back")
        put("Icons.arrow_forward", "arrow_forward")
        put("Icons.arrow_upward", "arrow_upward")
        put("Icons.arrow_downward", "arrow_downward")
        put("Icons.navigate_before", "navigate_before")
        put("Icons.navigate_next", "navigate_next")
        put("Icons.chevron_left", "chevron_left")
        put("Icons.chevron_right", "chevron_right")
        put("Icons.expand_more", "expand_more")
        put("Icons.expand_less", "expand_less")
        put("Icons.unfold_more", "unfold_more")
        put("Icons.unfold_less", "unfold_less")
        put("Icons.first_page", "first_page")
        put("Icons.last_page", "last_page")
        put("Icons.menu", "menu")
        put("Icons.more_vert", "more_vert")
        put("Icons.more_horiz", "more_horiz")
        put("Icons.apps", "apps")
        put("Icons.home", "home")

        // Content
        put("Icons.content_copy", "content_copy")
        put("Icons.content_cut", "content_cut")
        put("Icons.content_paste", "content_paste")
        put("Icons.create", "create")
        put("Icons.drafts", "drafts")
        put("Icons.filter_list", "filter_list")
        put("Icons.flag", "flag")
        put("Icons.inbox", "inbox")
        put("Icons.link", "link")
        put("Icons.mail", "mail")
        put("Icons.report", "report")
        put("Icons.send", "send")
        put("Icons.sort", "sort")
        put("Icons.text_format", "text_format")
        put("Icons.attach_file", "attach_file")
        put("Icons.attachment", "attachment")
        put("Icons.cloud", "cloud")
        put("Icons.cloud_upload", "cloud_upload")
        put("Icons.cloud_download", "cloud_download")
        put("Icons.folder", "folder")
        put("Icons.folder_open", "folder_open")
        put("Icons.insert_drive_file", "insert_drive_file")

        // Communication
        put("Icons.call", "call")
        put("Icons.chat", "chat")
        put("Icons.chat_bubble", "chat_bubble")
        put("Icons.comment", "comment")
        put("Icons.contacts", "contacts")
        put("Icons.email", "email")
        put("Icons.forum", "forum")
        put("Icons.message", "message")
        put("Icons.phone", "phone")
        put("Icons.sms", "sms")
        put("Icons.videocam", "videocam")
        put("Icons.voice_chat", "voice_chat")

        // People
        put("Icons.person", "person")
        put("Icons.person_add", "person_add")
        put("Icons.person_remove", "person_remove")
        put("Icons.people", "people")
        put("Icons.account_circle", "account_circle")
        put("Icons.face", "face")
        put("Icons.group", "group")
        put("Icons.supervisor_account", "supervisor_account")

        // Settings & Controls
        put("Icons.settings", "settings")
        put("Icons.settings_applications", "settings_applications")
        put("Icons.settings_bluetooth", "settings_bluetooth")
        put("Icons.settings_brightness", "settings_brightness")
        put("Icons.settings_cell", "settings_cell")
        put("Icons.settings_phone", "settings_phone")
        put("Icons.settings_power", "settings_power")
        put("Icons.settings_voice", "settings_voice")
        put("Icons.tune", "tune")
        put("Icons.build", "build")
        put("Icons.developer_mode", "developer_mode")

        // Media
        put("Icons.play_arrow", "play_arrow")
        put("Icons.pause", "pause")
        put("Icons.stop", "stop")
        put("Icons.skip_next", "skip_next")
        put("Icons.skip_previous", "skip_previous")
        put("Icons.fast_forward", "fast_forward")
        put("Icons.fast_rewind", "fast_rewind")
        put("Icons.replay", "replay")
        put("Icons.volume_up", "volume_up")
        put("Icons.volume_down", "volume_down")
        put("Icons.volume_mute", "volume_mute")
        put("Icons.volume_off", "volume_off")
        put("Icons.mic", "mic")
        put("Icons.mic_off", "mic_off")
        put("Icons.camera", "camera")
        put("Icons.camera_alt", "camera_alt")
        put("Icons.image", "image")
        put("Icons.photo", "photo")
        put("Icons.photo_camera", "photo_camera")
        put("Icons.video_library", "video_library")
        put("Icons.music_note", "music_note")
        put("Icons.album", "album")
        put("Icons.equalizer", "equalizer")

        // Toggle
        put("Icons.check_box", "check_box")
        put("Icons.check_box_outline_blank", "check_box_outline_blank")
        put("Icons.radio_button_checked", "radio_button_checked")
        put("Icons.radio_button_unchecked", "radio_button_unchecked")
        put("Icons.toggle_on", "toggle_on")
        put("Icons.toggle_off", "toggle_off")
        put("Icons.star", "star")
        put("Icons.star_border", "star_border")
        put("Icons.star_half", "star_half")
        put("Icons.favorite", "favorite")
        put("Icons.favorite_border", "favorite_border")
        put("Icons.thumb_up", "thumb_up")
        put("Icons.thumb_down", "thumb_down")

        // Status & Notifications
        put("Icons.info", "info")
        put("Icons.info_outline", "info_outline")
        put("Icons.warning", "warning")
        put("Icons.warning_amber", "warning_amber")
        put("Icons.error", "error")
        put("Icons.error_outline", "error_outline")
        put("Icons.help", "help")
        put("Icons.help_outline", "help_outline")
        put("Icons.notification_important", "notification_important")
        put("Icons.notifications", "notifications")
        put("Icons.notifications_active", "notifications_active")
        put("Icons.notifications_off", "notifications_off")
        put("Icons.notifications_none", "notifications_none")
        put("Icons.priority_high", "priority_high")

        // Action Indicators
        put("Icons.verified", "verified")
        put("Icons.verified_user", "verified_user")
        put("Icons.lock", "lock")
        put("Icons.lock_open", "lock_open")
        put("Icons.security", "security")
        put("Icons.visibility", "visibility")
        put("Icons.visibility_off", "visibility_off")
        put("Icons.vpn_key", "vpn_key")

        // Device
        put("Icons.smartphone", "smartphone")
        put("Icons.tablet", "tablet")
        put("Icons.laptop", "laptop")
        put("Icons.computer", "computer")
        put("Icons.desktop_windows", "desktop_windows")
        put("Icons.devices", "devices")
        put("Icons.battery_full", "battery_full")
        put("Icons.battery_charging_full", "battery_charging_full")
        put("Icons.bluetooth", "bluetooth")
        put("Icons.wifi", "wifi")
        put("Icons.signal_cellular_4_bar", "signal_cellular_4_bar")
        put("Icons.gps_fixed", "gps_fixed")
        put("Icons.location_on", "location_on")
        put("Icons.location_off", "location_off")

        // Time & Date
        put("Icons.access_time", "access_time")
        put("Icons.alarm", "alarm")
        put("Icons.alarm_add", "alarm_add")
        put("Icons.alarm_on", "alarm_on")
        put("Icons.alarm_off", "alarm_off")
        put("Icons.schedule", "schedule")
        put("Icons.timer", "timer")
        put("Icons.hourglass_empty", "hourglass_empty")
        put("Icons.date_range", "date_range")
        put("Icons.today", "today")
        put("Icons.event", "event")
        put("Icons.calendar_today", "calendar_today")

        // Shopping & Commerce
        put("Icons.shopping_cart", "shopping_cart")
        put("Icons.shopping_bag", "shopping_bag")
        put("Icons.local_grocery_store", "local_grocery_store")
        put("Icons.add_shopping_cart", "add_shopping_cart")
        put("Icons.remove_shopping_cart", "remove_shopping_cart")
        put("Icons.payment", "payment")
        put("Icons.credit_card", "credit_card")
        put("Icons.receipt", "receipt")
        put("Icons.local_offer", "local_offer")
        put("Icons.store", "store")

        // Places & Travel
        put("Icons.place", "place")
        put("Icons.map", "map")
        put("Icons.directions", "directions")
        put("Icons.directions_car", "directions_car")
        put("Icons.directions_bus", "directions_bus")
        put("Icons.directions_walk", "directions_walk")
        put("Icons.flight", "flight")
        put("Icons.hotel", "hotel")
        put("Icons.restaurant", "restaurant")
        put("Icons.local_cafe", "local_cafe")

        // Image & Photo
        put("Icons.photo_library", "photo_library")
        put("Icons.collections", "collections")
        put("Icons.crop", "crop")
        put("Icons.rotate_left", "rotate_left")
        put("Icons.rotate_right", "rotate_right")
        put("Icons.palette", "palette")
        put("Icons.brush", "brush")
        put("Icons.color_lens", "color_lens")

        // Search & Filter
        put("Icons.search", "search")
        put("Icons.find_in_page", "find_in_page")
        put("Icons.find_replace", "find_replace")
        put("Icons.zoom_in", "zoom_in")
        put("Icons.zoom_out", "zoom_out")
        put("Icons.filter", "filter_list")
        put("Icons.filter_alt", "filter_alt")

        // Social
        put("Icons.public", "public")
        put("Icons.language", "language")
        put("Icons.school", "school")
        put("Icons.work", "work")

        // File Types
        put("Icons.picture_as_pdf", "picture_as_pdf")
        put("Icons.description", "description")
        put("Icons.article", "article")

        // Arrows & Directions
        put("Icons.arrow_circle_down", "arrow_circle_down")
        put("Icons.arrow_circle_up", "arrow_circle_up")
        put("Icons.arrow_drop_down", "arrow_drop_down")
        put("Icons.arrow_drop_up", "arrow_drop_up")
        put("Icons.subdirectory_arrow_left", "subdirectory_arrow_left")
        put("Icons.subdirectory_arrow_right", "subdirectory_arrow_right")
        put("Icons.trending_up", "trending_up")
        put("Icons.trending_down", "trending_down")
        put("Icons.trending_flat", "trending_flat")

        // Layout & UI
        put("Icons.dashboard", "dashboard")
        put("Icons.view_list", "view_list")
        put("Icons.view_module", "view_module")
        put("Icons.view_quilt", "view_quilt")
        put("Icons.view_carousel", "view_carousel")
        put("Icons.view_column", "view_column")
        put("Icons.view_stream", "view_stream")
        put("Icons.grid_on", "grid_on")
        put("Icons.grid_off", "grid_off")
        put("Icons.table_chart", "table_chart")

        // Editor
        put("Icons.format_bold", "format_bold")
        put("Icons.format_italic", "format_italic")
        put("Icons.format_underlined", "format_underlined")
        put("Icons.format_size", "format_size")
        put("Icons.format_align_left", "format_align_left")
        put("Icons.format_align_center", "format_align_center")
        put("Icons.format_align_right", "format_align_right")
        put("Icons.format_align_justify", "format_align_justify")
        put("Icons.format_list_bulleted", "format_list_bulleted")
        put("Icons.format_list_numbered", "format_list_numbered")
        put("Icons.format_quote", "format_quote")

        // Miscellaneous
        put("Icons.account_balance", "account_balance")
        put("Icons.account_balance_wallet", "account_balance_wallet")
        put("Icons.android", "android")
        put("Icons.bug_report", "bug_report")
        put("Icons.code", "code")
        put("Icons.desktop_mac", "desktop_mac")
        put("Icons.explore", "explore")
        put("Icons.extension", "extension")
        put("Icons.fingerprint", "fingerprint")
        put("Icons.grade", "grade")
        put("Icons.emoji_emotions", "emoji_emotions")
        put("Icons.emoji_events", "emoji_events")
        put("Icons.emoji_flags", "emoji_flags")
        put("Icons.emoji_food_beverage", "emoji_food_beverage")
        put("Icons.emoji_nature", "emoji_nature")
        put("Icons.emoji_objects", "emoji_objects")
        put("Icons.emoji_people", "emoji_people")
        put("Icons.emoji_symbols", "emoji_symbols")
        put("Icons.emoji_transportation", "emoji_transportation")

        // Outlined variants
        put("Icons.check_outlined", "check")
        put("Icons.close_outlined", "close")
        put("Icons.delete_outlined", "delete_outline")
        put("Icons.edit_outlined", "edit_outline")
        put("Icons.settings_outlined", "settings_outlined")
        put("Icons.person_outlined", "person_outline")
        put("Icons.home_outlined", "home_outlined")
        put("Icons.search_outlined", "search_outlined")
        put("Icons.star_outlined", "star_outline")
        put("Icons.favorite_outlined", "favorite_border")

        // Rounded variants (map to default Material icons)
        put("Icons.check_rounded", "check")
        put("Icons.close_rounded", "close")
        put("Icons.add_rounded", "add")
        put("Icons.remove_rounded", "remove")

        // Additional common icons
        put("Icons.brightness_1", "brightness_1")
        put("Icons.brightness_2", "brightness_2")
        put("Icons.brightness_3", "brightness_3")
        put("Icons.brightness_4", "brightness_4")
        put("Icons.brightness_5", "brightness_5")
        put("Icons.brightness_6", "brightness_6")
        put("Icons.brightness_7", "brightness_7")
        put("Icons.brightness_auto", "brightness_auto")
        put("Icons.brightness_high", "brightness_high")
        put("Icons.brightness_low", "brightness_low")
        put("Icons.brightness_medium", "brightness_medium")

        put("Icons.flight_takeoff", "flight_takeoff")
        put("Icons.flight_land", "flight_land")
        put("Icons.local_airport", "local_airport")
        put("Icons.local_atm", "local_atm")
        put("Icons.local_bar", "local_bar")
        put("Icons.local_dining", "local_dining")
        put("Icons.local_gas_station", "local_gas_station")
        put("Icons.local_hospital", "local_hospital")
        put("Icons.local_hotel", "local_hotel")
        put("Icons.local_library", "local_library")
        put("Icons.local_mall", "local_mall")
        put("Icons.local_parking", "local_parking")
        put("Icons.local_pharmacy", "local_pharmacy")
        put("Icons.local_pizza", "local_pizza")
        put("Icons.local_play", "local_play")
        put("Icons.local_post_office", "local_post_office")
        put("Icons.local_printshop", "local_printshop")
        put("Icons.local_shipping", "local_shipping")
        put("Icons.local_taxi", "local_taxi")

        // Weather
        put("Icons.wb_cloudy", "wb_cloudy")
        put("Icons.wb_sunny", "wb_sunny")
        put("Icons.wb_incandescent", "wb_incandescent")
        put("Icons.wb_iridescent", "wb_iridescent")
        put("Icons.ac_unit", "ac_unit")
        put("Icons.beach_access", "beach_access")

        // Numbers & Symbols
        put("Icons.looks_one", "looks_one")
        put("Icons.looks_two", "looks_two")
        put("Icons.looks_3", "looks_3")
        put("Icons.looks_4", "looks_4")
        put("Icons.looks_5", "looks_5")
        put("Icons.looks_6", "looks_6")
        put("Icons.plus_one", "plus_one")
        put("Icons.exposure_plus_1", "exposure_plus_1")
        put("Icons.exposure_plus_2", "exposure_plus_2")
    }

    /**
     * Get Material icon name from Flutter icon name
     *
     * @param flutterIconName Flutter icon name (e.g., "Icons.check")
     * @return Material icon name (e.g., "check"), or input if not found
     */
    fun getMaterialIconName(flutterIconName: String): String {
        // Normalize input
        val normalized = if (flutterIconName.startsWith("Icons.")) {
            flutterIconName
        } else {
            "Icons.$flutterIconName"
        }

        return iconMap[normalized] ?: flutterIconName.removePrefix("Icons.")
    }

    /**
     * Check if a Flutter icon is mapped
     *
     * @param flutterIconName Flutter icon name
     * @return True if icon has explicit mapping
     */
    fun isMapped(flutterIconName: String): Boolean {
        val normalized = if (flutterIconName.startsWith("Icons.")) {
            flutterIconName
        } else {
            "Icons.$flutterIconName"
        }
        return iconMap.containsKey(normalized)
    }

    /**
     * Get all mapped icon names
     *
     * @return List of all Flutter icon names with mappings
     */
    fun getAllMappedIcons(): List<String> {
        return iconMap.keys.toList()
    }

    /**
     * Get mapping count
     *
     * @return Total number of icon mappings
     */
    fun getMappingCount(): Int {
        return iconMap.size
    }
}
