package com.lucky.quota.gateway.filter;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 在负载均衡选实例前检查 Nacos 是否有可用实例，避免 RoundRobinLoadBalancer 反复打印 WARN。
 */
@Component
public class ServiceInstanceCheckFilter implements GlobalFilter, Ordered {

    private static final int ORDER = 10140;

    private final DiscoveryClient discoveryClient;

    public ServiceInstanceCheckFilter(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route == null) {
            return chain.filter(exchange);
        }

        URI uri = route.getUri();
        if (!"lb".equals(uri.getScheme())) {
            return chain.filter(exchange);
        }

        String serviceId = uri.getHost();
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
        if (!CollectionUtils.isEmpty(instances)) {
            return chain.filter(exchange);
        }

        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format(
                "{\"success\":false,\"code\":503,\"message\":\"服务 %s 暂不可用，请确认该微服务已启动并注册到 Nacos\"}",
                serviceId
        );
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
        );
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
