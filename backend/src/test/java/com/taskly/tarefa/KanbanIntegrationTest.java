package com.taskly.tarefa;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.JsonPath;
import com.taskly.ApiIntegrationTest;
import com.taskly.usuario.Perfil;

/** Quadros, colunas, tarefas e comentários contra um PostgreSQL real. */
class KanbanIntegrationTest extends ApiIntegrationTest {

	private Conta gestor;
	private Conta colaborador;
	private Conta estranho;
	private long projeto;
	private long quadro;
	private long aFazer;
	private long emAndamento;
	private long concluido;

	@BeforeEach
	void prepararQuadro() throws Exception {
		gestor = conta("Gil", Perfil.GERENTE);
		colaborador = conta("Cora", Perfil.MEMBRO);
		estranho = conta("Esther", Perfil.MEMBRO);
		projeto = id(post(gestor, "/api/projetos", "{\"nome\":\"Kanban\"}"), "$.id");
		post(gestor, "/api/projetos/{id}/membros", "{\"usuarioId\":%d}".formatted(colaborador.id()), projeto)
			.andExpect(status().isCreated());

		String corpo = post(gestor, "/api/projetos/{id}/quadros", "{\"nome\":\"Sprint 1\"}", projeto)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.colunas[*].nome").value(contains("A fazer", "Em andamento", "Concluído")))
			.andExpect(jsonPath("$.membros.length()").value(2))
			.andReturn().getResponse().getContentAsString();
		quadro = ((Number) JsonPath.read(corpo, "$.id")).longValue();
		aFazer = ((Number) JsonPath.read(corpo, "$.colunas[0].id")).longValue();
		emAndamento = ((Number) JsonPath.read(corpo, "$.colunas[1].id")).longValue();
		concluido = ((Number) JsonPath.read(corpo, "$.colunas[2].id")).longValue();
	}

	private long tarefa(Conta conta, long coluna, String titulo) throws Exception {
		return id(post(conta, "/api/colunas/{id}/tarefas", "{\"titulo\":\"%s\"}".formatted(titulo), coluna)
			.andExpect(status().isCreated()), "$.id");
	}

	@Test
	void moverEntreColunasEReordenarMantemAsPosicoesContinuas() throws Exception {
		long t1 = tarefa(colaborador, aFazer, "T1");
		long t2 = tarefa(colaborador, aFazer, "T2");
		long t3 = tarefa(colaborador, aFazer, "T3");
		long t4 = tarefa(colaborador, emAndamento, "T4");

		// T2 vai para o topo de "Em andamento".
		patch(colaborador, "/api/tarefas/{id}/mover", "{\"colunaId\":%d,\"posicao\":0}".formatted(emAndamento), t2)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.colunaId").value(emAndamento))
			.andExpect(jsonPath("$.posicao").value(0));

		// T3 sobe para o topo da própria coluna; T1 vai para o fim de "Concluído" com posição exagerada.
		patch(colaborador, "/api/tarefas/{id}/mover", "{\"colunaId\":%d,\"posicao\":0}".formatted(aFazer), t3)
			.andExpect(status().isOk());
		patch(colaborador, "/api/tarefas/{id}/mover", "{\"colunaId\":%d,\"posicao\":99}".formatted(concluido), t1)
			.andExpect(jsonPath("$.posicao").value(0));

		get(colaborador, "/api/quadros/{id}", quadro)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.podeGerenciar").value(false))
			.andExpect(jsonPath("$.colunas[0].tarefas[*].titulo").value(contains("T3")))
			.andExpect(jsonPath("$.colunas[0].tarefas[*].posicao").value(contains(0)))
			.andExpect(jsonPath("$.colunas[1].tarefas[*].titulo").value(contains("T2", "T4")))
			.andExpect(jsonPath("$.colunas[1].tarefas[*].posicao").value(contains(0, 1)))
			.andExpect(jsonPath("$.colunas[2].tarefas[*].titulo").value(contains("T1")));

		// Excluir T2 reposiciona T4 no topo.
		delete(colaborador, "/api/tarefas/{id}", t2).andExpect(status().isNoContent());
		get(colaborador, "/api/tarefas/{id}", t4).andExpect(jsonPath("$.posicao").value(0));
	}

