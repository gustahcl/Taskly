package com.taskly.apontamento;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ApontamentoRepository extends JpaRepository<Apontamento, Long> {

	@Query("""
			select a from Apontamento a join fetch a.usuario
			where a.tarefa.id = :tarefaId
			order by a.data desc, a.id desc""")
	List<Apontamento> daTarefa(Long tarefaId);

	/** Apontamentos do usuário no período, já com tarefa, quadro e projeto (para a visão semanal). */
	@Query("""
			select a from Apontamento a
			join fetch a.usuario join fetch a.tarefa t join fetch t.coluna c join fetch c.quadro q join fetch q.projeto
			where a.usuario.id = :usuarioId and a.data between :inicio and :fim
			order by a.data, a.id""")
	List<Apontamento> doUsuarioNoPeriodo(Long usuarioId, LocalDate inicio, LocalDate fim);

	@Query("""
			select a from Apontamento a
			join fetch a.usuario join fetch a.tarefa t join fetch t.coluna c join fetch c.quadro q join fetch q.projeto
			where a.id = :id""")
	Optional<Apontamento> buscarCompleto(Long id);

	/** Total de minutos que o usuário já apontou no dia, desconsiderando o apontamento informado (0 = nenhum). */
	@Query("""
			select coalesce(sum(a.minutos), 0) from Apontamento a
			where a.usuario.id = :usuarioId and a.data = :data and a.id <> :ignorarId""")
	long minutosNoDia(Long usuarioId, LocalDate data, Long ignorarId);

	/** Horas por projeto e pessoa no período, em todos os projetos (visão do ADMIN). */
	@Query("""
			select p.id as projetoId, p.nome as projetoNome, p.cliente as cliente,
			       u.id as usuarioId, u.nome as usuarioNome, sum(a.minutos) as minutos
			from Apontamento a join a.usuario u join a.tarefa t join t.coluna c join c.quadro q join q.projeto p
			where a.data between :inicio and :fim
			group by p.id, p.nome, p.cliente, u.id, u.nome
			order by p.nome, u.nome""")
	List<HorasPorProjetoEPessoa> consolidado(LocalDate inicio, LocalDate fim);

	/** Horas por projeto e pessoa no período, só nos projetos informados. */
	@Query("""
			select p.id as projetoId, p.nome as projetoNome, p.cliente as cliente,
			       u.id as usuarioId, u.nome as usuarioNome, sum(a.minutos) as minutos
			from Apontamento a join a.usuario u join a.tarefa t join t.coluna c join c.quadro q join q.projeto p
			where a.data between :inicio and :fim and p.id in :projetos
			group by p.id, p.nome, p.cliente, u.id, u.nome
			order by p.nome, u.nome""")
	List<HorasPorProjetoEPessoa> consolidado(LocalDate inicio, LocalDate fim, Collection<Long> projetos);

	interface HorasPorProjetoEPessoa {

		Long getProjetoId();

		String getProjetoNome();

		String getCliente();

		Long getUsuarioId();

		String getUsuarioNome();

		long getMinutos();

	}

}
