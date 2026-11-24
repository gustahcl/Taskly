package com.taskly.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.taskly.auth.AuthDtos.CadastroRequest;
import com.taskly.auth.AuthDtos.LoginRequest;
import com.taskly.auth.AuthDtos.TokenResponse;
import com.taskly.TestData;
import com.taskly.common.ConflitoException;
import com.taskly.config.JwtProperties;
import com.taskly.usuario.Perfil;
import com.taskly.usuario.Usuario;
import com.taskly.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final Instant AGORA = Instant.parse("2026-11-24T12:00:00Z");

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private TokenService tokenService;

	private AuthService service;

	@BeforeEach
	void setUp() {
		JwtProperties propriedades = new JwtProperties("segredo-de-teste-com-mais-de-32-bytes!!",
				Duration.ofMinutes(15), Duration.ofDays(7));
		service = new AuthService(usuarioRepository, refreshTokenRepository, passwordEncoder, tokenService,
				propriedades, Clock.fixed(AGORA, ZoneOffset.UTC));
	}

	@Test
	void cadastroCriaMembroComSenhaCriptografadaEEmailNormalizado() {
		when(usuarioRepository.existsByEmailIgnoreCase("ana@taskly.dev")).thenReturn(false);
		when(passwordEncoder.encode("senha-forte")).thenReturn("hash");
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

		Usuario criado = service.cadastrar(new CadastroRequest(" Ana ", " Ana@Taskly.dev ", "senha-forte"));

		assertThat(criado.getNome()).isEqualTo("Ana");
		assertThat(criado.getEmail()).isEqualTo("ana@taskly.dev");
		assertThat(criado.getSenhaHash()).isEqualTo("hash");
		assertThat(criado.getPerfil()).isEqualTo(Perfil.MEMBRO);
	}

	@Test
	void cadastroComEmailRepetidoFalha() {
		when(usuarioRepository.existsByEmailIgnoreCase("ana@taskly.dev")).thenReturn(true);

		assertThatThrownBy(() -> service.cadastrar(new CadastroRequest("Ana", "ana@taskly.dev", "senha-forte")))
			.isInstanceOf(ConflitoException.class);
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void loginValidoEmiteTokensESalvaHashDoRefresh() {
		Usuario ana = TestData.usuario(1L, Perfil.MEMBRO);
		when(usuarioRepository.findByEmailIgnoreCase("ana@taskly.dev")).thenReturn(Optional.of(ana));
		when(passwordEncoder.matches("senha-forte", "hash")).thenReturn(true);
		when(tokenService.gerarAccessToken(ana)).thenReturn("jwt");
		when(tokenService.gerarRefreshToken()).thenReturn("refresh");
		when(tokenService.hash("refresh")).thenReturn("hash-do-refresh");

		TokenResponse resposta = service.login(new LoginRequest("ana@taskly.dev", "senha-forte"));

		assertThat(resposta.accessToken()).isEqualTo("jwt");
		assertThat(resposta.refreshToken()).isEqualTo("refresh");
		assertThat(resposta.tipo()).isEqualTo("Bearer");
		assertThat(resposta.accessTokenExpiraEm()).isEqualTo(AGORA.plus(Duration.ofMinutes(15)));

		ArgumentCaptor<RefreshToken> salvo = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(salvo.capture());
		assertThat(salvo.getValue().getTokenHash()).isEqualTo("hash-do-refresh");
		assertThat(salvo.getValue().getExpiraEm()).isEqualTo(AGORA.plus(Duration.ofDays(7)));
	}

	@Test
	void loginComSenhaErradaFalha() {
		Usuario ana = TestData.usuario(1L, Perfil.MEMBRO);
		when(usuarioRepository.findByEmailIgnoreCase("ana@taskly.dev")).thenReturn(Optional.of(ana));
		when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

		assertThatThrownBy(() -> service.login(new LoginRequest("ana@taskly.dev", "errada")))
			.isInstanceOf(CredenciaisInvalidasException.class);
	}

	@Test
	void loginDeUsuarioInativoFalha() {
		Usuario ana = TestData.usuario(1L, Perfil.MEMBRO);
		ana.setAtivo(false);
		when(usuarioRepository.findByEmailIgnoreCase("ana@taskly.dev")).thenReturn(Optional.of(ana));

		assertThatThrownBy(() -> service.login(new LoginRequest("ana@taskly.dev", "senha-forte")))
			.isInstanceOf(CredenciaisInvalidasException.class);
	}

	@Test
	void refreshValidoRevogaOAntigoEEmiteNovoPar() {
		Usuario ana = TestData.usuario(1L, Perfil.MEMBRO);
		RefreshToken atual = new RefreshToken(ana, "hash-antigo", AGORA.plus(Duration.ofDays(1)));
		when(tokenService.hash("antigo")).thenReturn("hash-antigo");
		when(refreshTokenRepository.findByTokenHash("hash-antigo")).thenReturn(Optional.of(atual));
		when(tokenService.gerarAccessToken(ana)).thenReturn("jwt-novo");
		when(tokenService.gerarRefreshToken()).thenReturn("novo");
		when(tokenService.hash("novo")).thenReturn("hash-novo");

		TokenResponse resposta = service.renovar("antigo");

		assertThat(atual.isRevogado()).isTrue();
		assertThat(resposta.refreshToken()).isEqualTo("novo");
	}

	@Test
	void refreshExpiradoFalha() {
		Usuario ana = TestData.usuario(1L, Perfil.MEMBRO);
		RefreshToken expirado = new RefreshToken(ana, "h", AGORA.minusSeconds(1));
		when(tokenService.hash("x")).thenReturn("h");
		when(refreshTokenRepository.findByTokenHash("h")).thenReturn(Optional.of(expirado));

		assertThatThrownBy(() -> service.renovar("x")).isInstanceOf(CredenciaisInvalidasException.class);
	}

	@Test
	void reusoDeRefreshRevogadoDerrubaTodasAsSessoesDoUsuario() {
		Usuario ana = TestData.usuario(7L, Perfil.MEMBRO);
		RefreshToken revogado = new RefreshToken(ana, "h", AGORA.plus(Duration.ofDays(1)));
		revogado.revogar();
		when(tokenService.hash("x")).thenReturn("h");
		when(refreshTokenRepository.findByTokenHash("h")).thenReturn(Optional.of(revogado));

		assertThatThrownBy(() -> service.renovar("x")).isInstanceOf(CredenciaisInvalidasException.class);
		verify(refreshTokenRepository).revogarTodosDoUsuario(7L);
	}

	@Test
	void refreshDesconhecidoFalha() {
		when(tokenService.hash("x")).thenReturn("h");
		when(refreshTokenRepository.findByTokenHash("h")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.renovar("x")).isInstanceOf(CredenciaisInvalidasException.class);
	}

}
