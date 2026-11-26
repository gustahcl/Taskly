package com.taskly.apontamento;

import java.net.URI;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.taskly.apontamento.ApontamentoDtos.ApontamentoRequest;
import com.taskly.apontamento.ApontamentoDtos.ApontamentoResponse;
import com.taskly.apontamento.ApontamentoDtos.Consolidado;
import com.taskly.apontamento.ApontamentoDtos.HorasDaTarefa;
import com.taskly.apontamento.ApontamentoDtos.MinhasHoras;
import com.taskly.common.UsuarioLogado;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ApontamentoController {

	private final ApontamentoService apontamentoService;

	public ApontamentoController(ApontamentoService apontamentoService) {
		this.apontamentoService = apontamentoService;
	}

	@GetMapping("/tarefas/{tarefaId}/apontamentos")
	public HorasDaTarefa daTarefa(@AuthenticationPrincipal Jwt jwt, @PathVariable Long tarefaId) {
		return apontamentoService.daTarefa(tarefaId, UsuarioLogado.de(jwt));
	}

	@PostMapping("/tarefas/{tarefaId}/apontamentos")
	public ResponseEntity<ApontamentoResponse> registrar(@AuthenticationPrincipal Jwt jwt,
			@PathVariable Long tarefaId, @Valid @RequestBody ApontamentoRequest request) {
		ApontamentoResponse apontamento = apontamentoService.registrar(tarefaId, UsuarioLogado.de(jwt), request);
		return ResponseEntity.created(URI.create("/api/apontamentos/" + apontamento.id())).body(apontamento);
	}

	@PutMapping("/apontamentos/{id}")
	public ApontamentoResponse atualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@Valid @RequestBody ApontamentoRequest request) {
		return apontamentoService.atualizar(id, UsuarioLogado.de(jwt), request);
	}

	@DeleteMapping("/apontamentos/{id}")
	public ResponseEntity<Void> excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		apontamentoService.excluir(id, UsuarioLogado.de(jwt));
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/apontamentos/meus")
	public MinhasHoras minhas(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate inicio,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate fim) {
		return apontamentoService.minhas(UsuarioLogado.de(jwt), inicio, fim);
	}

	@GetMapping("/apontamentos/consolidado")
	public Consolidado consolidado(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate inicio,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate fim,
			@RequestParam(required = false) Long projetoId) {
		return apontamentoService.consolidado(UsuarioLogado.de(jwt), inicio, fim, projetoId);
	}

}
