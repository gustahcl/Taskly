package com.taskly.usuario;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Cria o primeiro ADMIN na subida da aplicação, se ainda não houver nenhum e se
 * {@code TASKLY_ADMIN_EMAIL} e {@code TASKLY_ADMIN_SENHA} estiverem definidos.
 */
@Component
public class AdminInicial implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final String email;
	private final String senha;

	public AdminInicial(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
			@Value("${taskly.admin.email:}") String email, @Value("${taskly.admin.senha:}") String senha) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.email = email;
		this.senha = senha;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!StringUtils.hasText(email) || !StringUtils.hasText(senha) || usuarioRepository.existsByPerfil(Perfil.ADMIN)) {
			return;
		}
		String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
		usuarioRepository.findByEmailIgnoreCase(emailNormalizado).ifPresentOrElse(
				usuario -> usuario.setPerfil(Perfil.ADMIN),
				() -> usuarioRepository.save(new Usuario("Administrador", emailNormalizado, passwordEncoder.encode(senha),
						Perfil.ADMIN)));
		log.info("Usuário ADMIN inicial configurado: {}", emailNormalizado);
	}

}
