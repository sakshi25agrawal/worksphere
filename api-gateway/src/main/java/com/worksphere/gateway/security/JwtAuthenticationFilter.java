package com.worksphere.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter
        implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        String path =
                exchange.getRequest()
                        .getURI()
                        .getPath();

        /*
         * Authentication endpoint must be publicly accessible.
         */
        if (path.startsWith("/auth-service/api/v1/auth/")) {

            return chain.filter(exchange);
        }

        /*
         * Swagger endpoints can remain public for now.
         */
        if (path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")) {

            return chain.filter(exchange);
        }

        /*
         * Read Authorization header.
         */
        String authHeader =
                exchange.getRequest()
                        .getHeaders()
                        .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {

            return unauthorized(exchange);
        }

        String token = authHeader.substring(7);

        /*
         * Validate JWT.
         */
        if (!jwtService.isTokenValid(token)) {

            return unauthorized(exchange);
        }

        /*
         * JWT is valid.
         */
        return chain.filter(exchange);
    }

    private Mono<Void> unauthorized(
            ServerWebExchange exchange) {

        exchange.getResponse()
                .setStatusCode(HttpStatus.UNAUTHORIZED);

        return exchange.getResponse()
                .setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}