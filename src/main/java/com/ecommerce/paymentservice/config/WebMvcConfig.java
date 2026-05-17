package com.ecommerce.paymentservice.config;

import com.ecommerce.paymentservice.filter.TracingMdcInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TracingMdcInterceptor tracingMdcInterceptor;

    public WebMvcConfig(TracingMdcInterceptor tracingMdcInterceptor) {
        this.tracingMdcInterceptor = tracingMdcInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tracingMdcInterceptor);
    }
}