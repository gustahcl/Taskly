package com.taskly.auth;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taskly.auth.AuthDtos.CadastroRequest;
import com.taskly.auth.AuthDtos.LoginRequest;
import com.taskly.auth.AuthDtos.RefreshRequest;
import com.taskly.auth.AuthDtos.TokenResponse;
import com.taskly.usuario.UsuarioResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/cadastro")
	public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody CadastroRequest request) {
		UsuarioResponse usuario = UsuarioResponse.de(authService.cadastrar(request));
		return ResponseEntity.created(URI.create("/api/usuarios/" + usuario.id())).body(usuario);
	}

	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/refresh")
	public TokenResponse renovar(@Valid @RequestBody RefreshRequest request) {
		return authService.renovar(request.refreshToken());
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
		authService.logout(request.refreshToken());
		return ResponseEntity.noContent().build();
	}

}
