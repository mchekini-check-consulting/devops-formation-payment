package com.ecommerce.paymentservice.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class HttpLoggingFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI();

        if (path.equals("/api/health") || path.equals("/actuator/prometheus")) {
            chain.doFilter(req, res);
            return;
        }

        long start = System.currentTimeMillis();

        MDC.put("httpMethod", request.getMethod());
        MDC.put("httpPath", path);

        LOGGER.info("Incoming request");

        try {
            chain.doFilter(req, res);
        } finally {
            long duration = System.currentTimeMillis() - start;

            MDC.put("httpStatusCode", String.valueOf(response.getStatus()));
            MDC.put("duration", duration + "ms");

            LOGGER.info("Request completed");

            MDC.remove("httpMethod");
            MDC.remove("httpPath");
            MDC.remove("httpStatusCode");
            MDC.remove("duration");
        }
    }
}
