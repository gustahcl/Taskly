package com.taskly.usuario;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskly.common.RecursoNaoEncontradoException;

@Service
public class UsuarioService {

	private final UsuarioRepository usuarioRepository;

	public UsuarioService(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional(readOnly = true)
	public Usuario buscarPorId(Long id) {
		return usuarioRepository.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
	}

	@Transactional(readOnly = true)
	public List<Usuario> listar() {
		return usuarioRepository.findAll(Sort.by("nome"));
	}

	/** Busca para convidar pessoas a um projeto: até 10 usuários ativos por nome ou e-mail. */
	@Transactional(readOnly = true)
	public List<Usuario> buscar(String termo) {
		String normalizado = termo == null ? "" : termo.trim().toLowerCase(Locale.ROOT);
		if (normalizado.length() < 2) {
			return List.of();
		}
		return usuarioRepository.buscarAtivos(normalizado, PageRequest.of(0, 10));
	}

	@Transactional
	public Usuario alterarPerfil(Long id, Perfil perfil) {
		Usuario usuario = buscarPorId(id);
		usuario.setPerfil(perfil);
		return usuario;
	}

}
