package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.dto.LinkResponse;
import java.util.List;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration(CacheProperties properties, JsonMapper jsonMapper) {
        CacheProperties.Redis redis = properties.getRedis();

        JavaType linksType = jsonMapper.getTypeFactory().constructCollectionType(List.class, LinkResponse.class);
        JacksonJsonRedisSerializer<List<LinkResponse>> serializer =
                new JacksonJsonRedisSerializer<>(jsonMapper, linksType);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(SerializationPair.fromSerializer(serializer));

        if (redis.getTimeToLive() != null) {
            config = config.entryTtl(redis.getTimeToLive());
        }
        if (redis.getKeyPrefix() != null) {
            config = config.prefixCacheNameWith(redis.getKeyPrefix());
        }
        if (!redis.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        if (!redis.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }

        return config;
    }
}
