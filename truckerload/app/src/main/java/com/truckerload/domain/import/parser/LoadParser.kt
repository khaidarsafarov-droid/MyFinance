package com.truckerload.domain.import.parser

import com.truckerload.domain.model.Load

interface LoadParser {
    fun parse(input: String): List<Load>
    fun parse(input: String, referenceMillis: Long): List<Load> = parse(input)
}
