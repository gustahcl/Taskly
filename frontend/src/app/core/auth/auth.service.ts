import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize, map, Observable, shareReplay, switchMap, tap } from 'rxjs';

import { CadastroRequest, LoginRequest, Perfil, TokenResponse, Usuario } from './auth.models';

const CHAVE_ACCESS = 'taskly.accessToken';
const CHAVE_REFRESH = 'taskly.refreshToken';
const CHAVE_USUARIO = 'taskly.usuario';

/**
 * Guarda a sessão (tokens e usuário logado) e expõe o estado como signals.
 * A sessão fica no localStorage para sobreviver a um reload da página.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _usuario = signal<Usuario | null>(lerUsuario());
  private renovacaoEmAndamento: Observable<string> | null = null;

  readonly usuario = this._usuario.asReadonly();
  readonly autenticado = computed(() => this._usuario() !== null && this.accessToken !== null);

  get accessToken(): string | null {
    return localStorage.getItem(CHAVE_ACCESS);
  }

  get refreshToken(): string | null {
    return localStorage.getItem(CHAVE_REFRESH);
  }

  temPerfil(...perfis: Perfil[]): boolean {
    const usuario = this._usuario();
    return usuario !== null && perfis.includes(usuario.perfil);
  }

  login(request: LoginRequest): Observable<Usuario> {
    return this.http.post<TokenResponse>('/api/auth/login', request).pipe(
      tap((tokens) => this.salvarTokens(tokens)),
      switchMap(() => this.carregarUsuario()),
    );
  }

  cadastrar(request: CadastroRequest): Observable<Usuario> {
    return this.http.post<Usuario>('/api/auth/cadastro', request);
  }

  carregarUsuario(): Observable<Usuario> {
    return this.http.get<Usuario>('/api/usuarios/me').pipe(
      tap((usuario) => {
        localStorage.setItem(CHAVE_USUARIO, JSON.stringify(usuario));
        this._usuario.set(usuario);
      }),
    );
  }

  /**
   * Troca o refresh token por um par novo. Chamadas simultâneas compartilham a mesma
   * requisição, porque o refresh token é de uso único no back-end.
   */
  renovar(): Observable<string> {
    if (!this.renovacaoEmAndamento) {
      this.renovacaoEmAndamento = this.http
        .post<TokenResponse>('/api/auth/refresh', { refreshToken: this.refreshToken })
        .pipe(
          tap((tokens) => this.salvarTokens(tokens)),
          map((tokens) => tokens.accessToken),
          finalize(() => (this.renovacaoEmAndamento = null)),
          shareReplay(1),
        );
    }
    return this.renovacaoEmAndamento;
  }

  logout(): void {
    const refreshToken = this.refreshToken;
    if (refreshToken) {
      // Melhor esforço: a sessão local é encerrada mesmo se a API falhar.
      this.http.post('/api/auth/logout', { refreshToken }).subscribe({ error: () => undefined });
    }
    this.encerrarSessao();
  }

  /** Limpa a sessão local e volta para o login, sem chamar a API. */
  encerrarSessao(): void {
    localStorage.removeItem(CHAVE_ACCESS);
    localStorage.removeItem(CHAVE_REFRESH);
    localStorage.removeItem(CHAVE_USUARIO);
    this._usuario.set(null);
    this.router.navigate(['/login']);
  }

  private salvarTokens(tokens: TokenResponse): void {
    localStorage.setItem(CHAVE_ACCESS, tokens.accessToken);
    localStorage.setItem(CHAVE_REFRESH, tokens.refreshToken);
  }
}

function lerUsuario(): Usuario | null {
  const valor = localStorage.getItem(CHAVE_USUARIO);
  if (!valor || !localStorage.getItem(CHAVE_ACCESS)) {
    return null;
  }
  try {
    return JSON.parse(valor) as Usuario;
  } catch {
    return null;
  }
}
