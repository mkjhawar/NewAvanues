package com.augmentalis.cockpit

/**
 * Desktop (JVM) implementation of [IExternalAppResolver].
 *
 * On desktop platforms, 3rd-party app embedding is not applicable — there is
 * no PackageManager or activity system. All apps resolve as [ExternalAppStatus.NOT_INSTALLED].
 *
 * Future enhancement: could use [ProcessBuilder] to launch native desktop
 * applications alongside the Cockpit window, similar to Android's
 * FLAG_ACTIVITY_LAUNCH_ADJACENT behavior.
 */
class DesktopExternalAppResolver : IExternalAppResolver {

    override fun resolveApp(packageName: String): ExternalAppStatus {
        return ExternalAppStatus.NOT_INSTALLED
    }

    override fun launchAdjacent(packageName: String, activityName: String) {
        // Desktop: no-op for now.
        // Future: ProcessBuilder("open", "-a", appName) on macOS,
        // or Runtime.exec() on Linux/Windows.
    }
}
