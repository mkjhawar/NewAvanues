package com.augmentalis.avanueui.renderer.android

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * IconResolver - Centralized icon resolution for all mappers
 *
 * Supports 100+ Material Icons with multiple naming conventions:
 * - snake_case: "arrow_back"
 * - camelCase: "arrowBack"
 * - lowercase: "arrowback"
 */
object IconResolver {

    /**
     * Resolve icon name to ImageVector
     * @param name Icon name in any common format
     * @param style Icon style (filled or outlined)
     * @return ImageVector for the icon, defaults to Info if not found
     */
    fun resolve(name: String?, style: IconStyle = IconStyle.FILLED): ImageVector {
        if (name.isNullOrBlank()) return Icons.Default.Info

        val normalizedName = name.lowercase().replace("_", "").replace("-", "")

        return when (style) {
            IconStyle.FILLED -> getFilledIcon(normalizedName)
            IconStyle.OUTLINED -> getOutlinedIcon(normalizedName)
        }
    }

    private fun getFilledIcon(name: String): ImageVector {
        return when (name) {
            // Navigation
            "home" -> Icons.Filled.Home
            "menu" -> Icons.Filled.Menu
            "close" -> Icons.Filled.Close
            "arrowback" -> Icons.Filled.ArrowBack
            "arrowforward" -> Icons.Filled.ArrowForward
            "arrowdropdown" -> Icons.Filled.ArrowDropDown
            "arrowdropup" -> Icons.Filled.ArrowDropUp
            "chevronleft" -> Icons.Filled.ChevronLeft
            "chevronright" -> Icons.Filled.ChevronRight
            "expandmore" -> Icons.Filled.ExpandMore
            "expandless" -> Icons.Filled.ExpandLess
            "morevert" -> Icons.Filled.MoreVert
            "morehoriz" -> Icons.Filled.MoreHoriz
            "apps" -> Icons.Filled.Apps
            "refresh" -> Icons.Filled.Refresh
            "fullscreen" -> Icons.Filled.Fullscreen
            "fullscreenexit" -> Icons.Filled.FullscreenExit

            // Actions
            "add" -> Icons.Filled.Add
            "remove" -> Icons.Filled.Remove
            "check" -> Icons.Filled.Check
            "clear" -> Icons.Filled.Clear
            "delete" -> Icons.Filled.Delete
            "edit" -> Icons.Filled.Edit
            "save" -> Icons.Filled.Save
            "done" -> Icons.Filled.Done
            "search" -> Icons.Filled.Search
            "settings" -> Icons.Filled.Settings
            "build" -> Icons.Filled.Build
            "lock" -> Icons.Filled.Lock
            "lockopen" -> Icons.Filled.LockOpen
            "visibility" -> Icons.Filled.Visibility
            "visibilityoff" -> Icons.Filled.VisibilityOff
            "zoomin" -> Icons.Filled.ZoomIn
            "zoomout" -> Icons.Filled.ZoomOut
            "filterlist" -> Icons.Filled.FilterList
            "sort" -> Icons.Filled.Sort
            "print" -> Icons.Filled.Print
            "share" -> Icons.Filled.Share
            "send" -> Icons.Filled.Send
            "upload" -> Icons.Filled.Upload
            "download" -> Icons.Filled.Download
            "attachfile" -> Icons.Filled.AttachFile
            "link" -> Icons.Filled.Link
            "code" -> Icons.Filled.Code
            "contentcopy" -> Icons.Filled.ContentCopy
            "contentpaste" -> Icons.Filled.ContentPaste
            "undo" -> Icons.Filled.Undo
            "redo" -> Icons.Filled.Redo
            "selectall" -> Icons.Filled.SelectAll

            // Content
            "create" -> Icons.Filled.Create
            "folder" -> Icons.Filled.Folder
            "folderopen" -> Icons.Filled.FolderOpen
            "insertdrivefile" -> Icons.Filled.InsertDriveFile
            "description" -> Icons.Filled.Description
            "image" -> Icons.Filled.Image
            "movie" -> Icons.Filled.Movie
            "audiotrack" -> Icons.Filled.Audiotrack

            // Communication
            "email" -> Icons.Filled.Email
            "mail" -> Icons.Filled.Mail
            "phone" -> Icons.Filled.Phone
            "call" -> Icons.Filled.Call
            "chat" -> Icons.Filled.Chat
            "message" -> Icons.Filled.Message
            "comment" -> Icons.Filled.Comment
            "forum" -> Icons.Filled.Forum
            "notifications" -> Icons.Filled.Notifications
            "notificationsoff" -> Icons.Filled.NotificationsOff

            // Social
            "person" -> Icons.Filled.Person
            "people" -> Icons.Filled.People
            "group" -> Icons.Filled.Group
            "personadd" -> Icons.Filled.PersonAdd
            "public" -> Icons.Filled.Public
            "share" -> Icons.Filled.Share
            "thumbup" -> Icons.Filled.ThumbUp
            "thumbdown" -> Icons.Filled.ThumbDown

            // Feedback
            "info" -> Icons.Filled.Info
            "warning" -> Icons.Filled.Warning
            "error" -> Icons.Filled.Error
            "help" -> Icons.Filled.Help
            "feedback" -> Icons.Filled.Feedback
            "reportproblem" -> Icons.Filled.ReportProblem
            "checkcircle" -> Icons.Filled.CheckCircle
            "cancel" -> Icons.Filled.Cancel
            "highlight" -> Icons.Filled.Highlight

            // Rating
            "star" -> Icons.Filled.Star
            "starborder" -> Icons.Filled.StarBorder
            "starhalf" -> Icons.Filled.StarHalf
            "favorite" -> Icons.Filled.Favorite
            "favoriteborder" -> Icons.Filled.FavoriteBorder
            "grade" -> Icons.Filled.Grade

            // Toggle
            "checkbox" -> Icons.Filled.CheckBox
            "checkboxblank" -> Icons.Filled.CheckBoxOutlineBlank
            "radiobutton" -> Icons.Filled.RadioButtonChecked
            "radiobuttonunchecked" -> Icons.Filled.RadioButtonUnchecked
            "toggleon" -> Icons.Filled.ToggleOn
            "toggleoff" -> Icons.Filled.ToggleOff

            // Media
            "play" -> Icons.Filled.PlayArrow
            "playarrow" -> Icons.Filled.PlayArrow
            "pause" -> Icons.Filled.Pause
            "stop" -> Icons.Filled.Stop
            "skipnext" -> Icons.Filled.SkipNext
            "skipprevious" -> Icons.Filled.SkipPrevious
            "fastforward" -> Icons.Filled.FastForward
            "fastrewind" -> Icons.Filled.FastRewind
            "volumeup" -> Icons.Filled.VolumeUp
            "volumedown" -> Icons.Filled.VolumeDown
            "volumemute" -> Icons.Filled.VolumeMute
            "volumeoff" -> Icons.Filled.VolumeOff
            "mic" -> Icons.Filled.Mic
            "micoff" -> Icons.Filled.MicOff
            "videocam" -> Icons.Filled.Videocam
            "videocamoff" -> Icons.Filled.VideocamOff
            "camera" -> Icons.Filled.Camera
            "cameraalt" -> Icons.Filled.CameraAlt
            "photo" -> Icons.Filled.Photo
            "photolibrary" -> Icons.Filled.PhotoLibrary

            // Places
            "place" -> Icons.Filled.Place
            "locationon" -> Icons.Filled.LocationOn
            "locationoff" -> Icons.Filled.LocationOff
            "map" -> Icons.Filled.Map
            "mylocation" -> Icons.Filled.MyLocation
            "navigation" -> Icons.Filled.Navigation
            "directions" -> Icons.Filled.Directions

            // Date/Time
            "schedule" -> Icons.Filled.Schedule
            "accesstime" -> Icons.Filled.AccessTime
            "today" -> Icons.Filled.Today
            "event" -> Icons.Filled.Event
            "daterange" -> Icons.Filled.DateRange
            "calendartoday" -> Icons.Filled.CalendarToday
            "alarm" -> Icons.Filled.Alarm
            "timer" -> Icons.Filled.Timer

            // Device
            "phone" -> Icons.Filled.Phone
            "tablet" -> Icons.Filled.Tablet
            "computer" -> Icons.Filled.Computer
            "laptop" -> Icons.Filled.Laptop
            "watch" -> Icons.Filled.Watch
            "keyboard" -> Icons.Filled.Keyboard
            "mouse" -> Icons.Filled.Mouse
            "bluetooth" -> Icons.Filled.Bluetooth
            "wifi" -> Icons.Filled.Wifi
            "wifioff" -> Icons.Filled.WifiOff
            "battery" -> Icons.Filled.BatteryFull
            "signal" -> Icons.Filled.SignalCellular4Bar
            "flashon" -> Icons.Filled.FlashOn
            "flashoff" -> Icons.Filled.FlashOff

            // Formatting
            "formatbold" -> Icons.Filled.FormatBold
            "formatitalic" -> Icons.Filled.FormatItalic
            "formatunderlined" -> Icons.Filled.FormatUnderlined
            "formatlistbulleted" -> Icons.Filled.FormatListBulleted
            "formatlistnumbered" -> Icons.Filled.FormatListNumbered
            "formatalignleft" -> Icons.Filled.FormatAlignLeft
            "formataligncenter" -> Icons.Filled.FormatAlignCenter
            "formatalignright" -> Icons.Filled.FormatAlignRight

            // Shopping
            "shoppingcart" -> Icons.Filled.ShoppingCart
            "shoppingbasket" -> Icons.Filled.ShoppingBasket
            "store" -> Icons.Filled.Store
            "payment" -> Icons.Filled.Payment
            "creditcard" -> Icons.Filled.CreditCard
            "receipt" -> Icons.Filled.Receipt
            "localoffer" -> Icons.Filled.LocalOffer

            // Misc
            "flag" -> Icons.Filled.Flag
            "bookmark" -> Icons.Filled.Bookmark
            "bookmarkborder" -> Icons.Filled.BookmarkBorder
            "label" -> Icons.Filled.Label
            "lightbulb" -> Icons.Filled.Lightbulb
            "extension" -> Icons.Filled.Extension
            "dashboard" -> Icons.Filled.Dashboard
            "list" -> Icons.Filled.List
            "gridview" -> Icons.Filled.GridView
            "viewlist" -> Icons.Filled.ViewList
            "viewmodule" -> Icons.Filled.ViewModule
            "accountcircle" -> Icons.Filled.AccountCircle
            "accountbox" -> Icons.Filled.AccountBox
            "face" -> Icons.Filled.Face
            "fingerprint" -> Icons.Filled.Fingerprint

            // Default fallback
            else -> Icons.Filled.Info
        }
    }

    private fun getOutlinedIcon(name: String): ImageVector {
        return when (name) {
            "home" -> Icons.Outlined.Home
            "settings" -> Icons.Outlined.Settings
            "person" -> Icons.Outlined.Person
            "email" -> Icons.Outlined.Email
            "phone" -> Icons.Outlined.Phone
            "search" -> Icons.Outlined.Search
            "info" -> Icons.Outlined.Info
            "warning" -> Icons.Outlined.Warning
            "star" -> Icons.Outlined.Star
            "favorite" -> Icons.Outlined.FavoriteBorder
            "check" -> Icons.Outlined.Check
            "close" -> Icons.Outlined.Close
            "add" -> Icons.Outlined.Add
            "delete" -> Icons.Outlined.Delete
            "edit" -> Icons.Outlined.Edit
            "notifications" -> Icons.Outlined.Notifications
            "lock" -> Icons.Outlined.Lock
            "visibility" -> Icons.Outlined.Visibility
            "visibilityoff" -> Icons.Outlined.VisibilityOff
            else -> getFilledIcon(name) // Fallback to filled
        }
    }

    enum class IconStyle {
        FILLED, OUTLINED
    }
}
