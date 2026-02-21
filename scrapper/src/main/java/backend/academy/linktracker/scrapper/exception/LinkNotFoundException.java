package backend.academy.linktracker.scrapper.exception;

import java.net.URI;

public class LinkNotFoundException extends RuntimeException {
    public LinkNotFoundException(URI url) {
        super("Ссылка не найдена: " + url);
    }
}
