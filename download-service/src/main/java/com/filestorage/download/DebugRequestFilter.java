package com.filestorage.download;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DebugRequestFilter extends OncePerRequestFilter {

    private static final String PREFIX = ">>> [DEBUG DOWNLOAD]";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long start = System.currentTimeMillis();

        System.out.println();
        System.out.println(
                PREFIX + " ==================== REQUEST ===================="
        );

        logRequest(request);

        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {

            System.out.println(
                    PREFIX + " ==================== EXCEPTION ===================="
            );

            System.out.println(
                    PREFIX + " ExceptionType=" + ex.getClass().getName()
            );

            System.out.println(
                    PREFIX + " Message=" + ex.getMessage()
            );

            Throwable cause = ex.getCause();

            if (cause != null) {
                System.out.println(
                        PREFIX + " Cause="
                                + cause.getClass().getName()
                                + ": "
                                + cause.getMessage()
                );
            }

            throw ex;

        } finally {

            long duration = System.currentTimeMillis() - start;

            System.out.println();
            System.out.println(
                    PREFIX + " ==================== RESPONSE ===================="
            );

            logResponse(request, response, duration);

            System.out.println(
                    PREFIX + " =================================================="
            );
            System.out.println();
        }
    }

    private void logRequest(HttpServletRequest request) {

        System.out.println(
                PREFIX + " Method=" + request.getMethod()
        );

        System.out.println(
                PREFIX + " URI=" + request.getRequestURI()
        );

        System.out.println(
                PREFIX + " QueryString=" + request.getQueryString()
        );

        System.out.println(
                PREFIX + " RemoteAddr=" + request.getRemoteAddr()
        );

        System.out.println(
                PREFIX + " RemoteHost=" + request.getRemoteHost()
        );

        System.out.println(
                PREFIX + " Protocol=" + request.getProtocol()
        );

        System.out.println(
                PREFIX + " Scheme=" + request.getScheme()
        );

        System.out.println(
                PREFIX + " ServerName=" + request.getServerName()
        );

        System.out.println(
                PREFIX + " ServerPort=" + request.getServerPort()
        );

        System.out.println(
                PREFIX + " ContentType=" + request.getContentType()
        );

        System.out.println(
                PREFIX + " ContentLength=" + request.getContentLengthLong()
        );

        System.out.println(
                PREFIX + " Origin=" + request.getHeader("Origin")
        );

        System.out.println(
                PREFIX + " Referer=" + request.getHeader("Referer")
        );

        System.out.println(
                PREFIX + " UserAgent=" + request.getHeader("User-Agent")
        );

        System.out.println(
                PREFIX + " XForwardedFor="
                        + request.getHeader("X-Forwarded-For")
        );

        System.out.println(
                PREFIX + " XForwardedProto="
                        + request.getHeader("X-Forwarded-Proto")
        );

        System.out.println(
                PREFIX + " XForwardedHost="
                        + request.getHeader("X-Forwarded-Host")
        );

        System.out.println(
                PREFIX + " XForwardedPort="
                        + request.getHeader("X-Forwarded-Port")
        );

        System.out.println(
                PREFIX + " CorrelationId="
                        + getCorrelationId(request)
        );

        System.out.println(
                PREFIX + " Authorization="
                        + maskAuthorization(
                                request.getHeader("Authorization")
                        )
        );

        System.out.println(
                PREFIX + " X-User-Id="
                        + request.getHeader("X-User-Id")
        );

        System.out.println(
                PREFIX + " X-User-Role="
                        + request.getHeader("X-User-Role")
        );

        System.out.println(
                PREFIX + " X-Internal-Token="
                        + maskSecret(
                                request.getHeader("X-Internal-Token")
                        )
        );

        logHeaders(request);
    }

    private void logHeaders(HttpServletRequest request) {

        System.out.println(PREFIX + " Headers:");

        Enumeration<String> headerNames =
                request.getHeaderNames();

        if (headerNames == null) {
            System.out.println(PREFIX + " <none>");
            return;
        }

        for (String headerName :
                Collections.list(headerNames)) {

            if (isSensitiveHeader(headerName)) {

                System.out.println(
                        PREFIX + " "
                                + headerName
                                + "=***"
                );

                continue;
            }

            String value =
                    request.getHeader(headerName);

            System.out.println(
                    PREFIX + " "
                            + headerName
                            + "="
                            + value
            );
        }
    }

    private void logResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            long duration
    ) {

        System.out.println(
                PREFIX + " Method="
                        + request.getMethod()
        );

        System.out.println(
                PREFIX + " URI="
                        + request.getRequestURI()
        );

        System.out.println(
                PREFIX + " Status="
                        + response.getStatus()
        );

        System.out.println(
                PREFIX + " ContentType="
                        + response.getContentType()
        );

        System.out.println(
                PREFIX + " CharacterEncoding="
                        + response.getCharacterEncoding()
        );

        System.out.println(
                PREFIX + " X-User-Id="
                        + request.getHeader("X-User-Id")
        );

        System.out.println(
                PREFIX + " X-User-Role="
                        + request.getHeader("X-User-Role")
        );

        System.out.println(
                PREFIX + " Duration="
                        + duration
                        + "ms"
        );

        System.out.println(
                PREFIX + " CompletedAt="
                        + Instant.now()
        );
    }

    private String getCorrelationId(
            HttpServletRequest request
    ) {

        String correlationId =
                request.getHeader("X-Correlation-Id");

        if (correlationId == null
                || correlationId.isBlank()) {

            correlationId =
                    request.getHeader("X-Request-Id");
        }

        return correlationId;
    }

    private String maskAuthorization(
            String authorization
    ) {

        if (authorization == null
                || authorization.isBlank()) {

            return "null";
        }

        if (authorization.startsWith("Bearer ")) {
            return "Bearer ***";
        }

        return "***";
    }

    private String maskSecret(String value) {

        if (value == null || value.isBlank()) {
            return "null";
        }

        return "***";
    }

    private boolean isSensitiveHeader(
            String headerName
    ) {

        String name =
                headerName.toLowerCase();

        return name.equals("authorization")
                || name.equals("cookie")
                || name.equals("set-cookie")
                || name.equals("proxy-authorization")
                || name.equals("x-internal-token");
    }
}