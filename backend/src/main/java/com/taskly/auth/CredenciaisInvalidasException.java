package com.taskly.auth;

public class CredenciaisInvalidasException extends RuntimeException {

	public CredenciaisInvalidasException(String mensagem) {
		super(mensagem);
	}

}
