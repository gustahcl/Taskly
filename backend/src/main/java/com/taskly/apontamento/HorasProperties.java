package com.taskly.apontamento;

import java.time.Duration;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Regras do apontamento de horas.
 *
 * @param limiteDiario máximo que uma pessoa pode apontar num mesmo dia, somando todas as tarefas
 * @param fuso fuso horário usado para saber que dia é "hoje" (não se apontam horas no futuro)
 */
@ConfigurationProperties(prefix = "taskly.horas")
public record HorasProperties(Duration limiteDiario, ZoneId fuso) {

	public HorasProperties {
		if (limiteDiario == null) {
			limiteDiario = Duration.ofHours(12);
		}
		if (limiteDiario.isNegative() || limiteDiario.isZero() || limiteDiario.compareTo(Duration.ofHours(24)) > 0) {
			throw new IllegalArgumentException("taskly.horas.limite-diario precisa estar entre 1 minuto e 24 horas");
		}
		if (fuso == null) {
			fuso = ZoneId.of("America/Sao_Paulo");
		}
	}

	public int limiteDiarioEmMinutos() {
		return (int) limiteDiario.toMinutes();
	}

}
