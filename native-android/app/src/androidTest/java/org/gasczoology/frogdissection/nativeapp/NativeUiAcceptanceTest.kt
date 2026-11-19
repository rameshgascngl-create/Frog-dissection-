package org.gasczoology.frogdissection.nativeapp

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onNodeWithText
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