	@Test
	void naoMoveParaColunaDeOutroQuadro() throws Exception {
		long outroQuadro = id(post(gestor, "/api/projetos/{id}/quadros", "{\"nome\":\"Outro\"}", projeto), "$.id");
		long colunaDeFora = id(get(gestor, "/api/quadros/{id}", outroQuadro), "$.colunas[0].id");
		long t = tarefa(colaborador, aFazer, "T");

		patch(colaborador, "/api/tarefas/{id}/mover", "{\"colunaId\":%d,\"posicao\":0}".formatted(colunaDeFora), t)
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("A coluna de destino não pertence a este quadro"));
	}

	@Test
	void detalhesDaTarefaResponsavelEComentarios() throws Exception {
		long t = id(post(colaborador, "/api/colunas/{id}/tarefas", """
				{"titulo":"  Login social  ","descricao":"Google e GitHub","prioridade":"ALTA","prazo":"2026-12-15",
				 "responsavelId":%d}""".formatted(colaborador.id()), aFazer)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.titulo").value("Login social"))
			.andExpect(jsonPath("$.responsavel.nome").value("Cora"))
			.andExpect(jsonPath("$.criadoPor.id").value(colaborador.id()))
			.andExpect(jsonPath("$.colunaNome").value("A fazer")), "$.id");

		// Responsável precisa ser membro do projeto.
		put(colaborador, "/api/tarefas/{id}", "{\"titulo\":\"X\",\"responsavelId\":%d}".formatted(estranho.id()), t)
			.andExpect(status().isUnprocessableContent());

		put(colaborador, "/api/tarefas/{id}", """
				{"titulo":"Login social","prioridade":"URGENTE","responsavelId":%d}""".formatted(gestor.id()), t)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.prioridade").value("URGENTE"))
			.andExpect(jsonPath("$.prazo").doesNotExist())
			.andExpect(jsonPath("$.responsavel.id").value(gestor.id()));

		long comentario = id(post(gestor, "/api/tarefas/{id}/comentarios", "{\"texto\":\"Começo amanhã\"}", t)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.autor.nome").value("Gil")), "$.id");
		post(colaborador, "/api/tarefas/{id}/comentarios", "{\"texto\":\"Combinado\"}", t)
			.andExpect(status().isCreated());

		get(colaborador, "/api/tarefas/{id}/comentarios", t)
			.andExpect(jsonPath("$[*].texto").value(contains("Começo amanhã", "Combinado")))
			.andExpect(jsonPath("$[0].podeExcluir").value(false))
			.andExpect(jsonPath("$[1].podeExcluir").value(true));
		get(colaborador, "/api/quadros/{id}", quadro)
			.andExpect(jsonPath("$.colunas[0].tarefas[0].totalComentarios").value(2))
			.andExpect(jsonPath("$.colunas[0].tarefas[0].prioridade").value("URGENTE"));

		delete(colaborador, "/api/comentarios/{id}", comentario).andExpect(status().isForbidden());
		delete(gestor, "/api/comentarios/{id}", comentario).andExpect(status().isNoContent());
	}

	@Test
	void quemNaoEhMembroNaoVeNadaEColaboradorNaoAlteraOQuadro() throws Exception {
		long t = tarefa(gestor, aFazer, "Segredo");

		get(estranho, "/api/quadros/{id}", quadro).andExpect(status().isNotFound());
		get(estranho, "/api/tarefas/{id}", t).andExpect(status().isNotFound());
		get(estranho, "/api/projetos/{id}/quadros", projeto).andExpect(status().isNotFound());
		post(estranho, "/api/colunas/{id}/tarefas", "{\"titulo\":\"X\"}", aFazer).andExpect(status().isNotFound());

		post(colaborador, "/api/projetos/{id}/quadros", "{\"nome\":\"X\"}", projeto).andExpect(status().isForbidden());
		post(colaborador, "/api/quadros/{id}/colunas", "{\"nome\":\"X\"}", quadro).andExpect(status().isForbidden());
		put(colaborador, "/api/colunas/{id}", "{\"nome\":\"X\"}", aFazer).andExpect(status().isForbidden());

		// Colaborador não exclui a tarefa de outra pessoa; o gestor exclui qualquer uma.
		delete(colaborador, "/api/tarefas/{id}", t).andExpect(status().isForbidden());
		get(colaborador, "/api/tarefas/{id}", t).andExpect(jsonPath("$.podeExcluir").value(false));
	}

	@Test
	void gestorAdministraColunas() throws Exception {
		long revisao = id(post(gestor, "/api/quadros/{id}/colunas", "{\"nome\":\"Revisão\"}", quadro)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.posicao").value(3)), "$.id");

		patch(gestor, "/api/colunas/{id}/mover", "{\"posicao\":2}", revisao)
			.andExpect(jsonPath("$.posicao").value(2));
		put(gestor, "/api/colunas/{id}", "{\"nome\":\"Feito\"}", concluido).andExpect(status().isOk());
		get(gestor, "/api/quadros/{id}", quadro)
			.andExpect(jsonPath("$.colunas[*].nome").value(contains("A fazer", "Em andamento", "Revisão", "Feito")))
			.andExpect(jsonPath("$.colunas[*].posicao").value(contains(0, 1, 2, 3)));

		tarefa(gestor, aFazer, "Bloqueia");
		delete(gestor, "/api/colunas/{id}", aFazer).andExpect(status().isUnprocessableContent());
		delete(gestor, "/api/colunas/{id}", revisao).andExpect(status().isNoContent());
		get(gestor, "/api/quadros/{id}", quadro).andExpect(jsonPath("$.colunas[*].posicao").value(contains(0, 1, 2)));

		put(gestor, "/api/quadros/{id}", "{\"nome\":\"Sprint 2\"}", quadro).andExpect(jsonPath("$.nome").value("Sprint 2"));
		get(colaborador, "/api/projetos/{id}/quadros", projeto).andExpect(jsonPath("$[0].nome").value("Sprint 2"));
		delete(gestor, "/api/quadros/{id}", quadro).andExpect(status().isNoContent());
		get(gestor, "/api/quadros/{id}", quadro).andExpect(status().isNotFound());
	}

	@Test
	void errosDeEntradaViramProblemDetails() throws Exception {
		post(colaborador, "/api/colunas/{id}/tarefas", "{\"titulo\":\"\",\"prioridade\":\"MEDIA\"}", aFazer)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros.titulo").exists());
		post(colaborador, "/api/colunas/{id}/tarefas", "{\"titulo\":\"X\",\"prioridade\":\"ALTISSIMA\"}", aFazer)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400));
		post(colaborador, "/api/colunas/{id}/tarefas", "{nao e json", aFazer).andExpect(status().isBadRequest());
		patch(colaborador, "/api/tarefas/{id}/mover", "{\"colunaId\":%d,\"posicao\":-1}".formatted(aFazer),
				tarefa(colaborador, aFazer, "T"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros.posicao").exists());
		get(colaborador, "/api/tarefas/abc").andExpect(status().isBadRequest());
		get(colaborador, "/api/tarefas/999999").andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Tarefa não encontrada"));
	}

}
