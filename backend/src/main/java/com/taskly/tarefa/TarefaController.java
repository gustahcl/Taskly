package com.taskly.tarefa;

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
import com.taskly.tarefa.TarefaDtos.ComentarioRequest;
import com.taskly.tarefa.TarefaDtos.ComentarioResponse;
import com.taskly.tarefa.TarefaDtos.MoverTarefaRequest;
import com.taskly.tarefa.TarefaDtos.TarefaOpcao;
import com.taskly.tarefa.TarefaDtos.TarefaRequest;
import com.taskly.tarefa.TarefaDtos.TarefaResponse;
import com.taskly.tarefa.TarefaDtos.TarefaResumo;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class TarefaController {

	private final TarefaService tarefaService;

	public TarefaController(TarefaService tarefaService) {
		this.tarefaService = tarefaService;
	}

	@PostMapping("/colunas/{colunaId}/tarefas")
	public ResponseEntity<TarefaResponse> criar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long colunaId,
			@Valid @RequestBody TarefaRequest request) {
		TarefaResponse tarefa = tarefaService.criar(colunaId, UsuarioLogado.de(jwt), request);
		return ResponseEntity.created(URI.create("/api/tarefas/" + tarefa.id())).body(tarefa);
	}

	@GetMapping("/projetos/{projetoId}/tarefas")
	public List<TarefaOpcao> doProjeto(@AuthenticationPrincipal Jwt jwt, @PathVariable Long projetoId) {
		return tarefaService.doProjeto(projetoId, UsuarioLogado.de(jwt));
	}

	@GetMapping("/tarefas/{id}")
	public TarefaResponse detalhar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		return tarefaService.detalhar(id, UsuarioLogado.de(jwt));
	}

	@PutMapping("/tarefas/{id}")
	public TarefaResponse atualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@Valid @RequestBody TarefaRequest request) {
		return tarefaService.atualizar(id, UsuarioLogado.de(jwt), request);
	}

	@DeleteMapping("/tarefas/{id}")
	public ResponseEntity<Void> excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		tarefaService.excluir(id, UsuarioLogado.de(jwt));
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/tarefas/{id}/mover")
	public TarefaResumo mover(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@Valid @RequestBody MoverTarefaRequest request) {
		return tarefaService.mover(id, UsuarioLogado.de(jwt), request.colunaId(), request.posicao());
	}

	@GetMapping("/tarefas/{id}/comentarios")
	public List<ComentarioResponse> comentarios(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		return tarefaService.comentarios(id, UsuarioLogado.de(jwt));
	}

	@PostMapping("/tarefas/{id}/comentarios")
	public ResponseEntity<ComentarioResponse> comentar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
			@Valid @RequestBody ComentarioRequest request) {
		ComentarioResponse comentario = tarefaService.comentar(id, UsuarioLogado.de(jwt), request.texto());
		return ResponseEntity.created(URI.create("/api/comentarios/" + comentario.id())).body(comentario);
	}

	@DeleteMapping("/comentarios/{id}")
	public ResponseEntity<Void> excluirComentario(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		tarefaService.excluirComentario(id, UsuarioLogado.de(jwt));
		return ResponseEntity.noContent().build();
	}

}
