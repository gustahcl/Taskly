package com.taskly.quadro;

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
@Table(name = "colunas")
public class Coluna {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quadro_id", nullable = false, updatable = false)
	private Quadro quadro;

	@Column(nullable = false, length = 60)
	private String nome;

	@Column(nullable = false)
	private int posicao;

	protected Coluna() {
	}

	Coluna(Quadro quadro, String nome, int posicao) {
		this.quadro = quadro;
		this.nome = nome;
		this.posicao = posicao;
	}

	public void renomear(String nome) {
		this.nome = nome;
	}

	void setPosicao(int posicao) {
		this.posicao = posicao;
	}

	public Long getId() {
		return id;
	}

	public Quadro getQuadro() {
		return quadro;
	}

	public String getNome() {
		return nome;
	}

	public int getPosicao() {
		return posicao;
	}

}
