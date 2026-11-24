package com.taskly.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import com.taskly.config.JwtProperties;
import com.taskly.config.SecurityConfig;
import com.taskly.usuario.Perfil;
import com.taskly.usuario.Usuario;

class TokenServiceTest {

	private final JwtProperties propriedades = new JwtProperties("segredo-de-teste-com-mais-de-32-bytes!!",
			Duration.ofMinutes(15), Duration.ofDays(7));

	private final SecurityConfig config = new SecurityConfig();

	private final JwtDecoder decoder = config.jwtDecoder(propriedades);

	@Test
	void accessTokenLevaIdPerfilEExpiracao() {
		Instant agora = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
		TokenService service = new TokenService(config.jwtEncoder(propriedades), propriedades,
				Clock.fixed(agora, ZoneOffset.UTC));

		Jwt jwt = decoder.decode(service.gerarAccessToken(usuario(42L, Perfil.GERENTE)));

		assertThat(jwt.getSubject()).isEqualTo("42");
		assertThat(jwt.getClaimAsString("perfil")).isEqualTo("GERENTE");
		assertThat(jwt.getClaimAsString("email")).isEqualTo("ana@taskly.dev");
		assertThat(jwt.getExpiresAt()).isEqualTo(agora.plus(Duration.ofMinutes(15)));
	}

	@Test
	void tokenExpiradoERejeitado() {
		Instant passado = Instant.now().minus(Duration.ofHours(1));
		TokenService service = new TokenService(config.jwtEncoder(propriedades), propriedades,
				Clock.fixed(passado, ZoneOffset.UTC));

		String token = service.gerarAccessToken(usuario(1L, Perfil.MEMBRO));

		assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void tokenAssinadoComOutroSegredoERejeitado() {
		JwtProperties outras = new JwtProperties("outro-segredo-qualquer-com-mais-de-32-bytes", null, null);
		TokenService service = new TokenService(config.jwtEncoder(outras), outras, Clock.systemUTC());

		String token = service.gerarAccessToken(usuario(1L, Perfil.ADMIN));

		assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void refreshTokenEAleatorioEOHashEDeterministico() {
		TokenService service = new TokenService(config.jwtEncoder(propriedades), propriedades, Clock.systemUTC());

		String a = service.gerarRefreshToken();
		String b = service.gerarRefreshToken();

		assertThat(a).isNotEqualTo(b);
		assertThat(service.hash(a)).hasSize(64).isEqualTo(service.hash(a)).isNotEqualTo(service.hash(b));
	}

	@Test
	void segredoCurtoNaoEAceito() {
		assertThatThrownBy(() -> new JwtProperties("curto", null, null)).isInstanceOf(IllegalArgumentException.class);
	}

	private static Usuario usuario(Long id, Perfil perfil) {
		return com.taskly.TestData.usuario(id, perfil);
	}

}
