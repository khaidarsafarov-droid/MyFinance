package com.truckerload.data.social

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class VoiceNoteRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAt = 0L

    fun start(): File {
        stop()
        val file = File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
        outputFile = file
        startedAt = System.currentTimeMillis()
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(64_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        return file
    }

    fun stop(): Pair<File?, Long> {
        val file = outputFile
        val duration = if (startedAt > 0) System.currentTimeMillis() - startedAt else 0L
        runCatching {
            recorder?.apply {
                stop()
                release()
            }
        }
        recorder = null
        outputFile = null
        startedAt = 0L
        return file to duration.coerceAtLeast(0L)
    }

    fun isRecording(): Boolean = recorder != null
}
