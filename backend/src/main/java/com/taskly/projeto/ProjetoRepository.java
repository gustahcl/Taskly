package com.taskly.projeto;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ProjetoRepository extends JpaRepository<Projeto, Long>, JpaSpecificationExecutor<Projeto> {

	/** Ids dos projetos em que o usuário é gestor. */
	@Query("select m.projeto.id from MembroProjeto m where m.usuario.id = :usuarioId and m.papel = com.taskly.projeto.PapelProjeto.GESTOR")
	List<Long> geridosPor(Long usuarioId);

}
