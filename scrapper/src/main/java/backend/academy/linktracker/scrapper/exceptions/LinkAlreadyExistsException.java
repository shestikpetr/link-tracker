package backend.academy.linktracker.scrapper.exceptions;

import java.net.URI;

public class LinkAlreadyExistsException extends RuntimeException {
    public LinkAlreadyExistsException(URI url) {
        super("Ссылка уже отслеживается: " + url);
    }
}
