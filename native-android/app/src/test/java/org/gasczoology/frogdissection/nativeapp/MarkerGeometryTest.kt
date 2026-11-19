package org.gasczoology.frogdissection.nativeapp

import org.junit.Assert.*
import org.junit.Test

class MarkerGeometryTest {
    @Test fun pinsRemainLegibleWithoutHidingOrgans() {
        for (edge in listOf(160f, 350f, 900f, 1800f)) {
            val small = markerRadiusPx(edge, false)
            val selected = markerRadiusPx(edge, true)
            assertTrue("Unselected marker oversized", small <= 5f)
            assertTrue("Selected marker oversized", selected <= 10f)
            assertTrue("Selected marker should be distinctive", selected > small)
        }
    }
    @Test fun markerSizingRejectsZeroSizeCanvas() {
        try {
            markerRadiusPx(0f, false)
            fail("Expected a validation error")
        } catch (_: IllegalArgumentException) {}
    }
}
