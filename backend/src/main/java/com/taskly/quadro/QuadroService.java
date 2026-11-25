package com.taskly.quadro;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskly.common.AcessoNegadoException;
import com.taskly.common.RecursoNaoEncontradoException;
import com.taskly.common.RegraDeNegocioException;
import com.taskly.common.UsuarioLogado;
import com.taskly.projeto.Projeto;
import com.taskly.projeto.ProjetoDtos.MembroResponse;
import com.taskly.projeto.ProjetoService;
import com.taskly.quadro.QuadroDtos.ColunaResponse;
import com.taskly.quadro.QuadroDtos.QuadroResponse;
import com.taskly.tarefa.ComentarioRepository;
import com.taskly.tarefa.ComentarioRepository.TotalPorTarefa;
import com.taskly.tarefa.Tarefa;
import com.taskly.tarefa.TarefaDtos.TarefaResumo;
import com.taskly.tarefa.TarefaRepository;

/**
 * Quadros e colunas. Qualquer membro do projeto vê o quadro; criar, renomear e excluir quadros
 * e colunas é do gestor do projeto (ou de um ADMIN).
 */
@Service
public class QuadroService {

	private final QuadroRepository quadroRepository;
	private final ColunaRepository colunaRepository;
	private final TarefaRepository tarefaRepository;
	private final ComentarioRepository comentarioRepository;
	private final ProjetoService projetoService;

	public QuadroService(QuadroRepository quadroRepository, ColunaRepository colunaRepository,
			TarefaRepository tarefaRepository, ComentarioRepository comentarioRepository,
			ProjetoService projetoService) {
		this.quadroRepository = quadroRepository;
		this.colunaRepository = colunaRepository;
		this.tarefaRepository = tarefaRepository;
		this.comentarioRepository = comentarioRepository;
		this.projetoService = projetoService;
	}

	@Transactional(readOnly = true)
	public List<Quadro> listar(Long projetoId, UsuarioLogado usuario) {
		projetoService.buscarVisivel(projetoId, usuario);
		return quadroRepository.findByProjetoIdOrderByIdAsc(projetoId);
	}

	@Transactional
	public Quadro criar(Long projetoId, UsuarioLogado usuario, String nome) {
		Projeto projeto = projetoService.buscarGerenciavel(projetoId, usuario);
		return quadroRepository.save(new Quadro(projeto, nome.trim()));
	}

	/** Quadro de um projeto que o usuário enxerga; caso contrário, 404. */
	@Transactional(readOnly = true)
	public Quadro buscarVisivel(Long quadroId, UsuarioLogado usuario) {
		Quadro quadro = quadroRepository.findById(quadroId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Quadro não encontrado"));
		if (!projetoService.podeVer(quadro.getProjeto(), usuario)) {
			throw new RecursoNaoEncontradoException("Quadro não encontrado");
		}
		return quadro;
	}

	@Transactional(readOnly = true)
	public Quadro buscarGerenciavel(Long quadroId, UsuarioLogado usuario) {
		Quadro quadro = buscarVisivel(quadroId, usuario);
		if (!projetoService.podeGerenciar(quadro.getProjeto(), usuario)) {
			throw new AcessoNegadoException("Só o gestor do projeto pode alterar o quadro");
		}
		return quadro;
	}

	/** Monta o quadro completo (colunas, tarefas e membros) em poucas consultas. */
	@Transactional(readOnly = true)
	public QuadroResponse detalhar(Long quadroId, UsuarioLogado usuario) {
		Quadro quadro = buscarVisivel(quadroId, usuario);
		Projeto projeto = quadro.getProjeto();

		Map<Long, Long> comentarios = comentarioRepository.contarPorTarefa(quadroId).stream()
			.collect(Collectors.toMap(TotalPorTarefa::getTarefaId, TotalPorTarefa::getTotal));
		Map<Long, List<TarefaResumo>> tarefasPorColuna = tarefaRepository.doQuadro(quadroId).stream()
			.collect(Collectors.groupingBy(t -> t.getColuna().getId(),
					Collectors.mapping((Tarefa t) -> TarefaResumo.de(t, comentarios.getOrDefault(t.getId(), 0L)),
							Collectors.toList())));

		List<ColunaResponse> colunas = quadro.getColunas().stream()
			.map(c -> new ColunaResponse(c.getId(), c.getNome(), c.getPosicao(),
					tarefasPorColuna.getOrDefault(c.getId(), List.of())))
			.toList();
		List<MembroResponse> membros = projeto.getMembros().stream()
			.map(MembroResponse::de)
			.sorted(Comparator.comparing(MembroResponse::nome))
			.toList();
		return new QuadroResponse(quadro.getId(), quadro.getNome(), projeto.getId(), projeto.getNome(),
				projetoService.podeGerenciar(projeto, usuario), colunas, membros);
	}

	@Transactional
	public Quadro renomear(Long quadroId, UsuarioLogado usuario, String nome) {
		Quadro quadro = buscarGerenciavel(quadroId, usuario);
		quadro.renomear(nome.trim());
		return quadro;
	}

	@Transactional
	public void excluir(Long quadroId, UsuarioLogado usuario) {
		quadroRepository.delete(buscarGerenciavel(quadroId, usuario));
	}

	@Transactional
	public Coluna adicionarColuna(Long quadroId, UsuarioLogado usuario, String nome) {
		Quadro quadro = buscarGerenciavel(quadroId, usuario);
		Coluna coluna = quadro.adicionarColuna(nome.trim());
		quadroRepository.flush();
		return coluna;
	}

	/** Coluna de um quadro que o usuário enxerga; caso contrário, 404. */
	@Transactional(readOnly = true)
	public Coluna buscarColunaVisivel(Long colunaId, UsuarioLogado usuario) {
		Coluna coluna = colunaRepository.findById(colunaId)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Coluna não encontrada"));
		if (!projetoService.podeVer(coluna.getQuadro().getProjeto(), usuario)) {
			throw new RecursoNaoEncontradoException("Coluna não encontrada");
		}
		return coluna;
	}

	@Transactional
	public Coluna renomearColuna(Long colunaId, UsuarioLogado usuario, String nome) {
		Coluna coluna = colunaGerenciavel(colunaId, usuario);
		coluna.renomear(nome.trim());
		return coluna;
	}

	@Transactional
	public Coluna moverColuna(Long colunaId, UsuarioLogado usuario, int posicao) {
		Coluna coluna = colunaGerenciavel(colunaId, usuario);
		coluna.getQuadro().moverColuna(coluna, posicao);
		return coluna;
	}

	@Transactional
	public void excluirColuna(Long colunaId, UsuarioLogado usuario) {
		Coluna coluna = colunaGerenciavel(colunaId, usuario);
		if (tarefaRepository.countByColunaId(colunaId) > 0) {
			throw new RegraDeNegocioException("Mova ou exclua as tarefas antes de excluir a coluna");
		}
		coluna.getQuadro().removerColuna(coluna);
	}

	private Coluna colunaGerenciavel(Long colunaId, UsuarioLogado usuario) {
		Coluna coluna = buscarColunaVisivel(colunaId, usuario);
		if (!projetoService.podeGerenciar(coluna.getQuadro().getProjeto(), usuario)) {
			throw new AcessoNegadoException("Só o gestor do projeto pode alterar as colunas");
		}
		return coluna;
	}

}
