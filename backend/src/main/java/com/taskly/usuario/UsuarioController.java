package com.taskly.usuario;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

	private final UsuarioService usuarioService;

	public UsuarioController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	@GetMapping("/me")
	public UsuarioResponse me(@AuthenticationPrincipal Jwt jwt) {
		return UsuarioResponse.de(usuarioService.buscarPorId(Long.valueOf(jwt.getSubject())));
	}

	/** Busca resumida (sem perfil e datas), usada para adicionar membros a um projeto. */
	@GetMapping("/busca")
	public List<UsuarioResumo> buscar(@RequestParam(defaultValue = "") String termo) {
		return usuarioService.buscar(termo).stream().map(UsuarioResumo::de).toList();
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public List<UsuarioResponse> listar() {
		return usuarioService.listar().stream().map(UsuarioResponse::de).toList();
	}

	@PatchMapping("/{id}/perfil")
	@PreAuthorize("hasRole('ADMIN')")
	public UsuarioResponse alterarPerfil(@PathVariable Long id, @Valid @RequestBody AlterarPerfilRequest request) {
		return UsuarioResponse.de(usuarioService.alterarPerfil(id, request.perfil()));
	}

}
