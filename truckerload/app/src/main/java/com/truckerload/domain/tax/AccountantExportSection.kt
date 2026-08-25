package com.truckerload.domain.tax

/**
 * What the driver can include in the accountant export workbook.
 * [ALL] expands to every section that has rows for the selected year.
 */
enum class AccountantExportSection {
    LOADS,
    DIESEL,
    PER_DIEM,
    MAINTENANCE,
    ALL,
}
