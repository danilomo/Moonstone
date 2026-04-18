package net.sourceforge.moonstone.debug

import java.nio.file.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Monitors a script file for changes and triggers reload callbacks.
 * Uses Java NIO WatchService for efficient file system monitoring.
 */
class HotReloader(
    private val filePath: Path,
    private val debounceMs: Long = 100,
    private val onReload: (Path) -> Unit,
    private val onError: (Throwable) -> Unit = {}
) {
    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    private val running = AtomicBoolean(false)
    private var watchThread: Thread? = null
    private var lastModified: Long = 0
    private var lastReloadTime: Long = 0

    /**
     * Start watching the file for changes.
     */
    fun start() {
        if (running.getAndSet(true)) {
            return // Already running
        }

        val directory = filePath.parent ?: filePath.toAbsolutePath().parent
        val fileName = filePath.fileName.toString()

        try {
            directory.register(
                watchService,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_CREATE
            )
        } catch (e: Exception) {
            onError(RuntimeException("Failed to watch directory: ${directory}", e))
            running.set(false)
            return
        }

        lastModified = filePath.toFile().lastModified()

        watchThread = thread(name = "HotReloader", isDaemon = true) {
            while (running.get()) {
                try {
                    val key = watchService.poll(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                    if (key == null) continue

                    for (event in key.pollEvents()) {
                        val kind = event.kind()

                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue
                        }

                        val context = event.context() as? Path
                        if (context?.toString() == fileName) {
                            val currentModified = filePath.toFile().lastModified()
                            val now = System.currentTimeMillis()

                            // Debounce: only reload if enough time has passed
                            if (currentModified != lastModified && now - lastReloadTime > debounceMs) {
                                lastModified = currentModified
                                lastReloadTime = now

                                // Small delay to ensure file write is complete
                                Thread.sleep(debounceMs)

                                try {
                                    onReload(filePath)
                                } catch (e: Exception) {
                                    onError(e)
                                }
                            }
                        }
                    }

                    if (!key.reset()) {
                        break
                    }
                } catch (e: InterruptedException) {
                    break
                } catch (e: ClosedWatchServiceException) {
                    break
                } catch (e: Exception) {
                    onError(e)
                }
            }
        }
    }

    /**
     * Stop watching for changes.
     */
    fun stop() {
        running.set(false)
        try {
            watchService.close()
        } catch (e: Exception) {
            // Ignore close errors
        }
        watchThread?.interrupt()
        watchThread = null
    }

    /**
     * Check if the reloader is currently running.
     */
    fun isRunning(): Boolean = running.get()
}
