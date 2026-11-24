import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { vi } from 'vitest';

import { AuthService } from './auth.service';
import { salvarSessao, tokens, USUARIO_MEMBRO } from './testing';

describe('AuthService', () => {
  let http: HttpTestingController;

  function criar(): AuthService {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    return TestBed.inject(AuthService);
  }

  beforeEach(() => localStorage.clear());
  afterEach(() => http.verify());

  it('começa deslogado quando não há sessão salva', () => {
    const auth = criar();
    expect(auth.autenticado()).toBe(false);
    expect(auth.usuario()).toBeNull();
  });

  it('restaura a sessão salva no localStorage', () => {
    salvarSessao();
    const auth = criar();
    expect(auth.autenticado()).toBe(true);
    expect(auth.usuario()?.email).toBe('ana@taskly.dev');
  });

  it('login salva os tokens e carrega o usuário', () => {
    const auth = criar();
    let resultado: unknown;

    auth.login({ email: 'ana@taskly.dev', senha: 'senha-forte' }).subscribe((u) => (resultado = u));
    const login = http.expectOne('/api/auth/login');
    expect(login.request.body).toEqual({ email: 'ana@taskly.dev', senha: 'senha-forte' });
    login.flush(tokens());
    http.expectOne('/api/usuarios/me').flush(USUARIO_MEMBRO);

    expect(resultado).toEqual(USUARIO_MEMBRO);
    expect(auth.accessToken).toBe('access-1');
    expect(auth.refreshToken).toBe('refresh-1');
    expect(auth.autenticado()).toBe(true);
    expect(auth.temPerfil('MEMBRO')).toBe(true);
    expect(auth.temPerfil('ADMIN', 'GERENTE')).toBe(false);
  });

  it('renovações simultâneas fazem uma única chamada ao back-end', () => {
    salvarSessao();
    const auth = criar();
    const recebidos: string[] = [];

    auth.renovar().subscribe((t) => recebidos.push(t));
    auth.renovar().subscribe((t) => recebidos.push(t));
    const refresh = http.expectOne('/api/auth/refresh');
    expect(refresh.request.body).toEqual({ refreshToken: 'refresh-salvo' });
    refresh.flush(tokens('2'));

    expect(recebidos).toEqual(['access-2', 'access-2']);
    expect(auth.refreshToken).toBe('refresh-2');
  });

  it('logout revoga o refresh token, limpa a sessão e volta para o login', () => {
    salvarSessao();
    const auth = criar();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    auth.logout();

    expect(http.expectOne('/api/auth/logout').request.body).toEqual({ refreshToken: 'refresh-salvo' });
    expect(auth.autenticado()).toBe(false);
    expect(localStorage.getItem('taskly.accessToken')).toBeNull();
    expect(navegar).toHaveBeenCalledWith(['/login']);
  });
});
