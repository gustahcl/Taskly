package com.taskly.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração dos tokens.
 *
 * @param secret segredo HMAC usado para assinar o access token (mínimo de 32 bytes)
 * @param accessTokenValidade tempo de vida do access token (JWT)
 * @param refreshTokenValidade tempo de vida do refresh token
 */
@ConfigurationProperties(prefix = "taskly.jwt")
public record JwtProperties(String secret, Duration accessTokenValidade, Duration refreshTokenValidade) {

	public JwtProperties {
		if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
			throw new IllegalArgumentException("taskly.jwt.secret precisa ter pelo menos 32 bytes");
		}
		if (accessTokenValidade == null) {
			accessTokenValidade = Duration.ofMinutes(15);
		}
		if (refreshTokenValidade == null) {
			refreshTokenValidade = Duration.ofDays(7);
		}
	}

}
