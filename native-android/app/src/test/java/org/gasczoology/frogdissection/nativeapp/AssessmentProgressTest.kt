package org.gasczoology.frogdissection.nativeapp

import org.junit.Assert.*
import org.junit.Test

class AssessmentProgressTest {
    @Test fun finishesAtExactlyFinalQuestion() {
        assertEquals(1, advanceAssessment(0, 26))
        assertEquals(25, advanceAssessment(24, 26))
        assertEquals(26, advanceAssessment(25, 26))
        assertEquals(26, advanceAssessment(26, 26))
    }

    @Test fun spotterDoesNotAwardExtraRoundsOnWrap() {
        assertEquals(9, advanceAssessment(8, 9))
        assertEquals(9, advanceAssessment(9, 9))
        assertEquals(0, advanceAssessment(-1, 9))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyAssessment() {
        advanceAssessment(0, 0)
    }
}
