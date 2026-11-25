package com.taskly;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base dos testes de integração: sobe um PostgreSQL 16 real via Testcontainers.
 * O container é iniciado pelo Spring junto com o primeiro contexto e reaproveitado por todas as classes
 * (sem {@code @Container}, que o pararia ao fim de cada classe enquanto o contexto em cache continua apontando
 * para ele). Os testes são ignorados quando o Docker não está disponível.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTest {

	@ServiceConnection
	protected static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

}
