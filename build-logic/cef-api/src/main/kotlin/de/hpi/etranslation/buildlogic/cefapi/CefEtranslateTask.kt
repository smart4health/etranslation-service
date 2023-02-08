package de.hpi.etranslation.buildlogic.cefapi

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.property
import org.http4k.client.OkHttp
import org.http4k.core.Body
import org.http4k.core.Credentials
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.filter.DigestAuth
import org.http4k.format.Jackson.auto
import java.io.File
import java.util.Base64
import java.util.UUID

abstract class CefEtranslateTask : DefaultTask() {

    @get:Input
    val cefEndpoint: Property<String> = project.objects.property<String>()
        .convention("https://webgate.ec.europa.eu/etranslation/si/translate")

    @get:Input
    abstract val credentials: Property<String>

    @get:Input
    @set:Option(option = "inputFile", description = "File of stuff to translate")
    abstract var inputFile: String

    @get:Input
    @set:Option(option = "email", description = "Destination for translated file")
    abstract var emailAddress: String

    @get:Input
    @set:Option(option = "lang", description = "Language to translate to: {EN, DE, FR, IT, PT}")
    abstract var lang: String

    @get:Input
    @set:Option(option = "format", description = "Format of the document")
    var format: String = "xml"

    @get:Input
    @set:Option(option = "sourceLang", description = "Language to translate from: {EN, DE, FR, IT, PT}")
    var sourceLang: String = "EN"

    @get:Input
    @set:Option(
        option = "addXmlHeader",
        description = "Add a utf-8 xml header to the given inputFile.  CEF is unhappy if it isn't there",
    )
    var addXmlHeader: Boolean = false

    @TaskAction
    fun run() {
        val (user, pass) = credentials.get().split(":")

        val client = Credentials(user, pass)
            .let(ClientFilters::DigestAuth)
            .then(cefEndpoint.get().let(Uri.Companion::of).let(ClientFilters::SetBaseUriFrom))
            .then(OkHttp())

        val document = File(inputFile)
            .readBytes()
            .let { document ->
                if (addXmlHeader)
                    "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n"
                        .toByteArray()
                        .plus(document)
                else document
            }
            .let(Base64.getEncoder()::encodeToString)

        val externalReference = UUID.randomUUID()

        val requestBody = CefTranslationRequestBody(
            callerInformation = CefTranslationRequestBody.CallerInformation(
                application = user,
            ),
            documentToTranslateBase64 = CefTranslationRequestBody.DocumentToTranslateBase64(
                content = document,
                format = format,
            ),
            sourceLanguage = sourceLang,
            targetLanguages = listOf(lang),
            externalReference = externalReference.toString(),
            destinations = CefTranslationRequestBody.Destinations(
                emailDestinations = listOf(emailAddress),
            ),
        )

        val response = Request(Method.POST, "")
            .with(Body.auto<CefTranslationRequestBody>().toLens() of requestBody)
            .let(client::invoke)

        println("External reference: $externalReference")
        println("Response:")
        println(response.toMessage())
    }
}
