package com.ecommerce.paymentservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Payment Service API")
                .version("1.0.0")
                .description("API de gestion des paiements pour l'application e-commerce")
                .contact(new Contact()
                    .name("Support E-commerce")
                    .email("support@ecommerce.com")
                    .url("https://ecommerce.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("http://springdoc.org")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8082")
                    .description("Serveur de développement"),
                new Server()
                    .url("http://payment-service:8082")
                    .description("Serveur Docker")
            ));
    }
}