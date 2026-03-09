package backend.academy.linktracker.scrapper.exceptions;

import java.net.URI;

public class UnsupportedLinkException extends RuntimeException {
    public UnsupportedLinkException(URI url) {
        super("Ссылка не поддерживается: " + url);
    }
}
