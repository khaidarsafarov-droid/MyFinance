package com.truckerload.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random

/** The zip keeps photo/scan bytes (and so their EXIF GPS/date/time) exactly as they were. */
class BackupArchiveTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val json = """{"schemaVersion":2,"loads":[]}"""

    @Test
    fun roundTripKeepsBytesExactly() {
        val photoBytes = jpegWithExif(size = 1_500_000)
        val scanBytes = "%PDF-1.7 scan".toByteArray() + Random(7).nextBytes(200_000)
        val photo = tmp.newFile("garner.jpg").apply { writeBytes(photoBytes) }
        val scan = tmp.newFile("bol.pdf").apply { writeBytes(scanBytes) }
        val backup = BackupData(
            photos = listOf(BackupPhoto(id = "photo-1", fileName = photo.name, filePath = photo.absolutePath)),
            scans = listOf(BackupScan(id = "scan-1", fileName = scan.name, filePath = scan.absolutePath)),
        )

        val media = BackupArchive.mediaEntries(backup)
        assertEquals(2, media.size)
        val out = ByteArrayOutputStream()
        BackupArchive.write(out, json.toByteArray(), media)
        val bytes = out.toByteArray()
        assertTrue(BackupArchive.isZip(bytes.copyOf(4)))

        val staging = tmp.newFolder("staging")
        val contents = BackupArchive.read(ByteArrayInputStream(bytes), staging, maxJsonBytes = 1024)

        assertEquals(json, contents.json)
        assertArrayEquals(photoBytes, contents.photos.getValue("photo-1").readBytes())
        assertArrayEquals(scanBytes, contents.scans.getValue("scan-1").readBytes())
    }

    @Test
    fun missingFilesAndUnsafeIdsAreLeftOut() {
        val photo = tmp.newFile("a.jpg").apply { writeBytes(jpegWithExif(64)) }
        val backup = BackupData(
            photos = listOf(
                BackupPhoto(id = "ok", filePath = photo.absolutePath),
                BackupPhoto(id = "gone", filePath = File(tmp.root, "gone.jpg").absolutePath),
                BackupPhoto(id = "../evil", filePath = photo.absolutePath),
                BackupPhoto(id = "..", filePath = photo.absolutePath),
            ),
        )
        assertEquals(listOf("media/photos/ok"), BackupArchive.mediaEntries(backup).map { it.name })
    }

    @Test
    fun entryNamesNeverBecomePaths() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry(BackupArchive.JSON_ENTRY))
            zip.write(json.toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("media/photos/../../escape"))
            zip.write(byteArrayOf(1, 2, 3))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("../outside.txt"))
            zip.write(byteArrayOf(4))
            zip.closeEntry()
        }
        val staging = tmp.newFolder("staging")
        val contents = BackupArchive.read(ByteArrayInputStream(out.toByteArray()), staging, maxJsonBytes = 1024)

        assertTrue(contents.photos.isEmpty())
        assertFalse(File(tmp.root, "outside.txt").exists())
        assertFalse(File(tmp.root, "escape").exists())
        assertTrue(staging.listFiles().isNullOrEmpty())
    }

    @Test(expected = BackupRestoreException.InvalidFormat::class)
    fun zipWithoutJsonIsRejected() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("media/photos/x"))
            zip.write(byteArrayOf(1))
            zip.closeEntry()
        }
        BackupArchive.read(ByteArrayInputStream(out.toByteArray()), tmp.newFolder(), maxJsonBytes = 1024)
    }

    @Test(expected = BackupRestoreException.TooLarge::class)
    fun oversizedJsonIsRejected() {
        val out = ByteArrayOutputStream()
        BackupArchive.write(out, ByteArray(4096) { 'a'.code.toByte() }, emptyList())
        BackupArchive.read(ByteArrayInputStream(out.toByteArray()), tmp.newFolder(), maxJsonBytes = 1024)
    }

    @Test(expected = IOException::class)
    fun stopsWhenTheDeviceRunsLowOnSpace() {
        val photo = tmp.newFile("a.jpg").apply { writeBytes(jpegWithExif(1024)) }
        val out = ByteArrayOutputStream()
        BackupArchive.write(
            out,
            json.toByteArray(),
            BackupArchive.mediaEntries(BackupData(photos = listOf(BackupPhoto(id = "p", filePath = photo.absolutePath)))),
        )
        BackupArchive.read(ByteArrayInputStream(out.toByteArray()), tmp.newFolder(), 1024, minFreeBytes = Long.MAX_VALUE)
    }

    @Test
    fun plainJsonIsNotAnArchive() {
        assertFalse(BackupArchive.isZip(json.toByteArray().copyOf(4)))
        assertFalse(BackupArchive.isZip(ByteArray(2)))
    }

    /** SOI, an APP1 "Exif" segment with GPS-looking payload, then filler and EOI. */
    private fun jpegWithExif(size: Int): ByteArray {
        val header = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE1.toByte(), 0x00, 0x20) +
            "Exif\u0000\u0000GPS 35.711001 -78.614002".toByteArray()
        val body = Random(size).nextBytes(maxOf(0, size - header.size - 2))
        return header + body + byteArrayOf(0xFF.toByte(), 0xD9.toByte())
    }
}
