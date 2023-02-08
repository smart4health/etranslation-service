package de.hpi.etranslation.fhirlens.resource

import com.github.michaelbull.result.unwrap
import de.hpi.etranslation.fhirlens.Format
import de.hpi.etranslation.fhirlens.encodeBase64
import de.hpi.etranslation.fhirlens.requireDecodeBase64
import org.hl7.fhir.r4.model.DocumentReference
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DocumentReferenceLensTest : FhirTest() {
    private val underTest = DocumentReferenceLens(xmlMapper)

    @Test
    fun `extracting a pdf and description works`() {
        val translatables = loadResource<DocumentReference>()
            .let(underTest::extract)
            .unwrap()

        val expectedXml = """
            <TranslatableDocumentReference>
                <Description>this is the title</Description>
            </TranslatableDocumentReference>
        """.trimIndent()

        val expected = xmlMapper.readTree(expectedXml)

        val actualXml = translatables
            .first { it.first == Format.XML }
            .second
            .requireDecodeBase64()
            .decodeToString()
            .let(xmlMapper::readTree)

        assertEquals(2, translatables.size)
        assertEquals("changeme", translatables.first { it.first == Format.PDF }.second)
        assertEquals(expected, actualXml)
    }

    @Test
    fun `injecting a pdf and description works`() {
        val newPdf = "almostanystringisbase64="
        val newDescription = """
            <?xml version="1.0" encoding="utf-8"?> 
            <TranslatableDocumentReference>
                <Description>translated title</Description>
            </TranslatableDocumentReference>
        """.trimIndent().encodeToByteArray().encodeBase64()

        val translatables = listOf(
            Format.PDF to newPdf,
            Format.XML to newDescription,
        )

        val docRef = underTest
            .inject(loadResource(), translatables)
            .unwrap()

        assertEquals(newPdf, docRef.content.single().attachment.dataElement.valueAsString)
        assertEquals("translated title", docRef.description)
    }
}
