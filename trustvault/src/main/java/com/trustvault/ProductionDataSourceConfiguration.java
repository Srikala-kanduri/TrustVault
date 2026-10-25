package com.trustvault;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
public class ProductionDataSourceConfiguration {

    @Bean
    DataSource productionDataSource(
            @Value("${TRUSTVAULT_DATABASE_URL}") String databaseUrl,
            @Value("${TRUSTVAULT_DATABASE_USERNAME:}") String databaseUsername,
            @Value("${TRUSTVAULT_DATABASE_PASSWORD:}") String databasePassword) {
        if (databaseUrl.startsWith("jdbc:postgresql:")) {
            DataSourceBuilder<?> builder = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName("org.postgresql.Driver")
                .url(databaseUrl);
            if (!databaseUsername.isBlank()) builder.username(databaseUsername);
            if (!databasePassword.isBlank()) builder.password(databasePassword);
            return builder.build();
        }

        try {
            URI uri = URI.create(databaseUrl);
            if (!"postgresql".equals(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getRawPath() == null
                    || uri.getRawUserInfo() == null) {
                throw new IllegalArgumentException("Unsupported PostgreSQL URL format");
            }
            String[] credentials = uri.getRawUserInfo().split(":", 2);
            if (credentials.length != 2) {
                throw new IllegalArgumentException("PostgreSQL URL is missing credentials");
            }
            String username = decodeUriComponent(credentials[0]);
            String password = decodeUriComponent(credentials[1]);
            int port = uri.getPort() < 0 ? 5432 : uri.getPort();
            String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port
                + uri.getRawPath()
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());

            return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName("org.postgresql.Driver")
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .build();
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                "TRUSTVAULT_DATABASE_URL must be a JDBC URL or a PostgreSQL connection URI",
                exception
            );
        }
    }

    private String decodeUriComponent(String value) {
        return URLDecoder.decode(
            value.replace("+", "%2B"), StandardCharsets.UTF_8
        );
    }
}
