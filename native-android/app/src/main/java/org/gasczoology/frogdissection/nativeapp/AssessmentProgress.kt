package org.gasczoology.frogdissection.nativeapp

/**
 * Advances through a finite assessment. The terminal position (total) is a
 * completion screen, not another question; it never wraps automatically.
 */
internal fun advanceAssessment(current: Int, total: Int): Int {
    require(total > 0) { "An assessment must contain at least one question" }
    return (current + 1).coerceIn(0, total)
}
