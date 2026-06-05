package nep.timeline.cirno.ui.utils

import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import nep.timeline.cirno.GlobalVars
import java.io.InputStream
import java.nio.charset.StandardCharsets

data class LogSnapshot(
    val lines: List<String>,
    val size: Long,
    val truncated: Boolean,
)

object RootLogRepository {
    private const val MAX_LOG_BYTES = 4 * 1024 * 1024L
    private const val MAX_LOG_LINES = 10_000

    fun getLogSnapshot(): LogSnapshot {
        val file = SuFile(GlobalVars.LOG_DIR, "current.log")
        return readSnapshot(file)
    }

    fun getLogContentAfter(byteOffset: Long): LogSnapshot {
        val file = SuFile(GlobalVars.LOG_DIR, "current.log")
        return try {
            if (!file.exists()) return LogSnapshot(emptyList(), 0L, false)
            val size = file.length()
            if (size < byteOffset || byteOffset < 0L) return readSnapshot(file)
            if (size == byteOffset) return LogSnapshot(emptyList(), size, false)

            SuFileInputStream.open(file).use { input ->
                input.skipFully(byteOffset)
                LogSnapshot(
                    lines = bytesToLines(input.readBytes()),
                    size = size,
                    truncated = false,
                )
            }
        } catch (_: Throwable) {
            LogSnapshot(emptyList(), 0L, false)
        }
    }

    private fun readSnapshot(file: SuFile): LogSnapshot {
        return try {
            if (!file.exists()) return LogSnapshot(emptyList(), 0L, false)
            val size = file.length()
            val readOffset = (size - MAX_LOG_BYTES).coerceAtLeast(0L)
            val truncatedByBytes = readOffset > 0L

            SuFileInputStream.open(file).use { input ->
                input.skipFully(readOffset)
                var content = String(input.readBytes(), StandardCharsets.UTF_8)
                if (truncatedByBytes) {
                    content = content.substringAfter('\n', "")
                }
                val lines = content.lines().filter { it.isNotBlank() }
                val limitedLines = if (lines.size > MAX_LOG_LINES) {
                    lines.takeLast(MAX_LOG_LINES)
                } else {
                    lines
                }
                LogSnapshot(
                    lines = limitedLines,
                    size = size,
                    truncated = truncatedByBytes || lines.size > MAX_LOG_LINES,
                )
            }
        } catch (_: Throwable) {
            LogSnapshot(emptyList(), 0L, false)
        }
    }

    private fun bytesToLines(bytes: ByteArray): List<String> {
        if (bytes.isEmpty()) return emptyList()
        return String(bytes, StandardCharsets.UTF_8).lines().filter { it.isNotBlank() }
    }

    private fun InputStream.skipFully(byteCount: Long) {
        var remaining = byteCount
        while (remaining > 0L) {
            val skipped = skip(remaining)
            if (skipped <= 0L) break
            remaining -= skipped
        }
    }
}
