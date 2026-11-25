package com.taskly.quadro;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface QuadroRepository extends JpaRepository<Quadro, Long> {

	List<Quadro> findByProjetoIdOrderByIdAsc(Long projetoId);

	/**
	 * Trava o quadro até o fim da transação. Usado ao reordenar tarefas para que dois movimentos
	 * simultâneos no mesmo quadro não embaralhem as posições.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select q from Quadro q where q.id = :id")
	Optional<Quadro> travar(Long id);

}
