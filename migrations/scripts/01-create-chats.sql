--liquibase formatted sql
--changeset author:01-create-chats

CREATE TABLE chats
(
    id         BIGINT PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
