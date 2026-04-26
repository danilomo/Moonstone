package net.sourceforge.moonstone.debug

import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
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
    private val onError: (Throwable) -> Unit = {},
) {
    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    private val running = AtomicBoolean(false)
    private var watchThread: Thread? = null
    private var lastModified: Long = 0
    private var lastReloadTime: Long = 0

    /**
     * Process a single watch event.
     */
    private fun processEvent(
        event: java.nio.file.WatchEvent<*>,
        fileName: String,
    ) {
        val kind = event.kind()
        if (kind == StandardWatchEventKinds.OVERFLOW) {
            return
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

    /**
     * Start watching the file for changes.
     */
    fun start() {
        if (running.getAndSet(true)) {
            return // Already running
        }

        val directory = filePath.parent ?: filePath.toAbsolutePath().parent
        val fileName = filePath.fileName.toString()

        if (!registerWatchDirectory(directory)) {
            return
        }

        lastModified = filePath.toFile().lastModified()
        startWatchThread(fileName)
    }

    private fun registerWatchDirectory(directory: Path): Boolean =
        try {
            directory.register(
                watchService,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_CREATE,
            )
            true
        } catch (e: Exception) {
            onError(RuntimeException("Failed to watch directory: $directory", e))
            running.set(false)
            false
        }

    private fun startWatchThread(fileName: String) {
        watchThread =
            thread(name = "HotReloader", isDaemon = true) {
                watchLoop(fileName)
            }
    }

    private fun watchLoop(fileName: String) {
        while (running.get() && !Thread.currentThread().isInterrupted) {
            try {
                processWatchKey(fileName)
            } catch (e: InterruptedException) {
                return
            } catch (e: ClosedWatchServiceException) {
                return
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private fun processWatchKey(fileName: String) {
        val key =
            watchService.poll(
                500,
                java.util.concurrent.TimeUnit.MILLISECONDS,
            ) ?: return

        key.pollEvents().forEach { event -> processEvent(event, fileName) }

        if (!key.reset()) {
            throw InterruptedException("Watch key reset failed")
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
