package org.gasczoology.frogdissection.nativeapp

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.SemanticsActions
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeUiAcceptanceTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test fun dissectionQuizAndGlossaryNavigateWithoutWebView() {
        composeRule.onNodeWithText("Anatomical plates").assertExists()
        composeRule.onNodeWithText("Quiz").performClick()
        composeRule.onNodeWithText("Practical assessment").assertExists()
        composeRule.onNodeWithText("Glossary").performClick()
        composeRule.onNodeWithText("Anatomical glossary").assertExists()
        composeRule.onNodeWithText("Dissection").performClick()
        composeRule.onNodeWithText("Anatomical plates").assertExists()
    }

    @Test fun photographExposesNamedOrgansToAccessibilityServices() {
        val image = composeRule.onNode(
            hasContentDescription("Frog anatomical photograph:", substring = true)
        )
        image.assertExists()
        val actions = image.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertTrue("Liver must have an independent accessibility action",
            actions.any { it.label.contains("Liver", ignoreCase = true) })
        assertTrue("Stomach must have an independent accessibility action",
            actions.any { it.label.contains("Stomach", ignoreCase = true) })
    }

    @Test fun calibratedPhotoCanBeAdjustedWithoutPixelPerfectTouch() {
        composeRule.onNodeWithText("Calibrate pin").performScrollTo().performClick()
        composeRule.onNodeWithText("Precision pin adjustment").assertExists()
        composeRule.onNodeWithText("Move right").performScrollTo().performClick()
        composeRule.onNodeWithText("Move up").performScrollTo().performClick()
    }

    @Test fun allFiveAnatomicalLayersHaveNativeSelection() {
        composeRule.onNodeWithText("Anatomical plates").assertExists()
        composeRule.onNodeWithText("4", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Layer 4 · Circulatory System").assertExists()
        composeRule.onNodeWithText("5", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Male specimen").assertExists()
        composeRule.onNodeWithText("Female specimen").performClick()
        composeRule.onNodeWithText("Layer 5 · Urogenital & Nervous").assertExists()
    }
}
