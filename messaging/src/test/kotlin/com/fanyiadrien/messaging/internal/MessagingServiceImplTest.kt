package com.fanyiadrien.messaging.internal

import com.fanyiadrien.auth.AuthService
import com.fanyiadrien.auth.AuthUser
import com.fanyiadrien.messaging.internal.persistence.ConversationEntity
import com.fanyiadrien.messaging.internal.persistence.ConversationRepository
import com.fanyiadrien.messaging.internal.persistence.MessageEntity
import com.fanyiadrien.messaging.internal.persistence.MessageRepository
import com.fanyiadrien.shared.events.MessageSentEvent
import com.fanyiadrien.shared.kafka.EventPublisher
import com.fanyiadrien.shared.kafka.KafkaTopics
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.Optional
import java.util.UUID

class MessagingServiceImplTest {

    private val conversationRepository: ConversationRepository = mock()
    private val messageRepository: MessageRepository = mock()
    private val eventPublisher: EventPublisher = mock()
    private val authService: AuthService = mock()

    private lateinit var service: MessagingServiceImpl

    private val userA = UUID.randomUUID()
    private val userB = UUID.randomUUID()
    private val conversationId = UUID.randomUUID()

    private lateinit var first: UUID
    private lateinit var second: UUID

    @BeforeEach
    fun setup() {
        // Mirror the service's canonical ordering logic
        val pair = if (userA.toString() < userB.toString()) userA to userB else userB to userA
        first = pair.first
        second = pair.second

        service = MessagingServiceImpl(conversationRepository, messageRepository, eventPublisher, authService)
    }

    // ==================== GET OR CREATE CONVERSATION ====================

    @Test
    fun `getOrCreateConversation returns existing conversation when listingId is null`() {
        val existing = buildConversation()
        whenever(conversationRepository.findByParticipantAAndParticipantBAndListingIdIsNull(first, second))
            .thenReturn(existing)

        val result = service.getOrCreateConversation(userA, userB, null)

        assertEquals(existing.id, result.id)
        verify(conversationRepository, never()).save(any())
        verify(conversationRepository, never()).findByParticipantAAndParticipantBAndListingId(any(), any(), any())
    }

    @Test
    fun `getOrCreateConversation returns existing conversation when listingId is provided`() {
        val listingId = UUID.randomUUID()
        val existing = buildConversation(listingId = listingId)
        whenever(conversationRepository.findByParticipantAAndParticipantBAndListingId(first, second, listingId))
            .thenReturn(existing)

        val result = service.getOrCreateConversation(userA, userB, listingId)

        assertEquals(existing.id, result.id)
        verify(conversationRepository, never()).save(any())
        verify(conversationRepository, never()).findByParticipantAAndParticipantBAndListingIdIsNull(any(), any())
    }

    @Test
    fun `getOrCreateConversation creates new conversation when none exists`() {
        val saved = buildConversation()
        whenever(conversationRepository.findByParticipantAAndParticipantBAndListingIdIsNull(first, second))
            .thenReturn(null)
        whenever(conversationRepository.save(any())).thenReturn(saved)

        val result = service.getOrCreateConversation(userA, userB, null)

        assertEquals(saved.id, result.id)
        verify(conversationRepository).save(any())
    }

    @Test
    fun `getOrCreateConversation stores participants in canonical order regardless of who initiates`() {
        val saved = buildConversation()
        whenever(conversationRepository.findByParticipantAAndParticipantBAndListingIdIsNull(first, second))
            .thenReturn(null)
        whenever(conversationRepository.save(any())).thenReturn(saved)

        // userB initiates — should still look up and save with (first, second) ordering
        service.getOrCreateConversation(userB, userA, null)

        verify(conversationRepository).findByParticipantAAndParticipantBAndListingIdIsNull(first, second)
    }

    @Test
    fun `getOrCreateConversation throws when initiator and other are the same user`() {
        val ex = assertThrows<IllegalArgumentException> {
            service.getOrCreateConversation(userA, userA, null)
        }
        assertEquals("Cannot start a conversation with yourself", ex.message)
        verify(conversationRepository, never()).save(any())
        verify(conversationRepository, never()).findByParticipantAAndParticipantBAndListingIdIsNull(any(), any())
    }

    // ==================== SEND MESSAGE ====================

    @Test
    fun `sendMessage succeeds when sender is participantA`() {
        val conversation = buildConversation()
        val savedMessage = buildMessage(senderId = first)

        whenever(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation))
        whenever(messageRepository.save(any())).thenReturn(savedMessage)
        whenever(authService.getUserById(first)).thenReturn(buildAuthUser(id = first, displayName = "User A"))
        whenever(authService.getUserById(second)).thenReturn(buildAuthUser(id = second, displayName = "User B"))

        val result = service.sendMessage(conversationId, first, "Hello!")

