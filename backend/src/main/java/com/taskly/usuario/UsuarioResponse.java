package com.taskly.usuario;

import java.time.Instant;

public record UsuarioResponse(Long id, String nome, String email, Perfil perfil, boolean ativo, Instant criadoEm) {

	public static UsuarioResponse de(Usuario usuario) {
		return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil(),
				usuario.isAtivo(), usuario.getCriadoEm());
	}

}
