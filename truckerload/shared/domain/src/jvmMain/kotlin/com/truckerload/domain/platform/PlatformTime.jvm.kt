package com.truckerload.domain.platform

actual object PlatformTime {
    actual fun epochMillis(): Long = System.currentTimeMillis()
}
