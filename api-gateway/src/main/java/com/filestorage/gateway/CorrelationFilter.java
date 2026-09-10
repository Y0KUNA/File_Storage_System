package com.filestorage.gateway;

import com.filestorage.common.Headers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
class CorrelationFilter implements GlobalFilter, Ordered {

    private final String internalToken;

    CorrelationFilter(
            @Value("${internal.token}") String internalToken) {
        this.internalToken = internalToken;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {
        String existingCorrelationId = exchange.getRequest()
                .getHeaders()
                .getFirst(Headers.CORRELATION_ID);

        final String correlationId = existingCorrelationId == null || existingCorrelationId.isBlank()
                ? UUID.randomUUID().toString()
                : existingCorrelationId;

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    headers.remove(Headers.USER_ID);
                    headers.remove(Headers.USER_ROLE);
                    headers.remove(Headers.INTERNAL_TOKEN);
                    headers.set(Headers.CORRELATION_ID, correlationId);
                })
                .build();

        ServerWebExchange correlatedExchange = exchange.mutate()
                .request(request)
                .build();

        return correlatedExchange.getPrincipal()
                .cast(Authentication.class)
                .filter(Authentication::isAuthenticated)
                .flatMap(authentication -> chain.filter(
                        withUserHeaders(
                                correlatedExchange,
                                authentication)))
                .switchIfEmpty(
                        chain.filter(correlatedExchange));
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private ServerWebExchange withUserHeaders(
            ServerWebExchange exchange,
            Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return exchange;
        }

        String userId = jwt.getSubject();
        String userRole = jwt.getClaimAsString("role");

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    headers.remove(Headers.USER_ID);
                    headers.remove(Headers.USER_ROLE);
                    headers.remove(Headers.INTERNAL_TOKEN);

                    headers.add(
                            Headers.USER_ID,
                            userId);

                    headers.add(
                            Headers.USER_ROLE,
                            userRole);

                    headers.add(
                            Headers.INTERNAL_TOKEN,
                            internalToken);
                })
                .build();

        return exchange.mutate()
                .request(request)
                .build();
    }
}