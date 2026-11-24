package com.taskly;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base dos testes de integração: sobe um PostgreSQL 16 real via Testcontainers,
 * compartilhado entre as classes de teste. Os testes são ignorados quando o Docker não está disponível.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTest {

	@Container
	@ServiceConnection
	protected static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

}
