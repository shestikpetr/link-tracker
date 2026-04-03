package backend.academy.linktracker.bot.dto;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;

public record LinkUpdate(
        Long id,
        @NotNull URI url,
        String description,
        @NotNull List<Long> tgChatIds) {}
