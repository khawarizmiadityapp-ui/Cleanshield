package com.rizmi.cleanshield

import com.rizmi.cleanshield.ui.navigation.Screen
import org.junit.Assert.*
import org.junit.Test

class NavigationTest {

    @Test
    fun testNavItemsNotNullAndValid() {
        val items = Screen.navItems
        assertEquals(6, items.size)
        for (item in items) {
            assertNotNull(item)
            assertNotNull(item.route)
            assertTrue(item.route.isNotEmpty())
            assertNotNull(item.title)
            assertTrue(item.title.isNotEmpty())
            assertNotNull(item.icon)
        }
    }
}
