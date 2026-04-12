package backend.academy.linktracker.scrapper.dto;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;

public record AddLinkRequest(@NotNull URI link, List<String> tags, List<String> filters) {
    public AddLinkRequest {
        tags = tags != null ? tags : List.of();
        filters = filters != null ? filters : List.of();
    }
}
