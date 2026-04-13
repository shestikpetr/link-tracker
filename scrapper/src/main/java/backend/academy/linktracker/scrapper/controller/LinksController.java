package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.service.LinkService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/links")
@RequiredArgsConstructor
public class LinksController {
    private final LinkService linkService;

    @GetMapping
    public List<LinkResponse> getLinks(
            @RequestHeader("Tg-Chat-Id") Long chatId, @RequestParam(required = false) List<String> tags) {
        return linkService.getLinks(chatId, tags);
    }

    @PostMapping
    public LinkResponse addLink(@RequestHeader("Tg-Chat-Id") Long chatId, @RequestBody @Valid AddLinkRequest request) {
        return linkService.addLink(chatId, request.link(), request.tags(), request.filters());
    }

    @DeleteMapping
    public LinkResponse removeLink(
            @RequestHeader("Tg-Chat-Id") Long chatId, @RequestBody @Valid RemoveLinkRequest request) {
        return linkService.removeLink(chatId, request.link());
    }
}
