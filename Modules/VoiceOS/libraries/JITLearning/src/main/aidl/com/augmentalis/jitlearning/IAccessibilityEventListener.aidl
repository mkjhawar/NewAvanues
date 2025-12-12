/**
 * IAccessibilityEventListener.aidl - Event callback interface for LearnApp
 *
 * Callback interface for receiving accessibility events from JIT service.
 * Implements the "event streaming" pattern for LearnApp exploration.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md Section 3.2
 *
 * ## Event Format (IPC Compatible)
 *
 * Events follow the AVU IPC code format:
 * - SCH:hash:activity:timestamp:element_count - Screen change
 * - ELA:uuid:action_type:result:timestamp - Element action
 * - SCR:direction:distance:new_elements - Scroll detected
 * - DYN:screen_hash:region_id:change_type - Dynamic content
 * - MNU:menu_id:total_items:visible_items - Menu discovered
 * - LGN:package:screen_hash:login_type - Login screen
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.jitlearning;

import com.augmentalis.jitlearning.ScreenChangeEvent;
import com.augmentalis.jitlearning.ParcelableNodeInfo;

/**
 * Accessibility Event Listener Interface
 *
 * Callback interface implemented by LearnApp to receive events from JIT service.
 * Events are streamed in real-time as accessibility events occur.
 */
interface IAccessibilityEventListener {

    /**
     * Called when screen content changes
     *
     * Triggered by TYPE_WINDOW_STATE_CHANGED or TYPE_WINDOW_CONTENT_CHANGED events.
     * Contains new screen hash, activity name, and element count.
     *
     * IPC Format: SCH:hash:activity:timestamp:element_count
     *
     * @param event ScreenChangeEvent with screen details
     */
    void onScreenChanged(in ScreenChangeEvent event);

    /**
     * Called when an element action is performed
     *
     * Reports result of click, longClick, scroll, or other actions.
     * Used by LearnApp to track which actions succeeded/failed.
     *
     * IPC Format: ELA:uuid:action_type:result:timestamp
     *
     * @param elementUuid UUID of element that was acted upon
     * @param actionType Type of action ("click", "longClick", "scroll", etc.)
     * @param success Whether the action succeeded
     */
    void onElementAction(in String elementUuid, in String actionType, boolean success);

    /**
     * Called when scroll is detected
     *
     * Reports scroll events with new elements discovered after scroll.
     * Used to track infinite scroll and content loading.
     *
     * IPC Format: SCR:direction:distance:new_elements_count
     *
     * @param direction Scroll direction ("up", "down", "left", "right")
     * @param distance Distance scrolled in pixels
     * @param newElementsCount Number of new elements discovered
     */
    void onScrollDetected(in String direction, int distance, int newElementsCount);

    /**
     * Called when dynamic content region is detected
     *
     * Reports regions that have changed content multiple times.
     * Used to mark regions for logging-only (not re-scanning).
     *
     * IPC Format: DYN:screen_hash:region_id:change_type
     *
     * @param screenHash Hash of screen containing dynamic content
     * @param regionId Identifier for the dynamic region
     */
    void onDynamicContentDetected(in String screenHash, in String regionId);

    /**
     * Called when a menu is discovered
     *
     * Reports overflow menus, dropdowns, or expandable content.
     * Contains total items vs visible items count.
     *
     * IPC Format: MNU:menu_id:total_items:visible_items
     *
     * @param menuId Identifier for the menu
     * @param totalItems Total number of menu items
     * @param visibleItems Number of currently visible items
     */
    void onMenuDiscovered(in String menuId, int totalItems, int visibleItems);

    /**
     * Called when a login screen is detected
     *
     * Reports login/authentication screens for user prompting.
     * LearnApp should pause exploration and prompt user.
     *
     * IPC Format: LGN:package:screen_hash:login_type
     *
     * @param packageName Package with login screen
     * @param screenHash Hash of the login screen
     */
    void onLoginScreenDetected(in String packageName, in String screenHash);

    /**
     * Called when JIT encounters an error
     *
     * Reports errors that may affect exploration.
     *
     * @param errorCode Error code (e.g., "NODE_RECYCLED", "ACTION_FAILED")
     * @param message Human-readable error message
     */
    void onError(in String errorCode, in String message);
}
