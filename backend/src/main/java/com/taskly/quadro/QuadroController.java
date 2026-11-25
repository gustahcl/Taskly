package com.taskly.quadro;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.RestController;

import com.taskly.common.UsuarioLogado;
import com.taskly.quadro.QuadroDtos.ColunaRequest;
import com.taskly.quadro.QuadroDtos.ColunaResponse;
import com.taskly.quadro.QuadroDtos.MoverColunaRequest;
import com.taskly.quadro.QuadroDtos.QuadroRequest;
import com.taskly.quadro.QuadroDtos.QuadroResponse;
import com.taskly.quadro.QuadroDtos.QuadroResumo;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class QuadroController {

	private final QuadroService quadroService;

	public QuadroController(QuadroService quadroService) {
		this.quadroService = quadroService;
	}

	@GetMapping("/projetos/{projetoId}/quadros")
	public List<QuadroResumo> listar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long projetoId) {
		return quadroService.listar(projetoId, UsuarioLogado.de(jwt)).stream().map(QuadroResumo::de).toList();
	}

	@PostMapping("/projetos/{projetoId}/quadros")
	public ResponseEntity<QuadroResponse> criar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long projetoId,
			@Valid @RequestBody QuadroRequest request) {
		UsuarioLogado usuario = UsuarioLogado.de(jwt);
		Quadro quadro = quadroService.criar(projetoId, usuario, request.nome());
		return ResponseEntity.created(URI.create("/api/quadros/" + quadro.getId()))
			.body(quadroService.detalhar(quadro.getId(), usuario));
	}

	@GetMapping("/quadros/{id}")
	public QuadroResponse detalhar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		return quadroService.detalhar(id, UsuarioLogado.de(jwt));
	}

	@PutMapping("/quadros/{id}")
	public QuadroResumo renomear(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@Valid @RequestBody QuadroRequest request) {
		return QuadroResumo.de(quadroService.renomear(id, UsuarioLogado.de(jwt), request.nome()));
	}

	@DeleteMapping("/quadros/{id}")
	public ResponseEntity<Void> excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		quadroService.excluir(id, UsuarioLogado.de(jwt));
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/quadros/{id}/colunas")
	public ResponseEntity<ColunaResponse> adicionarColuna(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@Valid @RequestBody ColunaRequest request) {
		Coluna coluna = quadroService.adicionarColuna(id, UsuarioLogado.de(jwt), request.nome());
		return ResponseEntity.created(URI.create("/api/colunas/" + coluna.getId())).body(resposta(coluna));
	}

	@PutMapping("/colunas/{id}")
	public ColunaResponse renomearColuna(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@Valid @RequestBody ColunaRequest request) {
		return resposta(quadroService.renomearColuna(id, UsuarioLogado.de(jwt), request.nome()));
	}

	@PatchMapping("/colunas/{id}/mover")
	public ColunaResponse moverColuna(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@Valid @RequestBody MoverColunaRequest request) {
		return resposta(quadroService.moverColuna(id, UsuarioLogado.de(jwt), request.posicao()));
	}

	@DeleteMapping("/colunas/{id}")
	public ResponseEntity<Void> excluirColuna(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		quadroService.excluirColuna(id, UsuarioLogado.de(jwt));
		return ResponseEntity.noContent().build();
	}

	/** As tarefas não vêm aqui: o front recarrega o quadro ou mantém as que já tem. */
	private static ColunaResponse resposta(Coluna coluna) {
		return new ColunaResponse(coluna.getId(), coluna.getNome(), coluna.getPosicao(), List.of());
	}

}
