package nep.timeline.cirno.ui.utils

import android.content.ContentResolver
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import nep.timeline.cirno.configs.settings.ApplicationSettings
import nep.timeline.cirno.configs.settings.GlobalSettings
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ConfigBackupZipUtils {
    private const val GLOBAL_SETTINGS_NAME = "GlobalSettings.json"
    private const val APPLICATION_SETTINGS_NAME = "ApplicationSettings.json"
    private val gson = Gson()

    data class RestoredConfig(val globalJson: String, val applicationJson: String)

    enum class RestoreError {
        OPEN_INPUT_FAILED,
        INVALID_ZIP_STRUCTURE,
        MISSING_REQUIRED_FILES,
        INVALID_JSON,
        IO_ERROR
    }

    class RestoreException(val error: RestoreError, cause: Throwable? = null) : Exception(cause)

    fun writeBackupZip(
        resolver: ContentResolver,
        outputUri: Uri,
        globalJson: String,
        applicationJson: String
    ) {
        val output = resolver.openOutputStream(outputUri) ?: throw IOException("Cannot open output stream")
        output.use { stream ->
            ZipOutputStream(BufferedOutputStream(stream)).use { zip ->
                writeZipEntry(zip, GLOBAL_SETTINGS_NAME, globalJson)
                writeZipEntry(zip, APPLICATION_SETTINGS_NAME, applicationJson)
            }
        }
    }

    fun readAndValidateBackupZip(resolver: ContentResolver, inputUri: Uri): RestoredConfig {
        val input = resolver.openInputStream(inputUri) ?: throw RestoreException(RestoreError.OPEN_INPUT_FAILED)
        input.use { stream ->
            try {
                ZipInputStream(BufferedInputStream(stream)).use { zip ->
                    var globalJson: String? = null
                    var applicationJson: String? = null
                    var entryCount = 0
                    var entry = zip.nextEntry
                    while (entry != null) {
                        entryCount++
                        if (entry.isDirectory) {
                            throw RestoreException(RestoreError.INVALID_ZIP_STRUCTURE)
                        }
                        val entryName = entry.name
                        val content = zip.readBytes().toString(Charsets.UTF_8)
                        when (entryName) {
                            GLOBAL_SETTINGS_NAME -> {
                                if (globalJson != null) throw RestoreException(RestoreError.INVALID_ZIP_STRUCTURE)
                                globalJson = content
                            }

                            APPLICATION_SETTINGS_NAME -> {
                                if (applicationJson != null) throw RestoreException(RestoreError.INVALID_ZIP_STRUCTURE)
                                applicationJson = content
                            }

                            else -> throw RestoreException(RestoreError.INVALID_ZIP_STRUCTURE)
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }

                    if (entryCount != 2 || globalJson == null || applicationJson == null) {
                        throw RestoreException(RestoreError.MISSING_REQUIRED_FILES)
                    }

                    validateJson(globalJson, applicationJson)
                    return RestoredConfig(globalJson, applicationJson)
                }
            } catch (e: RestoreException) {
                throw e
            } catch (e: java.util.zip.ZipException) {
                throw RestoreException(RestoreError.INVALID_ZIP_STRUCTURE, e)
            } catch (e: IOException) {
                throw RestoreException(RestoreError.IO_ERROR, e)
            }
        }
    }

    private fun validateJson(globalJson: String, applicationJson: String) {
        try {
            gson.fromJson(globalJson, GlobalSettings::class.java) ?: throw RestoreException(RestoreError.INVALID_JSON)
            val app = gson.fromJson(applicationJson, ApplicationSettings::class.java) ?: throw RestoreException(RestoreError.INVALID_JSON)
            ApplicationSettings.ensureInitialized(app)
        } catch (e: RestoreException) {
            throw e
        } catch (e: JsonSyntaxException) {
            throw RestoreException(RestoreError.INVALID_JSON, e)
        } catch (e: Throwable) {
            throw RestoreException(RestoreError.INVALID_JSON, e)
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        val entry = ZipEntry(name)
        zip.putNextEntry(entry)
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }
}
