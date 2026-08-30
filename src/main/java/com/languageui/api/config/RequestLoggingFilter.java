package com.languageui.api.config;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = requestId(request);
        long startedAt = System.nanoTime();
        MDC.put("requestId", requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("request method={} path={} status={} durationMs={}", request.getMethod(),
                    request.getRequestURI(), response.getStatus(), durationMs);
            MDC.remove("requestId");
        }
    }

    private String requestId(HttpServletRequest request) {
        String supplied = request.getHeader(REQUEST_ID_HEADER);
        return supplied == null || supplied.isBlank() ? UUID.randomUUID().toString() : supplied;
    }
}
