package com.taskly.usuario;

public record UsuarioResumo(Long id, String nome, String email) {

	public static UsuarioResumo de(Usuario usuario) {
		return new UsuarioResumo(usuario.getId(), usuario.getNome(), usuario.getEmail());
	}

}
