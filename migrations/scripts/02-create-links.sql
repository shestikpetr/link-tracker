--liquibase formatted sql
--changeset shestikpetr:02-create-links

CREATE TABLE links
(
    id              BIGSERIAL PRIMARY KEY,
    url             TEXT        NOT NULL UNIQUE,
    last_checked_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
