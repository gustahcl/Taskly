import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { vi } from 'vitest';

import { tokens, USUARIO_MEMBRO } from '../../../core/auth/testing';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let http: HttpTestingController;
  let elemento: HTMLElement;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    fixture = TestBed.createComponent(Login);
    http = TestBed.inject(HttpTestingController);
    elemento = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  function preencher(email: string, senha: string): void {
    const [campoEmail, campoSenha] = Array.from(elemento.querySelectorAll('input'));
    campoEmail.value = email;
    campoEmail.dispatchEvent(new Event('input'));
    campoSenha.value = senha;
    campoSenha.dispatchEvent(new Event('input'));
  }

  function enviar(): void {
    elemento.querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  it('não chama a API com o formulário inválido', () => {
    preencher('nao-e-email', '');
    enviar();

    http.expectNone('/api/auth/login');
    expect(elemento.textContent).toContain('E-mail inválido');
  });

  it('entra e navega para o início', () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    preencher('ana@taskly.dev', 'senha-forte');
    enviar();

    http.expectOne('/api/auth/login').flush(tokens());
    http.expectOne('/api/usuarios/me').flush(USUARIO_MEMBRO);

    expect(navegar).toHaveBeenCalledWith('/');
  });

  it('mostra a mensagem da API quando as credenciais estão erradas', () => {
    preencher('ana@taskly.dev', 'errada');
    enviar();

    http
      .expectOne('/api/auth/login')
      .flush({ status: 401, detail: 'E-mail ou senha inválidos' }, { status: 401, statusText: 'Unauthorized' });
    fixture.detectChanges();

    expect(elemento.querySelector('[role="alert"]')?.textContent).toContain('E-mail ou senha inválidos');
  });
});
