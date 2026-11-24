package com.taskly.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskly.auth.AuthDtos.CadastroRequest;
import com.taskly.auth.AuthDtos.LoginRequest;
import com.taskly.auth.AuthDtos.TokenResponse;
import com.taskly.common.ConflitoException;
import com.taskly.config.JwtProperties;
import com.taskly.usuario.Perfil;
import com.taskly.usuario.Usuario;
import com.taskly.usuario.UsuarioRepository;

@Service
public class AuthService {

	private static final String CREDENCIAIS_INVALIDAS = "E-mail ou senha inválidos";

	private final UsuarioRepository usuarioRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;
	private final JwtProperties propriedades;
	private final Clock clock;

	public AuthService(UsuarioRepository usuarioRepository, RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder, TokenService tokenService, JwtProperties propriedades, Clock clock) {
		this.usuarioRepository = usuarioRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.propriedades = propriedades;
		this.clock = clock;
	}

	/** Cadastro público: todo usuário novo entra como MEMBRO; só um ADMIN promove perfis. */
	@Transactional
	public Usuario cadastrar(CadastroRequest request) {
		String email = normalizar(request.email());
		if (usuarioRepository.existsByEmailIgnoreCase(email)) {
			throw new ConflitoException("Já existe um usuário com este e-mail");
		}
		Usuario usuario = new Usuario(request.nome().trim(), email, passwordEncoder.encode(request.senha()),
				Perfil.MEMBRO);
		return usuarioRepository.save(usuario);
	}

	@Transactional
	public TokenResponse login(LoginRequest request) {
		Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizar(request.email()))
			.filter(Usuario::isAtivo)
			.filter(u -> passwordEncoder.matches(request.senha(), u.getSenhaHash()))
			.orElseThrow(() -> new CredenciaisInvalidasException(CREDENCIAIS_INVALIDAS));
		return emitirTokens(usuario);
	}

	/**
	 * Troca um refresh token válido por um novo par de tokens. O token usado é revogado (rotação).
	 * Se um token já revogado for reapresentado, é sinal de vazamento: todos os tokens do usuário são revogados.
	 */
	@Transactional(noRollbackFor = CredenciaisInvalidasException.class)
	public TokenResponse renovar(String refreshToken) {
		RefreshToken atual = refreshTokenRepository.findByTokenHash(tokenService.hash(refreshToken))
			.orElseThrow(() -> new CredenciaisInvalidasException("Refresh token inválido"));

		if (atual.isRevogado()) {
			refreshTokenRepository.revogarTodosDoUsuario(atual.getUsuario().getId());
			throw new CredenciaisInvalidasException("Refresh token inválido");
		}
		if (!atual.valido(clock.instant()) || !atual.getUsuario().isAtivo()) {
			throw new CredenciaisInvalidasException("Refresh token expirado");
		}

		atual.revogar();
		return emitirTokens(atual.getUsuario());
	}

	@Transactional
	public void logout(String refreshToken) {
		refreshTokenRepository.findByTokenHash(tokenService.hash(refreshToken)).ifPresent(RefreshToken::revogar);
	}

	private TokenResponse emitirTokens(Usuario usuario) {
		Instant agora = clock.instant();
		String accessToken = tokenService.gerarAccessToken(usuario);
		String refreshToken = tokenService.gerarRefreshToken();
		refreshTokenRepository.save(new RefreshToken(usuario, tokenService.hash(refreshToken),
				agora.plus(propriedades.refreshTokenValidade())));
		return new TokenResponse(accessToken, refreshToken, agora.plus(propriedades.accessTokenValidade()));
	}

	private static String normalizar(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
