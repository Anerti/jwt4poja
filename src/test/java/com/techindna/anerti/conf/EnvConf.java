package com.techindna.anerti.conf;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

public class EnvConf {

  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16-alpine")
          .withInitScripts(
              "test-init.sql",
              "test-course-init.sql",
              "test-group-init.sql",
              "test-class-init.sql");
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

  static {
    POSTGRES.start();
    REDIS.start();
  }

  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.jpa.properties.hibernate.default_schema", () -> "jwt4poja_app");
    registry.add(
        "spring.data.redis.url",
        () -> "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
    registry.add("app.jwt.secret", () -> "2qdeDHejlEEpEzwcE1DEOAqoY7JOw0HOqROW+FLYAXY=");
    registry.add("app.jwt.expiration-ms", () -> "3600000");
    registry.add("app.base-url", () -> "http://localhost:8080");
  }
}
