package com.truckerload.sync.import

import android.content.Context
import com.truckerload.domain.import.model.ImportReport
import com.truckerload.domain.import.model.ParsedLoad
import com.truckerload.domain.import.model.SkipReason
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ImportReportFormatterTest {

    private lateinit var context: Context
    private lateinit var formatter: ImportReportFormatter

    @Before
    fun setUp() {
        context = mock()
        whenever(context.getString(any<Int>())).thenReturn("label")
        whenever(context.getString(any<Int>(), any())).thenAnswer { invocation ->
            invocation.arguments.drop(1).joinToString(" ")
        }
        whenever(context.getString(any<Int>(), any(), any())).thenAnswer { invocation ->
            invocation.arguments.drop(1).joinToString(" ")
        }
        whenever(context.getString(any<Int>(), any(), any(), any())).thenAnswer { invocation ->
            invocation.arguments.drop(1).joinToString(" ")
        }
        whenever(context.getString(any<Int>(), any(), any(), any(), any())).thenAnswer { invocation ->
            invocation.arguments.drop(1).joinToString(" ")
        }
        formatter = ImportReportFormatter(context)
    }

    @Test
    fun format_includesCountsAndAddedTripId() {
        val report = ImportReport(
            totalFound = 2,
            added = 1,
            updated = 0,
            replaced = 0,
            skipped = 1,
            failed = 0,
            addedLoads = listOf(
                ParsedLoad(
                    tripId = "T-IMPORT1",
                    totalRate = 2_500.0,
                    totalMiles = 850.0,
                    pointA = "Garner, NC",
                    pointB = "Atlanta, GA",
                    stopCount = 2,
                ),
            ),
            skippedLoads = listOf("T-DUP" to SkipReason.DUPLICATE),
            failedBlocks = emptyList(),
            durationMs = 120,
            filesProcessed = 1,
            fileName = "relay_export.txt",
        )

        val formatted = formatter.format(report)

        assertTrue(formatted.contains("T-IMPORT1"))
        assertTrue(formatted.contains("T-DUP"))
        assertTrue(formatted.contains("relay_export.txt"))
    }
}
