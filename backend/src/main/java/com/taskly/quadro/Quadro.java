package com.taskly.quadro;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.taskly.common.RegraDeNegocioException;
import com.taskly.projeto.Projeto;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/** Quadro kanban de um projeto. A lista de colunas é mantida sempre ordenada e com posições contínuas. */
@Entity
@Table(name = "quadros")
public class Quadro {

	/** Colunas criadas junto com um quadro novo. */
	public static final List<String> COLUNAS_PADRAO = List.of("A fazer", "Em andamento", "Concluído");

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "projeto_id", nullable = false, updatable = false)
	private Projeto projeto;

	@Column(nullable = false, length = 120)
	private String nome;

	@OneToMany(mappedBy = "quadro", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("posicao")
	private List<Coluna> colunas = new ArrayList<>();

	@CreationTimestamp
	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	@UpdateTimestamp
	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	protected Quadro() {
	}

	public Quadro(Projeto projeto, String nome) {
		this.projeto = projeto;
		this.nome = nome;
		COLUNAS_PADRAO.forEach(this::adicionarColuna);
	}

	public void renomear(String nome) {
		this.nome = nome;
	}

	public Coluna adicionarColuna(String nome) {
		Coluna coluna = new Coluna(this, nome, colunas.size());
		colunas.add(coluna);
		return coluna;
	}

	public void moverColuna(Coluna coluna, int posicao) {
		colunas.remove(coluna);
		colunas.add(Math.max(0, Math.min(posicao, colunas.size())), coluna);
		renumerar();
	}

	/** Remove a coluna. Quem chama garante que ela está vazia. */
	public void removerColuna(Coluna coluna) {
		if (colunas.size() == 1) {
			throw new RegraDeNegocioException("O quadro precisa ter pelo menos uma coluna");
		}
		colunas.remove(coluna);
		renumerar();
	}

	public Optional<Coluna> coluna(Long colunaId) {
		return colunas.stream().filter(c -> Objects.equals(c.getId(), colunaId)).findFirst();
	}

	private void renumerar() {
		for (int i = 0; i < colunas.size(); i++) {
			colunas.get(i).setPosicao(i);
		}
	}

	public Long getId() {
		return id;
	}

	public Projeto getProjeto() {
		return projeto;
	}

	public String getNome() {
		return nome;
	}

	public List<Coluna> getColunas() {
		return colunas;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

	public Instant getAtualizadoEm() {
		return atualizadoEm;
	}

}
