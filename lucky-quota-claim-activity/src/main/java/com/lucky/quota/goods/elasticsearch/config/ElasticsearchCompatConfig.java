package com.lucky.quota.goods.elasticsearch.config;

import org.apache.http.HttpHeaders;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 使 Spring Boot 3 自带的 ES 8 客户端兼容本地 Elasticsearch 7.17
 */
@Configuration
@ConditionalOnProperty(prefix = "goods.elasticsearch", name = "enabled", havingValue = "true")
public class ElasticsearchCompatConfig {

    private static final String COMPAT_HEADER = "application/vnd.elasticsearch+json;compatible-with=7";

    @Bean
    RestClientBuilderCustomizer elasticsearch7CompatCustomizer() {
        return (RestClientBuilder builder) -> builder.setDefaultHeaders(new BasicHeader[]{
                new BasicHeader(HttpHeaders.ACCEPT, COMPAT_HEADER),
                new BasicHeader(HttpHeaders.CONTENT_TYPE, COMPAT_HEADER)
        });
    }
}
