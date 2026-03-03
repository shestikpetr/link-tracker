package backend.academy.linktracker.scrapper.model;

import java.net.URI;
import java.time.Instant;
import java.util.List;

public record TrackedLink(Long id, URI url, List<String> tags, List<String> filters, Instant lastCheckedAt) {}
