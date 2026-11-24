import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';

import { autenticadoGuard, perfilGuard, visitanteGuard } from './auth.guards';
import { salvarSessao, USUARIO_MEMBRO } from './testing';

describe('guards de rota', () => {
  const rota = {} as ActivatedRouteSnapshot;
  const estado = { url: '/usuarios' } as RouterStateSnapshot;

  function configurar(): Router {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideRouter([])] });
    return TestBed.inject(Router);
  }

  function executar(guard: typeof autenticadoGuard): unknown {
    return TestBed.runInInjectionContext(() => guard(rota, estado));
  }

  beforeEach(() => localStorage.clear());

  it('autenticadoGuard manda o visitante para o login guardando a URL de volta', () => {
    const router = configurar();
    const resultado = executar(autenticadoGuard) as UrlTree;
    expect(router.serializeUrl(resultado)).toBe('/login?voltarPara=%2Fusuarios');
  });

  it('autenticadoGuard libera quem está logado', () => {
    salvarSessao();
    configurar();
    expect(executar(autenticadoGuard)).toBe(true);
  });

  it('visitanteGuard tira quem já está logado das telas de login', () => {
    salvarSessao();
    const router = configurar();
    expect(router.serializeUrl(executar(visitanteGuard) as UrlTree)).toBe('/');
  });

  it('perfilGuard bloqueia perfis não permitidos', () => {
    salvarSessao(USUARIO_MEMBRO);
    const router = configurar();
    const resultado = executar(perfilGuard('ADMIN')) as UrlTree;
    expect(router.serializeUrl(resultado)).toBe('/acesso-negado');
  });

  it('perfilGuard libera os perfis permitidos', () => {
    salvarSessao({ ...USUARIO_MEMBRO, perfil: 'ADMIN' });
    configurar();
    expect(executar(perfilGuard('ADMIN', 'GERENTE'))).toBe(true);
  });
});
