package com.taskly.projeto;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.taskly.usuario.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "projeto_membros")
public class MembroProjeto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "projeto_id", nullable = false)
	private Projeto projeto;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PapelProjeto papel;

	@CreationTimestamp
	@Column(name = "adicionado_em", nullable = false, updatable = false)
	private Instant adicionadoEm;

	protected MembroProjeto() {
	}

	MembroProjeto(Projeto projeto, Usuario usuario, PapelProjeto papel) {
		this.projeto = projeto;
		this.usuario = usuario;
		this.papel = papel;
	}

	public Long getId() {
		return id;
	}

	public Projeto getProjeto() {
		return projeto;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public PapelProjeto getPapel() {
		return papel;
	}

	void setPapel(PapelProjeto papel) {
		this.papel = papel;
	}

	public Instant getAdicionadoEm() {
		return adicionadoEm;
	}

}
