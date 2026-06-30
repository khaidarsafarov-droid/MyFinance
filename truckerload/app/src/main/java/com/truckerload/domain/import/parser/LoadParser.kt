package com.truckerload.domain.import.parser

import com.truckerload.domain.model.Load

interface LoadParser {
    fun parse(input: String): List<Load>
}
