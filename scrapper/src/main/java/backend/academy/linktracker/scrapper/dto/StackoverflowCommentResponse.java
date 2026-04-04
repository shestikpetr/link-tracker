package backend.academy.linktracker.scrapper.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record StackoverflowCommentResponse(List<Item> items) {
    public record Item(
            Owner owner,

            @JsonProperty("creation_date") @JsonFormat(shape = JsonFormat.Shape.NUMBER)
            Instant creationDate,

            String body) {}

    public record Owner(@JsonProperty("display_name") String displayName) {}
}
