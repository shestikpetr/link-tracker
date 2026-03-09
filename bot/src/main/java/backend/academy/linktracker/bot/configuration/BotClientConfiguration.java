package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.exceptions.LinkAlreadyTrackedException;
import backend.academy.linktracker.bot.exceptions.LinkNotFoundException;
import backend.academy.linktracker.bot.exceptions.UnsupportedLinkException;
import backend.academy.linktracker.bot.properties.ScrapperProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class BotClientConfiguration {
    @Bean
    public ScrapperClient scrapperClient(ScrapperProperties properties) {
        var restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultStatusHandler(status -> status.value() == 409, (_, _) -> {
                    throw new LinkAlreadyTrackedException();
                })
                .defaultStatusHandler(status -> status.value() == 422, (_, _) -> {
                    throw new UnsupportedLinkException();
                })
                .defaultStatusHandler(status -> status.value() == 404, (_, _) -> {
                    throw new LinkNotFoundException();
                })
                .build();

        var factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build();

        return factory.createClient(ScrapperClient.class);
    }
}
