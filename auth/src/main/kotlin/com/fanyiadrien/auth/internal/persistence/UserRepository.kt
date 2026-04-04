package com.fanyiadrien.auth.internal.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findByEmail(email: String): UserEntity?
    fun findByStudentId(studentId: String): UserEntity?
    fun existsByEmail(email: String): Boolean
    fun existsByStudentId(studentId: String): Boolean
}