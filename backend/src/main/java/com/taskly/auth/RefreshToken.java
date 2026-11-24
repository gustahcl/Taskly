package com.taskly.auth;

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
@Table(name = "refresh_tokens")
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Column(name = "token_hash", nullable = false, length = 64, unique = true)
	private String tokenHash;

	@Column(name = "expira_em", nullable = false)
	private Instant expiraEm;

	@Column(nullable = false)
	private boolean revogado;

	@CreationTimestamp
	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	protected RefreshToken() {
	}

	public RefreshToken(Usuario usuario, String tokenHash, Instant expiraEm) {
		this.usuario = usuario;
		this.tokenHash = tokenHash;
		this.expiraEm = expiraEm;
	}

	public boolean valido(Instant agora) {
		return !revogado && agora.isBefore(expiraEm);
	}

	public void revogar() {
		this.revogado = true;
	}

	public Long getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public Instant getExpiraEm() {
		return expiraEm;
	}

	public boolean isRevogado() {
		return revogado;
	}

}
