package backend.academy.linktracker.scrapper.service;

import java.net.URI;
import java.util.List;

public interface NotificationSender {
    void send(URI url, String description, List<Long> chatIds);
}
