package com.ecommerce.paymentservice.config;

import com.ecommerce.paymentservice.config.ReadWriteRoutingDataSource.DataSourceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Value("${app.datasource.write.url}")
    private String writeUrl;

    @Value("${app.datasource.read.url}")
    private String readUrl;

    @Value("${app.datasource.username}")
    private String username;

    @Value("${app.datasource.password}")
    private String password;

    @Value("${app.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Bean
    @Primary
    public DataSource dataSource() {
        DataSource writeDataSource = DataSourceBuilder.create()
                .url(writeUrl)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();

        DataSource readDataSource = DataSourceBuilder.create()
                .url(readUrl)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();

        ReadWriteRoutingDataSource routingDataSource = new ReadWriteRoutingDataSource();
        routingDataSource.setTargetDataSources(Map.of(
                DataSourceType.WRITE, writeDataSource,
                DataSourceType.READ, readDataSource
        ));
        routingDataSource.setDefaultTargetDataSource(writeDataSource);

        return routingDataSource;
    }
}
