--liquibase formatted sql
--changeset shestikpetr:04-create-tags

CREATE TABLE tags
(
    id   BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);
