package com.ecommerce.paymentservice.config;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class HealthCheckTelemetryFilterTest {

    private static final AttributeKey<String> URL_PATH = AttributeKey.stringKey("url.path");
    private static final AttributeKey<String> HTTP_TARGET = AttributeKey.stringKey("http.target");

    private Sampler delegate;
    private Sampler sampler;

    @BeforeEach
    void setUp() throws Exception {
        delegate = mock(Sampler.class);
        // HealthCheckExcludingSampler is a private nested class; access it via reflection
        // since it's only reachable through the enclosing @Bean method otherwise.
        Class<?> samplerClass = Class.forName(
                "com.ecommerce.paymentservice.config.HealthCheckTelemetryFilter$HealthCheckExcludingSampler");
        Constructor<?> constructor = samplerClass.getDeclaredConstructor(Sampler.class);
        constructor.setAccessible(true);
        sampler = (Sampler) constructor.newInstance(delegate);
    }

    @Test
    void shouldSample_whenSpanNameContainsHealthPath_drops() {
        SamplingResult result = sampler.shouldSample(
                Context.root(), "trace-id", "GET /api/health", SpanKind.SERVER,
                Attributes.empty(), Collections.emptyList());

        assertThat(result.getDecision()).isEqualTo(SamplingDecision.DROP);
        verifyNoInteractions(delegate);
    }

    @Test
    void shouldSample_whenUrlPathAttributeIsHealth_drops() {
        Attributes attributes = Attributes.builder().put(URL_PATH, "/api/health").build();

        SamplingResult result = sampler.shouldSample(
                Context.root(), "trace-id", "some-span", SpanKind.SERVER,
                attributes, Collections.emptyList());

        assertThat(result.getDecision()).isEqualTo(SamplingDecision.DROP);
        verifyNoInteractions(delegate);
    }

    @Test
    void shouldSample_whenHttpTargetAttributeIsHealth_drops() {
        Attributes attributes = Attributes.builder().put(HTTP_TARGET, "/api/health").build();

        SamplingResult result = sampler.shouldSample(
                Context.root(), "trace-id", "some-span", SpanKind.SERVER,
                attributes, Collections.emptyList());

        assertThat(result.getDecision()).isEqualTo(SamplingDecision.DROP);
        verifyNoInteractions(delegate);
    }

    @Test
    void shouldSample_whenNotHealthCheck_delegatesToWrappedSampler() {
        SamplingResult expected = SamplingResult.recordAndSample();
        when(delegate.shouldSample(any(), any(), any(), any(), any(), any())).thenReturn(expected);

        SamplingResult result = sampler.shouldSample(
                Context.root(), "trace-id", "GET /api/payments", SpanKind.SERVER,
                Attributes.empty(), Collections.emptyList());

        assertThat(result).isEqualTo(expected);
        verify(delegate).shouldSample(eq(Context.root()), eq("trace-id"), eq("GET /api/payments"),
                eq(SpanKind.SERVER), eq(Attributes.empty()), eq(Collections.emptyList()));
    }

    @Test
    void getDescription_wrapsDelegateDescription() {
        when(delegate.getDescription()).thenReturn("delegate-desc");

        assertThat(sampler.getDescription()).isEqualTo("HealthCheckExcludingSampler{delegate=delegate-desc}");
    }
}
