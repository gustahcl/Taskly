package com.taskly;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class TasklyBackendApplicationTests extends IntegrationTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextoSobeEMigrationsSaoAplicadas() {
		Integer tabelas = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM information_schema.tables WHERE table_name IN ('usuarios', 'refresh_tokens')",
				Integer.class);

		assertThat(tabelas).isEqualTo(2);
	}

}
