package com.fanyiadrien.ictuexbackend

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.math.BigDecimal

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class E2EJourneyTest {

    companion object {
        var studentToken: String = ""
        var listingId: String = ""
        const val BASE_URL = "https://api.ictuex.teamnest.me"
        const val TEST_EMAIL = "e2e.test@ictuniversity.edu.cm"
        const val TEST_PASSWORD = "SecurePassword123!"
        const val TEST_STUDENT_ID = "ICT2026E2E" // This might not be directly used in API calls but good to have for context
        var journeyEmail: String = TEST_EMAIL
        var journeyStudentId: String = TEST_STUDENT_ID
        var journeyListingTitle: String = ""
        var journeyListingDescription: String = ""

        @BeforeAll
        @JvmStatic
        fun setup() {
            RestAssured.baseURI = BASE_URL
            // Enable logging of request and response details if a validation fails
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()

            val suffix = System.currentTimeMillis()
            journeyEmail = "e2e.test2.$suffix@ictuniversity.edu.cm"
            journeyStudentId = "ICT20266E2E-$suffix"
            journeyListingTitle = "Introduction to Kotlin E2E Testing - $suffix"
            journeyListingDescription = "A clean, lightly used university textbook for E2E verification. Run: $suffix"
        }
    }

    @Test
    @Order(1)
    @DisplayName("Register a new ICTU student and capture the JWT token")
    fun journey_step1_registerNewStudent() {
        val registerPayload = mapOf(
            "email" to journeyEmail,
            "password" to TEST_PASSWORD,
            "displayName" to "E2E Test Student",
            "studentId" to journeyStudentId
        )

        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(registerPayload)
            .post("/api/auth/register")

        response.then()
            .statusCode(200)
            .body("token", allOf(notNullValue(), not(blankOrNullString())))
            .body("user.email", equalTo(journeyEmail))
            .body("user.studentId", equalTo(journeyStudentId))

        response.also {
            studentToken = it.jsonPath().getString("token")
        }
    }

    @Test
    @Order(2)
    @DisplayName("Login with the same credentials and receive a JWT token")
    fun journey_step2_loginWithSameCredentials() {
        val loginPayload = mapOf(
            "email" to journeyEmail,
            "password" to TEST_PASSWORD
        )

        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(loginPayload)
            .post("/api/auth/login")

        response.then()
            .statusCode(200)
            .body("token", allOf(notNullValue(), not(blankOrNullString())))
            .body("user.email", equalTo(journeyEmail))

        response.also {
            studentToken = it.jsonPath().getString("token")
        }
    }

    @Test
    @Order(3)
    @DisplayName("Create a listing with the authenticated student token")
    fun journey_step3_createListing() {
        val listingPayload = mapOf(
            "title" to journeyListingTitle,
            "description" to journeyListingDescription,
            "price" to BigDecimal("4500.00"),
            "category" to "TEXTBOOK",
            "condition" to "GOOD"
        )

        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $studentToken")
            .body(listingPayload)
            .post("/api/listings")

        response.then()
            .statusCode(201)
            .body("id", allOf(notNullValue(), not(blankOrNullString())))
            .body("title", equalTo(journeyListingTitle))

        response.also {
            listingId = it.jsonPath().getString("id")
        }
    }

    @Test
    @Order(4)
    @DisplayName("Browse all listings and confirm the created listing is present")
    fun journey_step4_browseListing() {
        val response = RestAssured.given()
            .header("Authorization", "Bearer $studentToken")
            .get("/api/listings")

        response.then()
            .statusCode(200)
            .body("id", hasItem(listingId))
    }

    @Test
    @Order(5)
    @DisplayName("Fetch the created listing by ID and verify its data")
    fun journey_step5_getListingById() {
        RestAssured.given()
            .header("Authorization", "Bearer $studentToken")
            .get("/api/listings/$listingId")
            .then()
            .statusCode(200)
            .body("id", equalTo(listingId))
            .body("title", equalTo(journeyListingTitle))
    }

    @Test
    @Order(6)
    @DisplayName("Search listings and confirm the journey listing is returned")
    fun journey_step6_searchListings() {
        RestAssured.given()
            .header("Authorization", "Bearer $studentToken")
            .get("/api/listings/search?title=Kotlin&category=TEXTBOOK")
            .then()
            .statusCode(200)
            .body("id", hasItem(listingId))
            .body("size()", greaterThan(0))
    }

    @Test
    @Order(7)
    @DisplayName("Logout invalidates the token and validation is rejected")
    fun journey_step7_logoutInvalidatesToken() {
        RestAssured.given()
            .header("Authorization", "Bearer $studentToken")
            .post("/api/auth/logout")
            .then()
            .statusCode(200)
            .body("message", containsString("Logged out"))

        RestAssured.given()
            .header("Authorization", "Bearer $studentToken")
            .get("/api/auth/validate")
            .then()
            .statusCode(401)
    }
}