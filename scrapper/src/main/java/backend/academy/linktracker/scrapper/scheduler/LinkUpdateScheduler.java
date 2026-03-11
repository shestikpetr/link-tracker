package backend.academy.linktracker.scrapper.scheduler;

import backend.academy.linktracker.scrapper.service.LinkUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkUpdateScheduler {
    private final LinkUpdateService linkUpdateService;

    @Scheduled(fixedDelayString = "${app.scheduler.interval}")
    public void checkUpdates() {
        log.debug("Запуск проверки обновлений");
        linkUpdateService.checkAndNotify();
        log.debug("Проверка обновлений завершена");
    }
}
