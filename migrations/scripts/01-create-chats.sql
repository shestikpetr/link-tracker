--liquibase formatted sql
--changeset shestikpetr:01-create-chats

CREATE TABLE chats
(
    id         BIGINT PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
