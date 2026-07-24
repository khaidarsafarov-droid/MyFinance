package com.truckerload.utils

import java.util.Locale

fun String.normalizeKey(): String = trim().lowercase(Locale.US)
