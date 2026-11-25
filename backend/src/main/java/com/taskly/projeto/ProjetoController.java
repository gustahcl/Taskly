package com.taskly.projeto;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.taskly.common.PaginaResponse;
import com.taskly.common.UsuarioLogado;
import com.taskly.projeto.ProjetoDtos.AlterarPapelRequest;
import com.taskly.projeto.ProjetoDtos.MembroRequest;
import com.taskly.projeto.ProjetoDtos.MembroResponse;
import com.taskly.projeto.ProjetoDtos.ProjetoRequest;
import com.taskly.projeto.ProjetoDtos.ProjetoResponse;
import com.taskly.projeto.ProjetoDtos.ProjetoResumo;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/projetos")
public class ProjetoController {

	private final ProjetoService projetoService;

	public ProjetoController(ProjetoService projetoService) {
		this.projetoService = projetoService;
	}

	@GetMapping
	public PaginaResponse<ProjetoResumo> listar(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) StatusProjeto status, @RequestParam(required = false) String busca,
			@PageableDefault(size = 10, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
		UsuarioLogado usuario = UsuarioLogado.de(jwt);
		return PaginaResponse.de(projetoService.listar(usuario, status, busca, pageable),
				p -> ProjetoResumo.de(p, usuario.id()));
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
	public ResponseEntity<ProjetoResponse> criar(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody ProjetoRequest request) {
		UsuarioLogado usuario = UsuarioLogado.de(jwt);
		Projeto projeto = projetoService.criar(usuario, request);
		return ResponseEntity.created(URI.create("/api/projetos/" + projeto.getId()))
			.body(ProjetoResponse.de(projeto, usuario.id(), true));
	}

	@GetMapping("/{id}")
	public ProjetoResponse buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		UsuarioLogado usuario = UsuarioLogado.de(jwt);
		Projeto projeto = projetoService.buscarVisivel(id, usuario);
		return ProjetoResponse.de(projeto, usuario.id(), projetoService.podeGerenciar(projeto, usuario));
	}

	@PutMapping("/{id}")
	public ProjetoResponse atualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@Valid @RequestBody ProjetoRequest request) {
		UsuarioLogado usuario = UsuarioLogado.de(jwt);
		return ProjetoResponse.de(projetoService.atualizar(id, usuario, request), usuario.id(), true);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		projetoService.excluir(id, UsuarioLogado.de(jwt));
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/membros")
	public List<MembroResponse> membros(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		Projeto projeto = projetoService.buscarVisivel(id, UsuarioLogado.de(jwt));
		return ProjetoResponse.de(projeto, null, false).membros();
	}

	@PostMapping("/{id}/membros")
	public ResponseEntity<MembroResponse> adicionarMembro(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@Valid @RequestBody MembroRequest request) {
		MembroProjeto membro = projetoService.adicionarMembro(id, UsuarioLogado.de(jwt), request.usuarioId(),
				request.papel());
		return ResponseEntity.created(URI.create("/api/projetos/" + id + "/membros/" + request.usuarioId()))
			.body(MembroResponse.de(membro));
	}

	@PatchMapping("/{id}/membros/{usuarioId}")
	public MembroResponse alterarPapel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@PathVariable Long usuarioId, @Valid @RequestBody AlterarPapelRequest request) {
		return MembroResponse.de(projetoService.alterarPapel(id, UsuarioLogado.de(jwt), usuarioId, request.papel()));
	}

	@DeleteMapping("/{id}/membros/{usuarioId}")
	public ResponseEntity<Void> removerMembro(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@PathVariable Long usuarioId) {
		projetoService.removerMembro(id, UsuarioLogado.de(jwt), usuarioId);
		return ResponseEntity.noContent().build();
	}

}
