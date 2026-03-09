package backend.academy.linktracker.bot.controller;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.bot.service.LinkUpdateNotifier;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/updates")
@RequiredArgsConstructor
public class LinkUpdateController {
    private final LinkUpdateNotifier linkUpdateNotifier;

    @PostMapping
    public void getUpdate(@RequestBody @Valid LinkUpdate update) {
        linkUpdateNotifier.notify(update);
    }
}
