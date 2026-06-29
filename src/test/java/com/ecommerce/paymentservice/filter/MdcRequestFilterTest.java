package com.ecommerce.paymentservice.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class MdcRequestFilterTest {

    private final MdcRequestFilter filter = new MdcRequestFilter();

    @Test
    void doFilter_setsProvidedCorrelationIdAndUserId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "corr-abc");
        request.addHeader("X-User-Id", "user-456");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] capturedCorrelationId = {null};
        String[] capturedUserId = {null};
        FilterChain chain = (req, res) -> {
            capturedCorrelationId[0] = MDC.get("correlationId");
            capturedUserId[0] = MDC.get("userId");
        };

        filter.doFilter(request, response, chain);

        assertThat(capturedCorrelationId[0]).isEqualTo("corr-abc");
        assertThat(capturedUserId[0]).isEqualTo("user-456");
    }

    @Test
    void doFilter_generatesCorrelationIdWhenMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] capturedCorrelationId = {null};
        FilterChain chain = (req, res) -> capturedCorrelationId[0] = MDC.get("correlationId");

        filter.doFilter(request, response, chain);

        assertThat(capturedCorrelationId[0]).isNotBlank();
    }

    @Test
    void doFilter_usesAnonymousWhenUserIdMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "corr-abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] capturedUserId = {null};
        FilterChain chain = (req, res) -> capturedUserId[0] = MDC.get("userId");

        filter.doFilter(request, response, chain);

        assertThat(capturedUserId[0]).isEqualTo("anonymous");
    }

    @Test
    void doFilter_clearsMdcAfterExecution() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "corr-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};

        filter.doFilter(request, response, chain);

        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("userId")).isNull();
    }
}
