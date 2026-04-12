package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.client.GithubClient;
import backend.academy.linktracker.scrapper.client.StackoverflowClient;
import backend.academy.linktracker.scrapper.properties.BotProperties;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class ScrapperClientConfiguration {
    @Bean
    public GithubClient githubClient(GithubProperties properties) {
        var restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Authorization", "Bearer " + properties.getToken())
                .build();

        var factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build();

        return factory.createClient(GithubClient.class);
    }

    @Bean
    public StackoverflowClient stackoverflowClient(StackoverflowProperties properties) {
        var uriFactory = new DefaultUriBuilderFactory("https://api.stackexchange.com");
        uriFactory.setDefaultUriVariables(java.util.Map.of(
                "key", properties.getKey(),
                "access_token", properties.getAccessToken()));

        var restClient = RestClient.builder().uriBuilderFactory(uriFactory).build();

        var factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build();

        return factory.createClient(StackoverflowClient.class);
    }

    @Bean
    public BotClient botClient(BotProperties properties) {
        var restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).build();

        var factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build();

        return factory.createClient(BotClient.class);
    }
}
