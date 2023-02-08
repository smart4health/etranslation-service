package de.hpi.etranslation.fhirlens.resource

import com.github.michaelbull.result.unwrap
import de.hpi.etranslation.fhirlens.Format
import de.hpi.etranslation.fhirlens.encodeBase64
import de.hpi.etranslation.fhirlens.requireDecodeBase64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class QuestionnaireResponseLensTest : FhirTest() {

    @Test
    fun `extracting the structure works`() {
        val translatables = QuestionnaireResponseLens(xmlMapper).extract(loadResource())
            .unwrap()

        val expectedXml = """
            <?xml version="1.0" encoding="utf-8"?> 
            <TranslatableQuestionnaireResponse>
              <Question>
                <Question>
                  <Text>this is the question</Text>
                  <Answer>
                    <Answer>this is the answer</Answer>
                  </Answer>
                </Question>
              </Question>
            </TranslatableQuestionnaireResponse>
        """.trimIndent()

        val expected = xmlMapper.readTree(expectedXml)
        val actual = translatables
            .first { it.first == Format.XML }
            .second
            .requireDecodeBase64()
            .decodeToString()
            .let(xmlMapper::readTree)

        assertEquals(1, translatables.size)
        assertEquals(expected, actual)
    }

    @Test
    fun `injecting the structure works`() {
        val translatedXml = """
            <?xml version="1.0" encoding="utf-8"?> 
            <TranslatableQuestionnaireResponse>
              <Question>
                <Question>
                  <Text>translated question</Text>
                  <Answer>
                    <Answer>translated answer</Answer>
                  </Answer>
                </Question>
              </Question>
            </TranslatableQuestionnaireResponse>
        """.trimIndent().encodeToByteArray().encodeBase64()

        val questionnaireResponse = QuestionnaireResponseLens(xmlMapper)
            .inject(loadResource(), listOf(Format.XML to translatedXml))
            .unwrap()

        assertEquals(questionnaireResponse.item.first().text, "translated question")
        assertEquals(questionnaireResponse.item.first().answer.first().valueStringType.value, "translated answer")
    }
}
