package com.taskly.projeto;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/** Filtros da listagem de projetos. */
final class ProjetoSpecs {

	private static final char ESCAPE = '\\';

	private ProjetoSpecs() {
	}

	/** Projetos em que o usuário é membro (qualquer papel). */
	static Specification<Projeto> comMembro(Long usuarioId) {
		return (root, query, cb) -> {
			Subquery<Long> membro = query.subquery(Long.class);
			Root<MembroProjeto> m = membro.from(MembroProjeto.class);
			membro.select(m.get("id"))
				.where(cb.equal(m.get("projeto"), root), cb.equal(m.get("usuario").get("id"), usuarioId));
			return cb.exists(membro);
		};
	}

	static Specification<Projeto> comStatus(StatusProjeto status) {
		return (root, query, cb) -> cb.equal(root.get("status"), status);
	}

	/** Busca por trecho do nome ou do cliente, sem diferenciar maiúsculas. */
	static Specification<Projeto> contendo(String termo) {
		String padrao = "%" + escapar(termo.trim().toLowerCase(Locale.ROOT)) + "%";
		return (root, query, cb) -> cb.or(cb.like(cb.lower(root.get("nome")), padrao, ESCAPE),
				cb.like(cb.lower(root.get("cliente")), padrao, ESCAPE));
	}

	private static String escapar(String texto) {
		StringBuilder sb = new StringBuilder();
		for (char c : texto.toCharArray()) {
			if (c == ESCAPE || c == '%' || c == '_') {
				sb.append(ESCAPE);
			}
			sb.append(c);
		}
		return sb.toString();
	}

}
