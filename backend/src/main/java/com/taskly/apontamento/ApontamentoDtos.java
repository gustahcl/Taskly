package com.taskly.apontamento;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.taskly.projeto.Projeto;
import com.taskly.tarefa.Tarefa;
import com.taskly.usuario.UsuarioResumo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Objetos de entrada e saída dos endpoints de horas. */
public final class ApontamentoDtos {

	private ApontamentoDtos() {
	}

	/** Duração em minutos (ex.: 90 = 1h30). */
	public record ApontamentoRequest(
			@NotNull LocalDate data,
			@NotNull @Positive @Max(1440) Integer minutos,
			@Size(max = 500) String descricao) {
	}

	/** Onde as horas foram apontadas: tarefa, quadro e projeto. */
	public record TarefaApontada(Long id, String titulo, Long quadroId, Long projetoId, String projetoNome) {

		static TarefaApontada de(Tarefa tarefa) {
			Projeto projeto = tarefa.getColuna().getQuadro().getProjeto();
			return new TarefaApontada(tarefa.getId(), tarefa.getTitulo(), tarefa.getColuna().getQuadro().getId(),
					projeto.getId(), projeto.getNome());
		}

	}

	public record ApontamentoResponse(Long id, LocalDate data, int minutos, String descricao, UsuarioResumo usuario,
			TarefaApontada tarefa, boolean podeEditar, Instant criadoEm) {

		static ApontamentoResponse de(Apontamento apontamento, boolean podeEditar) {
			return new ApontamentoResponse(apontamento.getId(), apontamento.getData(), apontamento.getMinutos(),
					apontamento.getDescricao(), UsuarioResumo.de(apontamento.getUsuario()),
					TarefaApontada.de(apontamento.getTarefa()), podeEditar, apontamento.getCriadoEm());
		}

	}

	public record HorasDaTarefa(long totalMinutos, List<ApontamentoResponse> apontamentos) {
	}

	/** Apontamentos do usuário logado no período, com o total de cada dia. */
	public record MinhasHoras(LocalDate inicio, LocalDate fim, int limiteDiarioMinutos, long totalMinutos,
			List<TotalDoDia> dias, List<ApontamentoResponse> apontamentos) {
	}

	public record TotalDoDia(LocalDate data, long minutos) {
	}

	/** Visão do gerente: horas por pessoa, por projeto e o cruzamento dos dois. */
	public record Consolidado(LocalDate inicio, LocalDate fim, long totalMinutos, List<TotalPorPessoa> porPessoa,
			List<TotalPorProjeto> porProjeto, List<Linha> linhas) {
	}

	public record TotalPorPessoa(Long usuarioId, String nome, long minutos) {
	}

	public record TotalPorProjeto(Long projetoId, String nome, String cliente, long minutos) {
	}

	public record Linha(Long projetoId, String projetoNome, Long usuarioId, String usuarioNome, long minutos) {
	}

}
