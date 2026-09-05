package com.filestorage.gateway;

import com.filestorage.common.Headers;
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
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(Headers.CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .header(Headers.CORRELATION_ID, correlationId)
                .build();
        ServerWebExchange correlatedExchange = exchange.mutate().request(request).build();
        return correlatedExchange.getPrincipal()
                .cast(Authentication.class)
                .filter(Authentication::isAuthenticated)
                .flatMap(authentication -> chain.filter(withUserHeaders(correlatedExchange, authentication)))
                .switchIfEmpty(chain.filter(correlatedExchange));
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private ServerWebExchange withUserHeaders(ServerWebExchange exchange, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return exchange;
        }
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(Headers.USER_ID);
                    headers.remove(Headers.USER_ROLE);
                    headers.add(Headers.USER_ID, jwt.getSubject());
                    headers.add(Headers.USER_ROLE, jwt.getClaimAsString("role"));
                })
                .build();
        return exchange.mutate().request(request).build();
    }
}
