package com.taskly.apontamento;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.taskly.ApiIntegrationTest;
import com.taskly.usuario.Perfil;

/** Apontamento de horas, visão semanal e consolidado do gerente contra um PostgreSQL real. */
class ApontamentoIntegrationTest extends ApiIntegrationTest {

	private static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

	private Conta gestor;
	private Conta colaborador;
	private Conta estranho;
	private long projeto;
	private long tarefa;

	@BeforeEach
	void prepararTarefa() throws Exception {
		gestor = conta("Gil", Perfil.GERENTE);
		colaborador = conta("Cora", Perfil.MEMBRO);
		estranho = conta("Esther", Perfil.MEMBRO);
		projeto = id(post(gestor, "/api/projetos", "{\"nome\":\"Portal\",\"cliente\":\"ACME\"}"), "$.id");
		post(gestor, "/api/projetos/{id}/membros", "{\"usuarioId\":%d}".formatted(colaborador.id()), projeto)
			.andExpect(status().isCreated());
		long quadro = id(post(gestor, "/api/projetos/{id}/quadros", "{\"nome\":\"Sprint 1\"}", projeto), "$.id");
		long coluna = id(get(gestor, "/api/quadros/{id}", quadro), "$.colunas[0].id");
		tarefa = id(post(gestor, "/api/colunas/{id}/tarefas", "{\"titulo\":\"Login\"}", coluna), "$.id");
	}

	private static String horas(LocalDate data, int minutos, String descricao) {
		return """
				{"data":"%s","minutos":%d,"descricao":"%s"}""".formatted(data, minutos, descricao);
	}

	@Test
	void registraListaEditaEExcluiHorasDaTarefa() throws Exception {
		long a1 = id(post(colaborador, "/api/tarefas/{id}/apontamentos", horas(HOJE, 90, "  Tela de login  "), tarefa)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.minutos").value(90))
			.andExpect(jsonPath("$.descricao").value("Tela de login"))
			.andExpect(jsonPath("$.usuario.nome").value("Cora"))
			.andExpect(jsonPath("$.tarefa.titulo").value("Login"))
			.andExpect(jsonPath("$.tarefa.projetoNome").value("Portal"))
			.andExpect(jsonPath("$.podeEditar").value(true)), "$.id");
		post(gestor, "/api/tarefas/{id}/apontamentos", horas(HOJE.minusDays(1), 30, "Revisão"), tarefa)
			.andExpect(status().isCreated());

		get(colaborador, "/api/tarefas/{id}/apontamentos", tarefa)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalMinutos").value(120))
			.andExpect(jsonPath("$.apontamentos[*].usuario.nome").value(contains("Cora", "Gil")))
			.andExpect(jsonPath("$.apontamentos[*].podeEditar").value(contains(true, false)));

		put(colaborador, "/api/apontamentos/{id}", horas(HOJE, 120, ""), a1)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.minutos").value(120))
			.andExpect(jsonPath("$.descricao").doesNotExist());

		// Edição e exclusão são só do autor, nem o gestor do projeto pode; quem não é do projeto recebe 404.
		put(gestor, "/api/apontamentos/{id}", horas(HOJE, 60, "x"), a1).andExpect(status().isForbidden());
		delete(gestor, "/api/apontamentos/{id}", a1).andExpect(status().isForbidden());
		delete(estranho, "/api/apontamentos/{id}", a1).andExpect(status().isNotFound());

		delete(colaborador, "/api/apontamentos/{id}", a1).andExpect(status().isNoContent());
		get(colaborador, "/api/tarefas/{id}/apontamentos", tarefa).andExpect(jsonPath("$.totalMinutos").value(30));
	}

