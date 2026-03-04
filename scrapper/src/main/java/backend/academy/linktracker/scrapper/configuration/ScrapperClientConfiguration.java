package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.client.GithubClient;
import backend.academy.linktracker.scrapper.client.StackoverflowClient;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

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
    public StackoverflowClient stackoverflowClient() {
        var restClient =
                RestClient.builder().baseUrl("https://api.stackexchange.com").build();

        var factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build();

        return factory.createClient(StackoverflowClient.class);
    }
}
