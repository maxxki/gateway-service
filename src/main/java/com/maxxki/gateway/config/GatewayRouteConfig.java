package com.maxxki.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouteLocator customRouteLocator(
            RouteLocatorBuilder builder,
            RedisRateLimiter redisRateLimiter,
            RedisRateLimiter adminRateLimiter,
            KeyResolver combinedKeyResolver) {
        
        return builder.routes()
            .route("user-service", r -> r
                .path("/api/users/**")
                .filters(f -> f
                    .circuitBreaker(cb -> cb
                        .setName("userServiceCB")
                        .setFallbackUri("forward:/fallback/users"))
                    .requestRateLimiter(rl -> rl
                        .setRateLimiter(redisRateLimiter)
                        .setKeyResolver(combinedKeyResolver))
                    .retry(config -> config
                        .setRetries(3)
                        .setStatuses(HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE)
                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, true))
                    .addRequestHeader("X-Forwarded-By", "gateway")
                    .dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_UNIQUE"))
                .uri("lb://USER-SERVICE"))
            
            .route("order-service", r -> r
                .path("/api/orders/**")
                .filters(f -> f
                    .circuitBreaker(cb -> cb
                        .setName("orderServiceCB")
                        .setFallbackUri("forward:/fallback/orders"))
                    .requestRateLimiter(rl -> rl
                        .setRateLimiter(redisRateLimiter)
                        .setKeyResolver(combinedKeyResolver)))
                .uri("lb://ORDER-SERVICE"))
            
            .route("admin-service", r -> r
                .path("/api/admin/**")
                .filters(f -> f
                    .circuitBreaker(cb -> cb
                        .setName("adminServiceCB"))
                    .requestRateLimiter(rl -> rl
                        .setRateLimiter(adminRateLimiter)
                        .setKeyResolver(combinedKeyResolver)))
                .uri("lb://ADMIN-SERVICE"))
            .build();
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // replenishRate: Anzahl Requests pro Sekunde
        // burstCapacity: Maximale Anzahl Requests pro Minute
        // requestedTokens: Wie viele Tokens pro Request (meist 1)
        return new RedisRateLimiter(100, 200, 1);
    }
    
    @Bean
    public RedisRateLimiter adminRateLimiter() {
        return new RedisRateLimiter(20, 40, 1);
    }
}
