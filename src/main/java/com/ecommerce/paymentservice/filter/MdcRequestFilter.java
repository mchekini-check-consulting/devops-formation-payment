package com.ecommerce.paymentservice.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class MdcRequestFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest http = (HttpServletRequest) req;

            String correlationId = Optional
                .ofNullable(http.getHeader("X-Correlation-Id"))
                .orElse(UUID.randomUUID().toString());

            String userId = Optional
                .ofNullable(http.getHeader("X-User-Id"))
                .orElse("anonymous");

            MDC.put("correlationId", correlationId);
            MDC.put("userId", userId);

            chain.doFilter(req, res);

        } finally {
            MDC.clear();
        }
    }
}
