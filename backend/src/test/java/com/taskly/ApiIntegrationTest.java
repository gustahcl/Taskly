package com.taskly;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.jayway.jsonpath.JsonPath;
import com.taskly.usuario.Perfil;
import com.taskly.usuario.UsuarioRepository;

/** Base dos testes de integração da API: cria usuários com o perfil desejado e faz as chamadas autenticadas. */
@AutoConfigureMockMvc
public abstract class ApiIntegrationTest extends IntegrationTest {

	@Autowired
	protected MockMvc mvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	/** Usuário logado nos testes: id e access token. */
	protected record Conta(long id, String token) {
	}

	/** Cadastra um usuário pela API, ajusta o perfil no banco e faz login (o token já sai com o perfil certo). */
	protected Conta conta(String nome, Perfil perfil) throws Exception {
		String email = nome.toLowerCase() + "-" + UUID.randomUUID() + "@taskly.dev";
		String cadastro = mvc.perform(MockMvcRequestBuilders.post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"nome":"%s","email":"%s","senha":"senha-forte"}""".formatted(nome, email)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		long id = ((Number) JsonPath.read(cadastro, "$.id")).longValue();
		usuarioRepository.findById(id).ifPresent(u -> {
			u.setPerfil(perfil);
			usuarioRepository.save(u);
		});
		String login = mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"email":"%s","senha":"senha-forte"}""".formatted(email)))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		return new Conta(id, JsonPath.read(login, "$.accessToken"));
	}

	protected ResultActions get(Conta conta, String url, Object... vars) throws Exception {
		return mvc.perform(autenticado(MockMvcRequestBuilders.get(url, vars), conta));
	}

	protected ResultActions delete(Conta conta, String url, Object... vars) throws Exception {
		return mvc.perform(autenticado(MockMvcRequestBuilders.delete(url, vars), conta));
	}

	protected ResultActions post(Conta conta, String url, String json, Object... vars) throws Exception {
		return mvc.perform(comCorpo(MockMvcRequestBuilders.post(url, vars), conta, json));
	}

	protected ResultActions put(Conta conta, String url, String json, Object... vars) throws Exception {
		return mvc.perform(comCorpo(MockMvcRequestBuilders.put(url, vars), conta, json));
	}

	protected ResultActions patch(Conta conta, String url, String json, Object... vars) throws Exception {
		return mvc.perform(comCorpo(MockMvcRequestBuilders.patch(url, vars), conta, json));
	}

	/** Lê um id numérico da resposta. */
	protected static long id(ResultActions resposta, String caminho) throws Exception {
		return ((Number) JsonPath.read(resposta.andReturn().getResponse().getContentAsString(), caminho)).longValue();
	}

	private static MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder req, Conta conta) {
		return req.header(HttpHeaders.AUTHORIZATION, "Bearer " + conta.token());
	}

	private static MockHttpServletRequestBuilder comCorpo(MockHttpServletRequestBuilder req, Conta conta,
			String json) {
		return autenticado(req, conta).contentType(MediaType.APPLICATION_JSON).content(json);
	}

}
