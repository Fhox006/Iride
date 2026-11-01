package com.metrolist.music.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePresentationTest {
    @Test fun limitsLongServerHeadingsAndNormalizesWhitespace() {
        assertEquals("One two three four five six…", compactHomeTitle(" One  two\nthree four five six seven "))
        assertEquals("Artist name", compactHomeTitle("Artist name"))
        assertEquals("", compactHomeTitle("   "))
    }

    @Test fun waitsForAnActualSeparatorWhenIntermediateCategoriesAreEmpty() {
        val order = HomeShelfOrder()
        assertTrue(order.canAdd(hasFourRows = true))
        order.added(hasFourRows = true)
        repeat(5) { assertFalse(order.canAdd(hasFourRows = true)) }
        order.added(hasFourRows = false)
        assertTrue(order.canAdd(hasFourRows = true))
        order.added(hasFourRows = true)
        assertFalse(order.canAdd(hasFourRows = true))
    }
}
