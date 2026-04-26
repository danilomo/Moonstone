package net.sourceforge.moonstone.android

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import net.sourceforge.kleinlisp.LispObject
import net.sourceforge.kleinlisp.objects.BooleanObject
import net.sourceforge.kleinlisp.objects.IntObject
import net.sourceforge.kleinlisp.objects.StringObject
import net.sourceforge.kleinlisp.objects.VoidObject
import net.sourceforge.moonstone.runtime.MoonstoneRuntime

/**
 * Extension functions for Android-specific functionality.
 *
 * These functions are registered with the KleinLisp runtime when running on Android,
 * allowing Scheme scripts to access Android-specific features.
 */
object AndroidExtensions {
    /**
     * Register Android-specific functions with the runtime.
     *
     * Functions registered:
     * - (toast message) - Show a toast message
     * - (vibrate ms) - Vibrate for specified milliseconds
     * - (dark-mode?) - Check if dark mode is enabled
     * - (screen-width) - Get screen width in dp
     * - (screen-height) - Get screen height in dp
     * - (android-version) - Get Android API level
     */
    fun register(
        runtime: MoonstoneRuntime,
        context: Context,
    ) {
        val env = runtime.environment()

        registerToastFunction(env, context)
        registerVibrateFunction(env, context)
        registerDarkModeFunction(env, context)
        registerScreenDimensionFunctions(env, context)
        registerDeviceInfoFunctions(env)
    }

    private fun registerToastFunction(
        env: net.sourceforge.kleinlisp.LispEnvironment,
        context: Context,
    ) {
        env.registerFunction("toast") { params ->
            if (params.isNotEmpty()) {
                val message =
                    when (val first = params[0]) {
                        is StringObject -> first.value()
                        else -> first.toString()
                    }
                val duration = getToastDuration(params.toList())
                Toast.makeText(context, message, duration).show()
            }
            VoidObject.VOID
        }
    }

    private fun getToastDuration(params: List<LispObject>): Int =
        if (params.size > 1) {
            val durationArg = params[1]
            when {
                durationArg is IntObject && durationArg.value().toLong() > 2000 -> Toast.LENGTH_LONG
                durationArg.toString() == "long" -> Toast.LENGTH_LONG
                else -> Toast.LENGTH_SHORT
            }
        } else {
            Toast.LENGTH_SHORT
        }

    private fun registerVibrateFunction(
        env: net.sourceforge.kleinlisp.LispEnvironment,
        context: Context,
    ) {
        env.registerFunction("vibrate") { params ->
            val ms =
                if (params.isNotEmpty()) {
                    when (val first = params[0]) {
                        is IntObject -> first.value().toLong()
                        else -> 100L
                    }
                } else {
                    100L
                }

            performVibration(context, ms)
            VoidObject.VOID
        }
    }

    private fun performVibration(
        context: Context,
        ms: Long,
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                performLegacyVibration(context, ms)
            }
        } catch (_: Exception) {
            // Vibration may not be available or permission denied
        }
    }

    @Suppress("DEPRECATION")
    private fun performLegacyVibration(
        context: Context,
        ms: Long,
    ) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(ms)
        }
    }

    private fun registerDarkModeFunction(
        env: net.sourceforge.kleinlisp.LispEnvironment,
        context: Context,
    ) {
        env.registerFunction("dark-mode?") { _ ->
            val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
                BooleanObject.TRUE
            } else {
                BooleanObject.FALSE
            }
        }
    }

    private fun registerScreenDimensionFunctions(
        env: net.sourceforge.kleinlisp.LispEnvironment,
        context: Context,
    ) {
        env.registerFunction("screen-width") { _ ->
            val displayMetrics = context.resources.displayMetrics
            IntObject.valueOf((displayMetrics.widthPixels / displayMetrics.density).toInt())
        }

        env.registerFunction("screen-height") { _ ->
            val displayMetrics = context.resources.displayMetrics
            IntObject.valueOf((displayMetrics.heightPixels / displayMetrics.density).toInt())
        }
    }

    private fun registerDeviceInfoFunctions(env: net.sourceforge.kleinlisp.LispEnvironment) {
        env.registerFunction("android-version") { _ ->
            IntObject.valueOf(Build.VERSION.SDK_INT)
        }

        env.registerFunction("device-model") { _ ->
            StringObject(Build.MODEL)
        }
    }
}
