package com.truckerload.widget

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.truckerload.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WidgetQuickActionsLayoutTest {

    @Test
    fun quickActions_buttonsAreScannerThenCameraThenDiesel() {
        assertButtonOrder(inflate(R.layout.widget_quick_actions))
    }

    @Test
    fun compact_buttonsAreScannerThenCameraThenDiesel_andDayDotsStayHidden() {
        val root = inflate(R.layout.widget_compact)
        val actions = root.findViewById<View>(R.id.widget_quick_actions)
        assertButtonOrder(actions)
        assertEquals(View.GONE, root.findViewById<View>(R.id.widget_day_dots_row).visibility)
    }

    @Test
    fun standardAndExpanded_placeActionsBelowTheMainRow() {
        listOf(R.layout.widget_standard, R.layout.widget_expanded).forEach { layout ->
            val root = inflate(layout)
            val main = root.findViewById<View>(R.id.widget_main_row)
            val actions = root.findViewById<View>(R.id.widget_quick_actions)
            val right = root.findViewById<View>(R.id.widget_right_column)
            val parent = actions.parent as ViewGroup
            assertEquals(parent, main.parent)
            assertTrue(parent.indexOfChild(actions) > parent.indexOfChild(main))
            assertTrue(!contains(right, actions))
            assertButtonOrder(actions)
        }
    }

    private fun assertButtonOrder(root: View) {
        assertEquals(
            listOf(R.id.widget_btn_scanner, R.id.widget_btn_camera, R.id.widget_btn_diesel),
            imageIds(root),
        )
    }

    private fun imageIds(root: View): List<Int> {
        val ids = mutableListOf<Int>()
        fun walk(view: View) {
            if (view is ImageView && view.id != View.NO_ID) ids += view.id
            if (view is ViewGroup) {
                for (index in 0 until view.childCount) walk(view.getChildAt(index))
            }
        }
        walk(root)
        return ids
    }

    private fun contains(ancestor: View, target: View): Boolean {
        var current: View? = target
        while (current != null) {
            if (current === ancestor) return true
            current = current.parent as? View
        }
        return false
    }

    private fun inflate(layout: Int): View =
        LayoutInflater.from(RuntimeEnvironment.getApplication()).inflate(layout, null)
}
