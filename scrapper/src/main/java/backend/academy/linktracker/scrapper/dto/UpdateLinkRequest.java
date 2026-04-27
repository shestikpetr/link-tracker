package backend.academy.linktracker.scrapper.dto;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;

public record UpdateLinkRequest(@NotNull URI link, List<String> tags, List<String> filters) {
    public UpdateLinkRequest {
        tags = tags != null ? tags : List.of();
        filters = filters != null ? filters : List.of();
    }
}
