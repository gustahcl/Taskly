import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';

import { ProblemDetail } from '../../../core/auth/auth.models';
import { AuthService } from '../../../core/auth/auth.service';
import { mensagemDeErro } from '../../../core/auth/mensagem-erro';

function senhasIguais(grupo: AbstractControl): ValidationErrors | null {
  const senha = grupo.get('senha')?.value;
  const confirmacao = grupo.get('confirmacao')?.value;
  return senha && confirmacao && senha !== confirmacao ? { senhasDiferentes: true } : null;
}

@Component({
  selector: 'app-cadastro',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './cadastro.html',
  styleUrl: '../auth-layout.scss',
})
export class Cadastro {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly form = inject(NonNullableFormBuilder).group(
    {
      nome: ['', [Validators.required, Validators.maxLength(120)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(160)]],
      senha: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
      confirmacao: ['', Validators.required],
    },
    { validators: senhasIguais },
  );
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected cadastrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { nome, email, senha } = this.form.getRawValue();
    this.enviando.set(true);
    this.erro.set(null);
    this.auth.cadastrar({ nome, email, senha }).subscribe({
      next: () => this.router.navigate(['/login'], { queryParams: { cadastrado: 1 } }),
      error: (erro: unknown) => {
        this.erro.set(this.mensagem(erro));
        this.enviando.set(false);
      },
    });
  }

  private mensagem(erro: unknown): string {
    const errosPorCampo = erro instanceof HttpErrorResponse ? (erro.error as ProblemDetail | null)?.erros : undefined;
    if (errosPorCampo && Object.keys(errosPorCampo).length > 0) {
      return Object.entries(errosPorCampo)
        .map(([campo, mensagem]) => `${campo}: ${mensagem}`)
        .join('; ');
    }
    return mensagemDeErro(erro, 'Não foi possível criar a conta.');
  }
}
