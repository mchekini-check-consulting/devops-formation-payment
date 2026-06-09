package com.ecommerce.paymentservice.controller;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    private final DataSource dataSource;

    @Value("${spring.application.name}")
    private String serviceName;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        String dbStatus = checkDatabase();
        String status = "UP".equals(dbStatus) ? "UP" : "DOWN";
        HttpStatus httpStatus = "UP".equals(status) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

        Map<String, Object> body = Map.of(
                "status", status,
                "service", serviceName,
                "timestamp", Instant.now().toString(),
                "checks", Map.of("database", dbStatus)
        );

        return ResponseEntity.status(httpStatus).body(body);
    }

    private String checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            conn.setNetworkTimeout(Runnable::run, 2000);
            try (Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(2);
                stmt.execute("SELECT 1");
            }
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
