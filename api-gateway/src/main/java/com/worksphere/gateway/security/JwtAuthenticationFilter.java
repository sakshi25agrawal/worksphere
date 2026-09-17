package com.worksphere.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;

@Component
public class JwtAuthenticationFilter
        implements GlobalFilter, Ordered {

    private final JwtService jwtService;
    private final WebClient webClient;

    @Value("${auth.service.url:http://localhost:8085}")
    private String authServiceUrl;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            WebClient webClient) {

        this.jwtService = jwtService;
        this.webClient = webClient;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        String path = exchange.getRequest()
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
        String authHeader = exchange.getRequest()
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
            System.out.println("JWT validation FAILED");
            return unauthorized(exchange);
        }

        System.out.println("JWT validation SUCCESS");

        /*
         * Extract permissions from JWT.
         *
         * These permissions were added to the JWT by Auth Service
         * during the login process.
         */
        List<String> permissions =
                jwtService.extractPermissions(token);

        System.out.println(
                "JWT Permissions: " + permissions
        );

        /*
         * Extract the HTTP method (GET, POST, PUT, DELETE, etc.)
         * from the incoming request.
         *
         * The HTTP method is required along with the request path
         * to identify which RBAC permission is needed.
         */
        String method = exchange.getRequest()
                .getMethod()
                .name();

        /*
         * Gateway routes contain the service name as the first
         * path segment.
         *
         * Auth Service resource mappings contain only the
         * actual API path.
         *
         * Example:
         *
         * Gateway path:
         * /employee-service/api/v1/employees/10
         *
         * Resource path:
         * /api/v1/employees/10
         */
        String resourcePath = path.substring(
                path.indexOf("/", 1)
        );

        /*
         * Ask Auth Service which permission is required
         * for this HTTP method and API path.
         *
         * WebClient is reactive, so we must NOT use .block().
         */
        return webClient.get()
                .uri(authServiceUrl + "/api/v1/authorization/resource"
                                + "?method={method}&path={path}",
                        method,
                        resourcePath)
                .retrieve()
                .bodyToMono(String.class)

                /*
                 * Auth Service returned the required permission.
                 */
                .flatMap(requiredPermission -> {

                    /*
                     * No permission mapping was found.
                     *
                     * We fail closed instead of allowing
                     * the request to continue.
                     */
                    if (requiredPermission == null
                            || requiredPermission.isBlank()) {

                        return unauthorized(exchange);
                    }

                    /*
                     * Check whether the user's JWT contains
                     * the permission required for this resource.
                     */
                    if (!permissions.contains(requiredPermission)) {

                        System.out.println(
                                "Access DENIED. Required permission: "
                                        + requiredPermission
                        );

                        return forbidden(exchange);
                    }

                    /*
                     * User has the required permission.
                     *
                     * Continue the request to the downstream service.
                     */
                    System.out.println(
                            "Access GRANTED. Required permission: "
                                    + requiredPermission
                    );

                    return chain.filter(exchange);
                })

                /*
                 * If Auth Service is unavailable or the WebClient
                 * call fails, fail closed.
                 *
                 * We do NOT allow the request through when
                 * authorization cannot be verified.
                 */
                .onErrorResume(error -> {

                    System.out.println(
                            "Authorization Service call FAILED: "
                                    + error.getMessage()
                    );

                    return unauthorized(exchange);
                });
    }

    /*
     * 401 Unauthorized
     *
     * Used when the request does not contain a valid
     * authentication token.
     */
    private Mono<Void> unauthorized(
            ServerWebExchange exchange) {

        exchange.getResponse()
                .setStatusCode(HttpStatus.UNAUTHORIZED);

        return exchange.getResponse().setComplete();
    }

    /*
     * 403 Forbidden
     *
     * Used when the JWT is valid but the user does not
     * have the permission required for the resource.
     */
    private Mono<Void> forbidden(
            ServerWebExchange exchange) {

        exchange.getResponse()
                .setStatusCode(HttpStatus.FORBIDDEN);

        return exchange.getResponse().setComplete();
    }

    /*
     * Run this Gateway filter early in the request-processing chain.
     */
    @Override
    public int getOrder() {
        return -100;
    }
}