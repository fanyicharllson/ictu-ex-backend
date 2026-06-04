package com.fanyiadrien.listing.internal

import com.fanyiadrien.listing.AIListingSuggestion
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class GeminiServiceTest {

    @Test
    fun `analyzeImage parses gemini response into ai suggestion`(): Unit = runBlocking {
        var capturedUrl: String? = null
        val responseJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\"title\":\"Calculus Textbook\",\"description\":\"Well kept calculus book\",\"suggestedPrice\":3500.0,\"category\":\"TEXTBOOK\",\"condition\":\"GOOD\"}"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val builder = WebClient.builder().exchangeFunction(ExchangeFunction { request ->
            capturedUrl = request.url().toString()
            Mono.just(
                ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body(responseJson)
                    .build()
            )
        })

        val service = GeminiService(builder, jacksonObjectMapper(), "test-key")
        val result = service.analyzeImage("YmFzZTY0", "image/png")

        assertEquals("Calculus Textbook", result.title)
        assertEquals("Well kept calculus book", result.description)
        assertEquals(3500.0, result.suggestedPrice)
        assertEquals("TEXTBOOK", result.category)
        assertEquals("GOOD", result.condition)
        assertTrue(capturedUrl!!.contains("generateContent?key=test-key"))
    }

    @Test
    fun `analyzeImage falls back when gemini text is blank`(): Unit = runBlocking {
        val responseJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "text": "   " }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val builder = WebClient.builder().exchangeFunction(ExchangeFunction {
            Mono.just(
                ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body(responseJson)
                    .build()
            )
        })

        val service = GeminiService(builder, jacksonObjectMapper(), "test-key")
        val result = service.analyzeImage("YmFzZTY0", "image/png")

        assertEquals(AIListingSuggestion("Suggested Item", "A marketplace item. Please provide more details.", 0.0, "OTHER", "FAIR"), result)
    }

    @Test
    fun `analyzeImage falls back when gemini returns non success status`(): Unit = runBlocking {
        val builder = WebClient.builder().exchangeFunction(ExchangeFunction {
            Mono.just(
                ClientResponse.create(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", MediaType.TEXT_PLAIN_VALUE)
                    .body("bad request")
                    .build()
            )
        })

        val service = GeminiService(builder, jacksonObjectMapper(), "test-key")
        val result = service.analyzeImage("YmFzZTY0", "image/png")

        assertEquals("Suggested Item", result.title)
        assertEquals("OTHER", result.category)
    }
}