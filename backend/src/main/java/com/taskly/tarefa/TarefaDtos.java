package com.taskly.tarefa;

import java.time.Instant;
import java.time.LocalDate;

import com.taskly.usuario.Usuario;
import com.taskly.usuario.UsuarioResumo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Objetos de entrada e saída dos endpoints de tarefa e comentário. */
public final class TarefaDtos {

	private TarefaDtos() {
	}

	public record TarefaRequest(
			@NotBlank @Size(max = 200) String titulo,
			@Size(max = 5000) String descricao,
			Long responsavelId,
			Prioridade prioridade,
			LocalDate prazo) {
	}

	/** Move a tarefa para a coluna informada (pode ser a mesma), na posição indicada (0 = topo). */
	public record MoverTarefaRequest(@NotNull Long colunaId, @NotNull @PositiveOrZero Integer posicao) {
	}

	public record ComentarioRequest(@NotBlank @Size(max = 2000) String texto) {
	}

	/** Versão enxuta da tarefa, usada nos cards do quadro. */
	public record TarefaResumo(Long id, Long colunaId, String titulo, Prioridade prioridade, LocalDate prazo,
			int posicao, UsuarioResumo responsavel, long totalComentarios) {

		public static TarefaResumo de(Tarefa tarefa, long totalComentarios) {
			return new TarefaResumo(tarefa.getId(), tarefa.getColuna().getId(), tarefa.getTitulo(),
					tarefa.getPrioridade(), tarefa.getPrazo(), tarefa.getPosicao(), pessoa(tarefa.getResponsavel()),
					totalComentarios);
		}

	}

	/** Item de lista para escolher uma tarefa do projeto (ex.: ao apontar horas). */
	public record TarefaOpcao(Long id, String titulo, Long quadroId, String quadroNome, String colunaNome) {

		static TarefaOpcao de(Tarefa tarefa) {
			return new TarefaOpcao(tarefa.getId(), tarefa.getTitulo(), tarefa.getColuna().getQuadro().getId(),
					tarefa.getColuna().getQuadro().getNome(), tarefa.getColuna().getNome());
		}

	}

	public record TarefaResponse(Long id, Long quadroId, Long colunaId, String colunaNome, String titulo,
			String descricao, Prioridade prioridade, LocalDate prazo, int posicao, UsuarioResumo responsavel,
			UsuarioResumo criadoPor, Instant criadoEm, Instant atualizadoEm, boolean podeExcluir) {

		static TarefaResponse de(Tarefa tarefa, boolean podeExcluir) {
			return new TarefaResponse(tarefa.getId(), tarefa.getColuna().getQuadro().getId(), tarefa.getColuna().getId(),
					tarefa.getColuna().getNome(), tarefa.getTitulo(), tarefa.getDescricao(), tarefa.getPrioridade(),
					tarefa.getPrazo(), tarefa.getPosicao(), pessoa(tarefa.getResponsavel()),
					pessoa(tarefa.getCriadoPor()), tarefa.getCriadoEm(), tarefa.getAtualizadoEm(), podeExcluir);
		}

	}

	public record ComentarioResponse(Long id, UsuarioResumo autor, String texto, Instant criadoEm,
			boolean podeExcluir) {

		static ComentarioResponse de(Comentario comentario, boolean podeExcluir) {
			return new ComentarioResponse(comentario.getId(), pessoa(comentario.getAutor()), comentario.getTexto(),
					comentario.getCriadoEm(), podeExcluir);
		}

	}

	private static UsuarioResumo pessoa(Usuario usuario) {
		return usuario == null ? null : UsuarioResumo.de(usuario);
	}

}
