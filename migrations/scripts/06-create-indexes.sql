--liquibase formatted sql
--changeset shestikpetr:06-create-indexes

CREATE INDEX idx_chat_links_chat_id ON chat_links (chat_id);
CREATE INDEX idx_chat_links_link_id ON chat_links (link_id);
CREATE INDEX idx_link_tags_link_id_tag_id ON link_tags (link_id, tag_id);
