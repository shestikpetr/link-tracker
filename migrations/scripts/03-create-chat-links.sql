--liquibase formatted sql
--changeset shestikpetr:03-create-chat-links

CREATE TABLE chat_links
(
    id         BIGSERIAL PRIMARY KEY,
    chat_id    BIGINT      NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    link_id    BIGINT      NOT NULL REFERENCES links (id) ON DELETE CASCADE,
    filters    TEXT[],
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (chat_id, link_id)
);

CREATE INDEX idx_chat_links_link_id ON chat_links (link_id);
