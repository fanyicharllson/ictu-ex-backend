package com.fanyiadrien.auth.internal.persistence

import jakarta.persistence.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.random.Random

@Entity
@Table(name = "users")
class UserEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(name = "display_name", nullable = false)
    val displayName: String,

    @Column(name = "student_id", nullable = false, unique = true)
    val studentId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, columnDefinition = "VARCHAR(255)")
    val userType: UserType = UserType.STUDENT,

    @Column(name = "password_hash", nullable = false)
    val passwordHash: String,

    @Column(name = "profile_image_url")
    val profileImageUrl: String? = null,

    // Verification fields
    @Column(name = "verification_code")
    var verificationCode: String? = null,

    @Column(name = "verification_code_expires_at")
    var verificationCodeExpiresAt: Instant? = null,

    @Column(name = "is_verified", nullable = false)
    var isVerified: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    fun generateVerificationCode() {
        val randomDigits = (100000..999999).random()
        this.verificationCode = "ICTUEx-$randomDigits"
        this.verificationCodeExpiresAt = Instant.now().plus(15, ChronoUnit.MINUTES)
    }

    fun isCodeValid(code: String): Boolean {
        return this.verificationCode == code && 
               this.verificationCodeExpiresAt?.isAfter(Instant.now()) == true
    }
}

enum class UserType {
    STUDENT,
    BUYER,
    SELLER
}
