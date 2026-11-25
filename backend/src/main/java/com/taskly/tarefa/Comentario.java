package com.taskly.tarefa;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

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

@Entity
@Table(name = "comentarios")
public class Comentario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tarefa_id", nullable = false, updatable = false)
	private Tarefa tarefa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "autor_id", nullable = false, updatable = false)
	private Usuario autor;

	@Column(nullable = false, length = 2000)
	private String texto;

	@CreationTimestamp
	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	protected Comentario() {
	}

	public Comentario(Tarefa tarefa, Usuario autor, String texto) {
		this.tarefa = tarefa;
		this.autor = autor;
		this.texto = texto;
	}

	public Long getId() {
		return id;
	}

	public Tarefa getTarefa() {
		return tarefa;
	}

	public Usuario getAutor() {
		return autor;
	}

	public String getTexto() {
		return texto;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

}
