package com.augmentalis.uuidcreator;

import com.augmentalis.uuidcreator.VUIDElementData;
import com.augmentalis.uuidcreator.VUIDCommandResultData;

/**
 * VUID Creator Service Interface
 *
 * Provides universal element identification and targeting system
 * for cross-app usage via IPC.
 */
interface IVUIDCreatorService {

    /**
     * Generate a new VUID
     *
     * @return Newly generated VUID string
     */
    String generateUUID();

    /**
     * Register an element with automatic VUID generation
     *
     * @param elementData Element data to register
     * @return Generated VUID for the registered element
     */
    String registerElement(in VUIDElementData elementData);

    /**
     * Unregister an element by VUID
     *
     * @param uuid VUID of element to unregister
     * @return true if element was unregistered, false if not found
     */
    boolean unregisterElement(String uuid);

    /**
     * Find element by VUID
     *
     * @param uuid VUID to search for
     * @return Element data if found, null otherwise
     */
    VUIDElementData findByUUID(String uuid);

    /**
     * Find elements by name
     *
     * @param name Element name to search for
     * @return List of matching elements
     */
    List<VUIDElementData> findByName(String name);

    /**
     * Find elements by type
     *
     * @param type Element type to search for
     * @return List of matching elements
     */
    List<VUIDElementData> findByType(String type);

    /**
     * Find element by position (1-indexed)
     *
     * @param position Position number (1 = first, 2 = second, -1 = last)
     * @return Element data if found at position, null otherwise
     */
    VUIDElementData findByPosition(int position);

    /**
     * Find element in direction from current element
     *
     * Directions: "left", "right", "up", "down", "forward", "backward",
     *             "next", "previous", "first", "last"
     *
     * @param fromUUID Starting element VUID
     * @param direction Direction to search
     * @return Element data if found, null otherwise
     */
    VUIDElementData findInDirection(String fromUUID, String direction);

    /**
     * Execute action on element
     *
     * @param uuid Target element VUID
     * @param action Action name (e.g., "click", "focus", "select")
     * @param parametersJson JSON string containing action parameters
     * @return true if action executed successfully, false otherwise
     */
    boolean executeAction(String uuid, String action, String parametersJson);

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
    VUIDCommandResultData processVoiceCommand(String command);

    /**
     * Get all registered elements
     *
     * @return List of all registered elements
     */
    List<VUIDElementData> getAllElements();

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
