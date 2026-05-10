package backend.academy.linktracker.scrapper.exceptions;

import java.io.IOException;
import lombok.Getter;

@Getter
public class RetryableHttpStatusException extends IOException {
    private static final long serialVersionUID = 1L;

    private final int statusCode;

    public RetryableHttpStatusException(int statusCode, String statusText) {
        super("HTTP " + statusCode + " " + statusText);
        this.statusCode = statusCode;
    }
}
