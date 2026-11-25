package com.taskly.projeto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.taskly.TestData;
import com.taskly.common.AcessoNegadoException;
import com.taskly.common.ConflitoException;
import com.taskly.common.RecursoNaoEncontradoException;
import com.taskly.common.RegraDeNegocioException;
import com.taskly.projeto.ProjetoDtos.ProjetoRequest;
import com.taskly.usuario.Perfil;
import com.taskly.usuario.Usuario;
import com.taskly.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ProjetoServiceTest {

	@Mock
	private ProjetoRepository projetoRepository;

	@Mock
	private UsuarioRepository usuarioRepository;

	private ProjetoService service;

	private final Usuario gerente = TestData.usuario(1L, "Gabi", Perfil.GERENTE);
	private final Usuario membro = TestData.usuario(2L, "Mauro", Perfil.MEMBRO);
	private final Usuario admin = TestData.usuario(3L, "Alice", Perfil.ADMIN);
	private final Usuario estranho = TestData.usuario(4L, "Eva", Perfil.GERENTE);

	@BeforeEach
	void setUp() {
		service = new ProjetoService(projetoRepository, usuarioRepository);
	}

	private Projeto projetoDoGerente() {
		Projeto projeto = TestData.projeto(10L, gerente);
		when(projetoRepository.findById(10L)).thenReturn(Optional.of(projeto));
		return projeto;
	}

	@Test
	void gerenteCriaProjetoEViraGestor() {
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(gerente));
		when(projetoRepository.save(any(Projeto.class))).thenAnswer(inv -> inv.getArgument(0));

		Projeto criado = service.criar(TestData.logado(gerente), new ProjetoRequest(" Portal ", " ", "ACME",
				LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 31), null));

		assertThat(criado.getNome()).isEqualTo("Portal");
		assertThat(criado.getDescricao()).isNull();
		assertThat(criado.getCliente()).isEqualTo("ACME");
		assertThat(criado.getStatus()).isEqualTo(StatusProjeto.PLANEJADO);
		assertThat(criado.papelDe(1L)).contains(PapelProjeto.GESTOR);
	}

	@Test
	void membroNaoCriaProjeto() {
		assertThatThrownBy(() -> service.criar(TestData.logado(membro),
				new ProjetoRequest("X", null, null, null, null, null)))
			.isInstanceOf(AcessoNegadoException.class);
		verify(projetoRepository, never()).save(any());
	}

	@Test
	void dataFimAntesDoInicioEhRejeitada() {
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(gerente));

		assertThatThrownBy(() -> service.criar(TestData.logado(gerente), new ProjetoRequest("X", null, null,
				LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 1), null)))
			.isInstanceOf(RegraDeNegocioException.class);
	}

	@Test
	void naoMembroNaoEnxergaOProjeto() {
		projetoDoGerente();

		assertThatThrownBy(() -> service.buscarVisivel(10L, TestData.logado(estranho)))
			.isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void adminEnxergaEGerenciaQualquerProjeto() {
		Projeto projeto = projetoDoGerente();

		assertThat(service.buscarGerenciavel(10L, TestData.logado(admin))).isSameAs(projeto);
	}

	@Test
	void colaboradorVeMasNaoGerencia() {
		Projeto projeto = projetoDoGerente();
		projeto.adicionarMembro(membro, PapelProjeto.COLABORADOR);

		assertThat(service.buscarVisivel(10L, TestData.logado(membro))).isSameAs(projeto);
		assertThatThrownBy(() -> service.buscarGerenciavel(10L, TestData.logado(membro)))
			.isInstanceOf(AcessoNegadoException.class);
	}

	@Test
	void membroPromovidoAGestorGerencia() {
		Projeto projeto = TestData.projeto(10L, gerente);
		projeto.adicionarMembro(membro, PapelProjeto.GESTOR);

		assertThat(service.podeGerenciar(projeto, TestData.logado(membro))).isTrue();
	}

	@Test
	void gestorAdicionaMembroComoColaboradorPorPadrao() {
		Projeto projeto = projetoDoGerente();
		when(usuarioRepository.findById(2L)).thenReturn(Optional.of(membro));

		MembroProjeto novo = service.adicionarMembro(10L, TestData.logado(gerente), 2L, null);

		assertThat(novo.getPapel()).isEqualTo(PapelProjeto.COLABORADOR);
		assertThat(projeto.getMembros()).hasSize(2);
	}

	@Test
	void naoAdicionaOMesmoMembroDuasVezes() {
		projetoDoGerente();
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(gerente));

		assertThatThrownBy(() -> service.adicionarMembro(10L, TestData.logado(gerente), 1L, PapelProjeto.GESTOR))
			.isInstanceOf(ConflitoException.class);
	}

	@Test
	void naoAdicionaUsuarioInativo() {
		projetoDoGerente();
		membro.setAtivo(false);
		when(usuarioRepository.findById(2L)).thenReturn(Optional.of(membro));

		assertThatThrownBy(() -> service.adicionarMembro(10L, TestData.logado(gerente), 2L, null))
			.isInstanceOf(RegraDeNegocioException.class);
	}

	@Test
	void naoRemoveOUnicoGestor() {
		projetoDoGerente();

		assertThatThrownBy(() -> service.removerMembro(10L, TestData.logado(gerente), 1L))
			.isInstanceOf(RegraDeNegocioException.class)
			.hasMessageContaining("pelo menos um gestor");
		assertThatThrownBy(() -> service.alterarPapel(10L, TestData.logado(gerente), 1L, PapelProjeto.COLABORADOR))
			.isInstanceOf(RegraDeNegocioException.class);
	}

	@Test
	void comOutroGestorOPrimeiroPodeSair() {
		Projeto projeto = projetoDoGerente();
		projeto.adicionarMembro(membro, PapelProjeto.GESTOR);

		service.removerMembro(10L, TestData.logado(gerente), 1L);

		assertThat(projeto.membro(1L)).isEmpty();
		assertThat(projeto.papelDe(2L)).contains(PapelProjeto.GESTOR);
	}

	@Test
	void removerQuemNaoEhMembroRetornaNaoEncontrado() {
		projetoDoGerente();

		assertThatThrownBy(() -> service.removerMembro(10L, TestData.logado(gerente), 99L))
			.isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void colaboradorNaoExcluiProjeto() {
		Projeto projeto = projetoDoGerente();
		projeto.adicionarMembro(membro, PapelProjeto.COLABORADOR);

		assertThatThrownBy(() -> service.excluir(10L, TestData.logado(membro)))
			.isInstanceOf(AcessoNegadoException.class);
		verify(projetoRepository, never()).delete(any(Projeto.class));
	}

}
