package backend.academy.linktracker.scrapper.repository.sql;

import java.time.Instant;

record LinkRow(Long chatId, Long id, String url, Instant lastCheckedAt, String[] filters, String tagName) {}
