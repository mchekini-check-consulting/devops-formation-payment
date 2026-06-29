package com.ecommerce.paymentservice.filter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TracingMdcInterceptorTest {

    private final TracingMdcInterceptor interceptor = new TracingMdcInterceptor();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void preHandle_whenNoActiveSpan_doesNotSetMdcAndReturnsTrue() throws Exception {
        // Span.current() returns NOOP span → isValid() = false → MDC untouched
        boolean result = interceptor.preHandle(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                new Object());

        assertThat(result).isTrue();
        assertThat(MDC.get("traceId")).isNull();
        assertThat(MDC.get("spanId")).isNull();
    }

    @Test
    void preHandle_whenActiveSpan_setsMdcTraceAndSpanIdAndReturnsTrue() throws Exception {
        SpanContext mockCtx = mock(SpanContext.class);
        when(mockCtx.isValid()).thenReturn(true);
        when(mockCtx.getTraceId()).thenReturn("abc123trace000000000000000000000");
        when(mockCtx.getSpanId()).thenReturn("def456span000000");

        Span mockSpan = mock(Span.class);
        when(mockSpan.getSpanContext()).thenReturn(mockCtx);

        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            spanMock.when(Span::current).thenReturn(mockSpan);

            boolean result = interceptor.preHandle(
                    new MockHttpServletRequest(),
                    new MockHttpServletResponse(),
                    new Object());

            assertThat(result).isTrue();
            assertThat(MDC.get("traceId")).isEqualTo("abc123trace000000000000000000000");
            assertThat(MDC.get("spanId")).isEqualTo("def456span000000");
        }
    }
}
