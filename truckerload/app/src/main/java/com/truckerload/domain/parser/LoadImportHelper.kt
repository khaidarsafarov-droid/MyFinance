package com.truckerload.domain.parser

import com.truckerload.domain.model.Load
import com.truckerload.utils.FeedbackManager

data class LoadImportStats(
    val added: Int = 0,
    val updated: Int = 0,
    val replaced: Int = 0,
    val skipped: Int = 0,
) {
    val changed: Int get() = added + updated + replaced
}

object LoadImportHelper {

    suspend fun processAll(
        processor: LoadProcessor,
        loads: List<Load>,
        config: ParserConfig,
        messageDateSeconds: Long? = null,
        playFeedback: Boolean = false,
    ): LoadImportStats {
        var added = 0
        var updated = 0
        var replaced = 0
        var skipped = 0

        val results = processor.processLoads(
            parsedLoads = loads,
            config = config,
            messageDateSeconds = messageDateSeconds,
            playFeedback = false,
        )
        for (result in results) {
            when (result) {
                ProcessingResult.Added -> added++
                is ProcessingResult.Updated -> updated++
                is ProcessingResult.Replaced -> replaced++
                is ProcessingResult.Skipped -> skipped++
            }
        }

        if (playFeedback && (added + updated + replaced) > 0) {
            FeedbackManager.onLoadAdded()
        }

        return LoadImportStats(
            added = added,
            updated = updated,
            replaced = replaced,
            skipped = skipped,
        )
    }
}
