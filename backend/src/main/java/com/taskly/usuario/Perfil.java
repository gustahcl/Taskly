package com.taskly.usuario;

/**
 * Perfis de acesso da plataforma.
 * <ul>
 *   <li>ADMIN: gerencia usuários e tem acesso total;</li>
 *   <li>GERENTE: cria e administra projetos;</li>
 *   <li>MEMBRO: participa dos projetos para os quais foi convidado.</li>
 * </ul>
 */
public enum Perfil {
	ADMIN,
	GERENTE,
	MEMBRO
}
