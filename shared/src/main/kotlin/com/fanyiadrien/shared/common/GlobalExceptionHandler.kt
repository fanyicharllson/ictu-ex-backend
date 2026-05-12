package com.fanyiadrien.shared.common

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException // Import this
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.time.LocalDateTime

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            message = ex.message ?: "Invalid request",
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            timestamp = LocalDateTime.now().toString()
        )
        return ResponseEntity.badRequest().body(error)
    }

    // New handler for JSON parsing errors
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            message = "Malformed JSON request body: ${ex.mostSpecificCause.message}",
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            timestamp = LocalDateTime.now().toString()
        )
        return ResponseEntity.badRequest().body(error)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            message = ex.message ?: "An unexpected error occurred",
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            timestamp = LocalDateTime.now().toString()
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }
}

/**
 * Standard error response structure for mobile clients.
 */
data class ErrorResponse(
    @field:Schema(description = "Human-readable error message")
    val message: String,
    @field:Schema(description = "HTTP status code")
    val status: Int,
    @field:Schema(description = "HTTP status reason")
    val error: String,
    @field:Schema(description = "Error timestamp in ISO-8601 format")
    val timestamp: String
)

/**
 * Optional: Standard success response structure.
 * You can wrap your data in this to provide extra metadata (like pagination).
 */
data class ApiResponse<T>(
    val success: Boolean = true,
    val data: T? = null,
    val message: String? = null,
    val timestamp: String = LocalDateTime.now().toString()
)
