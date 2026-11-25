package com.taskly.tarefa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

	@Query("select c from Comentario c join fetch c.autor where c.tarefa.id = :tarefaId order by c.criadoEm, c.id")
	List<Comentario> daTarefa(Long tarefaId);

	/** Quantidade de comentários por tarefa de um quadro, para mostrar nos cards. */
	@Query("""
			select c.tarefa.id as tarefaId, count(c) as total from Comentario c
			where c.tarefa.coluna.quadro.id = :quadroId
			group by c.tarefa.id""")
	List<TotalPorTarefa> contarPorTarefa(Long quadroId);

	interface TotalPorTarefa {

		Long getTarefaId();

		long getTotal();

	}

}
