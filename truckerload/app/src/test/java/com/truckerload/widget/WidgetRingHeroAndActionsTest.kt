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
    fun quickActions_areScannerThenCameraThenDiesel() {
        val xml = readRes("layout/widget_quick_actions.xml")
        val scanner = xml.indexOf("android:id=\"@+id/widget_btn_scanner\"")
        val camera = xml.indexOf("android:id=\"@+id/widget_btn_camera\"")
        val diesel = xml.indexOf("android:id=\"@+id/widget_btn_diesel\"")
        assertTrue(scanner >= 0)
        assertTrue(camera > scanner)
        assertTrue(diesel > camera)
        assertTrue(xml.contains("@drawable/ic_widget_diesel"))
        assertTrue(!xml.contains("@drawable/widget_action_btn_bg"))
        assertTrue(!xml.contains("widget_camera_short"))
    }

    @Test
    fun compactQuickActions_areScannerThenCameraThenDiesel() {
        val xml = readRes("layout/widget_compact.xml")
        val scanner = xml.indexOf("android:id=\"@+id/widget_btn_scanner\"")
        val camera = xml.indexOf("android:id=\"@+id/widget_btn_camera\"")
        val diesel = xml.indexOf("android:id=\"@+id/widget_btn_diesel\"")
        assertTrue(scanner >= 0)
        assertTrue(camera > scanner)
        assertTrue(diesel > camera)
        assertTrue(!xml.contains("@drawable/widget_action_btn_bg"))
        assertTrue(xml.contains("android:id=\"@+id/widget_day_dots_row\""))
        val dotsBlock = xml.substring(
            xml.indexOf("android:id=\"@+id/widget_day_dots_row\""),
            xml.indexOf("android:id=\"@+id/widget_right_column\""),
        )
        assertTrue(dotsBlock.contains("android:visibility=\"gone\""))
    }

    @Test
    fun standardAndExpanded_placeActionsFullWidthBelowMainRow() {
        listOf("layout/widget_standard.xml", "layout/widget_expanded.xml").forEach { path ->
            val xml = readRes(path)
            val mainRow = xml.indexOf("android:id=\"@+id/widget_main_row\"")
            val actions = xml.indexOf("@layout/widget_quick_actions")
            val rightCol = xml.indexOf("android:id=\"@+id/widget_right_column\"")
            assertTrue(path, mainRow >= 0)
            assertTrue(path, actions > mainRow)
            val rightBlock = xml.substring(rightCol, actions)
            assertTrue(path, !rightBlock.contains("@layout/widget_quick_actions"))
        }
    }

    @Test
    fun scannerIcon_isQrOutlineNotBriefcase() {
        val xml = readRes("drawable/ic_widget_scanner.xml")
        assertTrue(xml.contains("Thin QR scan glyph"))
        assertTrue(xml.contains("@color/widget_action_scanner"))
        assertTrue(!xml.contains("M19,7h-1V5"))
        assertTrue(readRes("drawable/ic_widget_camera.xml").contains("@color/widget_action_camera"))
        assertTrue(readRes("drawable/ic_widget_diesel.xml").contains("@color/widget_action_diesel"))
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
