package dev.paarudev.prana.insight

import dev.paarudev.prana.domain.insight.InsightGuardrail
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InsightGuardrailTest {

    private lateinit var guardrail: InsightGuardrail
    private val safeFallback = "Your cardiovascular and vocal rhythms are steady today. Maintain your gentle pace."

    @Before
    fun setUp() {
        guardrail = InsightGuardrail()
    }

    @Test
    fun testPassCompliantWellnessGuidance() {
        val compliantText = "Your HRV is slightly low today and your voice reads more tense. A short breathing break will help."
        val result = guardrail.validate(compliantText, safeFallback)

        assertTrue("Compliant text must pass", result.isCompliant)
        assertEquals(compliantText, result.sanitizedText)
        assertTrue(result.detectedViolations.isEmpty())
    }

    @Test
    fun testCatchDiagnosisViolation() {
        val badText = "Based on your measurements, we diagnose you with hypertension and early heart disease."
        val result = guardrail.validate(badText, safeFallback)

        assertFalse("Medical diagnosis must be flagged", result.isCompliant)
        assertEquals(safeFallback, result.sanitizedText)
        assertTrue("Should detect 'diagnos'", result.detectedViolations.contains("diagnos"))
        assertTrue("Should detect 'disease'", result.detectedViolations.contains("disease"))
        assertTrue("Should detect 'hypertens'", result.detectedViolations.contains("hypertens"))
    }

    @Test
    fun testCatchPrescriptionViolation() {
        val badText = "Please ask your physician to prescribe medication for your elevated pulse."
        val result = guardrail.validate(badText, safeFallback)

        assertFalse("Prescription advice must be blocked", result.isCompliant)
        assertEquals(safeFallback, result.sanitizedText)
        assertTrue("Should detect 'prescri'", result.detectedViolations.contains("prescri"))
    }

    @Test
    fun testCatchEmptyInput() {
        val result = guardrail.validate("   ", safeFallback)
        assertFalse("Blank input is invalid", result.isCompliant)
        assertEquals(safeFallback, result.sanitizedText)
    }
}
