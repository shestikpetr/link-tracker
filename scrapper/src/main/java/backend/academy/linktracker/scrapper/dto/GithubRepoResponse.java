package backend.academy.linktracker.scrapper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record GithubRepoResponse(@JsonProperty("pushed_at") Instant pushedAt) {}
