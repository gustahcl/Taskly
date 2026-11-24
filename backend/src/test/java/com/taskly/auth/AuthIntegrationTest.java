package com.taskly.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.taskly.IntegrationTest;

/** Fluxo completo de autenticação contra um PostgreSQL real. */
@AutoConfigureMockMvc
class AuthIntegrationTest extends IntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void cadastroLoginRotaProtegidaRefreshELogout() throws Exception {
		String email = "user-" + UUID.randomUUID() + "@taskly.dev";

		mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
			.content(json("nome", "Ana", "email", email, "senha", "senha-forte")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.perfil").value("MEMBRO"));

		mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
			.content(json("nome", "Ana", "email", email, "senha", "senha-forte")))
			.andExpect(status().isConflict());

		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content(json("email", email, "senha", "errada")))
			.andExpect(status().isUnauthorized());

		String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content(json("email", email, "senha", "senha-forte")))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		String accessToken = JsonPath.read(login, "$.accessToken");
		String refreshToken = JsonPath.read(login, "$.refreshToken");

		mvc.perform(get("/api/usuarios/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value(email));

		mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isForbidden());

		mvc.perform(get("/api/usuarios/me").header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
			.andExpect(status().isUnauthorized());

		String renovado = mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
			.content(json("refreshToken", refreshToken)))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		String novoRefresh = JsonPath.read(renovado, "$.refreshToken");
		assertThat(novoRefresh).isNotEqualTo(refreshToken);

		// O refresh antigo já foi usado: reapresentá-lo invalida também o novo.
		mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
			.content(json("refreshToken", refreshToken)))
			.andExpect(status().isUnauthorized());
		mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
			.content(json("refreshToken", novoRefresh)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void logoutRevogaORefreshToken() throws Exception {
		String email = "user-" + UUID.randomUUID() + "@taskly.dev";
		mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
			.content(json("nome", "Bia", "email", email, "senha", "senha-forte")))
			.andExpect(status().isCreated());
		String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content(json("email", email, "senha", "senha-forte")))
			.andReturn().getResponse().getContentAsString();
		String refreshToken = JsonPath.read(login, "$.refreshToken");

		mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
			.content(json("refreshToken", refreshToken)))
			.andExpect(status().isNoContent());

		mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
			.content(json("refreshToken", refreshToken)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void cadastroInvalidoRetorna400ComErrosPorCampo() throws Exception {
		mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
			.content(json("nome", "", "email", "nao-e-email", "senha", "123")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros.nome").exists())
			.andExpect(jsonPath("$.erros.email").exists())
			.andExpect(jsonPath("$.erros.senha").exists());
	}

	private static String json(String... chavesEValores) {
		StringBuilder sb = new StringBuilder("{");
		for (int i = 0; i < chavesEValores.length; i += 2) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append('"').append(chavesEValores[i]).append("\":\"").append(chavesEValores[i + 1]).append('"');
		}
		return sb.append('}').toString();
	}

}