        assertNotNull(result)
        assertEquals(first, result.senderId)
        assertEquals("Hello!", result.content)
    }

    @Test
    fun `sendMessage succeeds when sender is participantB`() {
        val conversation = buildConversation()
        val savedMessage = buildMessage(senderId = second)

        whenever(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation))
        whenever(messageRepository.save(any())).thenReturn(savedMessage)
        whenever(authService.getUserById(second)).thenReturn(buildAuthUser(id = second, displayName = "User B"))
        whenever(authService.getUserById(first)).thenReturn(buildAuthUser(id = first, displayName = "User A"))

        val result = service.sendMessage(conversationId, second, "Hi back!")

        assertNotNull(result)
        assertEquals(second, result.senderId)
    }

    @Test
    fun `sendMessage publishes event with correct receiverId when participantA sends`() {
        val conversation = buildConversation()
        val savedMessage = buildMessage(senderId = first)

        whenever(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation))
        whenever(messageRepository.save(any())).thenReturn(savedMessage)
        whenever(authService.getUserById(first)).thenReturn(buildAuthUser(id = first, displayName = "User A", email = "usera@example.com"))
        whenever(authService.getUserById(second)).thenReturn(buildAuthUser(id = second, displayName = "User B", email = "userb@example.com"))

        service.sendMessage(conversationId, first, "Hello!")

        val eventCaptor = argumentCaptor<MessageSentEvent>()
        verify(eventPublisher).publish(eq(KafkaTopics.MESSAGE_SENT), eventCaptor.capture())
        assertEquals(first, eventCaptor.firstValue.senderId)
        assertEquals(second, eventCaptor.firstValue.receiverId)
    }

    @Test
    fun `sendMessage publishes event with correct receiverId when participantB sends`() {
        val conversation = buildConversation()
        val savedMessage = buildMessage(senderId = second)

        whenever(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation))
        whenever(messageRepository.save(any())).thenReturn(savedMessage)
        whenever(authService.getUserById(second)).thenReturn(buildAuthUser(id = second, displayName = "User B", email = "userb@example.com"))
        whenever(authService.getUserById(first)).thenReturn(buildAuthUser(id = first, displayName = "User A", email = "usera@example.com"))

        service.sendMessage(conversationId, second, "Hello!")

        val eventCaptor = argumentCaptor<MessageSentEvent>()
        verify(eventPublisher).publish(eq(KafkaTopics.MESSAGE_SENT), eventCaptor.capture())
        assertEquals(second, eventCaptor.firstValue.senderId)
        assertEquals(first, eventCaptor.firstValue.receiverId)
    }

    @Test
    fun `sendMessage trims whitespace from content before saving`() {
        val conversation = buildConversation()
        val savedMessage = buildMessage(senderId = first, content = "Hello!")

        whenever(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation))
        whenever(messageRepository.save(any())).thenReturn(savedMessage)
        whenever(authService.getUserById(first)).thenReturn(buildAuthUser(id = first, displayName = "User A", email = "usera@example.com"))
        whenever(authService.getUserById(second)).thenReturn(buildAuthUser(id = second, displayName = "User B", email = "userb@example.com"))

        service.sendMessage(conversationId, first, "  Hello!  ")

        val entityCaptor = argumentCaptor<MessageEntity>()
        verify(messageRepository).save(entityCaptor.capture())
        assertEquals("Hello!", entityCaptor.firstValue.content)
    }

    @Test
    fun `sendMessage throws when sender is not a participant`() {
        val outsider = UUID.randomUUID()
        whenever(conversationRepository.findById(conversationId)).thenReturn(Optional.of(buildConversation()))

        val ex = assertThrows<IllegalArgumentException> {
            service.sendMessage(conversationId, outsider, "Hello!")
        }
        assertEquals("You are not a participant of this conversation", ex.message)
        verify(messageRepository, never()).save(any())
        verify(eventPublisher, never()).publish(any(), any())
    }

    @Test
    fun `sendMessage throws for blank content`() {
        whenever(conversationRepository.findById(conversationId)).thenReturn(Optional.of(buildConversation()))

        val ex = assertThrows<IllegalArgumentException> {
            service.sendMessage(conversationId, first, "   ")
        }
        assertEquals("Message content cannot be empty", ex.message)
        verify(messageRepository, never()).save(any())
        verify(eventPublisher, never()).publish(any(), any())
    }

    @Test
    fun `sendMessage throws when conversation not found`() {
        whenever(conversationRepository.findById(conversationId)).thenReturn(Optional.empty())

        val ex = assertThrows<IllegalArgumentException> {
            service.sendMessage(conversationId, first, "Hello!")
        }
        assertEquals("Conversation not found", ex.message)
        verify(messageRepository, never()).save(any())
    }

    @Test
    fun `sendMessage publishes exactly one Kafka event per message`() {
        val conversation = buildConversation()
        val savedMessage = buildMessage(senderId = first)

        whenever(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation))
        whenever(messageRepository.save(any())).thenReturn(savedMessage)
        whenever(authService.getUserById(first)).thenReturn(buildAuthUser(id = first, displayName = "User A", email = "usera@example.com"))
        whenever(authService.getUserById(second)).thenReturn(buildAuthUser(id = second, displayName = "User B", email = "userb@example.com"))

        service.sendMessage(conversationId, first, "Hello!")

        verify(eventPublisher, times(1)).publish(any(), any())
    }

    // ==================== GET MESSAGES ====================

    @Test
    fun `getMessages returns messages in order for participantA`() {
        val messages = listOf(
            buildMessage(senderId = first, content = "Hi"),
            buildMessage(senderId = second, content = "Hey"),
            buildMessage(senderId = first, content = "How are you?")
        )
        whenever(conversationRepository.findById(conversationId)).thenReturn(Optional.of(buildConversation()))
        whenever(messageRepository.findByConversationIdOrderBySentAtAsc(conversationId)).thenReturn(messages)

        val result = service.getMessages(conversationId, first)

        assertEquals(3, result.size)
        assertEquals("Hi", result[0].content)
        assertEquals("Hey", result[1].content)
        assertEquals("How are you?", result[2].content)
    }

    @Test
    fun `getMessages returns messages for participantB as well`() {
        whenever(conversationRepository.findById(conversationId)).thenReturn(Optional.of(buildConversation()))
        whenever(messageRepository.findByConversationIdOrderBySentAtAsc(conversationId))
            .thenReturn(listOf(buildMessage(senderId = first)))

        val result = service.getMessages(conversationId, second)

        assertEquals(1, result.size)
    }

    @Test
    fun `getMessages returns empty list when no messages exist`() {
        whenever(conversationRepository.findById(conversationId)).thenReturn(Optional.of(buildConversation()))
        whenever(messageRepository.findByConversationIdOrderBySentAtAsc(conversationId)).thenReturn(emptyList())

        val result = service.getMessages(conversationId, first)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getMessages throws when requester is not a participant`() {
        val outsider = UUID.randomUUID()
        whenever(conversationRepository.findById(conversationId)).thenReturn(Optional.of(buildConversation()))

        val ex = assertThrows<IllegalArgumentException> {
            service.getMessages(conversationId, outsider)
        }
        assertEquals("You are not a participant of this conversation", ex.message)
        verify(messageRepository, never()).findByConversationIdOrderBySentAtAsc(any())
    }

    @Test
    fun `getMessages throws when conversation not found`() {
        whenever(conversationRepository.findById(conversationId)).thenReturn(Optional.empty())

        val ex = assertThrows<IllegalArgumentException> {
            service.getMessages(conversationId, first)
        }
        assertEquals("Conversation not found", ex.message)
    }

    // ==================== GET CONVERSATIONS FOR USER ====================

    @Test
    fun `getConversationsForUser returns all conversations where user is participantA or participantB`() {
        val conversations = listOf(buildConversation(), buildConversation(id = UUID.randomUUID()))
        whenever(conversationRepository.findByParticipantAOrParticipantB(first, first))
            .thenReturn(conversations)

        val result = service.getConversationsForUser(first)

        assertEquals(2, result.size)
    }

    @Test
    fun `getConversationsForUser returns empty list when user has no conversations`() {
        whenever(conversationRepository.findByParticipantAOrParticipantB(first, first))
            .thenReturn(emptyList())

        val result = service.getConversationsForUser(first)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getConversationsForUser maps listingId correctly`() {
        val listingId = UUID.randomUUID()
        val conversation = buildConversation(listingId = listingId)
        whenever(conversationRepository.findByParticipantAOrParticipantB(first, first))
            .thenReturn(listOf(conversation))

        val result = service.getConversationsForUser(first)

        assertEquals(listingId, result[0].listingId)
    }

    // ==================== HELPERS ====================

    private fun buildConversation(
        id: UUID = conversationId,
        listingId: UUID? = null
    ) = ConversationEntity(id = id, participantA = first, participantB = second, listingId = listingId)

    private fun buildMessage(
        id: UUID = UUID.randomUUID(),
        senderId: UUID = first,
        content: String = "Hello!"
    ) = MessageEntity(id = id, conversationId = conversationId, senderId = senderId, content = content)

    private fun buildAuthUser(
        id: UUID,
        email: String = "user@example.com",
        displayName: String = "Test User",
        studentId: String = "STU001",
        userType: String = "BUYER"
    ) = AuthUser(
        id = id,
        email = email,
        displayName = displayName,
        studentId = studentId,
        userType = userType
    )
}

