package com.augmentalis.uuidcreator;

import com.augmentalis.uuidcreator.UUIDElementData;
import com.augmentalis.uuidcreator.UUIDCommandResultData;

/**
 * UUID Creator Service Interface
 *
 * Provides universal element identification and targeting system
 * for cross-app usage via IPC.
 */
interface IUUIDCreatorService {

    /**
     * Generate a new UUID
     *
     * @return Newly generated UUID string
     */
    String generateUUID();

    /**
     * Register an element with automatic UUID generation
     *
     * @param elementData Element data to register
     * @return Generated UUID for the registered element
     */
    String registerElement(in UUIDElementData elementData);

    /**
     * Unregister an element by UUID
     *
     * @param uuid UUID of element to unregister
     * @return true if element was unregistered, false if not found
     */
    boolean unregisterElement(String uuid);

    /**
     * Find element by UUID
     *
     * @param uuid UUID to search for
     * @return Element data if found, null otherwise
     */
    UUIDElementData findByUUID(String uuid);

    /**
     * Find elements by name
     *
     * @param name Element name to search for
     * @return List of matching elements
     */
    List<UUIDElementData> findByName(String name);

    /**
     * Find elements by type
     *
     * @param type Element type to search for
     * @return List of matching elements
     */
    List<UUIDElementData> findByType(String type);

    /**
     * Find element by position (1-indexed)
     *
     * @param position Position number (1 = first, 2 = second, -1 = last)
     * @return Element data if found at position, null otherwise
     */
    UUIDElementData findByPosition(int position);

    /**
     * Find element in direction from current element
     *
     * Directions: "left", "right", "up", "down", "forward", "backward",
     *             "next", "previous", "first", "last"
     *
     * @param fromUUID Starting element UUID
     * @param direction Direction to search
     * @return Element data if found, null otherwise
     */
    UUIDElementData findInDirection(String fromUUID, String direction);

    /**
     * Execute action on element
     *
     * @param uuid Target element UUID
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
    UUIDCommandResultData processVoiceCommand(String command);

    /**
     * Get all registered elements
     *
     * @return List of all registered elements
     */
    List<UUIDElementData> getAllElements();

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
