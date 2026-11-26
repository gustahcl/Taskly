package com.taskly.tarefa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TarefaRepository extends JpaRepository<Tarefa, Long> {

	List<Tarefa> findByColunaIdOrderByPosicaoAsc(Long colunaId);

	long countByColunaId(Long colunaId);

	/** Todas as tarefas de um quadro, com o responsável, na ordem das colunas. */
	@Query("""
			select t from Tarefa t left join fetch t.responsavel
			where t.coluna.quadro.id = :quadroId
			order by t.posicao""")
	List<Tarefa> doQuadro(Long quadroId);

	/** A tarefa já com coluna, quadro e projeto carregados (para checar o acesso). */
	@Query("""
			select t from Tarefa t
			join fetch t.coluna c join fetch c.quadro q join fetch q.projeto
			left join fetch t.responsavel join fetch t.criadoPor
			where t.id = :id""")
	Optional<Tarefa> buscarCompleta(Long id);

	/** Tarefas de todos os quadros do projeto, na ordem de quadro, coluna e posição. */
	@Query("""
			select t from Tarefa t join fetch t.coluna c join fetch c.quadro q
			where q.projeto.id = :projetoId
			order by q.id, c.posicao, t.posicao""")
	List<Tarefa> doProjeto(Long projetoId);

}
