package com.taskly.common;

import org.springframework.security.oauth2.jwt.Jwt;

import com.taskly.config.SecurityConfig;
import com.taskly.usuario.Perfil;

/** Quem está fazendo a requisição, extraído do access token. */
public record UsuarioLogado(Long id, Perfil perfil) {

	public static UsuarioLogado de(Jwt jwt) {
		return new UsuarioLogado(Long.valueOf(jwt.getSubject()),
				Perfil.valueOf(jwt.getClaimAsString(SecurityConfig.CLAIM_PERFIL)));
	}

	public boolean admin() {
		return perfil == Perfil.ADMIN;
	}

}
