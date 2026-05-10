package backend.academy.linktracker.scrapper.exceptions;

import java.io.IOException;
import java.io.Serial;
import lombok.Getter;

@Getter
public class RetryableHttpStatusException extends IOException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int statusCode;

    public RetryableHttpStatusException(int statusCode, String statusText) {
        super("HTTP " + statusCode + " " + statusText);
        this.statusCode = statusCode;
    }
}
