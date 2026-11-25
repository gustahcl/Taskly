package com.taskly.tarefa;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.taskly.quadro.Coluna;
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
@Table(name = "tarefas")
public class Tarefa {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "coluna_id", nullable = false)
	private Coluna coluna;

	@Column(nullable = false, length = 200)
	private String titulo;

	@Column(length = 5000)
	private String descricao;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "responsavel_id")
	private Usuario responsavel;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Prioridade prioridade = Prioridade.MEDIA;

	private LocalDate prazo;

	@Column(nullable = false)
	private int posicao;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "criado_por", nullable = false, updatable = false)
	private Usuario criadoPor;

	@CreationTimestamp
	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	@UpdateTimestamp
	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	protected Tarefa() {
	}

	public Tarefa(Coluna coluna, int posicao, Usuario criadoPor) {
		this.coluna = coluna;
		this.posicao = posicao;
		this.criadoPor = criadoPor;
	}

	public void atualizar(String titulo, String descricao, Usuario responsavel, Prioridade prioridade,
			LocalDate prazo) {
		this.titulo = titulo;
		this.descricao = descricao;
		this.responsavel = responsavel;
		this.prioridade = prioridade != null ? prioridade : Prioridade.MEDIA;
		this.prazo = prazo;
	}

	void posicionar(Coluna coluna, int posicao) {
		this.coluna = coluna;
		this.posicao = posicao;
	}

	public Long getId() {
		return id;
	}

	public Coluna getColuna() {
		return coluna;
	}

	public String getTitulo() {
		return titulo;
	}

	public String getDescricao() {
		return descricao;
	}

	public Usuario getResponsavel() {
		return responsavel;
	}

	public Prioridade getPrioridade() {
		return prioridade;
	}

	public LocalDate getPrazo() {
		return prazo;
	}

	public int getPosicao() {
		return posicao;
	}

	public Usuario getCriadoPor() {
		return criadoPor;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

	public Instant getAtualizadoEm() {
		return atualizadoEm;
	}

}
