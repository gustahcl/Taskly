package com.taskly;

import org.springframework.test.util.ReflectionTestUtils;

import com.taskly.common.UsuarioLogado;
import com.taskly.projeto.Projeto;
import com.taskly.usuario.Perfil;
import com.taskly.usuario.Usuario;

/** Objetos de domínio prontos para os testes unitários. */
public final class TestData {

	private TestData() {
	}

	public static Usuario usuario(Long id, Perfil perfil) {
		return usuario(id, "Ana", perfil);
	}

	public static Usuario usuario(Long id, String nome, Perfil perfil) {
		Usuario usuario = new Usuario(nome, nome.toLowerCase() + "@taskly.dev", "hash", perfil);
		ReflectionTestUtils.setField(usuario, "id", id);
		return usuario;
	}

	public static UsuarioLogado logado(Usuario usuario) {
		return new UsuarioLogado(usuario.getId(), usuario.getPerfil());
	}

	/** Projeto com id definido, criado pelo usuário informado (que vira GESTOR). */
	public static Projeto projeto(Long id, Usuario gestor) {
		Projeto projeto = new Projeto("Portal do cliente", gestor);
		ReflectionTestUtils.setField(projeto, "id", id);
		return projeto;
	}

}
