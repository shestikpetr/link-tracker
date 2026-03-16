--liquibase formatted sql
--changeset author:04-create-tags

CREATE TABLE tags
(
    id   BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);
