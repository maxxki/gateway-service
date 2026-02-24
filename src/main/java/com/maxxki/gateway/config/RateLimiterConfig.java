package com.maxxki.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.security.Principal;

@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal()
            .map(Principal::getName)
            .defaultIfEmpty("anonymous");
    }
    
    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            String apiKey = exchange.getRequest().getHeaders()
                .getFirst("X-API-Key");
            if (apiKey != null) {
                return Mono.just(apiKey);
            }
            // Fallback: Client-IP (aber Achtung: hinter Proxy/Load Balancer)
            String clientIp = exchange.getRequest().getRemoteAddress() != null ?
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() :
                "unknown";
            return Mono.just(clientIp);
        };
    }
    
    @Bean
    public KeyResolver combinedKeyResolver() {
        return exchange -> exchange.getPrincipal()
            .map(Principal::getName)
            .switchIfEmpty(Mono.just(
                exchange.getRequest().getHeaders().getFirst("X-API-Key")
            ))
            .switchIfEmpty(Mono.just(
                exchange.getRequest().getRemoteAddress() != null ?
                    exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() :
                    "unknown"
            ))
            .defaultIfEmpty("anonymous");
    }
}
