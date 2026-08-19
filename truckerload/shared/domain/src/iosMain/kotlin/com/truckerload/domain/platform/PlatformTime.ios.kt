package com.truckerload.domain.platform

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual object PlatformTime {
    actual fun epochMillis(): Long =
        (NSDate().timeIntervalSince1970 * 1000.0).toLong()
}
