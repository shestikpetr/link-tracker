package backend.academy.linktracker.scrapper.model;

import java.time.Instant;

public record LinkUpdateInfo(Instant timestamp, String description) {}
