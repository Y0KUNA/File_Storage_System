package com.filestorage.gateway;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DebugCorsFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        System.out.println(">>> [DEBUG] Method=" + exchange.getRequest().getMethod()
                + " Path=" + exchange.getRequest().getPath()
                + " Origin=" + exchange.getRequest().getHeaders().getOrigin());
        return chain.filter(exchange)
                .doFinally(signal -> System.out.println(">>> [DEBUG] Response status="
                        + exchange.getResponse().getStatusCode()));
    }
}