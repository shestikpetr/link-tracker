package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.RateLimitProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfiguration {

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(RateLimitProperties properties) {
        FilterRegistrationBean<RateLimitFilter> registration =
                new FilterRegistrationBean<>(new RateLimitFilter(properties));
        registration.addUrlPatterns("/links/*", "/links", "/tg-chat/*");
        registration.setOrder(1);
        return registration;
    }
}
