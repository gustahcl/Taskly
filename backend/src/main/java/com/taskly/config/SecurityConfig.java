package com.taskly.config;

import java.nio.charset.StandardCharsets;
import java.time.Clock;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * API stateless: cada requisição traz um access token JWT (HS256) no header Authorization.
 * O perfil do usuário vai na claim {@code perfil} e vira a authority {@code ROLE_<PERFIL>}.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

	public static final String CLAIM_PERFIL = "perfil";

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
			.sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(rotas -> rotas
				.requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
				.requestMatchers("/actuator/health", "/error").permitAll()
				.anyRequest().authenticated())
			.oauth2ResourceServer(oauth -> oauth
				.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public JwtEncoder jwtEncoder(JwtProperties propriedades) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(chave(propriedades)));
	}

	@Bean
	public JwtDecoder jwtDecoder(JwtProperties propriedades) {
		return NimbusJwtDecoder.withSecretKey(chave(propriedades)).macAlgorithm(MacAlgorithm.HS256).build();
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	private static JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName(CLAIM_PERFIL);
		authorities.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
		conversor.setJwtGrantedAuthoritiesConverter(authorities);
		return conversor;
	}

	private static SecretKey chave(JwtProperties propriedades) {
		return new SecretKeySpec(propriedades.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

}
