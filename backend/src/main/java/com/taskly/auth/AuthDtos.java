package com.taskly.auth;

import java.time.Instant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Objetos de entrada e saída dos endpoints de autenticação. */
public final class AuthDtos {

	private AuthDtos() {
	}

	public record CadastroRequest(
			@NotBlank @Size(max = 120) String nome,
			@NotBlank @Email @Size(max = 160) String email,
			@NotBlank @Size(min = 8, max = 72, message = "a senha deve ter entre 8 e 72 caracteres") String senha) {
	}

	public record LoginRequest(@NotBlank @Email String email, @NotBlank String senha) {
	}

	public record RefreshRequest(@NotBlank String refreshToken) {
	}

	public record TokenResponse(String accessToken, String refreshToken, String tipo, Instant accessTokenExpiraEm) {

		public TokenResponse(String accessToken, String refreshToken, Instant accessTokenExpiraEm) {
			this(accessToken, refreshToken, "Bearer", accessTokenExpiraEm);
		}

	}

}
