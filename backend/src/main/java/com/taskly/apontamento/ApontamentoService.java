package com.taskly.apontamento;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.taskly.apontamento.ApontamentoDtos.ApontamentoRequest;
import com.taskly.apontamento.ApontamentoDtos.ApontamentoResponse;
import com.taskly.apontamento.ApontamentoDtos.Consolidado;
import com.taskly.apontamento.ApontamentoDtos.HorasDaTarefa;
import com.taskly.apontamento.ApontamentoDtos.Linha;
import com.taskly.apontamento.ApontamentoDtos.MinhasHoras;
import com.taskly.apontamento.ApontamentoDtos.TotalDoDia;
import com.taskly.apontamento.ApontamentoDtos.TotalPorPessoa;
import com.taskly.apontamento.ApontamentoDtos.TotalPorProjeto;
import com.taskly.apontamento.ApontamentoRepository.HorasPorProjetoEPessoa;
import com.taskly.common.AcessoNegadoException;
import com.taskly.common.RecursoNaoEncontradoException;
import com.taskly.common.RegraDeNegocioException;
import com.taskly.common.UsuarioLogado;
import com.taskly.projeto.Projeto;
import com.taskly.projeto.ProjetoRepository;
import com.taskly.projeto.ProjetoService;
import com.taskly.tarefa.Tarefa;
import com.taskly.tarefa.TarefaService;
import com.taskly.usuario.Usuario;
import com.taskly.usuario.UsuarioRepository;

/**
 * Apontamento de horas. Regras:
 * <ul>
 *   <li>cada pessoa aponta as próprias horas, e só em tarefas de projetos de que é membro;</li>
 *   <li>não se apontam horas no futuro, e a soma de um dia não passa do limite diário;</li>
 *   <li>só o autor edita ou exclui um apontamento;</li>
 *   <li>o consolidado mostra os projetos que o usuário gere (ADMIN vê todos).</li>
 * </ul>
 */
@Service
@EnableConfigurationProperties(HorasProperties.class)
public class ApontamentoService {

	/** Maior período aceito nas consultas, para não varrer a base inteira por engano. */
	static final int MAX_DIAS_CONSULTA = 366;

	private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final ApontamentoRepository apontamentoRepository;
	private final TarefaService tarefaService;
	private final ProjetoService projetoService;
	private final ProjetoRepository projetoRepository;
	private final UsuarioRepository usuarioRepository;
	private final HorasProperties propriedades;
	private final Clock clock;

	public ApontamentoService(ApontamentoRepository apontamentoRepository, TarefaService tarefaService,
			ProjetoService projetoService, ProjetoRepository projetoRepository, UsuarioRepository usuarioRepository,
			HorasProperties propriedades, Clock clock) {
		this.apontamentoRepository = apontamentoRepository;
		this.tarefaService = tarefaService;
		this.projetoService = projetoService;
		this.projetoRepository = projetoRepository;
		this.usuarioRepository = usuarioRepository;
		this.propriedades = propriedades;
		this.clock = clock;
	}

	@Transactional
	public ApontamentoResponse registrar(Long tarefaId, UsuarioLogado usuario, ApontamentoRequest request) {
		Tarefa tarefa = tarefaService.buscarVisivel(tarefaId, usuario);
		Projeto projeto = tarefa.getColuna().getQuadro().getProjeto();
		if (projeto.membro(usuario.id()).isEmpty()) {
			throw new AcessoNegadoException("Só membros do projeto apontam horas nas tarefas dele");
		}
		Usuario autor = usuarioRepository.travar(usuario.id())
			.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
		validar(usuario.id(), 0L, request);

		Apontamento apontamento = new Apontamento(tarefa, autor);
		apontamento.atualizar(request.data(), request.minutos(), texto(request.descricao()));
		return ApontamentoResponse.de(apontamentoRepository.save(apontamento), true);
	}

