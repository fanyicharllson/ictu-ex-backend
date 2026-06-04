package com.fanyiadrien.shared.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `handleIllegalArgumentException returns bad request payload`() {
        val response = handler.handleIllegalArgumentException(IllegalArgumentException("invalid input"))

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("invalid input", response.body?.message)
        assertEquals(400, response.body?.status)
        assertEquals("Bad Request", response.body?.error)
        assertNotNull(response.body?.timestamp)
    }

    @Test
    fun `handleHttpMessageNotReadableException returns malformed json response`() {
        val ex = HttpMessageNotReadableException("bad json", RuntimeException("Unexpected character"))

        val response = handler.handleHttpMessageNotReadableException(ex)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertTrue(response.body?.message?.contains("Malformed JSON request body") == true)
        assertTrue(response.body?.message?.contains("Unexpected character") == true)
        assertEquals(400, response.body?.status)
        assertEquals("Bad Request", response.body?.error)
    }

    @Test
    fun `handleGeneralException returns internal server error payload`() {
        val response = handler.handleGeneralException(RuntimeException("boom"))

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("boom", response.body?.message)
        assertEquals(500, response.body?.status)
        assertEquals("Internal Server Error", response.body?.error)
        assertNotNull(response.body?.timestamp)
    }

    @Test
    fun `api response defaults success and timestamp`() {
        val response = ApiResponse(data = "ok")

        assertTrue(response.success)
        assertEquals("ok", response.data)
        assertNotNull(response.timestamp)
    }
}

