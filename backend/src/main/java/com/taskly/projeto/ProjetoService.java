package com.taskly.projeto;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.taskly.common.AcessoNegadoException;
import com.taskly.common.RecursoNaoEncontradoException;
import com.taskly.common.RegraDeNegocioException;
import com.taskly.common.UsuarioLogado;
import com.taskly.projeto.ProjetoDtos.ProjetoRequest;
import com.taskly.usuario.Perfil;
import com.taskly.usuario.Usuario;
import com.taskly.usuario.UsuarioRepository;

/**
 * Regras de acesso a projetos:
 * <ul>
 *   <li>só ADMIN e GERENTE criam projetos, e quem cria vira GESTOR;</li>
 *   <li>ADMIN vê e administra todos; os demais só veem os projetos de que são membros;</li>
 *   <li>editar, excluir e administrar a equipe exige ser ADMIN ou GESTOR do projeto.</li>
 * </ul>
 */
@Service
public class ProjetoService {

	private final ProjetoRepository projetoRepository;
	private final UsuarioRepository usuarioRepository;

	public ProjetoService(ProjetoRepository projetoRepository, UsuarioRepository usuarioRepository) {
		this.projetoRepository = projetoRepository;
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional(readOnly = true)
	public Page<Projeto> listar(UsuarioLogado usuario, StatusProjeto status, String busca, Pageable pageable) {
		List<Specification<Projeto>> filtros = new ArrayList<>();
		if (!usuario.admin()) {
			filtros.add(ProjetoSpecs.comMembro(usuario.id()));
		}
		if (status != null) {
			filtros.add(ProjetoSpecs.comStatus(status));
		}
		if (StringUtils.hasText(busca)) {
			filtros.add(ProjetoSpecs.contendo(busca));
		}
		Page<Projeto> pagina = projetoRepository.findAll(Specification.allOf(filtros), pageable);
		pagina.forEach(p -> p.getMembros().size());
		return pagina;
	}

	@Transactional
	public Projeto criar(UsuarioLogado usuario, ProjetoRequest request) {
		if (usuario.perfil() == Perfil.MEMBRO) {
			throw new AcessoNegadoException("Só administradores e gerentes criam projetos");
		}
		Usuario criador = usuarioRepository.findById(usuario.id())
			.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
		Projeto projeto = new Projeto(request.nome().trim(), criador);
		aplicar(projeto, request);
		return projetoRepository.save(projeto);
	}

	/** Projeto que o usuário pode ver. Para quem não é membro, o projeto simplesmente "não existe". */
	@Transactional(readOnly = true)
	public Projeto buscarVisivel(Long id, UsuarioLogado usuario) {
		Projeto projeto = projetoRepository.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Projeto não encontrado"));
		if (!usuario.admin() && projeto.membro(usuario.id()).isEmpty()) {
			throw new RecursoNaoEncontradoException("Projeto não encontrado");
		}
		projeto.getMembros().forEach(m -> m.getUsuario().getNome());
		return projeto;
	}

	/** Projeto que o usuário pode administrar (ADMIN ou GESTOR). */
	@Transactional(readOnly = true)
	public Projeto buscarGerenciavel(Long id, UsuarioLogado usuario) {
		Projeto projeto = buscarVisivel(id, usuario);
		if (!podeGerenciar(projeto, usuario)) {
			throw new AcessoNegadoException("Só o gestor do projeto pode fazer isso");
		}
		return projeto;
	}

	public boolean podeGerenciar(Projeto projeto, UsuarioLogado usuario) {
		return usuario.admin() || projeto.papelDe(usuario.id()).filter(p -> p == PapelProjeto.GESTOR).isPresent();
	}

	@Transactional
	public Projeto atualizar(Long id, UsuarioLogado usuario, ProjetoRequest request) {
		Projeto projeto = buscarGerenciavel(id, usuario);
		aplicar(projeto, request);
		return projeto;
	}

	@Transactional
	public void excluir(Long id, UsuarioLogado usuario) {
		projetoRepository.delete(buscarGerenciavel(id, usuario));
	}

	@Transactional
	public MembroProjeto adicionarMembro(Long projetoId, UsuarioLogado usuario, Long novoMembroId, PapelProjeto papel) {
		Projeto projeto = buscarGerenciavel(projetoId, usuario);
		Usuario novo = usuarioRepository.findById(novoMembroId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
		if (!novo.isAtivo()) {
			throw new RegraDeNegocioException("Usuário inativo não pode entrar em projetos");
		}
		return projeto.adicionarMembro(novo, papel != null ? papel : PapelProjeto.COLABORADOR);
	}

	@Transactional
	public MembroProjeto alterarPapel(Long projetoId, UsuarioLogado usuario, Long membroId, PapelProjeto papel) {
		Projeto projeto = buscarGerenciavel(projetoId, usuario);
		MembroProjeto membro = membroDoProjeto(projeto, membroId);
		projeto.alterarPapel(membro, papel);
		return membro;
	}

	@Transactional
	public void removerMembro(Long projetoId, UsuarioLogado usuario, Long membroId) {
		Projeto projeto = buscarGerenciavel(projetoId, usuario);
		projeto.removerMembro(membroDoProjeto(projeto, membroId));
	}

	private static MembroProjeto membroDoProjeto(Projeto projeto, Long usuarioId) {
		return projeto.membro(usuarioId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Este usuário não é membro do projeto"));
	}

	private static void aplicar(Projeto projeto, ProjetoRequest request) {
		projeto.atualizar(request.nome().trim(), vazioParaNulo(request.descricao()), vazioParaNulo(request.cliente()),
				request.dataInicio(), request.dataFim(), request.status());
	}

	private static String vazioParaNulo(String texto) {
		return StringUtils.hasText(texto) ? texto.trim() : null;
	}

}
