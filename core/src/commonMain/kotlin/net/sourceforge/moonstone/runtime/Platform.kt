package net.sourceforge.moonstone.runtime

/**
 * Enumeration of supported platforms.
 */
enum class Platform {
    ANDROID,
    DESKTOP_JVM;

    companion object {
        fun detect(): Platform = when {
            System.getProperty("java.vm.name")?.contains("Android") == true -> ANDROID
            else -> DESKTOP_JVM
        }
    }
}
