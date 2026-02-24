package com.maxxki.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class Resilience4JConfig {

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> circuitBreakerCustomizer() {
        return factory -> {
            // CircuitBreaker für User Service
            factory.configure(builder -> builder
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                    .slidingWindowSize(10)
                    .failureRateThreshold(50.0f)
                    .waitDurationInOpenState(Duration.ofSeconds(30))
                    .permittedNumberOfCallsInHalfOpenState(3)
                    .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                    .timeoutDuration(Duration.ofSeconds(3))
                    .build()), 
                "userServiceCB");
            
            // CircuitBreaker für Order Service
            factory.configure(builder -> builder
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                    .slidingWindowSize(20)
                    .failureRateThreshold(40.0f)
                    .waitDurationInOpenState(Duration.ofSeconds(60))
                    .permittedNumberOfCallsInHalfOpenState(5)
                    .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                    .timeoutDuration(Duration.ofSeconds(5))
                    .build()), 
                "orderServiceCB");
        };
    }
}
