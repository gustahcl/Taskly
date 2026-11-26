package com.taskly.apontamento;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.taskly.tarefa.Tarefa;
import com.taskly.usuario.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Horas que um usuário trabalhou em uma tarefa num determinado dia. */
@Entity
@Table(name = "apontamentos")
public class Apontamento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tarefa_id", nullable = false, updatable = false)
	private Tarefa tarefa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false, updatable = false)
	private Usuario usuario;

	@Column(nullable = false)
	private LocalDate data;

	@Column(nullable = false)
	private int minutos;

	@Column(length = 500)
	private String descricao;

	@CreationTimestamp
	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	@UpdateTimestamp
	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	protected Apontamento() {
	}

	public Apontamento(Tarefa tarefa, Usuario usuario) {
		this.tarefa = tarefa;
		this.usuario = usuario;
	}

	public void atualizar(LocalDate data, int minutos, String descricao) {
		this.data = data;
		this.minutos = minutos;
		this.descricao = descricao;
	}

	public Long getId() {
		return id;
	}

	public Tarefa getTarefa() {
		return tarefa;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public LocalDate getData() {
		return data;
	}

	public int getMinutos() {
		return minutos;
	}

	public String getDescricao() {
		return descricao;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

	public Instant getAtualizadoEm() {
		return atualizadoEm;
	}

}
