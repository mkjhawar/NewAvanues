package com.augmentalis.voiceavanue;

// Import all Parcelable models
import com.augmentalis.voiceavanue.models.User;
import com.augmentalis.voiceavanue.models.VoiceCommand;
import com.augmentalis.voiceavanue.models.AppSettings;

/**
 * AIDL interface for Database cross-process operations.
 * All methods are synchronous - wrap in coroutines on client side.
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
interface IDatabase {

    // ===== User Operations (6 methods) =====

    /**
     * Get all users from database.
     * @return List of all users, empty list if none
     */
    List<User> getAllUsers();

    /**
     * Get user by ID.
     * @param userId User's unique identifier
     * @return User if found, null otherwise
     */
    User getUserById(int userId);

    /**
     * Insert new user into database.
     * @param user User to insert
     */
    void insertUser(in User user);

    /**
     * Update existing user.
     * @param user User with updated data
     */
    void updateUser(in User user);

    /**
     * Delete user by ID.
     * @param userId User's unique identifier
     */
    void deleteUser(int userId);

    /**
     * Get total count of users.
     * @return Number of users in database
     */
    int getUserCount();

    // ===== Voice Command Operations (6 methods) =====

    /**
     * Get all voice commands.
     * @return List of all commands, empty list if none
     */
    List<VoiceCommand> getAllVoiceCommands();

    /**
     * Get voice command by ID.
     * @param commandId Command's unique identifier
     * @return VoiceCommand if found, null otherwise
     */
    VoiceCommand getVoiceCommandById(int commandId);

    /**
     * Get voice commands by category.
     * @param category Category to filter by
     * @return List of commands in category, empty if none
     */
    List<VoiceCommand> getVoiceCommandsByCategory(String category);

    /**
     * Insert new voice command.
     * @param command Voice command to insert
     */
    void insertVoiceCommand(in VoiceCommand command);

    /**
     * Update existing voice command.
     * @param command Voice command with updated data
     */
    void updateVoiceCommand(in VoiceCommand command);

    /**
     * Delete voice command by ID.
     * @param commandId Command's unique identifier
     */
    void deleteVoiceCommand(int commandId);

    // ===== Settings Operations (4 methods) =====

    /**
     * Get application settings.
     * @return AppSettings if exists, null otherwise
     */
    AppSettings getSettings();

    /**
     * Update application settings.
     * @param settings Settings with updated values
     */
    void updateSettings(in AppSettings settings);

    /**
     * Get specific setting value by key.
     * @param key Setting key
     * @return Setting value, null if not found
     */
    String getSettingValue(String key);

    /**
     * Set specific setting value.
     * @param key Setting key
     * @param value Setting value
     */
    void setSettingValue(String key, String value);

    // ===== Maintenance Operations (4 methods) =====

    /**
     * Clear all data from database.
     * WARNING: This is destructive and cannot be undone.
     */
    void clearAllData();

    /**
     * Get database file size in bytes.
     * @return Database size in bytes
     */
    long getDatabaseSize();

    /**
     * Vacuum database to reclaim space.
     */
    void vacuum();

    /**
     * Get database schema version.
     * @return Version string (e.g., "1.0.0")
     */
    String getDatabaseVersion();

    // ===== Health Check (2 methods) =====

    /**
     * Check if database service is healthy.
     * @return true if healthy, false if issues detected
     */
    boolean isHealthy();

    /**
     * Get timestamp of last access.
     * @return Unix timestamp in milliseconds
     */
    long getLastAccessTime();
}
