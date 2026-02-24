package com.maxxki.gateway.config;

import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

import java.time.Duration;

@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // User Service Route
            .route("user-service", r -> r
                .path("/api/users/**")
                .filters(f -> f
                    .circuitBreaker(cb -> cb
                        .setName("userServiceCB")
                        .setFallbackUri("forward:/fallback/users"))
                    .requestRateLimiter(rl -> rl
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(userKeyResolver()))
                    .retry(config -> config
                        .setRetries(3)
                        .setStatuses(HttpStatus.SERVICE_UNAVAILABLE))
                    .addRequestHeader("X-Forwarded-By", "gateway"))
                .uri("lb://USER-SERVICE"))
            
            // Order Service Route
            .route("order-service", r -> r
                .path("/api/orders/**")
                .filters(f -> f
                    .circuitBreaker(cb -> cb
                        .setName("orderServiceCB")
                        .setFallbackUri("forward:/fallback/orders"))
                    .requestRateLimiter(rl -> rl
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(apiKeyResolver())))
                .uri("lb://ORDER-SERVICE"))
            
            // Admin Service Route (geschützt)
            .route("admin-service", r -> r
                .path("/api/admin/**")
                .filters(f -> f
                    .circuitBreaker(cb -> cb
                        .setName("adminServiceCB"))
                    .requestRateLimiter(rl -> rl
                        .setRateLimiter(adminRateLimiter())))
                .uri("lb://ADMIN-SERVICE"))
            .build();
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // 1000 Requests pro Minute pro User
        return new RedisRateLimiter(1000, 1000, 60);
    }
    
    @Bean
    public RedisRateLimiter adminRateLimiter() {
        // Strengeres Rate Limiting für Admin-Endpunkte
        return new RedisRateLimiter(100, 100, 60);
    }

    @Bean
    public KeyResolver userKeyResolver() {
        // Rate Limiting basierend auf User (aus JWT Principal)
        return exchange -> {
            String userId = exchange.getPrincipal()
                .map(principal -> principal.getName())
                .blockOptional()
                .orElse("anonymous");
            return Mono.just(userId);
        };
    }
    
    @Bean
    public KeyResolver apiKeyResolver() {
        // Rate Limiting basierend auf API Key oder IP
        return exchange -> {
            String apiKey = exchange.getRequest().getHeaders()
                .getFirst("X-API-Key");
            if (apiKey != null) {
                return Mono.just(apiKey);
            }
            String clientIp = exchange.getRequest().getRemoteAddress()
                .getAddress().getHostAddress();
            return Mono.just(clientIp);
        };
    }
}
