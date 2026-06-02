package nep.timeline.cirno.ui.utils

import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import nep.timeline.cirno.GlobalVars
import java.nio.charset.StandardCharsets

object RootLogRepository {
    fun getLogContentPage(startLine: Int, lineCount: Int): List<String> {
        if (lineCount <= 0) return emptyList()
        val content = readSuFile(SuFile(GlobalVars.LOG_DIR, "current.log")) ?: return emptyList()
        return content
            .lines()
            .drop(startLine.coerceAtLeast(0))
            .take(lineCount)
            .filter { it.isNotBlank() }
    }

    private fun readSuFile(file: SuFile): String? {
        return try {
            if (!file.exists()) return null
            SuFileInputStream.open(file).use { input ->
                String(input.readBytes(), StandardCharsets.UTF_8)
            }
        } catch (_: Throwable) {
            null
        }
    }
}
