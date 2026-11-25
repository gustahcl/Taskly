package com.taskly.common;

/** O usuário enxerga o recurso, mas não tem permissão para a operação (HTTP 403). */
public class AcessoNegadoException extends RuntimeException {

	public AcessoNegadoException(String mensagem) {
		super(mensagem);
	}

}
