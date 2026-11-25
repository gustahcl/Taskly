package com.taskly.projeto;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.taskly.ApiIntegrationTest;
import com.taskly.usuario.Perfil;

/** Projetos e membros contra um PostgreSQL real: regras de visibilidade, filtros e paginação. */
class ProjetoIntegrationTest extends ApiIntegrationTest {

	private long criarProjeto(Conta conta, String nome, String cliente, String status) throws Exception {
		return id(post(conta, "/api/projetos", """
				{"nome":"%s","cliente":"%s","status":"%s"}""".formatted(nome, cliente, status))
			.andExpect(status().isCreated()), "$.id");
	}

	@Test
	void cicloCompletoDeProjetoEMembros() throws Exception {
		Conta gerente = conta("Gabi", Perfil.GERENTE);
		Conta membro = conta("Mauro", Perfil.MEMBRO);
		Conta outro = conta("Otto", Perfil.MEMBRO);

		post(membro, "/api/projetos", "{\"nome\":\"Não pode\"}").andExpect(status().isForbidden());

		long projeto = criarProjeto(gerente, "Portal", "ACME", "EM_ANDAMENTO");
		get(gerente, "/api/projetos/{id}", projeto)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.meuPapel").value("GESTOR"))
			.andExpect(jsonPath("$.podeGerenciar").value(true))
			.andExpect(jsonPath("$.membros.length()").value(1));

		// Quem não é membro não enxerga o projeto.
		get(membro, "/api/projetos/{id}", projeto).andExpect(status().isNotFound());

		post(gerente, "/api/projetos/{id}/membros", "{\"usuarioId\":%d}".formatted(membro.id()), projeto)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.papel").value("COLABORADOR"));
		post(gerente, "/api/projetos/{id}/membros", "{\"usuarioId\":%d}".formatted(membro.id()), projeto)
			.andExpect(status().isConflict());

		get(membro, "/api/projetos/{id}", projeto)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.meuPapel").value("COLABORADOR"))
			.andExpect(jsonPath("$.podeGerenciar").value(false));

		// Colaborador não edita nem administra a equipe.
		put(membro, "/api/projetos/{id}", "{\"nome\":\"Outro nome\"}", projeto).andExpect(status().isForbidden());
		post(membro, "/api/projetos/{id}/membros", "{\"usuarioId\":%d}".formatted(outro.id()), projeto)
			.andExpect(status().isForbidden());

		// O único gestor não pode sair nem ser rebaixado.
		delete(gerente, "/api/projetos/{id}/membros/{u}", projeto, gerente.id())
			.andExpect(status().isUnprocessableContent());

		patch(gerente, "/api/projetos/{id}/membros/{u}", "{\"papel\":\"GESTOR\"}", projeto, membro.id())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.papel").value("GESTOR"));
		put(membro, "/api/projetos/{id}", """
				{"nome":"Portal v2","status":"PAUSADO","dataInicio":"2026-12-01","dataFim":"2026-11-01"}""", projeto)
			.andExpect(status().isUnprocessableContent());
		put(membro, "/api/projetos/{id}", """
				{"nome":"Portal v2","status":"PAUSADO","dataInicio":"2026-12-01","dataFim":"2027-01-31"}""", projeto)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nome").value("Portal v2"))
			.andExpect(jsonPath("$.dataFim").value("2027-01-31"));

		delete(membro, "/api/projetos/{id}/membros/{u}", projeto, gerente.id()).andExpect(status().isNoContent());
		get(gerente, "/api/projetos/{id}", projeto).andExpect(status().isNotFound());

		delete(membro, "/api/projetos/{id}", projeto).andExpect(status().isNoContent());
		get(membro, "/api/projetos/{id}", projeto).andExpect(status().isNotFound());
	}

	@Test
	void listagemMostraSoOsProjetosDoUsuarioComFiltrosEPaginacao() throws Exception {
		String sufixo = UUID.randomUUID().toString().substring(0, 8);
		Conta gerente = conta("Gina", Perfil.GERENTE);
		Conta admin = conta("Adm", Perfil.ADMIN);
		Conta membro = conta("Mia", Perfil.MEMBRO);

		criarProjeto(gerente, "Alfa " + sufixo, "Cliente Um", "EM_ANDAMENTO");
		criarProjeto(gerente, "Beta " + sufixo, "Cliente Dois", "PLANEJADO");
		long gama = criarProjeto(gerente, "Gama " + sufixo, "Cliente Um", "EM_ANDAMENTO");
		post(gerente, "/api/projetos/{id}/membros", "{\"usuarioId\":%d}".formatted(membro.id()), gama)
			.andExpect(status().isCreated());

		get(gerente, "/api/projetos?busca={b}&size=2", sufixo)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElementos").value(3))
			.andExpect(jsonPath("$.totalPaginas").value(2))
			.andExpect(jsonPath("$.conteudo[0].nome").value("Alfa " + sufixo))
			.andExpect(jsonPath("$.conteudo[0].meuPapel").value("GESTOR"));

		get(gerente, "/api/projetos?busca={b}&status=EM_ANDAMENTO&sort=nome,desc", sufixo)
			.andExpect(jsonPath("$.totalElementos").value(2))
			.andExpect(jsonPath("$.conteudo[0].nome").value("Gama " + sufixo));

		// A busca também procura no nome do cliente, sem diferenciar maiúsculas.
		get(gerente, "/api/projetos?busca={b}", "cliente dois")
			.andExpect(jsonPath("$.conteudo[*].nome").value(hasItem("Beta " + sufixo)));

		get(membro, "/api/projetos?busca={b}", sufixo)
			.andExpect(jsonPath("$.totalElementos").value(1))
			.andExpect(jsonPath("$.conteudo[0].nome").value("Gama " + sufixo))
			.andExpect(jsonPath("$.conteudo[0].totalMembros").value(2));

		get(admin, "/api/projetos?busca={b}", sufixo)
			.andExpect(jsonPath("$.totalElementos").value(3))
			.andExpect(jsonPath("$.conteudo[0].meuPapel").doesNotExist());

		get(admin, "/api/projetos?status=INVALIDO").andExpect(status().isBadRequest());
		get(admin, "/api/projetos?sort=naoExiste").andExpect(status().isBadRequest());
	}

	@Test
	void buscaDeUsuariosParaConvite() throws Exception {
		String nome = "Zeca" + UUID.randomUUID().toString().substring(0, 6);
		Conta zeca = conta(nome, Perfil.MEMBRO);

		get(zeca, "/api/usuarios/busca?termo={t}", nome.toLowerCase())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(zeca.id()))
			.andExpect(jsonPath("$[0].perfil").doesNotExist());
		get(zeca, "/api/usuarios/busca?termo=z")
			.andExpect(jsonPath("$.length()").value(0));
		get(zeca, "/api/usuarios/busca?termo={t}", "inexistente-" + UUID.randomUUID())
			.andExpect(jsonPath("$[*].id").value(not(hasItem((int) zeca.id()))));
	}

}
