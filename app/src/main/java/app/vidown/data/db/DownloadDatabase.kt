package app.vidown.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import app.vidown.data.model.DownloadStatus
import app.vidown.data.model.DownloadTask

class DownloadDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_DOWNLOADS (
                $COL_ID TEXT PRIMARY KEY,
                $COL_URL TEXT NOT NULL,
                $COL_TITLE TEXT NOT NULL,
                $COL_THUMBNAIL_URL TEXT,
                $COL_UPLOADER TEXT,
                $COL_DURATION_SECONDS INTEGER,
                $COL_FORMAT_ID TEXT,
                $COL_FORMAT_LABEL TEXT,
                $COL_IS_AUDIO_ONLY INTEGER,
                $COL_AUDIO_CODEC TEXT,
                $COL_EMBED_SUBTITLES INTEGER DEFAULT 0,
                $COL_SUBTITLE_LANG TEXT,
                $COL_STATUS TEXT NOT NULL,
                $COL_PROGRESS_PERCENT REAL,
                $COL_SPEED_BYTES REAL,
                $COL_ETA_SECONDS INTEGER,
                $COL_FILE_SIZE INTEGER,
                $COL_LOCAL_FILE_PATH TEXT,
                $COL_MEDIA_STORE_URI TEXT,
                $COL_ERROR_MESSAGE TEXT,
                $COL_CREATED_AT INTEGER,
                $COL_COMPLETED_AT INTEGER
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_DOWNLOADS ADD COLUMN $COL_AUDIO_CODEC TEXT")
                db.execSQL("ALTER TABLE $TABLE_DOWNLOADS ADD COLUMN $COL_EMBED_SUBTITLES INTEGER DEFAULT 0")
                db.execSQL("ALTER TABLE $TABLE_DOWNLOADS ADD COLUMN $COL_SUBTITLE_LANG TEXT")
            } catch (e: Exception) {
                db.execSQL("DROP TABLE IF EXISTS $TABLE_DOWNLOADS")
                onCreate(db)
            }
        }
    }

    fun getAllTasks(): List<DownloadTask> {
        val list = mutableListOf<DownloadTask>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_DOWNLOADS,
            null,
            null,
            null,
            null,
            null,
            "$COL_CREATED_AT DESC"
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                val audioCodecCol = c.getColumnIndex(COL_AUDIO_CODEC)
                val audioCodec = if (audioCodecCol >= 0 && !c.isNull(audioCodecCol)) c.getString(audioCodecCol) else null

                val embedSubCol = c.getColumnIndex(COL_EMBED_SUBTITLES)
                val embedSubtitles = if (embedSubCol >= 0) c.getInt(embedSubCol) == 1 else false

                val subLangCol = c.getColumnIndex(COL_SUBTITLE_LANG)
                val subtitleLang = if (subLangCol >= 0 && !c.isNull(subLangCol)) c.getString(subLangCol) else null

                list.add(
                    DownloadTask(
                        id = c.getString(c.getColumnIndexOrThrow(COL_ID)),
                        url = c.getString(c.getColumnIndexOrThrow(COL_URL)),
                        title = c.getString(c.getColumnIndexOrThrow(COL_TITLE)),
                        thumbnailUrl = c.getString(c.getColumnIndexOrThrow(COL_THUMBNAIL_URL)) ?: "",
                        uploader = c.getString(c.getColumnIndexOrThrow(COL_UPLOADER)) ?: "",
                        durationSeconds = c.getLong(c.getColumnIndexOrThrow(COL_DURATION_SECONDS)),
                        formatId = c.getString(c.getColumnIndexOrThrow(COL_FORMAT_ID)) ?: "b",
                        formatLabel = c.getString(c.getColumnIndexOrThrow(COL_FORMAT_LABEL)) ?: "Best",
                        isAudioOnly = c.getInt(c.getColumnIndexOrThrow(COL_IS_AUDIO_ONLY)) == 1,
                        audioCodec = audioCodec,
                        embedSubtitles = embedSubtitles,
                        subtitleLang = subtitleLang,
                        status = DownloadStatus.valueOf(c.getString(c.getColumnIndexOrThrow(COL_STATUS))),
                        progressPercent = c.getFloat(c.getColumnIndexOrThrow(COL_PROGRESS_PERCENT)),
                        speedBytesPerSec = c.getDouble(c.getColumnIndexOrThrow(COL_SPEED_BYTES)),
                        etaSeconds = c.getInt(c.getColumnIndexOrThrow(COL_ETA_SECONDS)),
                        fileSize = c.getLong(c.getColumnIndexOrThrow(COL_FILE_SIZE)),
                        localFilePath = c.getString(c.getColumnIndexOrThrow(COL_LOCAL_FILE_PATH)),
                        mediaStoreUri = c.getString(c.getColumnIndexOrThrow(COL_MEDIA_STORE_URI)),
                        errorMessage = c.getString(c.getColumnIndexOrThrow(COL_ERROR_MESSAGE)),
                        createdAt = c.getLong(c.getColumnIndexOrThrow(COL_CREATED_AT)),
                        completedAt = if (c.isNull(c.getColumnIndexOrThrow(COL_COMPLETED_AT))) null else c.getLong(c.getColumnIndexOrThrow(COL_COMPLETED_AT))
                    )
                )
            }
        }
        return list
    }

    fun insertOrUpdate(task: DownloadTask) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ID, task.id)
            put(COL_URL, task.url)
            put(COL_TITLE, task.title)
            put(COL_THUMBNAIL_URL, task.thumbnailUrl)
            put(COL_UPLOADER, task.uploader)
            put(COL_DURATION_SECONDS, task.durationSeconds)
            put(COL_FORMAT_ID, task.formatId)
            put(COL_FORMAT_LABEL, task.formatLabel)
            put(COL_IS_AUDIO_ONLY, if (task.isAudioOnly) 1 else 0)
            put(COL_AUDIO_CODEC, task.audioCodec)
            put(COL_EMBED_SUBTITLES, if (task.embedSubtitles) 1 else 0)
            put(COL_SUBTITLE_LANG, task.subtitleLang)
            put(COL_STATUS, task.status.name)
            put(COL_PROGRESS_PERCENT, task.progressPercent)
            put(COL_SPEED_BYTES, task.speedBytesPerSec)
            put(COL_ETA_SECONDS, task.etaSeconds)
            put(COL_FILE_SIZE, task.fileSize)
            put(COL_LOCAL_FILE_PATH, task.localFilePath)
            put(COL_MEDIA_STORE_URI, task.mediaStoreUri)
            put(COL_ERROR_MESSAGE, task.errorMessage)
            put(COL_CREATED_AT, task.createdAt)
            put(COL_COMPLETED_AT, task.completedAt)
        }
        db.insertWithOnConflict(TABLE_DOWNLOADS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun insertOrUpdateAll(tasks: List<DownloadTask>) {
        if (tasks.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (task in tasks) {
                val values = ContentValues().apply {
                    put(COL_ID, task.id)
                    put(COL_URL, task.url)
                    put(COL_TITLE, task.title)
                    put(COL_THUMBNAIL_URL, task.thumbnailUrl)
                    put(COL_UPLOADER, task.uploader)
                    put(COL_DURATION_SECONDS, task.durationSeconds)
                    put(COL_FORMAT_ID, task.formatId)
                    put(COL_FORMAT_LABEL, task.formatLabel)
                    put(COL_IS_AUDIO_ONLY, if (task.isAudioOnly) 1 else 0)
                    put(COL_AUDIO_CODEC, task.audioCodec)
                    put(COL_EMBED_SUBTITLES, if (task.embedSubtitles) 1 else 0)
                    put(COL_SUBTITLE_LANG, task.subtitleLang)
                    put(COL_STATUS, task.status.name)
                    put(COL_PROGRESS_PERCENT, task.progressPercent)
                    put(COL_SPEED_BYTES, task.speedBytesPerSec)
                    put(COL_ETA_SECONDS, task.etaSeconds)
                    put(COL_FILE_SIZE, task.fileSize)
                    put(COL_LOCAL_FILE_PATH, task.localFilePath)
                    put(COL_MEDIA_STORE_URI, task.mediaStoreUri)
                    put(COL_ERROR_MESSAGE, task.errorMessage)
                    put(COL_CREATED_AT, task.createdAt)
                    put(COL_COMPLETED_AT, task.completedAt)
                }
                db.insertWithOnConflict(TABLE_DOWNLOADS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteTask(taskId: String) {
        writableDatabase.delete(TABLE_DOWNLOADS, "$COL_ID = ?", arrayOf(taskId))
    }

    companion object {
        const val DATABASE_NAME = "vidown.db"
        const val DATABASE_VERSION = 2

        const val TABLE_DOWNLOADS = "downloads"
        const val COL_ID = "id"
        const val COL_URL = "url"
        const val COL_TITLE = "title"
        const val COL_THUMBNAIL_URL = "thumbnail_url"
        const val COL_UPLOADER = "uploader"
        const val COL_DURATION_SECONDS = "duration_seconds"
        const val COL_FORMAT_ID = "format_id"
        const val COL_FORMAT_LABEL = "format_label"
        const val COL_IS_AUDIO_ONLY = "is_audio_only"
        const val COL_AUDIO_CODEC = "audio_codec"
        const val COL_EMBED_SUBTITLES = "embed_subtitles"
        const val COL_SUBTITLE_LANG = "subtitle_lang"
        const val COL_STATUS = "status"
        const val COL_PROGRESS_PERCENT = "progress_percent"
        const val COL_SPEED_BYTES = "speed_bytes"
        const val COL_ETA_SECONDS = "eta_seconds"
        const val COL_FILE_SIZE = "file_size"
        const val COL_LOCAL_FILE_PATH = "local_file_path"
        const val COL_MEDIA_STORE_URI = "media_store_uri"
        const val COL_ERROR_MESSAGE = "error_message"
        const val COL_CREATED_AT = "created_at"
        const val COL_COMPLETED_AT = "completed_at"
    }
}
