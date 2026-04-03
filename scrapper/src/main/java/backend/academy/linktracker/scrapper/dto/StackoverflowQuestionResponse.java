package backend.academy.linktracker.scrapper.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record StackoverflowQuestionResponse(List<Item> items) {
    public record Item(
            @JsonProperty("last_activity_date") @JsonFormat(shape = JsonFormat.Shape.NUMBER)
            Instant lastActivityDate) {}
}
