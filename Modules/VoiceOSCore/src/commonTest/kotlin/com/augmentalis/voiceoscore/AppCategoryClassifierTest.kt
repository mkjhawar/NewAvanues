package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.classifier.AppCategory
import com.augmentalis.voiceoscore.classifier.AppCategoryClassifier
import com.augmentalis.voiceoscore.classifier.DynamicBehavior
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class AppCategoryClassifierTest {

    // =========== EMAIL CATEGORY ===========

    @Test
    fun `classifyPackage should return EMAIL for gmail`() {
        assertEquals(AppCategory.EMAIL, AppCategoryClassifier.classifyPackage("com.google.android.gm"))
    }

    @Test
    fun `classifyPackage should return EMAIL for outlook`() {
        assertEquals(AppCategory.EMAIL, AppCategoryClassifier.classifyPackage("com.microsoft.office.outlook"))
    }

    @Test
    fun `classifyPackage should return EMAIL for yahoo mail`() {
        assertEquals(AppCategory.EMAIL, AppCategoryClassifier.classifyPackage("com.yahoo.mobile.client.android.mail"))
    }

    @Test
    fun `classifyPackage should return EMAIL for protonmail`() {
        assertEquals(AppCategory.EMAIL, AppCategoryClassifier.classifyPackage("ch.protonmail.android"))
    }

    @Test
    fun `classifyPackage should return EMAIL for generic mail app`() {
        assertEquals(AppCategory.EMAIL, AppCategoryClassifier.classifyPackage("com.example.mailclient"))
    }

    @Test
    fun `classifyPackage should return EMAIL for inbox app`() {
        assertEquals(AppCategory.EMAIL, AppCategoryClassifier.classifyPackage("com.google.android.apps.inbox"))
    }

    // =========== MESSAGING CATEGORY ===========

    @Test
    fun `classifyPackage should return MESSAGING for whatsapp`() {
        assertEquals(AppCategory.MESSAGING, AppCategoryClassifier.classifyPackage("com.whatsapp"))
    }

    @Test
    fun `classifyPackage should return MESSAGING for slack`() {
        assertEquals(AppCategory.MESSAGING, AppCategoryClassifier.classifyPackage("com.slack"))
    }

    @Test
    fun `classifyPackage should return MESSAGING for teams`() {
        assertEquals(AppCategory.MESSAGING, AppCategoryClassifier.classifyPackage("com.microsoft.teams"))
    }

    @Test
    fun `classifyPackage should return MESSAGING for telegram`() {
        assertEquals(AppCategory.MESSAGING, AppCategoryClassifier.classifyPackage("org.telegram.messenger"))
    }

    @Test
    fun `classifyPackage should return MESSAGING for discord`() {
        assertEquals(AppCategory.MESSAGING, AppCategoryClassifier.classifyPackage("com.discord"))
    }

    @Test
    fun `classifyPackage should return MESSAGING for signal`() {
        assertEquals(AppCategory.MESSAGING, AppCategoryClassifier.classifyPackage("org.thoughtcrime.securesms"))
    }

    @Test
    fun `classifyPackage should return MESSAGING for android messages`() {
        assertEquals(AppCategory.MESSAGING, AppCategoryClassifier.classifyPackage("com.google.android.apps.messaging"))
    }

    @Test
    fun `classifyPackage should return MESSAGING for sms app`() {
        assertEquals(AppCategory.MESSAGING, AppCategoryClassifier.classifyPackage("com.android.mms"))
    }

    @Test
    fun `classifyPackage should return MESSAGING for chat app`() {
        assertEquals(AppCategory.MESSAGING, AppCategoryClassifier.classifyPackage("com.example.chatapp"))
    }

    // =========== SETTINGS CATEGORY ===========

    @Test
    fun `classifyPackage should return SETTINGS for android settings`() {
        assertEquals(AppCategory.SETTINGS, AppCategoryClassifier.classifyPackage("com.android.settings"))
    }

    @Test
    fun `classifyPackage should return SETTINGS for app preferences`() {
        assertEquals(AppCategory.SETTINGS, AppCategoryClassifier.classifyPackage("com.example.app.preferences"))
    }

    @Test
    fun `classifyPackage should return SETTINGS for config app`() {
        assertEquals(AppCategory.SETTINGS, AppCategoryClassifier.classifyPackage("com.example.config"))
    }

    @Test
    fun `classifyPackage should return SETTINGS for setup wizard`() {
        assertEquals(AppCategory.SETTINGS, AppCategoryClassifier.classifyPackage("com.android.setupwizard"))
    }

    // =========== ENTERPRISE CATEGORY ===========

    @Test
    fun `classifyPackage should return ENTERPRISE for realwear`() {
        assertEquals(AppCategory.ENTERPRISE, AppCategoryClassifier.classifyPackage("com.realwear.hmt"))
    }

    @Test
    fun `classifyPackage should return ENTERPRISE for augmentalis`() {
        assertEquals(AppCategory.ENTERPRISE, AppCategoryClassifier.classifyPackage("com.augmentalis.voiceoscoreng"))
    }

    @Test
    fun `classifyPackage should return ENTERPRISE for realwear hmt1`() {
        assertEquals(AppCategory.ENTERPRISE, AppCategoryClassifier.classifyPackage("com.realwear.hmt1"))
    }

    @Test
    fun `classifyPackage should return ENTERPRISE for teamviewer`() {
        assertEquals(AppCategory.ENTERPRISE, AppCategoryClassifier.classifyPackage("com.teamviewer.remote"))
    }

    @Test
    fun `classifyPackage should return ENTERPRISE for zoom`() {
        assertEquals(AppCategory.ENTERPRISE, AppCategoryClassifier.classifyPackage("us.zoom.videomeetings"))
    }

    @Test
    fun `classifyPackage should return ENTERPRISE for webex`() {
        assertEquals(AppCategory.ENTERPRISE, AppCategoryClassifier.classifyPackage("com.cisco.webex.meetings"))
    }

    @Test
    fun `classifyPackage should return ENTERPRISE for google meet`() {
        assertEquals(AppCategory.ENTERPRISE, AppCategoryClassifier.classifyPackage("com.google.android.apps.meetings"))
    }

    // =========== SOCIAL CATEGORY ===========

    @Test
    fun `classifyPackage should return SOCIAL for instagram`() {
        assertEquals(AppCategory.SOCIAL, AppCategoryClassifier.classifyPackage("com.instagram.android"))
    }

    @Test
    fun `classifyPackage should return SOCIAL for twitter`() {
        assertEquals(AppCategory.SOCIAL, AppCategoryClassifier.classifyPackage("com.twitter.android"))
    }

    @Test
    fun `classifyPackage should return SOCIAL for facebook`() {
        assertEquals(AppCategory.SOCIAL, AppCategoryClassifier.classifyPackage("com.facebook.katana"))
    }

    @Test
    fun `classifyPackage should return SOCIAL for tiktok`() {
        assertEquals(AppCategory.SOCIAL, AppCategoryClassifier.classifyPackage("com.zhiliaoapp.musically"))
    }

    @Test
    fun `classifyPackage should return SOCIAL for linkedin`() {
        assertEquals(AppCategory.SOCIAL, AppCategoryClassifier.classifyPackage("com.linkedin.android"))
    }

    @Test
    fun `classifyPackage should return SOCIAL for reddit`() {
        assertEquals(AppCategory.SOCIAL, AppCategoryClassifier.classifyPackage("com.reddit.frontpage"))
    }

    @Test
    fun `classifyPackage should return SOCIAL for threads`() {
        assertEquals(AppCategory.SOCIAL, AppCategoryClassifier.classifyPackage("com.instagram.barcelona"))
    }

    // =========== BROWSER CATEGORY ===========

    @Test
    fun `classifyPackage should return BROWSER for chrome`() {
        assertEquals(AppCategory.BROWSER, AppCategoryClassifier.classifyPackage("com.android.chrome"))
    }

    @Test
    fun `classifyPackage should return BROWSER for firefox`() {
        assertEquals(AppCategory.BROWSER, AppCategoryClassifier.classifyPackage("org.mozilla.firefox"))
    }

    @Test
    fun `classifyPackage should return BROWSER for edge`() {
        assertEquals(AppCategory.BROWSER, AppCategoryClassifier.classifyPackage("com.microsoft.emmx"))
    }

    @Test
    fun `classifyPackage should return BROWSER for brave`() {
        assertEquals(AppCategory.BROWSER, AppCategoryClassifier.classifyPackage("com.brave.browser"))
    }

    @Test
    fun `classifyPackage should return BROWSER for opera`() {
        assertEquals(AppCategory.BROWSER, AppCategoryClassifier.classifyPackage("com.opera.browser"))
    }

    @Test
    fun `classifyPackage should return BROWSER for duckduckgo`() {
        assertEquals(AppCategory.BROWSER, AppCategoryClassifier.classifyPackage("com.duckduckgo.mobile.android"))
    }

    // =========== MEDIA CATEGORY ===========

    @Test
    fun `classifyPackage should return MEDIA for spotify`() {
        assertEquals(AppCategory.MEDIA, AppCategoryClassifier.classifyPackage("com.spotify.music"))
    }

    @Test
    fun `classifyPackage should return MEDIA for youtube`() {
        assertEquals(AppCategory.MEDIA, AppCategoryClassifier.classifyPackage("com.google.android.youtube"))
    }

    @Test
    fun `classifyPackage should return MEDIA for netflix`() {
        assertEquals(AppCategory.MEDIA, AppCategoryClassifier.classifyPackage("com.netflix.mediaclient"))
    }

    @Test
    fun `classifyPackage should return MEDIA for music player`() {
        assertEquals(AppCategory.MEDIA, AppCategoryClassifier.classifyPackage("com.example.musicplayer"))
    }

    @Test
    fun `classifyPackage should return MEDIA for video app`() {
        assertEquals(AppCategory.MEDIA, AppCategoryClassifier.classifyPackage("com.example.videoplayer"))
    }

    @Test
    fun `classifyPackage should return MEDIA for photos`() {
        assertEquals(AppCategory.MEDIA, AppCategoryClassifier.classifyPackage("com.google.android.apps.photos"))
    }

    @Test
    fun `classifyPackage should return MEDIA for gallery`() {
        assertEquals(AppCategory.MEDIA, AppCategoryClassifier.classifyPackage("com.android.gallery3d"))
    }

    @Test
    fun `classifyPackage should return MEDIA for camera`() {
        assertEquals(AppCategory.MEDIA, AppCategoryClassifier.classifyPackage("com.android.camera2"))
    }

    @Test
    fun `classifyPackage should return MEDIA for podcast`() {
        assertEquals(AppCategory.MEDIA, AppCategoryClassifier.classifyPackage("com.google.android.apps.podcasts"))
    }

    // =========== PRODUCTIVITY CATEGORY ===========

    @Test
    fun `classifyPackage should return PRODUCTIVITY for google docs`() {
        assertEquals(AppCategory.PRODUCTIVITY, AppCategoryClassifier.classifyPackage("com.google.android.apps.docs"))
    }

    @Test
    fun `classifyPackage should return PRODUCTIVITY for google sheets`() {
        assertEquals(AppCategory.PRODUCTIVITY, AppCategoryClassifier.classifyPackage("com.google.android.apps.docs.editors.sheets"))
    }

    @Test
    fun `classifyPackage should return PRODUCTIVITY for calendar`() {
        assertEquals(AppCategory.PRODUCTIVITY, AppCategoryClassifier.classifyPackage("com.google.android.calendar"))
    }

    @Test
    fun `classifyPackage should return PRODUCTIVITY for notes app`() {
        assertEquals(AppCategory.PRODUCTIVITY, AppCategoryClassifier.classifyPackage("com.example.notes"))
    }

    @Test
    fun `classifyPackage should return PRODUCTIVITY for keep`() {
        assertEquals(AppCategory.PRODUCTIVITY, AppCategoryClassifier.classifyPackage("com.google.android.keep"))
    }

    @Test
    fun `classifyPackage should return PRODUCTIVITY for evernote`() {
        assertEquals(AppCategory.PRODUCTIVITY, AppCategoryClassifier.classifyPackage("com.evernote"))
    }

    @Test
    fun `classifyPackage should return PRODUCTIVITY for dropbox`() {
        assertEquals(AppCategory.PRODUCTIVITY, AppCategoryClassifier.classifyPackage("com.dropbox.android"))
    }

    @Test
    fun `classifyPackage should return PRODUCTIVITY for drive`() {
        assertEquals(AppCategory.PRODUCTIVITY, AppCategoryClassifier.classifyPackage("com.google.android.apps.docs.editors.drive"))
    }

    @Test
    fun `classifyPackage should return PRODUCTIVITY for tasks`() {
        assertEquals(AppCategory.PRODUCTIVITY, AppCategoryClassifier.classifyPackage("com.google.android.apps.tasks"))
    }

    // =========== SYSTEM CATEGORY ===========

    @Test
    fun `classifyPackage should return SYSTEM for launcher`() {
        assertEquals(AppCategory.SYSTEM, AppCategoryClassifier.classifyPackage("com.google.android.apps.nexuslauncher"))
    }

    @Test
    fun `classifyPackage should return SYSTEM for systemui`() {
        assertEquals(AppCategory.SYSTEM, AppCategoryClassifier.classifyPackage("com.android.systemui"))
    }

    @Test
    fun `classifyPackage should return SYSTEM for package installer`() {
        assertEquals(AppCategory.SYSTEM, AppCategoryClassifier.classifyPackage("com.android.packageinstaller"))
    }

    @Test
    fun `classifyPackage should return SYSTEM for play store (vending)`() {
        assertEquals(AppCategory.SYSTEM, AppCategoryClassifier.classifyPackage("com.android.vending"))
    }

    @Test
    fun `classifyPackage should return SYSTEM for permission controller`() {
        assertEquals(AppCategory.SYSTEM, AppCategoryClassifier.classifyPackage("com.google.android.permissioncontroller"))
    }

    @Test
    fun `classifyPackage should return SYSTEM for documents ui`() {
        assertEquals(AppCategory.SYSTEM, AppCategoryClassifier.classifyPackage("com.android.documentsui"))
    }

    // =========== UNKNOWN CATEGORY ===========

    @Test
    fun `classifyPackage should return UNKNOWN for unrecognized package`() {
        assertEquals(AppCategory.UNKNOWN, AppCategoryClassifier.classifyPackage("com.random.unknownapp"))
    }

    @Test
    fun `classifyPackage should return UNKNOWN for empty package name`() {
        assertEquals(AppCategory.UNKNOWN, AppCategoryClassifier.classifyPackage(""))
    }

    @Test
    fun `classifyPackage should return UNKNOWN for nonsense package`() {
        assertEquals(AppCategory.UNKNOWN, AppCategoryClassifier.classifyPackage("com.xyz.foobar"))
    }

    // =========== CASE INSENSITIVITY ===========

    @Test
    fun `classifyPackage should be case insensitive for gmail uppercase`() {
        assertEquals(AppCategory.EMAIL, AppCategoryClassifier.classifyPackage("COM.GOOGLE.ANDROID.GMAIL"))
    }

    @Test
    fun `classifyPackage should be case insensitive for mixed case`() {
        assertEquals(AppCategory.MESSAGING, AppCategoryClassifier.classifyPackage("Com.WhatsApp"))
    }

    // =========== DYNAMIC BEHAVIOR TESTS ===========

    @Test
    fun `EMAIL category should have MOSTLY_DYNAMIC behavior`() {
        assertEquals(DynamicBehavior.MOSTLY_DYNAMIC, AppCategory.EMAIL.dynamicBehavior)
    }

    @Test
    fun `MESSAGING category should have MOSTLY_DYNAMIC behavior`() {
        assertEquals(DynamicBehavior.MOSTLY_DYNAMIC, AppCategory.MESSAGING.dynamicBehavior)
    }

    @Test
    fun `SOCIAL category should have MOSTLY_DYNAMIC behavior`() {
        assertEquals(DynamicBehavior.MOSTLY_DYNAMIC, AppCategory.SOCIAL.dynamicBehavior)
    }

    @Test
    fun `BROWSER category should have MOSTLY_DYNAMIC behavior`() {
        assertEquals(DynamicBehavior.MOSTLY_DYNAMIC, AppCategory.BROWSER.dynamicBehavior)
    }

    @Test
    fun `SETTINGS category should have STATIC behavior`() {
        assertEquals(DynamicBehavior.STATIC, AppCategory.SETTINGS.dynamicBehavior)
    }

    @Test
    fun `SYSTEM category should have STATIC behavior`() {
        assertEquals(DynamicBehavior.STATIC, AppCategory.SYSTEM.dynamicBehavior)
    }

    @Test
    fun `ENTERPRISE category should have MIXED behavior`() {
        assertEquals(DynamicBehavior.MIXED, AppCategory.ENTERPRISE.dynamicBehavior)
    }

    @Test
    fun `MEDIA category should have MIXED behavior`() {
        assertEquals(DynamicBehavior.MIXED, AppCategory.MEDIA.dynamicBehavior)
    }

    @Test
    fun `PRODUCTIVITY category should have MIXED behavior`() {
        assertEquals(DynamicBehavior.MIXED, AppCategory.PRODUCTIVITY.dynamicBehavior)
    }

    @Test
    fun `UNKNOWN category should have MIXED behavior`() {
        assertEquals(DynamicBehavior.MIXED, AppCategory.UNKNOWN.dynamicBehavior)
    }

    // =========== isCategoryByPattern TESTS ===========

    @Test
    fun `isCategoryByPattern should return true for matching category`() {
        assertTrue(AppCategoryClassifier.isCategoryByPattern("com.google.android.gm", AppCategory.EMAIL))
    }

    @Test
    fun `isCategoryByPattern should return false for non-matching category`() {
        assertFalse(AppCategoryClassifier.isCategoryByPattern("com.google.android.gm", AppCategory.MESSAGING))
    }

    @Test
    fun `isCategoryByPattern should return true for unknown when package is unrecognized`() {
        assertTrue(AppCategoryClassifier.isCategoryByPattern("com.random.app", AppCategory.UNKNOWN))
    }

    // =========== getDynamicBehaviorByPattern TESTS ===========

    @Test
    fun `getDynamicBehaviorByPattern should return MOSTLY_DYNAMIC for gmail`() {
        assertEquals(DynamicBehavior.MOSTLY_DYNAMIC, AppCategoryClassifier.getDynamicBehaviorByPattern("com.google.android.gm"))
    }

    @Test
    fun `getDynamicBehaviorByPattern should return STATIC for settings`() {
        assertEquals(DynamicBehavior.STATIC, AppCategoryClassifier.getDynamicBehaviorByPattern("com.android.settings"))
    }

    @Test
    fun `getDynamicBehaviorByPattern should return MIXED for unknown apps`() {
        assertEquals(DynamicBehavior.MIXED, AppCategoryClassifier.getDynamicBehaviorByPattern("com.random.unknownapp"))
    }

    @Test
    fun `getDynamicBehaviorByPattern should return MIXED for enterprise apps`() {
        assertEquals(DynamicBehavior.MIXED, AppCategoryClassifier.getDynamicBehaviorByPattern("com.realwear.hmt"))
    }

    // =========== isStaticAppByPattern TESTS ===========

    @Test
    fun `isStaticAppByPattern should return true for settings`() {
        assertTrue(AppCategoryClassifier.isStaticAppByPattern("com.android.settings"))
    }

    @Test
    fun `isStaticAppByPattern should return true for system launcher`() {
        assertTrue(AppCategoryClassifier.isStaticAppByPattern("com.google.android.apps.nexuslauncher"))
    }

    @Test
    fun `isStaticAppByPattern should return false for gmail`() {
        assertFalse(AppCategoryClassifier.isStaticAppByPattern("com.google.android.gm"))
    }

    @Test
    fun `isStaticAppByPattern should return false for enterprise app`() {
        assertFalse(AppCategoryClassifier.isStaticAppByPattern("com.realwear.hmt"))
    }

    @Test
    fun `isStaticAppByPattern should return false for unknown app`() {
        assertFalse(AppCategoryClassifier.isStaticAppByPattern("com.random.unknownapp"))
    }

    // =========== isDynamicAppByPattern TESTS ===========

    @Test
    fun `isDynamicAppByPattern should return true for gmail`() {
        assertTrue(AppCategoryClassifier.isDynamicAppByPattern("com.google.android.gm"))
    }

    @Test
    fun `isDynamicAppByPattern should return true for whatsapp`() {
        assertTrue(AppCategoryClassifier.isDynamicAppByPattern("com.whatsapp"))
    }

    @Test
    fun `isDynamicAppByPattern should return true for instagram`() {
        assertTrue(AppCategoryClassifier.isDynamicAppByPattern("com.instagram.android"))
    }

    @Test
    fun `isDynamicAppByPattern should return true for chrome browser`() {
        assertTrue(AppCategoryClassifier.isDynamicAppByPattern("com.android.chrome"))
    }

    @Test
    fun `isDynamicAppByPattern should return false for settings`() {
        assertFalse(AppCategoryClassifier.isDynamicAppByPattern("com.android.settings"))
    }

    @Test
    fun `isDynamicAppByPattern should return false for enterprise app`() {
        assertFalse(AppCategoryClassifier.isDynamicAppByPattern("com.realwear.hmt"))
    }

    @Test
    fun `isDynamicAppByPattern should return false for unknown app`() {
        assertFalse(AppCategoryClassifier.isDynamicAppByPattern("com.random.unknownapp"))
    }

    @Test
    fun `isDynamicAppByPattern should return false for media app`() {
        assertFalse(AppCategoryClassifier.isDynamicAppByPattern("com.spotify.music"))
    }

    // =========== PATTERN PRIORITY TESTS ===========

    @Test
    fun `enterprise patterns should take priority over generic patterns`() {
        // "meet" could match enterprise (google meet) - enterprise comes first in priority
        assertEquals(AppCategory.ENTERPRISE, AppCategoryClassifier.classifyPackage("com.google.android.apps.meet"))
    }

    @Test
    fun `settings should take priority over system for settings package`() {
        // "settings" is checked before system patterns
        assertEquals(AppCategory.SETTINGS, AppCategoryClassifier.classifyPackage("com.android.settings"))
    }

    // =========== EDGE CASES ===========

    @Test
    fun `classifyPackage should handle package with multiple matching patterns`() {
        // "workspace" matches enterprise, should return ENTERPRISE (first in priority)
        assertEquals(AppCategory.ENTERPRISE, AppCategoryClassifier.classifyPackage("com.google.android.apps.workspace"))
    }

    @Test
    fun `classifyPackage should handle very long package names`() {
        val longPackage = "com.example.very.long.package.name.with.many.segments.gmail.client"
        assertEquals(AppCategory.EMAIL, AppCategoryClassifier.classifyPackage(longPackage))
    }

    @Test
    fun `classifyPackage should handle package name with special characters in pattern position`() {
        // Pattern is just a substring match, not regex
        assertEquals(AppCategory.EMAIL, AppCategoryClassifier.classifyPackage("com.mymail.app"))
    }
}
