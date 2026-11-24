import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Perfil } from './auth.models';
import { AuthService } from './auth.service';

/** Só deixa passar usuários logados; os demais vão para o login. */
export const autenticadoGuard: CanActivateFn = (_rota, estado) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.autenticado() ? true : router.createUrlTree(['/login'], { queryParams: { voltarPara: estado.url } });
};

/** Telas de login e cadastro: quem já está logado vai direto para o início. */
export const visitanteGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.autenticado() ? router.createUrlTree(['/']) : true;
};

/** Restringe a rota aos perfis informados. Use junto com {@link autenticadoGuard}. */
export function perfilGuard(...perfis: Perfil[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    return auth.temPerfil(...perfis) ? true : router.createUrlTree(['/acesso-negado']);
  };
}
