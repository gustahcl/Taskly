package com.taskly.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.taskly.config.JwtProperties;
import com.taskly.config.SecurityConfig;
import com.taskly.usuario.Usuario;

/**
 * Gera o access token (JWT assinado) e os valores opacos usados como refresh token.
 */
@Service
public class TokenService {

	static final String ISSUER = "taskly";

	private final JwtEncoder jwtEncoder;
	private final JwtProperties propriedades;
	private final Clock clock;
	private final SecureRandom random = new SecureRandom();

	public TokenService(JwtEncoder jwtEncoder, JwtProperties propriedades, Clock clock) {
		this.jwtEncoder = jwtEncoder;
		this.propriedades = propriedades;
		this.clock = clock;
	}

	public String gerarAccessToken(Usuario usuario) {
		Instant agora = clock.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(ISSUER)
			.subject(String.valueOf(usuario.getId()))
			.issuedAt(agora)
			.expiresAt(agora.plus(propriedades.accessTokenValidade()))
			.claim("email", usuario.getEmail())
			.claim("nome", usuario.getNome())
			.claim(SecurityConfig.CLAIM_PERFIL, usuario.getPerfil().name())
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	/** Valor aleatório de 256 bits, entregue ao cliente como refresh token. */
	public String gerarRefreshToken() {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** Hash SHA-256 (hex) do refresh token; é o que fica salvo no banco. */
	public String hash(String refreshToken) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(refreshToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 indisponível", ex);
		}
	}

}
