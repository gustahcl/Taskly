package com.taskly.quadro;

import java.time.Instant;
import java.util.List;

import com.taskly.projeto.ProjetoDtos.MembroResponse;
import com.taskly.tarefa.TarefaDtos.TarefaResumo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Objetos de entrada e saída dos endpoints de quadro e coluna. */
public final class QuadroDtos {

	private QuadroDtos() {
	}

	public record QuadroRequest(@NotBlank @Size(max = 120) String nome) {
	}

	public record ColunaRequest(@NotBlank @Size(max = 60) String nome) {
	}

	public record MoverColunaRequest(@NotNull @PositiveOrZero Integer posicao) {
	}

	public record QuadroResumo(Long id, String nome, Instant criadoEm) {

		static QuadroResumo de(Quadro quadro) {
			return new QuadroResumo(quadro.getId(), quadro.getNome(), quadro.getCriadoEm());
		}

	}

	public record ColunaResponse(Long id, String nome, int posicao, List<TarefaResumo> tarefas) {
	}

	/**
	 * O quadro completo, pronto para desenhar o kanban: colunas na ordem, tarefas de cada coluna na ordem
	 * e os membros do projeto (para escolher o responsável e filtrar).
	 */
	public record QuadroResponse(Long id, String nome, Long projetoId, String projetoNome, boolean podeGerenciar,
			List<ColunaResponse> colunas, List<MembroResponse> membros) {
	}

}
