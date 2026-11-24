package com.taskly.usuario;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.taskly.TestData;
import com.taskly.config.SecurityConfig;

/** Testes de autorização das rotas de usuário (sem banco). */
@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class)
class UsuarioControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private UsuarioService usuarioService;

	@Test
	void semTokenRetorna401() throws Exception {
		mvc.perform(get("/api/usuarios/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void meRetornaOUsuarioDoToken() throws Exception {
		when(usuarioService.buscarPorId(1L)).thenReturn(TestData.usuario(1L, Perfil.MEMBRO));

		mvc.perform(get("/api/usuarios/me").with(jwt().jwt(j -> j.subject("1")).authorities(role("MEMBRO"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("ana@taskly.dev"))
			.andExpect(jsonPath("$.perfil").value("MEMBRO"))
			.andExpect(jsonPath("$.senhaHash").doesNotExist());
	}

	@Test
	void membroNaoListaUsuarios() throws Exception {
		mvc.perform(get("/api/usuarios").with(jwt().authorities(role("MEMBRO")))).andExpect(status().isForbidden());
	}

	@Test
	void gerenteNaoListaUsuarios() throws Exception {
		mvc.perform(get("/api/usuarios").with(jwt().authorities(role("GERENTE")))).andExpect(status().isForbidden());
	}

	@Test
	void adminListaUsuarios() throws Exception {
		when(usuarioService.listar()).thenReturn(List.of(TestData.usuario(1L, Perfil.MEMBRO)));

		mvc.perform(get("/api/usuarios").with(jwt().authorities(role("ADMIN"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(1));
	}

	@Test
	void adminAlteraPerfil() throws Exception {
		when(usuarioService.alterarPerfil(1L, Perfil.GERENTE)).thenReturn(TestData.usuario(1L, Perfil.GERENTE));

		mvc.perform(patch("/api/usuarios/1/perfil").with(jwt().authorities(role("ADMIN")))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"perfil\":\"GERENTE\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.perfil").value("GERENTE"));
	}

	@Test
	void alterarPerfilSemPerfilNoCorpoRetorna400() throws Exception {
		mvc.perform(patch("/api/usuarios/1/perfil").with(jwt().authorities(role("ADMIN")))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erros.perfil").exists());
	}

	private static SimpleGrantedAuthority role(String perfil) {
		return new SimpleGrantedAuthority("ROLE_" + perfil);
	}

}
