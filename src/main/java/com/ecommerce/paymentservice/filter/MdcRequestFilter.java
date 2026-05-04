package com.ecommerce.paymentservice.filter;   // adapte le package à ton projet

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)   // s'exécute avant tous les autres filtres
public class MdcRequestFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest http = (HttpServletRequest) req;

            // Récupère l'ID de corrélation passé par le service appelant (order → payment)
            // ou en génère un nouveau si la requête vient de l'extérieur
            String correlationId = Optional
                .ofNullable(http.getHeader("X-Correlation-Id"))
                .orElse(UUID.randomUUID().toString());

            // Récupère le userId passé dans le header (posé par order-service ou le client)
            String userId = Optional
                .ofNullable(http.getHeader("X-User-Id"))
                .orElse("anonymous");

            MDC.put("correlationId", correlationId);
            MDC.put("userId", userId);

            chain.doFilter(req, res);   // continue vers le controller

        } finally {
            MDC.clear();   // OBLIGATOIRE : nettoie le MDC pour éviter les fuites
        }                  // entre requêtes sur le même thread (thread pool Tomcat)
    }
}