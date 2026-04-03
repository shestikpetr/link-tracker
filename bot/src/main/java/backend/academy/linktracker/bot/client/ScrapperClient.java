package backend.academy.linktracker.bot.client;

import backend.academy.linktracker.bot.dto.AddLinkRequest;
import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.dto.RemoveLinkRequest;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(accept = "application/json", contentType = "application/json")
public interface ScrapperClient {
    @GetExchange("/links")
    List<LinkResponse> getLinks(
            @RequestHeader("Tg-Chat-Id") Long chatId, @RequestParam(required = false) List<String> tags);

    @PostExchange("/links")
    void addLink(@RequestHeader("Tg-Chat-Id") Long chatId, @RequestBody AddLinkRequest request);

    @DeleteExchange("/links")
    void removeLink(@RequestHeader("Tg-Chat-Id") Long chatId, @RequestBody RemoveLinkRequest request);

    @PostExchange("/tg-chat/{id}")
    void registerChat(@PathVariable Long id);
}
