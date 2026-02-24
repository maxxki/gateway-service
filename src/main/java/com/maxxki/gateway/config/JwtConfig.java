package com.maxxki.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;

@Configuration
public class JwtConfig {

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        // Für Keycloak / Auth0 / etc.
        String issuerUri = "https://your-auth-server.com/issuer";
        
        NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder
            .withJwkSetUri(issuerUri + "/protocol/openid-connect/certs")
            .build();
        
        // Token Validatoren
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<>(
            "aud", audience -> audience != null && audience.contains("gateway-client")
        );
        
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> combinedValidator = new DelegatingOAuth2TokenValidator<>(
            withIssuer, audienceValidator
        );
        
        jwtDecoder.setJwtValidator(combinedValidator);
        
        return jwtDecoder;
    }
    
    @Bean
    public ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        // Hier können Claims in Authorities umgewandelt werden
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}
