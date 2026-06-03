package com.fanyiadrien.shared.common

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `handleIllegalArgumentException returns 400 with message`() {
        val ex = IllegalArgumentException("Email already registered")
        val response = handler.handleIllegalArgumentException(ex)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Email already registered", response.body?.message)
        assertEquals(400, response.body?.status)
        assertEquals("Bad Request", response.body?.error)
        assertNotNull(response.body?.timestamp)
    }

    @Test
    fun `handleIllegalArgumentException uses default message when null`() {
        val ex = IllegalArgumentException(null as String?)
        val response = handler.handleIllegalArgumentException(ex)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Invalid request", response.body?.message)
    }

    @Test
    fun `handleHttpMessageNotReadableException returns 400 with malformed json message`() {
        val ex = HttpMessageNotReadableException(
            "JSON parse error",
            RuntimeException("Unexpected character"),
            MockHttpInputMessage("{}".toByteArray())
        )
        val response = handler.handleHttpMessageNotReadableException(ex)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertTrue(response.body?.message?.contains("Malformed JSON") == true)
        assertEquals(400, response.body?.status)
    }

    @Test
    fun `handleGeneralException returns 500 with message`() {
        val ex = RuntimeException("Something went wrong")
        val response = handler.handleGeneralException(ex)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("Something went wrong", response.body?.message)
        assertEquals(500, response.body?.status)
        assertEquals("Internal Server Error", response.body?.error)
    }

    @Test
    fun `handleGeneralException uses default message when null`() {
        val ex = RuntimeException(null as String?)
        val response = handler.handleGeneralException(ex)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("An unexpected error occurred", response.body?.message)
    }
}
