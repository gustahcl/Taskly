package com.taskly.tarefa;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.taskly.common.AcessoNegadoException;
import com.taskly.common.RecursoNaoEncontradoException;
import com.taskly.common.RegraDeNegocioException;
import com.taskly.common.UsuarioLogado;
import com.taskly.projeto.MembroProjeto;
import com.taskly.projeto.Projeto;
import com.taskly.projeto.ProjetoService;
import com.taskly.quadro.Coluna;
import com.taskly.quadro.Quadro;
import com.taskly.quadro.QuadroRepository;
import com.taskly.quadro.QuadroService;
import com.taskly.tarefa.TarefaDtos.ComentarioResponse;
import com.taskly.tarefa.TarefaDtos.TarefaOpcao;
import com.taskly.tarefa.TarefaDtos.TarefaRequest;
import com.taskly.tarefa.TarefaDtos.TarefaResponse;
import com.taskly.tarefa.TarefaDtos.TarefaResumo;
import com.taskly.usuario.Usuario;
import com.taskly.usuario.UsuarioRepository;

/**
 * Tarefas e comentários. Qualquer membro do projeto cria, edita, move e comenta tarefas.
 * Excluir uma tarefa é do autor dela ou do gestor; excluir um comentário é do autor ou de um ADMIN.
 */
@Service
public class TarefaService {

	private final TarefaRepository tarefaRepository;
	private final ComentarioRepository comentarioRepository;
	private final QuadroRepository quadroRepository;
	private final QuadroService quadroService;
	private final ProjetoService projetoService;
	private final UsuarioRepository usuarioRepository;

	public TarefaService(TarefaRepository tarefaRepository, ComentarioRepository comentarioRepository,
			QuadroRepository quadroRepository, QuadroService quadroService, ProjetoService projetoService,
			UsuarioRepository usuarioRepository) {
		this.tarefaRepository = tarefaRepository;
		this.comentarioRepository = comentarioRepository;
		this.quadroRepository = quadroRepository;
		this.quadroService = quadroService;
		this.projetoService = projetoService;
		this.usuarioRepository = usuarioRepository;
	}

	/** Cria a tarefa no fim da coluna. */
	@Transactional
	public TarefaResponse criar(Long colunaId, UsuarioLogado usuario, TarefaRequest request) {
		Coluna coluna = quadroService.buscarColunaVisivel(colunaId, usuario);
		Projeto projeto = coluna.getQuadro().getProjeto();
		quadroRepository.travar(coluna.getQuadro().getId());

		Tarefa tarefa = new Tarefa(coluna, (int) tarefaRepository.countByColunaId(colunaId), usuarioAtual(usuario));
		aplicar(tarefa, projeto, request);
		tarefaRepository.save(tarefa);
		return TarefaResponse.de(tarefa, true);
	}

	/** Todas as tarefas do projeto, de todos os quadros, em formato de lista. */
	@Transactional(readOnly = true)
	public List<TarefaOpcao> doProjeto(Long projetoId, UsuarioLogado usuario) {
		projetoService.buscarVisivel(projetoId, usuario);
		return tarefaRepository.doProjeto(projetoId).stream().map(TarefaOpcao::de).toList();
	}

	@Transactional(readOnly = true)
	public TarefaResponse detalhar(Long tarefaId, UsuarioLogado usuario) {
		Tarefa tarefa = buscarVisivel(tarefaId, usuario);
		return TarefaResponse.de(tarefa, podeExcluir(tarefa, usuario));
	}

	@Transactional
	public TarefaResponse atualizar(Long tarefaId, UsuarioLogado usuario, TarefaRequest request) {
		Tarefa tarefa = buscarVisivel(tarefaId, usuario);
		aplicar(tarefa, projetoDa(tarefa), request);
		tarefaRepository.flush();
		return TarefaResponse.de(tarefa, podeExcluir(tarefa, usuario));
	}

	@Transactional
	public void excluir(Long tarefaId, UsuarioLogado usuario) {
		Tarefa tarefa = buscarVisivel(tarefaId, usuario);
		if (!podeExcluir(tarefa, usuario)) {
			throw new AcessoNegadoException("Só quem criou a tarefa ou o gestor do projeto pode excluí-la");
		}
		Coluna coluna = tarefa.getColuna();
		quadroRepository.travar(coluna.getQuadro().getId());
		List<Tarefa> restantes = new ArrayList<>(tarefaRepository.findByColunaIdOrderByPosicaoAsc(coluna.getId()));
		restantes.remove(tarefa);
		tarefaRepository.delete(tarefa);
		renumerar(restantes, coluna);
	}

