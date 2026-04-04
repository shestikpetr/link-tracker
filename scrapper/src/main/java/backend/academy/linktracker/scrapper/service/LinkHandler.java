package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.model.LinkUpdateInfo;
import java.net.URI;
import java.time.Instant;
import java.util.List;

public interface LinkHandler {
    boolean supports(URI url);

    List<LinkUpdateInfo> checkUpdates(URI url, Instant lastCheckedAt);
}
