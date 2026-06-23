package com.ecommerce.paymentservice.config;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;

@Configuration
public class HealthCheckTelemetryFilter {

    private static final AttributeKey<String> URL_PATH = AttributeKey.stringKey("url.path");
    private static final AttributeKey<String> HTTP_TARGET = AttributeKey.stringKey("http.target");

    @Bean
    public AutoConfigurationCustomizerProvider healthCheckFilterProvider() {
        return configCustomizer -> configCustomizer.addTracerProviderCustomizer(
                (tracerProviderBuilder, configProperties) -> {
                    // Récupère le sampler déjà configuré par Azure Monitor / OTel
                    Sampler existingSampler = Sampler.parentBased(
                            Sampler.traceIdRatioBased(
                                    Double.parseDouble(
                                            configProperties.getString(
                                                    "otel.traces.sampler.arg", "1.0"))));

                    return tracerProviderBuilder.setSampler(
                            new HealthCheckExcludingSampler(existingSampler));
                });
    }

    private static class HealthCheckExcludingSampler implements Sampler {
        private final Sampler delegate;

        HealthCheckExcludingSampler(Sampler delegate) {
            this.delegate = delegate;
        }

        @Override
        public SamplingResult shouldSample(Context parentContext, String traceId,
                                           String name, SpanKind spanKind,
                                           Attributes attributes, List<LinkData> parentLinks) {
            if (name != null && name.contains("/api/health")) {
                return SamplingResult.drop();
            }
            String urlPath = attributes.get(URL_PATH);
            if ("/api/health".equals(urlPath)) {
                return SamplingResult.drop();
            }
            String httpTarget = attributes.get(HTTP_TARGET);
            if ("/api/health".equals(httpTarget)) {
                return SamplingResult.drop();
            }
            return delegate.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
        }

        @Override
        public String getDescription() {
            return "HealthCheckExcludingSampler{delegate=" + delegate.getDescription() + "}";
        }
    }
}
