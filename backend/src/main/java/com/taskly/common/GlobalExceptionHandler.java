package com.taskly.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.taskly.auth.CredenciaisInvalidasException;

/**
 * Converte as exceções de negócio em respostas no formato Problem Details (RFC 9457).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	public ProblemDetail naoEncontrado(RecursoNaoEncontradoException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(ConflitoException.class)
	public ProblemDetail conflito(ConflitoException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(CredenciaisInvalidasException.class)
	public ProblemDetail credenciaisInvalidas(CredenciaisInvalidasException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail validacao(MethodArgumentNotValidException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos");
		Map<String, String> erros = new LinkedHashMap<>();
		for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
			erros.putIfAbsent(erro.getField(), erro.getDefaultMessage());
		}
		problema.setProperty("erros", erros);
		return problema;
	}

}
