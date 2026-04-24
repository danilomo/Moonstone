package net.sourceforge.moonstone.desktop.setup

import net.sourceforge.kleinlisp.objects.BooleanObject
import net.sourceforge.kleinlisp.objects.IntObject
import net.sourceforge.kleinlisp.objects.StringObject
import net.sourceforge.kleinlisp.objects.VoidObject
import net.sourceforge.moonstone.debug.ReloadableRuntime
import net.sourceforge.moonstone.runtime.MoonstoneRuntime

/**
 * Utilities for registering desktop-specific Scheme functions.
 */
object DesktopExtensionsRegistrar {
    /**
     * Register desktop-specific Scheme functions.
     * These provide desktop equivalents of Android-specific functions for cross-platform compatibility.
     */
    fun register(runtime: MoonstoneRuntime) {
        val env = runtime.environment()

        // (toast message) - Print message to console (desktop equivalent of Android toast)
        env.registerFunction("toast") { params ->
            if (params.isNotEmpty()) {
                val message =
                    when (val first = params[0]) {
                        is StringObject -> first.value()
                        else -> first.toString()
                    }
                println("[Toast] $message")
            }
            VoidObject.VOID
        }

        // (vibrate duration) - No-op on desktop
        env.registerFunction("vibrate") { _ ->
            VoidObject.VOID
        }

        // (dark-mode?) - Always returns false on desktop (could be improved)
        env.registerFunction("dark-mode?") { _ ->
            BooleanObject.FALSE
        }

        // (screen-width) - Return a default width
        env.registerFunction("screen-width") { _ ->
            IntObject(1200)
        }

        // (screen-height) - Return a default height
        env.registerFunction("screen-height") { _ ->
            IntObject(800)
        }

        // (android-version) - Return 0 on desktop
        env.registerFunction("android-version") { _ ->
            IntObject(0)
        }

        // (device-model) - Return "Desktop" on desktop
        env.registerFunction("device-model") { _ ->
            StringObject("Desktop")
        }
    }

    /**
     * Register desktop-specific Scheme functions for a reloadable runtime.
     */
    fun register(runtime: ReloadableRuntime) {
        register(runtime.baseRuntime)
    }
}
