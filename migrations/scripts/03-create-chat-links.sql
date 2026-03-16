--liquibase formatted sql
--changeset author:03-create-chat-links

CREATE TABLE chat_links
(
    chat_id    BIGINT REFERENCES chats (id) ON DELETE CASCADE,
    link_id    BIGINT REFERENCES links (id) ON DELETE CASCADE,
    filters    TEXT[],
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (chat_id, link_id)
);
