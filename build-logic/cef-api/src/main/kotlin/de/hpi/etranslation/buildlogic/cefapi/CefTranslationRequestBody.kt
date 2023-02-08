package de.hpi.etranslation.buildlogic.cefapi

internal data class CefTranslationRequestBody(
    val callerInformation: CallerInformation,
    val documentToTranslateBase64: DocumentToTranslateBase64,
    // Two-letter uppercase country code (ISO 639-1)
    val sourceLanguage: String,
    val targetLanguages: List<String>,
    // not required
    // val errorCallback: String,
    val externalReference: String,
    val destinations: Destinations,
) {
    data class CallerInformation(
        val application: String, // the application id, aka username?
    )

    data class DocumentToTranslateBase64(
        val content: String,
        // odt, ods,odp,odg, ott, ots, otp, otg, rtf, doc, docx, xls, xlsx, ppt, ppts, pdf, txt, htm, html, xhtml, xml, xlf, xliff, sdlxliff, tmx, rdf
        val format: String,
    )

    data class Destinations(
        // not actually required, but can't be empty
        // val httpDestinations: List<String>,
        val emailDestinations: List<String>,
    )
}
