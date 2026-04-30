CREATE TABLE IF NOT EXISTS conversations (
    id            UUID                     PRIMARY KEY DEFAULT gen_random_uuid(),
    participant_a UUID                     NOT NULL REFERENCES users(id),
    participant_b UUID                     NOT NULL REFERENCES users(id),
    listing_id    UUID,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_conversation  UNIQUE (participant_a, participant_b, listing_id),
    CONSTRAINT chk_no_self_chat CHECK  (participant_a <> participant_b)
);

CREATE TABLE IF NOT EXISTS messages (
    id              UUID                     PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID                     NOT NULL REFERENCES conversations(id),
    sender_id       UUID                     NOT NULL REFERENCES users(id),
    content         TEXT                     NOT NULL,
    sent_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_conversations_participant_a ON conversations(participant_a);
CREATE INDEX IF NOT EXISTS idx_conversations_participant_b ON conversations(participant_b);
