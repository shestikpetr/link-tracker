package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.GithubIssueResponse;
import backend.academy.linktracker.scrapper.dto.GithubRepoResponse;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(accept = "application/vnd.github+json")
public interface GithubClient {
    @GetExchange("/repos/{owner}/{repo}")
    GithubRepoResponse getRepository(@PathVariable String owner, @PathVariable String repo);

    @GetExchange("/repos/{owner}/{repo}/issues")
    List<GithubIssueResponse> getIssues(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam String state,
            @RequestParam String sort,
            @RequestParam String direction,
            @RequestParam String since,
            @RequestParam("per_page") int perPage);
}
