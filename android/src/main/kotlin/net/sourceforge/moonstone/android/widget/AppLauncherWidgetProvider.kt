package net.sourceforge.moonstone.android.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.widget.RemoteViews
import androidx.core.graphics.createBitmap
import net.sourceforge.moonstone.android.AppActivity
import net.sourceforge.moonstone.android.LauncherActivity
import net.sourceforge.moonstone.android.R
import java.io.File
import kotlin.math.absoluteValue

/**
 * Widget provider for KleinLisp app launcher widgets.
 *
 * Each widget instance can be configured to launch a specific KleinLisp app.
 */
class AppLauncherWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val repository = WidgetRepository(context)

        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId, repository)
        }
    }

    override fun onDeleted(
        context: Context,
        appWidgetIds: IntArray,
    ) {
        val repository = WidgetRepository(context)

        for (widgetId in appWidgetIds) {
            repository.deleteWidgetConfig(widgetId)
        }
    }

    companion object {
        /**
         * Update a single widget.
         */
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            repository: WidgetRepository = WidgetRepository(context),
        ) {
            val config = repository.loadWidgetConfig(widgetId)
            val views = RemoteViews(context.packageName, R.layout.widget_app_launcher)

            if (config == null) {
                // Widget not configured yet - show placeholder
                views.setTextViewText(R.id.widget_name, context.getString(R.string.widget_not_configured))
                views.setImageViewResource(R.id.widget_icon, R.drawable.ic_widget_default)
            } else {
                val appFolder = File(config.appFolder)
                val exists = if (config.isFolder) {
                    appFolder.exists() && appFolder.isDirectory
                } else {
                    appFolder.exists() && File(appFolder, "app.scm").exists()
                }

                if (!exists) {
                    views.setTextViewText(R.id.widget_name, context.getString(R.string.widget_app_not_found))
                    views.setImageViewResource(R.id.widget_icon, R.drawable.ic_widget_default)
                } else {
                    views.setTextViewText(R.id.widget_name, config.appName)

                    val iconBitmap = config.iconPath?.let { loadIconBitmap(it) }
                        ?: config.emoji?.let { generateEmojiIcon(it) }
                        ?: generateDefaultIcon(config.appName)
                    views.setImageViewBitmap(R.id.widget_icon, iconBitmap)

                    val launchIntent = if (config.isFolder) {
                        Intent(context, LauncherActivity::class.java).apply {
                            putExtra(LauncherActivity.EXTRA_FOLDER_PATH, config.appFolder)
                            putExtra(LauncherActivity.EXTRA_FOLDER_NAME, config.appName)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                    } else {
                        Intent(context, AppActivity::class.java).apply {
                            putExtra(AppActivity.EXTRA_APP_FOLDER, config.appFolder)
                            putExtra(AppActivity.EXTRA_APP_NAME, config.appName)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                    }

                    val pendingIntent =
                        PendingIntent.getActivity(
                            context,
                            widgetId,
                            launchIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        )

                    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
                }
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }

        /**
         * Load icon bitmap from file path.
         */
        private fun loadIconBitmap(iconPath: String): Bitmap? {
            return try {
                val file = File(iconPath)
                if (!file.exists() || !file.isFile) {
                    return null
                }

                val options =
                    BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                BitmapFactory.decodeFile(iconPath, options)

                // Calculate sample size for 96x96 target (widget size with some padding)
                options.inSampleSize = calculateInSampleSize(options, 96, 96)
                options.inJustDecodeBounds = false

                BitmapFactory.decodeFile(iconPath, options)
            } catch (e: Exception) {
                null
            }
        }

        private fun calculateInSampleSize(
            options: BitmapFactory.Options,
            reqWidth: Int,
            reqHeight: Int,
        ): Int {
            val (height, width) = options.outHeight to options.outWidth
            var inSampleSize = 1

            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2

                while ((halfHeight / inSampleSize) >= reqHeight &&
                    (halfWidth / inSampleSize) >= reqWidth
                ) {
                    inSampleSize *= 2
                }
            }

            return inSampleSize
        }

        private fun generateEmojiIcon(emoji: String): Bitmap {
            val size = 96
            val bitmap = createBitmap(size, size)
            val canvas = Canvas(bitmap)
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = size * 0.72f
                    textAlign = Paint.Align.CENTER
                }
            val textY = size / 2f - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(emoji, size / 2f, textY, paint)
            return bitmap
        }

        private fun generateDefaultIcon(appName: String): Bitmap {
            val size = 96
            val bitmap = createBitmap(size, size)
            val canvas = Canvas(bitmap)

            // Generate hue from app name (same algorithm as LauncherActivity)
            val hue = (appName.hashCode() % 360).absoluteValue.toFloat()

            // Create gradient colors
            val color1 = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.55f, 0.85f))
            val color2 = android.graphics.Color.HSVToColor(floatArrayOf((hue + 30) % 360, 0.60f, 0.70f))

            // Draw gradient background with rounded corners
            val backgroundPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader =
                        LinearGradient(
                            0f,
                            0f,
                            size.toFloat(),
                            size.toFloat(),
                            color1,
                            color2,
                            Shader.TileMode.CLAMP,
                        )
                }
            val cornerRadius = size * 0.22f
            canvas.drawRoundRect(
                0f,
                0f,
                size.toFloat(),
                size.toFloat(),
                cornerRadius,
                cornerRadius,
                backgroundPaint,
            )

            // Draw initials
            val initials = appName.take(2).uppercase()
            val textPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    textSize = size * 0.4f
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = true
                }

            val textX = size / 2f
            val textY = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(initials, textX, textY, textPaint)

            return bitmap
        }
    }
}