	/**
	 * Move a tarefa para outra coluna do mesmo quadro (ou reordena na mesma) e mantém as posições contínuas
	 * nas duas colunas. Uma posição além do fim coloca a tarefa por último.
	 */
	@Transactional
	public TarefaResumo mover(Long tarefaId, UsuarioLogado usuario, Long colunaDestinoId, int posicao) {
		Tarefa tarefa = buscarVisivel(tarefaId, usuario);
		Coluna origem = tarefa.getColuna();
		Quadro quadro = origem.getQuadro();
		quadroRepository.travar(quadro.getId());

		Coluna destino = quadro.coluna(colunaDestinoId)
			.orElseThrow(() -> new RegraDeNegocioException("A coluna de destino não pertence a este quadro"));

		List<Tarefa> daOrigem = new ArrayList<>(tarefaRepository.findByColunaIdOrderByPosicaoAsc(origem.getId()));
		daOrigem.remove(tarefa);
		List<Tarefa> doDestino;
		if (Objects.equals(origem.getId(), destino.getId())) {
			doDestino = daOrigem;
		}
		else {
			renumerar(daOrigem, origem);
			doDestino = new ArrayList<>(tarefaRepository.findByColunaIdOrderByPosicaoAsc(destino.getId()));
		}
		doDestino.add(Math.min(posicao, doDestino.size()), tarefa);
		renumerar(doDestino, destino);
		return TarefaResumo.de(tarefa, 0);
	}

	@Transactional(readOnly = true)
	public List<ComentarioResponse> comentarios(Long tarefaId, UsuarioLogado usuario) {
		buscarVisivel(tarefaId, usuario);
		return comentarioRepository.daTarefa(tarefaId).stream()
			.map(c -> ComentarioResponse.de(c, podeExcluir(c, usuario)))
			.toList();
	}

	@Transactional
	public ComentarioResponse comentar(Long tarefaId, UsuarioLogado usuario, String texto) {
		Tarefa tarefa = buscarVisivel(tarefaId, usuario);
		Comentario comentario = comentarioRepository.save(new Comentario(tarefa, usuarioAtual(usuario), texto.trim()));
		return ComentarioResponse.de(comentario, true);
	}

	@Transactional
	public void excluirComentario(Long comentarioId, UsuarioLogado usuario) {
		Comentario comentario = comentarioRepository.findById(comentarioId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Comentário não encontrado"));
		buscarVisivel(comentario.getTarefa().getId(), usuario);
		if (!podeExcluir(comentario, usuario)) {
			throw new AcessoNegadoException("Só o autor pode excluir o comentário");
		}
		comentarioRepository.delete(comentario);
	}

	/** Tarefa de um projeto que o usuário enxerga (com coluna, quadro e projeto carregados); caso contrário, 404. */
	@Transactional(readOnly = true)
	public Tarefa buscarVisivel(Long tarefaId, UsuarioLogado usuario) {
		Tarefa tarefa = tarefaRepository.buscarCompleta(tarefaId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Tarefa não encontrada"));
		if (!projetoService.podeVer(projetoDa(tarefa), usuario)) {
			throw new RecursoNaoEncontradoException("Tarefa não encontrada");
		}
		return tarefa;
	}

	private boolean podeExcluir(Tarefa tarefa, UsuarioLogado usuario) {
		return Objects.equals(tarefa.getCriadoPor().getId(), usuario.id())
				|| projetoService.podeGerenciar(projetoDa(tarefa), usuario);
	}

	private static boolean podeExcluir(Comentario comentario, UsuarioLogado usuario) {
		return usuario.admin() || Objects.equals(comentario.getAutor().getId(), usuario.id());
	}

	private static Projeto projetoDa(Tarefa tarefa) {
		return tarefa.getColuna().getQuadro().getProjeto();
	}

	private Usuario usuarioAtual(UsuarioLogado usuario) {
		return usuarioRepository.findById(usuario.id())
			.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
	}

	private static void aplicar(Tarefa tarefa, Projeto projeto, TarefaRequest request) {
		Usuario responsavel = null;
		if (request.responsavelId() != null) {
			responsavel = projeto.membro(request.responsavelId())
				.map(MembroProjeto::getUsuario)
				.orElseThrow(() -> new RegraDeNegocioException("O responsável precisa ser membro do projeto"));
		}
		String descricao = StringUtils.hasText(request.descricao()) ? request.descricao().trim() : null;
		tarefa.atualizar(request.titulo().trim(), descricao, responsavel, request.prioridade(), request.prazo());
	}

	private static void renumerar(List<Tarefa> tarefas, Coluna coluna) {
		for (int i = 0; i < tarefas.size(); i++) {
			tarefas.get(i).posicionar(coluna, i);
		}
	}

}
