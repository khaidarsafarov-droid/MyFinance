package com.truckerload.domain.platform

/** Wall-clock millis. JVM/Android use [System.currentTimeMillis]; iOS uses Foundation. */
expect object PlatformTime {
    fun epochMillis(): Long
}
