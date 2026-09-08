package com.filestorage.upload;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DebugRequestFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println(
                ">>> [DEBUG UPLOAD] Method=" + request.getMethod()
                        + " Path=" + request.getRequestURI()
                        + " Origin=" + request.getHeader("Origin")
                        + " User-Agent=" + request.getHeader("User-Agent")
                        + " X-Forwarded-For=" + request.getHeader("X-Forwarded-For")
                        + " Authorization=" + maskAuthorization(
                                request.getHeader("Authorization"))
        );

        try {
            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            System.out.println(
                    ">>> [DEBUG UPLOAD] EXCEPTION="
                            + ex.getClass().getName()
                            + ": "
                            + ex.getMessage()
            );
            throw ex;

        } finally {
            System.out.println(
                    ">>> [DEBUG UPLOAD] Response status="
                            + response.getStatus()
            );
        }
    }

    private String maskAuthorization(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return "null";
        }

        if (authorization.startsWith("Bearer ")) {
            return "Bearer ***";
        }

        return "***";
    }
}