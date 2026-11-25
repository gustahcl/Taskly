import { Component, inject, signal } from '@angular/core';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { mensagemDeErro } from '../../../core/auth/mensagem-erro';
import { NOMES_STATUS, Projeto, ProjetoRequest, STATUS } from '../../../core/projetos/projetos.models';
import { ProjetosService } from '../../../core/projetos/projetos.service';

/** Data de fim não pode ser anterior à de início (a API também valida). */
function periodoValido(grupo: AbstractControl): ValidationErrors | null {
  const inicio = grupo.get('dataInicio')?.value as string;
  const fim = grupo.get('dataFim')?.value as string;
  return inicio && fim && fim < inicio ? { periodo: true } : null;
}

/** Cria um projeto ou edita o recebido. Fecha com o projeto salvo. */
@Component({
  selector: 'app-projeto-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './projeto-dialog.html',
  styleUrl: './projeto-dialog.scss',
})
export class ProjetoDialog {
  protected readonly projeto = inject<Projeto | null>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<ProjetoDialog, Projeto>);
  private readonly service = inject(ProjetosService);

  protected readonly status = STATUS;
  protected readonly nomesStatus = NOMES_STATUS;
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group(
    {
      nome: [this.projeto?.nome ?? '', [Validators.required, Validators.maxLength(120)]],
      cliente: [this.projeto?.cliente ?? '', Validators.maxLength(120)],
      descricao: [this.projeto?.descricao ?? '', Validators.maxLength(2000)],
      status: [this.projeto?.status ?? STATUS[0]],
      dataInicio: [this.projeto?.dataInicio ?? ''],
      dataFim: [this.projeto?.dataFim ?? ''],
    },
    { validators: periodoValido },
  );

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const valor = this.form.getRawValue();
    const request: ProjetoRequest = {
      nome: valor.nome.trim(),
      cliente: valor.cliente.trim() || null,
      descricao: valor.descricao.trim() || null,
      status: valor.status,
      dataInicio: valor.dataInicio || null,
      dataFim: valor.dataFim || null,
    };
    this.salvando.set(true);
    this.erro.set(null);
    const chamada = this.projeto
      ? this.service.atualizar(this.projeto.id, request)
      : this.service.criar(request);
    chamada.subscribe({
      next: (salvo) => this.ref.close(salvo),
      error: (erro: unknown) => {
        this.erro.set(mensagemDeErro(erro, 'Não foi possível salvar o projeto.'));
        this.salvando.set(false);
      },
    });
  }
}
