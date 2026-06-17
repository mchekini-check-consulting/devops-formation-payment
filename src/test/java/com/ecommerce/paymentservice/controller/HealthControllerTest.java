package com.ecommerce.paymentservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @InjectMocks
    private HealthController healthController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(healthController, "serviceName", "payment-service");
    }

    @Test
    void health_whenDatabaseUp_returnsOkWithUpStatus() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        ResponseEntity<Map<String, Object>> response = healthController.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "UP");
        assertThat(response.getBody()).containsKey("service");
        assertThat(response.getBody()).containsKey("timestamp");
        @SuppressWarnings("unchecked")
        Map<String, Object> checks = (Map<String, Object>) response.getBody().get("checks");
        assertThat(checks).containsEntry("database", "UP");
    }

    @Test
    void health_whenDatabaseDown_returnsServiceUnavailableWithDownStatus() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("DB unreachable"));

        ResponseEntity<Map<String, Object>> response = healthController.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("status", "DOWN");
        @SuppressWarnings("unchecked")
        Map<String, Object> checks = (Map<String, Object>) response.getBody().get("checks");
        assertThat(checks).containsEntry("database", "DOWN");
    }
}
