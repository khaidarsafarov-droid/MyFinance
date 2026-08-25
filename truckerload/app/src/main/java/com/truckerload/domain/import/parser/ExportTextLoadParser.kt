package com.truckerload.domain.import.parser

import com.truckerload.domain.model.Load
import com.truckerload.utils.LoadImporter

/** TruckoRig export `.txt` format: date | A → B | miles | $rate */
class ExportTextLoadParser : LoadParser {
    override fun parse(input: String): List<Load> {
        val rows = LoadImporter.parseExportText(input)
        return LoadImporter.toLoads(rows, input)
    }
}
