package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.client.ScrapperErrorHandler;
import backend.academy.linktracker.bot.properties.ScrapperProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class BotClientConfiguration {
    @Bean
    public ScrapperClient scrapperClient(ScrapperProperties properties, ScrapperErrorHandler errorHandler) {
        var restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultStatusHandler(HttpStatusCode::isError, (_, response) -> {
                    throw errorHandler.handle(response.getBody());
                })
                .build();

        var factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build();

        return factory.createClient(ScrapperClient.class);
    }
}
