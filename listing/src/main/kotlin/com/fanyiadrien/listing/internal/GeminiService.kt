package com.fanyiadrien.listing.internal

import com.fanyiadrien.listing.AIListingSuggestion
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import reactor.core.publisher.Mono
import java.time.Duration

@Service
internal class GeminiService(
    private val webClientBuilder: WebClient.Builder,
    private val objectMapper: ObjectMapper,
    @Value("\${gemini.api.key}") private val apiKey: String
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val geminiWebClient: WebClient by lazy {
        webClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build()
    }

    // Fallback suggestion in case of API errors
    private val FALLBACK_SUGGESTION = AIListingSuggestion(
        title = "Suggested Item",
        description = "A marketplace item. Please provide more details.",
        suggestedPrice = 0.0,
        category = "OTHER",
        condition = "FAIR"
    )

    // Internal data classes to safely parse Gemini API response
    private data class GeminiResponse(
        val candidates: List<Candidate>?
    )

    private data class Candidate(
        val content: Content?
    )

    private data class Content(
        val parts: List<Part>?
    )

    private data class Part(
        val text: String?
    )

    suspend fun analyzeImage(base64Image: String, mimeType: String): AIListingSuggestion {
        val requestBody = mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to "Analyze this student marketplace item image. Return JSON with: title (string), description (string, max 100 words), suggestedPrice (number in FCFA), category (one of: TEXTBOOK, ELECTRONICS, HOSTEL_GEAR, OTHER), condition (one of: NEW, GOOD, FAIR). Only return valid JSON, no other text."),
                        mapOf(
                            "inline_data" to mapOf(
                                "mime_type" to mimeType,
                                "data" to base64Image
                            )
                        )
                    )
                )
            )
        )

        return try {
            val geminiApiResponse = geminiWebClient.post()
                .uri("/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .awaitExchange { clientResponse ->
                    if (clientResponse.statusCode().is2xxSuccessful) {
                        Mono.just(clientResponse.awaitBody<GeminiResponse>()) // Deserialize directly to GeminiResponse
                    } else {
                        val errorBody = clientResponse.awaitBody<String>()
                        log.error("Gemini API error: {} - {}", clientResponse.statusCode(), errorBody)
                        Mono.error(RuntimeException("Gemini API returned non-2xx status: ${clientResponse.statusCode()}"))
                    }
                }.block(Duration.ofSeconds(30)) // Block for a maximum of 30 seconds

            val text = geminiApiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (text.isNullOrBlank()) {
                log.warn("Gemini API returned empty or invalid text content. Using fallback.")
                FALLBACK_SUGGESTION
            } else {
                // Attempt to parse the JSON string
                objectMapper.readValue<AIListingSuggestion>(text)
            }
        } catch (e: Exception) {
            log.error("Error calling Gemini API or parsing response: {}", e.message, e)
            FALLBACK_SUGGESTION // Return fallback on any exception
        }
    }
}
