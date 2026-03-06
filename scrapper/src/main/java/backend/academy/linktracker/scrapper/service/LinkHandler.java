package backend.academy.linktracker.scrapper.service;

import java.net.URI;
import java.time.Instant;

public interface LinkHandler {
    boolean supports(URI url);

    Instant getLastActivity(URI url);
}
