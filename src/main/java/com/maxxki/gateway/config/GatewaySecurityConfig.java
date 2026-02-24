package com.maxxki.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Flux;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(
                    "/auth/**",
                    "/api/public/**",
                    "/actuator/health",
                    "/actuator/info",
                    "/fallback/**"
                ).permitAll()
                // IMPORTANT: hasRole sucht nach "ROLE_ADMIN", 
                // hasAuthority sucht genau nach dem Wert "ADMIN"
                .pathMatchers("/api/admin/**").hasAuthority("ADMIN")
                .pathMatchers("/api/internal/**").hasAuthority("SCOPE_internal")
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .build();
    }
    
    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        
        // Wenn dein JWT die Rollen ohne "ROLE_" Präfix liefert (z.B. "ADMIN")
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var roles = jwt.getClaimAsStringList("roles");
            if (roles == null) {
                roles = jwt.getClaimAsStringList("authorities");
            }
            if (roles == null) {
                roles = jwt.getClaimAsStringList("scp");
            }
            
            if (roles != null) {
                return Flux.fromIterable(roles)
                    .map(role -> {
                        // Entweder Präfix hinzufügen für hasRole()
                        // return new SimpleGrantedAuthority("ROLE_" + role);
                        
                        // Oder direkt für hasAuthority()
                        return new SimpleGrantedAuthority(role);
                    });
            }
            
            return Flux.empty();
        });
        
        return converter;
    }
}
