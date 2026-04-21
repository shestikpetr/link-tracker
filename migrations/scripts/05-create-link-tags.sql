--liquibase formatted sql
--changeset shestikpetr:05-create-link-tags

CREATE TABLE link_tags
(
    chat_link_id BIGINT NOT NULL REFERENCES chat_links (id) ON DELETE CASCADE,
    tag_id       BIGINT NOT NULL REFERENCES tags (id) ON DELETE CASCADE,
    PRIMARY KEY (chat_link_id, tag_id)
);
