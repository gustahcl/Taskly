package com.taskly.common;

/** A requisição é válida no formato, mas viola uma regra do domínio (HTTP 422). */
public class RegraDeNegocioException extends RuntimeException {

	public RegraDeNegocioException(String mensagem) {
		super(mensagem);
	}

}
