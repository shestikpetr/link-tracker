package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.client.GithubClient;
import backend.academy.linktracker.scrapper.client.StackoverflowAuthInterceptor;
import backend.academy.linktracker.scrapper.client.StackoverflowClient;
import backend.academy.linktracker.scrapper.properties.BotProperties;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ScrapperClientConfiguration {
    @Bean
    public GithubClient githubClient(GithubProperties properties, RetryHttpInterceptor retryHttpInterceptor) {
        var restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Authorization", "Bearer " + properties.getToken())
                .requestFactory(requestFactory(properties.getConnectTimeout(), properties.getReadTimeout()))
                .requestInterceptor(retryHttpInterceptor)
                .build();

        return createClient(restClient, GithubClient.class);
    }

    @Bean
    public StackoverflowClient stackoverflowClient(
            StackoverflowProperties properties, RetryHttpInterceptor retryHttpInterceptor) {
        var restClient = RestClient.builder()
                .baseUrl("https://api.stackexchange.com")
                .requestFactory(requestFactory(properties.getConnectTimeout(), properties.getReadTimeout()))
                .requestInterceptor(new StackoverflowAuthInterceptor(properties))
                .requestInterceptor(retryHttpInterceptor)
                .build();

        return createClient(restClient, StackoverflowClient.class);
    }

    @Bean
    public BotClient botClient(BotProperties properties, RetryHttpInterceptor retryHttpInterceptor) {
        var restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory(properties.getConnectTimeout(), properties.getReadTimeout()))
                .requestInterceptor(retryHttpInterceptor)
                .build();

        return createClient(restClient, BotClient.class);
    }

    public static ClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        var httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return factory;
    }

    private static <T> T createClient(RestClient restClient, Class<T> clientType) {
        var factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build();
        return factory.createClient(clientType);
    }
}
