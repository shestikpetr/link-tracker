package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.client.ScrapperClient;
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
        var restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).build();

        var factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build();

        return factory.createClient(ScrapperClient.class);
    }
}