	@Transactional
	public ApontamentoResponse atualizar(Long id, UsuarioLogado usuario, ApontamentoRequest request) {
		Apontamento apontamento = buscarDoAutor(id, usuario);
		usuarioRepository.travar(usuario.id());
		validar(usuario.id(), apontamento.getId(), request);
		apontamento.atualizar(request.data(), request.minutos(), texto(request.descricao()));
		apontamentoRepository.flush();
		return ApontamentoResponse.de(apontamento, true);
	}

	@Transactional
	public void excluir(Long id, UsuarioLogado usuario) {
		apontamentoRepository.delete(buscarDoAutor(id, usuario));
	}

	@Transactional(readOnly = true)
	public HorasDaTarefa daTarefa(Long tarefaId, UsuarioLogado usuario) {
		tarefaService.buscarVisivel(tarefaId, usuario);
		List<ApontamentoResponse> apontamentos = apontamentoRepository.daTarefa(tarefaId).stream()
			.map(a -> ApontamentoResponse.de(a, ehAutor(a, usuario)))
			.toList();
		long total = apontamentos.stream().mapToLong(ApontamentoResponse::minutos).sum();
		return new HorasDaTarefa(total, apontamentos);
	}

	/** Apontamentos do usuário logado no período; sem datas, a semana atual (segunda a domingo). */
	@Transactional(readOnly = true)
	public MinhasHoras minhas(UsuarioLogado usuario, LocalDate inicio, LocalDate fim) {
		LocalDate[] periodo = periodo(inicio, fim);
		List<ApontamentoResponse> apontamentos = apontamentoRepository
			.doUsuarioNoPeriodo(usuario.id(), periodo[0], periodo[1]).stream()
			.map(a -> ApontamentoResponse.de(a, true))
			.toList();

		Map<LocalDate, Long> porDia = new LinkedHashMap<>();
		for (LocalDate dia = periodo[0]; !dia.isAfter(periodo[1]); dia = dia.plusDays(1)) {
			porDia.put(dia, 0L);
		}
		apontamentos.forEach(a -> porDia.merge(a.data(), (long) a.minutos(), Long::sum));
		List<TotalDoDia> dias = porDia.entrySet().stream().map(e -> new TotalDoDia(e.getKey(), e.getValue())).toList();
		long total = porDia.values().stream().mapToLong(Long::longValue).sum();
		return new MinhasHoras(periodo[0], periodo[1], propriedades.limiteDiarioEmMinutos(), total, dias,
				apontamentos);
	}

	/**
	 * Horas por pessoa e por projeto no período, nos projetos que o usuário gere (ADMIN: todos).
	 * Com {@code projetoId}, só aquele projeto, que precisa ser gerenciável pelo usuário.
	 */
	@Transactional(readOnly = true)
	public Consolidado consolidado(UsuarioLogado usuario, LocalDate inicio, LocalDate fim, Long projetoId) {
		LocalDate[] periodo = periodo(inicio, fim);
		List<HorasPorProjetoEPessoa> linhas;
		if (projetoId != null) {
			projetoService.buscarGerenciavel(projetoId, usuario);
			linhas = apontamentoRepository.consolidado(periodo[0], periodo[1], List.of(projetoId));
		}
		else if (usuario.admin()) {
			linhas = apontamentoRepository.consolidado(periodo[0], periodo[1]);
		}
		else {
			List<Long> geridos = projetoRepository.geridosPor(usuario.id());
			linhas = geridos.isEmpty() ? List.of()
					: apontamentoRepository.consolidado(periodo[0], periodo[1], geridos);
		}
		return consolidar(periodo, linhas);
	}

