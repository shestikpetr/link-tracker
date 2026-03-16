package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.dto.AddLinkRequest;
import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.dto.RemoveLinkRequest;
import backend.academy.linktracker.bot.exceptions.LinkNotFoundException;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkTrackingService {
    private final ScrapperClient scrapperClient;

    public void register(Long chatId) {
        try {
            scrapperClient.registerChat(chatId);
        } catch (Exception e) {
            log.atDebug()
                    .setMessage("Чат уже зарегистрирован")
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("error", e.getMessage())
                    .log();
        }
    }

    public List<LinkResponse> getLinks(Long chatId, List<String> tags) {
        try {
            return scrapperClient.getLinks(chatId, tags);
        } catch (LinkNotFoundException e) {
            register(chatId);
            return scrapperClient.getLinks(chatId, tags);
        }
    }

    public void addLink(Long chatId, URI url, List<String> tags) {
        try {
            scrapperClient.addLink(chatId, new AddLinkRequest(url, tags, List.of()));
        } catch (LinkNotFoundException e) {
            register(chatId);
            scrapperClient.addLink(chatId, new AddLinkRequest(url, tags, List.of()));
        }
    }

    public void removeLink(Long chatId, URI url) {
        scrapperClient.removeLink(chatId, new RemoveLinkRequest(url));
    }
}
