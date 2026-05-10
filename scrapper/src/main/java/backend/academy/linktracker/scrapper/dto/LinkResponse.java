package backend.academy.linktracker.scrapper.dto;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.net.URI;
import java.util.List;

public record LinkResponse(Long id, URI url, List<String> tags, List<String> filters) {
    public static LinkResponse from(TrackedLink link) {
        return new LinkResponse(link.id(), link.url(), link.tags(), link.filters());
    }
}
