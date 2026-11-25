package com.taskly.projeto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.taskly.common.ConflitoException;
import com.taskly.common.RegraDeNegocioException;
import com.taskly.usuario.Usuario;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "projetos")
public class Projeto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String nome;

	@Column(length = 2000)
	private String descricao;

	@Column(length = 120)
	private String cliente;

	@Column(name = "data_inicio")
	private LocalDate dataInicio;

	@Column(name = "data_fim")
	private LocalDate dataFim;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusProjeto status = StatusProjeto.PLANEJADO;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "criado_por", nullable = false, updatable = false)
	private Usuario criadoPor;

	@OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, orphanRemoval = true)
	@BatchSize(size = 50)
	private List<MembroProjeto> membros = new ArrayList<>();

	@CreationTimestamp
	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	@UpdateTimestamp
	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	protected Projeto() {
	}

	/** Cria o projeto já com o criador como GESTOR. */
	public Projeto(String nome, Usuario criadoPor) {
		this.nome = nome;
		this.criadoPor = criadoPor;
		this.membros.add(new MembroProjeto(this, criadoPor, PapelProjeto.GESTOR));
	}

	public void atualizar(String nome, String descricao, String cliente, LocalDate dataInicio, LocalDate dataFim,
			StatusProjeto status) {
		if (dataInicio != null && dataFim != null && dataFim.isBefore(dataInicio)) {
			throw new RegraDeNegocioException("A data de fim não pode ser anterior à data de início");
		}
		this.nome = nome;
		this.descricao = descricao;
		this.cliente = cliente;
		this.dataInicio = dataInicio;
		this.dataFim = dataFim;
		this.status = status != null ? status : StatusProjeto.PLANEJADO;
	}

	public Optional<MembroProjeto> membro(Long usuarioId) {
		return membros.stream().filter(m -> Objects.equals(m.getUsuario().getId(), usuarioId)).findFirst();
	}

	public Optional<PapelProjeto> papelDe(Long usuarioId) {
		return membro(usuarioId).map(MembroProjeto::getPapel);
	}

	public MembroProjeto adicionarMembro(Usuario usuario, PapelProjeto papel) {
		if (membro(usuario.getId()).isPresent()) {
			throw new ConflitoException("Este usuário já é membro do projeto");
		}
		MembroProjeto membro = new MembroProjeto(this, usuario, papel);
		membros.add(membro);
		return membro;
	}

	public void alterarPapel(MembroProjeto membro, PapelProjeto papel) {
		if (membro.getPapel() == PapelProjeto.GESTOR && papel != PapelProjeto.GESTOR) {
			garantirOutroGestor(membro);
		}
		membro.setPapel(papel);
	}

	public void removerMembro(MembroProjeto membro) {
		if (membro.getPapel() == PapelProjeto.GESTOR) {
			garantirOutroGestor(membro);
		}
		membros.remove(membro);
	}

	private void garantirOutroGestor(MembroProjeto saindo) {
		boolean outro = membros.stream().anyMatch(m -> m != saindo && m.getPapel() == PapelProjeto.GESTOR);
		if (!outro) {
			throw new RegraDeNegocioException("O projeto precisa ter pelo menos um gestor");
		}
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getDescricao() {
		return descricao;
	}

	public String getCliente() {
		return cliente;
	}

	public LocalDate getDataInicio() {
		return dataInicio;
	}

	public LocalDate getDataFim() {
		return dataFim;
	}

	public StatusProjeto getStatus() {
		return status;
	}

	public Usuario getCriadoPor() {
		return criadoPor;
	}

	public List<MembroProjeto> getMembros() {
		return membros;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

	public Instant getAtualizadoEm() {
		return atualizadoEm;
	}

}
