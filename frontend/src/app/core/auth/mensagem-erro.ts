import { HttpErrorResponse } from '@angular/common/http';

import { ProblemDetail } from './auth.models';

/** Extrai uma mensagem legível de um erro HTTP da API. */
export function mensagemDeErro(erro: unknown, padrao = 'Algo deu errado. Tente novamente.'): string {
  if (erro instanceof HttpErrorResponse) {
    if (erro.status === 0) {
      return 'Não foi possível falar com o servidor. Verifique se a API está rodando.';
    }
    const corpo = erro.error as ProblemDetail | null;
    if (corpo?.detail) {
      return corpo.detail;
    }
  }
  return padrao;
}
