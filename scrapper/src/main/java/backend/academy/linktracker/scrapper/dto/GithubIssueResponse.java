package backend.academy.linktracker.scrapper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record GithubIssueResponse(
        String title,
        GithubUser user,
        @JsonProperty("created_at") Instant createdAt,
        String body,
        @JsonProperty("pull_request") PullRequest pullRequest) {

    public record GithubUser(String login) {}

    public record PullRequest(String url) {}

    public boolean isPullRequest() {
        return pullRequest != null;
    }
}
