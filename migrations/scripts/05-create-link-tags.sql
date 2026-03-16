--liquibase formatted sql
--changeset author:05-create-link-tags

CREATE TABLE link_tags
(
    chat_id BIGINT,
    link_id BIGINT,
    tag_id  BIGINT REFERENCES tags (id) ON DELETE CASCADE,
    PRIMARY KEY (chat_id, link_id, tag_id),
    FOREIGN KEY (chat_id, link_id) REFERENCES chat_links (chat_id, link_id) ON DELETE CASCADE
);
