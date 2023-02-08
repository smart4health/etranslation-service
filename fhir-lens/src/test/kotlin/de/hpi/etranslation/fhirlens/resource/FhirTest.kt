package de.hpi.etranslation.fhirlens.resource

import ca.uhn.fhir.context.FhirContext
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.DomainResource

open class FhirTest {
    internal val fhirContext = FhirContext.forR4()

    internal val xmlMapper = XmlMapper.builder()
        .addModule(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, false)
                .configure(KotlinFeature.NullToEmptyMap, false)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.SingletonSupport, false)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build(),
        )
        .configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)
        .build()

    internal inline fun <reified T : IBaseResource> String.parse(): T =
        fhirContext.newJsonParser().parseResource(T::class.java, this)

    internal inline fun <reified T : DomainResource> loadResource(prefix: String? = null): T {
        val resourceName = "${prefix?.plus(".") ?: ""}${T::class.simpleName}.json"

        return Thread.currentThread()
            .contextClassLoader
            .getResourceAsStream(resourceName)!!
            .let { fhirContext.newJsonParser().parseResource(T::class.java, it) }
    }
}