	private static Consolidado consolidar(LocalDate[] periodo, List<HorasPorProjetoEPessoa> linhas) {
		Map<Long, TotalPorPessoa> pessoas = new LinkedHashMap<>();
		Map<Long, TotalPorProjeto> projetos = new LinkedHashMap<>();
		List<Linha> detalhe = new ArrayList<>();
		long total = 0;
		for (HorasPorProjetoEPessoa l : linhas) {
			total += l.getMinutos();
			detalhe.add(new Linha(l.getProjetoId(), l.getProjetoNome(), l.getUsuarioId(), l.getUsuarioNome(),
					l.getMinutos()));
			pessoas.merge(l.getUsuarioId(), new TotalPorPessoa(l.getUsuarioId(), l.getUsuarioNome(), l.getMinutos()),
					(a, b) -> new TotalPorPessoa(a.usuarioId(), a.nome(), a.minutos() + b.minutos()));
			projetos.merge(l.getProjetoId(),
					new TotalPorProjeto(l.getProjetoId(), l.getProjetoNome(), l.getCliente(), l.getMinutos()),
					(a, b) -> new TotalPorProjeto(a.projetoId(), a.nome(), a.cliente(), a.minutos() + b.minutos()));
		}
		List<TotalPorPessoa> porPessoa = pessoas.values().stream()
			.sorted(Comparator.comparingLong(TotalPorPessoa::minutos).reversed().thenComparing(TotalPorPessoa::nome))
			.toList();
		List<TotalPorProjeto> porProjeto = projetos.values().stream()
			.sorted(Comparator.comparingLong(TotalPorProjeto::minutos).reversed().thenComparing(TotalPorProjeto::nome))
			.toList();
		return new Consolidado(periodo[0], periodo[1], total, porPessoa, porProjeto, detalhe);
	}

	/** Data não pode ser futura e o dia não pode passar do limite (contando os outros apontamentos do dia). */
	private void validar(Long usuarioId, Long ignorarId, ApontamentoRequest request) {
		if (request.data().isAfter(hoje())) {
			throw new RegraDeNegocioException("Não é possível apontar horas em datas futuras");
		}
		long noDia = apontamentoRepository.minutosNoDia(usuarioId, request.data(), ignorarId);
		int limite = propriedades.limiteDiarioEmMinutos();
		if (noDia + request.minutos() > limite) {
			String mensagem = "O limite é de " + duracao(limite) + " por dia";
			if (noDia > 0) {
				mensagem += "; você já tem " + duracao(noDia) + " apontadas em " + DATA_BR.format(request.data());
			}
			throw new RegraDeNegocioException(mensagem);
		}
	}

	private Apontamento buscarDoAutor(Long id, UsuarioLogado usuario) {
		Apontamento apontamento = apontamentoRepository.buscarCompleto(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Apontamento não encontrado"));
		if (!ehAutor(apontamento, usuario)) {
			Projeto projeto = apontamento.getTarefa().getColuna().getQuadro().getProjeto();
			if (!projetoService.podeVer(projeto, usuario)) {
				throw new RecursoNaoEncontradoException("Apontamento não encontrado");
			}
			throw new AcessoNegadoException("Só quem apontou as horas pode alterá-las");
		}
		return apontamento;
	}

	private LocalDate[] periodo(LocalDate inicio, LocalDate fim) {
		if (inicio == null && fim == null) {
			inicio = hoje().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
			fim = inicio.plusDays(6);
		}
		else if (inicio == null || fim == null) {
			throw new RegraDeNegocioException("Informe as duas datas do período (inicio e fim)");
		}
		if (fim.isBefore(inicio)) {
			throw new RegraDeNegocioException("O fim do período não pode ser anterior ao início");
		}
		if (ChronoUnit.DAYS.between(inicio, fim) >= MAX_DIAS_CONSULTA) {
			throw new RegraDeNegocioException("O período pode ter no máximo " + MAX_DIAS_CONSULTA + " dias");
		}
		return new LocalDate[] { inicio, fim };
	}

	private LocalDate hoje() {
		return LocalDate.ofInstant(clock.instant(), propriedades.fuso());
	}

	private static boolean ehAutor(Apontamento apontamento, UsuarioLogado usuario) {
		return Objects.equals(apontamento.getUsuario().getId(), usuario.id());
	}

	private static String texto(String descricao) {
		return StringUtils.hasText(descricao) ? descricao.trim() : null;
	}

	/** 90 → "1h30", 480 → "8h". */
	static String duracao(long minutos) {
		long h = minutos / 60;
		long m = minutos % 60;
		return m == 0 ? h + "h" : "%dh%02d".formatted(h, m);
	}

}
