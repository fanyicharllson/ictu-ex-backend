package com.fanyiadrien.ictuexbackend

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class E2EJourneyTest {

    companion object {
        var studentToken: String = ""
        var listingId: String = ""
        const val BASE_URL = "https://api.ictuex.teamnest.me"
        const val TEST_EMAIL = "e2e.test@ictuniversity.edu.cm"
        const val TEST_PASSWORD = "SecurePassword123!"
        const val TEST_STUDENT_ID = "ICT2026E2E" // This might not be directly used in API calls but good to have for context

        @BeforeAll
        @JvmStatic
        fun setup() {
            RestAssured.baseURI = BASE_URL
            // Enable logging of request and response details if a validation fails
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        }
    }

    @Test
    @Order(1)
    fun `1_should_login_student_and_get_token`() {
        val loginPayload = mapOf(
            "email" to TEST_EMAIL,
            "password" to TEST_PASSWORD
        )

        Given {
            contentType(ContentType.JSON)
            body(loginPayload)
        } When {
            post("/auth/login")
        } Then {
            statusCode(200)
            body("token", notNullValue())
        }.extract().response().also {
            studentToken = it.jsonPath().getString("token")
            println("Student Token: $studentToken")
        }
    }

    @Test
    @Order(2)
    fun `2_should_create_new_listing`() {
        val listingPayload = mapOf(
            "title" to "E2E Test Listing - ${System.currentTimeMillis()}",
            "description" to "This is a test listing created by E2E test.",
            "price" to 100.0,
            "category" to "BOOKS", // Assuming valid categories exist
            "condition" to "USED", // Assuming valid conditions exist
            "imageUrl" to "https://example.com/image.jpg",
            "location" to "Buea",
            "contactEmail" to TEST_EMAIL
        )

        Given {
            contentType(ContentType.JSON)
            header("Authorization", "Bearer $studentToken")
            body(listingPayload)
        } When {
            post("/listings")
        } Then {
            statusCode(201)
            body("id", notNullValue())
            body("title", equalTo(listingPayload["title"]))
        }.extract().response().also {
            listingId = it.jsonPath().getString("id")
            println("Created Listing ID: $listingId")
        }
    }

    @Test
    @Order(3)
    fun `3_should_get_all_listings_and_find_created_one`() {
        Given {
            header("Authorization", "Bearer $studentToken")
        } When {
            get("/listings")
        } Then {
            statusCode(200)
            body("content.id", hasItem(listingId)) // Assuming the response is paginated with a 'content' field
        }
    }

    @Test
    @Order(4)
    fun `4_should_get_listing_by_id`() {
        Given {
            header("Authorization", "Bearer $studentToken")
        } When {
            get("/listings/$listingId")
        } Then {
            statusCode(200)
            body("id", equalTo(listingId))
            body("title", startsWith("E2E Test Listing"))
        }
    }

    @Test
    @Order(5)
    fun `5_should_update_listing`() {
        val updatedTitle = "E2E Test Listing Updated - ${System.currentTimeMillis()}"
        val updatePayload = mapOf(
            "title" to updatedTitle,
            "description" to "This is an updated test listing.",
            "price" to 120.0,
            "category" to "ELECTRONICS", // Changed category
            "condition" to "NEW", // Changed condition
            "imageUrl" to "https://example.com/updated_image.jpg",
            "location" to "Douala", // Changed location
            "contactEmail" to TEST_EMAIL
        )

        Given {
            contentType(ContentType.JSON)
            header("Authorization", "Bearer $studentToken")
            body(updatePayload)
        } When {
            put("/listings/$listingId")
        } Then {
            statusCode(200)
            body("id", equalTo(listingId))
            body("title", equalTo(updatedTitle))
        }
    }

    @Test
    @Order(6)
    fun `6_should_delete_listing`() {
        Given {
            header("Authorization", "Bearer $studentToken")
        } When {
            delete("/listings/$listingId")
        } Then {
            statusCode(204) // No Content for successful deletion
        }
    }

    @Test
    @Order(7)
    fun `7_should_not_find_deleted_listing`() {
        Given {
            header("Authorization", "Bearer $studentToken")
        } When {
            get("/listings/$listingId")
        } Then {
            statusCode(404) // Not Found for a deleted resource
        }
    }
}