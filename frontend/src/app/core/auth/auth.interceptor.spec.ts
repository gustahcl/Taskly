import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { vi } from 'vitest';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { salvarSessao, tokens } from './testing';

describe('authInterceptor', () => {
  let http: HttpTestingController;
  let client: HttpClient;

  beforeEach(() => {
    localStorage.clear();
    salvarSessao();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    client = TestBed.inject(HttpClient);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => http.verify());

  it('anexa o access token nas chamadas à API', () => {
    client.get('/api/usuarios/me').subscribe();
    const req = http.expectOne('/api/usuarios/me');
    expect(req.request.headers.get('Authorization')).toBe('Bearer access-salvo');
    req.flush({});
  });

  it('não envia token para as rotas de autenticação', () => {
    client.post('/api/auth/login', {}).subscribe();
    const req = http.expectOne('/api/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('em 401 renova o token e repete a requisição', () => {
    let resposta: unknown;
    client.get('/api/usuarios/me').subscribe((r) => (resposta = r));

    http.expectOne('/api/usuarios/me').flush(null, { status: 401, statusText: 'Unauthorized' });
    http.expectOne('/api/auth/refresh').flush(tokens('2'));
    const repetida = http.expectOne('/api/usuarios/me');
    expect(repetida.request.headers.get('Authorization')).toBe('Bearer access-2');
    repetida.flush({ ok: true });

    expect(resposta).toEqual({ ok: true });
  });

  it('se a renovação falhar, encerra a sessão', () => {
    const auth = TestBed.inject(AuthService);
    let falhou = false;
    client.get('/api/usuarios/me').subscribe({ error: () => (falhou = true) });

    http.expectOne('/api/usuarios/me').flush(null, { status: 401, statusText: 'Unauthorized' });
    http.expectOne('/api/auth/refresh').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(falhou).toBe(true);
    expect(auth.autenticado()).toBe(false);
  });

  it('não tenta renovar em erros que não são 401', () => {
    let status = 0;
    client.get('/api/usuarios').subscribe({ error: (e) => (status = e.status) });

    http.expectOne('/api/usuarios').flush(null, { status: 403, statusText: 'Forbidden' });

    expect(status).toBe(403);
  });
});
