package com.taskly.usuario;

import java.util.List;

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

	@Transactional
	public Usuario alterarPerfil(Long id, Perfil perfil) {
		Usuario usuario = buscarPorId(id);
		usuario.setPerfil(perfil);
		return usuario;
	}

}
