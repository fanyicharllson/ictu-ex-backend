package com.fanyiadrien.messaging.internal.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ConversationRepository : JpaRepository<ConversationEntity, UUID> {

    fun findByParticipantAOrParticipantB(participantA: UUID, participantB: UUID): List<ConversationEntity>

    fun findByParticipantAAndParticipantBAndListingIdIsNull(
        participantA: UUID,
        participantB: UUID
    ): ConversationEntity?

    fun findByParticipantAAndParticipantBAndListingId(
        participantA: UUID,
        participantB: UUID,
        listingId: UUID
    ): ConversationEntity?
}

interface MessageRepository : JpaRepository<MessageEntity, UUID> {
    fun findByConversationIdOrderBySentAtAsc(conversationId: UUID): List<MessageEntity>
}