	@Test
	void respeitaLimiteDiarioEDataFutura() throws Exception {
		LocalDate ontem = HOJE.minusDays(1);
		long a = id(post(colaborador, "/api/tarefas/{id}/apontamentos", horas(ontem, 600, "Manhã e tarde"), tarefa)
			.andExpect(status().isCreated()), "$.id");

		// 10h + 2h30 passa do limite de 12h.
		post(colaborador, "/api/tarefas/{id}/apontamentos", horas(ontem, 150, "Noite"), tarefa)
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("O limite é de 12h por dia")));
		// Exatamente 12h é aceito.
		post(colaborador, "/api/tarefas/{id}/apontamentos", horas(ontem, 120, "Noite"), tarefa)
			.andExpect(status().isCreated());
		// Na edição, o próprio apontamento não conta duas vezes: 10h → 9h com mais 2h no dia = 11h.
		put(colaborador, "/api/apontamentos/{id}", horas(ontem, 540, "Manhã e tarde"), a)
			.andExpect(status().isOk());
		// O limite é por pessoa: outra pessoa aponta no mesmo dia normalmente.
		post(gestor, "/api/tarefas/{id}/apontamentos", horas(ontem, 480, "Gestão"), tarefa)
			.andExpect(status().isCreated());

		post(colaborador, "/api/tarefas/{id}/apontamentos", horas(HOJE.plusDays(1), 60, "Amanhã"), tarefa)
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("Não é possível apontar horas em datas futuras"));
		post(colaborador, "/api/tarefas/{id}/apontamentos", "{\"data\":\"%s\",\"minutos\":0}".formatted(HOJE), tarefa)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros.minutos").exists());
	}

	@Test
	void soMembrosApontamHoras() throws Exception {
		Conta admin = conta("Ada", Perfil.ADMIN);

		post(estranho, "/api/tarefas/{id}/apontamentos", horas(HOJE, 60, "x"), tarefa)
			.andExpect(status().isNotFound());
		get(estranho, "/api/tarefas/{id}/apontamentos", tarefa).andExpect(status().isNotFound());
		// ADMIN enxerga a tarefa, mas não aponta horas num projeto de que não participa.
		post(admin, "/api/tarefas/{id}/apontamentos", horas(HOJE, 60, "x"), tarefa)
			.andExpect(status().isForbidden());
		get(admin, "/api/tarefas/{id}/apontamentos", tarefa).andExpect(status().isOk());
	}

	@Test
	void visaoSemanalTrazOsTotaisDeCadaDia() throws Exception {
		LocalDate segunda = HOJE.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate semanaPassada = segunda.minusWeeks(1);
		post(colaborador, "/api/tarefas/{id}/apontamentos", horas(semanaPassada, 60, "A"), tarefa);
		post(colaborador, "/api/tarefas/{id}/apontamentos", horas(semanaPassada, 30, "B"), tarefa);
		post(colaborador, "/api/tarefas/{id}/apontamentos", horas(semanaPassada.plusDays(2), 45, "C"), tarefa);
		post(colaborador, "/api/tarefas/{id}/apontamentos", horas(segunda, 15, "Fora do período"), tarefa);
		post(gestor, "/api/tarefas/{id}/apontamentos", horas(semanaPassada, 60, "De outra pessoa"), tarefa);

		get(colaborador, "/api/apontamentos/meus?inicio={i}&fim={f}", semanaPassada, semanaPassada.plusDays(6))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.limiteDiarioMinutos").value(720))
			.andExpect(jsonPath("$.totalMinutos").value(135))
			.andExpect(jsonPath("$.dias.length()").value(7))
			.andExpect(jsonPath("$.dias[0].minutos").value(90))
			.andExpect(jsonPath("$.dias[1].minutos").value(0))
			.andExpect(jsonPath("$.dias[2].minutos").value(45))
			.andExpect(jsonPath("$.apontamentos[*].descricao").value(contains("A", "B", "C")));

		// Sem datas: a semana atual, de segunda a domingo.
		get(colaborador, "/api/apontamentos/meus")
			.andExpect(jsonPath("$.inicio").value(segunda.toString()))
			.andExpect(jsonPath("$.fim").value(segunda.plusDays(6).toString()))
			.andExpect(jsonPath("$.totalMinutos").value(15));

		get(colaborador, "/api/apontamentos/meus?inicio={i}&fim={f}", segunda, semanaPassada)
			.andExpect(status().isUnprocessableContent());
		get(colaborador, "/api/apontamentos/meus?inicio={i}", segunda).andExpect(status().isUnprocessableContent());
	}

	@Test
	void consolidadoMostraSoOsProjetosQueOUsuarioGere() throws Exception {
		Conta outroGestor = conta("Otto", Perfil.GERENTE);
		long outroProjeto = id(post(outroGestor, "/api/projetos", "{\"nome\":\"Intranet\"}"), "$.id");
		post(outroGestor, "/api/projetos/{id}/membros", "{\"usuarioId\":%d}".formatted(colaborador.id()), outroProjeto);
		long quadro = id(post(outroGestor, "/api/projetos/{id}/quadros", "{\"nome\":\"Q\"}", outroProjeto), "$.id");
		long coluna = id(get(outroGestor, "/api/quadros/{id}", quadro), "$.colunas[0].id");
		long outraTarefa = id(post(outroGestor, "/api/colunas/{id}/tarefas", "{\"titulo\":\"Busca\"}", coluna), "$.id");

		LocalDate dia = HOJE.minusDays(1);
		post(colaborador, "/api/tarefas/{id}/apontamentos", horas(dia, 120, "Login"), tarefa);
		post(colaborador, "/api/tarefas/{id}/apontamentos", horas(dia.minusDays(1), 60, "Login"), tarefa);
		post(gestor, "/api/tarefas/{id}/apontamentos", horas(dia, 240, "Gestão"), tarefa);
		post(colaborador, "/api/tarefas/{id}/apontamentos", horas(dia, 300, "Busca"), outraTarefa);

		String periodo = "?inicio=" + dia.minusDays(1) + "&fim=" + dia;
		get(gestor, "/api/apontamentos/consolidado" + periodo)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalMinutos").value(420))
			.andExpect(jsonPath("$.porProjeto[*].nome").value(contains("Portal")))
			.andExpect(jsonPath("$.porProjeto[0].cliente").value("ACME"))
			.andExpect(jsonPath("$.porPessoa[*].nome").value(contains("Gil", "Cora")))
			.andExpect(jsonPath("$.porPessoa[*].minutos").value(contains(240, 180)))
			.andExpect(jsonPath("$.linhas.length()").value(2));

		// Filtrar por um projeto que não gere: o colaborador não é gestor de nenhum.
		get(colaborador, "/api/apontamentos/consolidado" + periodo + "&projetoId=" + projeto)
			.andExpect(status().isForbidden());
		get(colaborador, "/api/apontamentos/consolidado" + periodo)
			.andExpect(jsonPath("$.totalMinutos").value(0))
			.andExpect(jsonPath("$.linhas.length()").value(0));
		get(outroGestor, "/api/apontamentos/consolidado" + periodo + "&projetoId=" + projeto)
			.andExpect(status().isNotFound());
		get(outroGestor, "/api/apontamentos/consolidado" + periodo + "&projetoId=" + outroProjeto)
			.andExpect(jsonPath("$.totalMinutos").value(300));
	}

	@Test
	void listaAsTarefasDoProjetoParaEscolherNoApontamento() throws Exception {
		get(colaborador, "/api/projetos/{id}/tarefas", projeto)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].titulo").value(contains("Login")))
			.andExpect(jsonPath("$[0].quadroNome").value("Sprint 1"))
			.andExpect(jsonPath("$[0].colunaNome").value("A fazer"));
		get(estranho, "/api/projetos/{id}/tarefas", projeto).andExpect(status().isNotFound());
	}

}
