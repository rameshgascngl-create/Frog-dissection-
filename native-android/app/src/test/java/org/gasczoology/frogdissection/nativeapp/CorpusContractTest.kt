package org.gasczoology.frogdissection.nativeapp

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CorpusContractTest {
    private val corpus by lazy {
        FrogCorpus.fromJson(File("src/main/assets/frog_corpus.json").readText())
    }

    @Test fun preservesCompleteSourceCorpus() {
        assertEquals(5, corpus.layers.size)
        assertEquals(31, corpus.organs.size)
        assertEquals(26, corpus.questions.size)
        assertEquals(6, corpus.steps.size)
        assertEquals(6, corpus.pins.size)
        assertTrue(corpus.organs.values.all { it.morphology.isNotBlank() && it.function.isNotBlank() && it.tamil.isNotBlank() })
        assertTrue(corpus.questions.all { it.options.size == 4 && it.correct in 0..3 && it.explanation.isNotBlank() })
    }

    @Test fun maleFemaleAnatomyRemainsDistinct() {
        assertEquals("Internal deep male.avif", corpus.imageName(5, false))
        assertEquals("Internal deep female.avif", corpus.imageName(5, true))
        assertTrue(corpus.organsForLayer(5, true).any { it.key == "oviducts" })
        assertFalse(corpus.organsForLayer(5, false).any { it.key == "oviducts" })
        assertTrue(corpus.platePins(5, true).first { it.key == "oviducts" }.unplaced)
        assertFalse(corpus.platePins(5, false).any { it.key == "brain" })
        assertFalse(corpus.platePins(5, true).any { it.key == "brain" })
    }

    @Test fun reproductiveDescriptionNeverCombinesTheSexes() {
        val source = corpus.organs.getValue("gonads")
        val male = sexSpecificOrgan(source, false)
        val female = sexSpecificOrgan(source, true)
        assertEquals("Testes", male.name)
        assertEquals("Ovaries", female.name)
        assertEquals("விந்தகங்கள்", male.tamil)
        assertEquals("அண்டகங்கள்", female.tamil)
        assertTrue(male.morphology.contains("testes"))
        assertFalse(female.morphology.contains("testes"))
        assertTrue(female.viva.contains("oviducts"))
        assertFalse(male.viva.contains("oviducts"))
    }

    @Test fun dentitionQuestionAvoidsUnqualifiedAttachmentTerminology() {
        val dentition = corpus.questions.first { it.topic == "Dentition" }
        assertTrue(dentition.options[dentition.correct].contains("vomerine"))
        assertTrue(dentition.explanation.contains("pedicellate"))
        assertFalse(dentition.options[dentition.correct].contains("acrodont"))
    }

    @Test fun preservesSourceLanguageAndUnverifiedPins() {
        assertEquals("புற நாசித்துளைகள்", corpus.organs.getValue("external_nares").tamil)
        assertTrue(corpus.platePins(1, false).first { it.key == "cloaca" }.unplaced)
        assertTrue(corpus.platePins(4, false).first { it.key == "venous_system" }.unplaced)
        for (plate in corpus.layers) {
            assertTrue("Missing " + plate.image, File("src/main/assets/" + plate.image).length() > 1000)
        }
    }
}
