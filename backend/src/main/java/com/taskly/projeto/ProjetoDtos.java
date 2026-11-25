package com.taskly.projeto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Objetos de entrada e saída dos endpoints de projeto. */
public final class ProjetoDtos {

	private ProjetoDtos() {
	}

	public record ProjetoRequest(
			@NotBlank @Size(max = 120) String nome,
			@Size(max = 2000) String descricao,
			@Size(max = 120) String cliente,
			LocalDate dataInicio,
			LocalDate dataFim,
			StatusProjeto status) {
	}

	public record MembroRequest(@NotNull Long usuarioId, PapelProjeto papel) {
	}

	public record AlterarPapelRequest(@NotNull PapelProjeto papel) {
	}

	public record MembroResponse(Long usuarioId, String nome, String email, PapelProjeto papel, Instant adicionadoEm) {

		public static MembroResponse de(MembroProjeto membro) {
			return new MembroResponse(membro.getUsuario().getId(), membro.getUsuario().getNome(),
					membro.getUsuario().getEmail(), membro.getPapel(), membro.getAdicionadoEm());
		}

	}

	/** Item da listagem. {@code meuPapel} é nulo quando um ADMIN vê um projeto do qual não participa. */
	public record ProjetoResumo(Long id, String nome, String cliente, StatusProjeto status, LocalDate dataInicio,
			LocalDate dataFim, int totalMembros, PapelProjeto meuPapel) {

		public static ProjetoResumo de(Projeto projeto, Long usuarioId) {
			return new ProjetoResumo(projeto.getId(), projeto.getNome(), projeto.getCliente(), projeto.getStatus(),
					projeto.getDataInicio(), projeto.getDataFim(), projeto.getMembros().size(),
					projeto.papelDe(usuarioId).orElse(null));
		}

	}

	public record ProjetoResponse(Long id, String nome, String descricao, String cliente, StatusProjeto status,
			LocalDate dataInicio, LocalDate dataFim, Instant criadoEm, PapelProjeto meuPapel, boolean podeGerenciar,
			List<MembroResponse> membros) {

		public static ProjetoResponse de(Projeto projeto, Long usuarioId, boolean podeGerenciar) {
			List<MembroResponse> membros = projeto.getMembros().stream()
				.map(MembroResponse::de)
				.sorted(Comparator.comparing(MembroResponse::papel).thenComparing(MembroResponse::nome))
				.toList();
			return new ProjetoResponse(projeto.getId(), projeto.getNome(), projeto.getDescricao(), projeto.getCliente(),
					projeto.getStatus(), projeto.getDataInicio(), projeto.getDataFim(), projeto.getCriadoEm(),
					projeto.papelDe(usuarioId).orElse(null), podeGerenciar, membros);
		}

	}

}
