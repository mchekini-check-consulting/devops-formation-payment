package com.ecommerce.paymentservice.filter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Injecte traceId/spanId dans le MDC après que le filtre OTel ait créé le span.
 * Les HandlerInterceptors s'exécutent après toute la chaîne de filtres Servlet,
 * donc le span OTel est garanti actif dans Span.current() à ce stade.
 */
@Component
public class TracingMdcInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        SpanContext ctx = Span.current().getSpanContext();
        if (ctx.isValid()) {
            MDC.put("traceId", ctx.getTraceId());
            MDC.put("spanId", ctx.getSpanId());
        }
        return true;
    }
}