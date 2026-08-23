package com.truckerload.widget

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetRingHeroAndActionsTest {

    @Test
    fun ringCenter_stacksGrossPercentAndGoal() {
        val xml = readRes("layout/widget_ring_center.xml")
        val gross = xml.indexOf("android:id=\"@+id/widget_gross_hero\"")
        val percent = xml.indexOf("android:id=\"@+id/widget_ring_percent\"")
        val goal = xml.indexOf("android:id=\"@+id/widget_goal_subtitle\"")
        assertTrue(gross >= 0)
        assertTrue(percent > gross)
        assertTrue(goal > percent)
    }

    @Test
    fun standardAndExpanded_includeRingCenterInsteadOfSideGross() {
        listOf("layout/widget_standard.xml", "layout/widget_expanded.xml").forEach { path ->
            val xml = readRes(path)
            assertTrue(path, xml.contains("@layout/widget_ring_center"))
            assertTrue(path, !xml.contains("android:id=\"@+id/widget_gross_hero\""))
            assertTrue(path, !xml.contains("android:id=\"@+id/widget_goal_subtitle\""))
        }
    }

    @Test
    fun quickActions_areCameraThenScannerThenDiesel() {
        val xml = readRes("layout/widget_quick_actions.xml")
        val camera = xml.indexOf("android:id=\"@+id/widget_btn_camera\"")
        val scanner = xml.indexOf("android:id=\"@+id/widget_btn_scanner\"")
        val diesel = xml.indexOf("android:id=\"@+id/widget_btn_diesel\"")
        assertTrue(camera >= 0)
        assertTrue(scanner > camera)
        assertTrue(diesel > scanner)
        assertTrue(xml.contains("@drawable/ic_widget_diesel"))
    }

    @Test
    fun compactQuickActions_includeDieselBesideCameraAndScan() {
        val xml = readRes("layout/widget_compact.xml")
        val camera = xml.indexOf("android:id=\"@+id/widget_btn_camera\"")
        val scanner = xml.indexOf("android:id=\"@+id/widget_btn_scanner\"")
        val diesel = xml.indexOf("android:id=\"@+id/widget_btn_diesel\"")
        assertTrue(camera >= 0)
        assertTrue(scanner > camera)
        assertTrue(diesel > scanner)
    }

    private fun readRes(relativePath: String): String {
        val candidates = listOf(
            File("src/main/res/$relativePath"),
            File("app/src/main/res/$relativePath"),
            File("../app/src/main/res/$relativePath"),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("Resource not found: $relativePath")
    }
}
