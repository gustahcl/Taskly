import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';

import { AuthService } from './auth.service';

/** Rotas de autenticação: não levam token e um 401 nelas não dispara refresh. */
const ROTAS_PUBLICAS = ['/api/auth/'];

/**
 * Anexa o access token nas chamadas à API. Se a API responder 401, tenta renovar
 * o token uma vez e repete a requisição; se a renovação falhar, encerra a sessão.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  if (!req.url.startsWith('/api/') || ROTAS_PUBLICAS.some((rota) => req.url.startsWith(rota))) {
    return next(req);
  }

  const token = auth.accessToken;
  return next(comToken(req, token)).pipe(
    catchError((erro: unknown) => {
      if (!(erro instanceof HttpErrorResponse) || erro.status !== 401 || !auth.refreshToken) {
        return throwError(() => erro);
      }
      return auth.renovar().pipe(
        catchError((erroRenovacao: unknown) => {
          auth.encerrarSessao();
          return throwError(() => erroRenovacao);
        }),
        switchMap((novoToken) => next(comToken(req, novoToken))),
      );
    }),
  );
};

function comToken(req: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  return token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
}
