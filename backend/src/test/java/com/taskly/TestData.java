package com.taskly;

import org.springframework.test.util.ReflectionTestUtils;

import com.taskly.usuario.Perfil;
import com.taskly.usuario.Usuario;

/** Objetos de domínio prontos para os testes unitários. */
public final class TestData {

	private TestData() {
	}

	public static Usuario usuario(Long id, Perfil perfil) {
		Usuario usuario = new Usuario("Ana", "ana@taskly.dev", "hash", perfil);
		ReflectionTestUtils.setField(usuario, "id", id);
		return usuario;
	}

}
