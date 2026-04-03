package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.dto.AddLinkRequest;
import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.dto.RemoveLinkRequest;
import backend.academy.linktracker.bot.exceptions.ChatAlreadyExistsException;
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

    private void ensureChatRegistered(Long chatId) {
        try {
            scrapperClient.registerChat(chatId);
        } catch (ChatAlreadyExistsException e) {
            log.atDebug()
                    .setMessage("Чат уже зарегистрирован")
                    .addKeyValue("chatId", chatId)
                    .log();
        }
    }

    public List<LinkResponse> getLinks(Long chatId, List<String> tags) {
        ensureChatRegistered(chatId);
        return scrapperClient.getLinks(chatId, tags);
    }

    public void addLink(Long chatId, URI url, List<String> tags) {
        ensureChatRegistered(chatId);
        scrapperClient.addLink(chatId, new AddLinkRequest(url, tags, List.of()));
    }

    public void removeLink(Long chatId, URI url) {
        scrapperClient.removeLink(chatId, new RemoveLinkRequest(url));
    }
}
