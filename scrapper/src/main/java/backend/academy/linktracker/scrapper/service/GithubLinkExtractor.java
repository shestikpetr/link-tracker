package backend.academy.linktracker.scrapper.service;

import java.net.URI;
import org.springframework.stereotype.Component;

@Component
public class GithubLinkExtractor {

    public boolean supports(URI url) {
        if (!"github.com".equals(url.getHost())) return false;
        String[] parts = url.getPath().split("/");
        return parts.length >= 3 && !parts[1].isBlank() && !parts[2].isBlank();
    }

    public String extractOwner(URI url) {
        return url.getPath().split("/")[1];
    }

    public String extractRepo(URI url) {
        return url.getPath().split("/")[2];
    }
}
