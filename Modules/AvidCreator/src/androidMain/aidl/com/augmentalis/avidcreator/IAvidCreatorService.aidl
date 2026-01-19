package com.augmentalis.avidcreator;

import com.augmentalis.avidcreator.AvidElementData;
import com.augmentalis.avidcreator.AvidCommandResultData;

/**
 * AVID Creator Service Interface
 *
 * Provides universal element identification and targeting system
 * for cross-app usage via IPC.
 */
interface IAvidCreatorService {

    /**
     * Generate a new AVID
     *
     * @return Newly generated AVID string
     */
    String generateAvid();

    /**
     * Register an element with automatic AVID generation
     *
     * @param elementData Element data to register
     * @return Generated AVID for the registered element
     */
    String registerElement(in AvidElementData elementData);

    /**
     * Unregister an element by AVID
     *
     * @param avid AVID of element to unregister
     * @return true if element was unregistered, false if not found
     */
    boolean unregisterElement(String avid);

    /**
     * Find element by AVID
     *
     * @param avid AVID to search for
     * @return Element data if found, null otherwise
     */
    AvidElementData findByAvid(String avid);

    /**
     * Find elements by name
     *
     * @param name Element name to search for
     * @return List of matching elements
     */
    List<AvidElementData> findByName(String name);

    /**
     * Find elements by type
     *
     * @param type Element type to search for
     * @return List of matching elements
     */
    List<AvidElementData> findByType(String type);

    /**
     * Find element by position (1-indexed)
     *
     * @param position Position number (1 = first, 2 = second, -1 = last)
     * @return Element data if found at position, null otherwise
     */
    AvidElementData findByPosition(int position);

    /**
     * Find element in direction from current element
     *
     * Directions: "left", "right", "up", "down", "forward", "backward",
     *             "next", "previous", "first", "last"
     *
     * @param fromAvid Starting element AVID
     * @param direction Direction to search
     * @return Element data if found, null otherwise
     */
    AvidElementData findInDirection(String fromAvid, String direction);

    /**
     * Execute action on element
     *
     * @param avid Target element AVID
     * @param action Action name (e.g., "click", "focus", "select")
     * @param parametersJson JSON string containing action parameters
     * @return true if action executed successfully, false otherwise
     */
    boolean executeAction(String avid, String action, String parametersJson);

    /**
     * Process voice command
     *
     * Parses voice command and executes on matching element.
     *
     * Commands:
     * - "click element <name>" - Click element by name
     * - "select first/second/third" - Select element by position
     * - "move left/right/up/down" - Navigate spatially
     *
     * @param command Voice command string
     * @return Command result with success status and details
     */
    AvidCommandResultData processVoiceCommand(String command);

    /**
     * Get all registered elements
     *
     * @return List of all registered elements
     */
    List<AvidElementData> getAllElements();

    /**
     * Get registry statistics
     *
     * @return JSON string with registry stats (count, types, etc.)
     */
    String getRegistryStats();

    /**
     * Clear all registered elements
     */
    void clearAll();
}
