import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { mensagemDeErro } from '../../../core/auth/mensagem-erro';
import { formatarDuracao, hojeIso, lerDuracao } from '../../../core/horas/duracao';
import { Apontamento, TarefaOpcao } from '../../../core/horas/horas.models';
import { HorasService } from '../../../core/horas/horas.service';
import { ProjetoResumo } from '../../../core/projetos/projetos.models';
import { ProjetosService } from '../../../core/projetos/projetos.service';

/** Tarefa já definida (quando o modal é aberto a partir dela). */
export interface TarefaFixa {
  id: number;
  titulo: string;
  projetoNome: string;
}

/** Tamanho padrão do modal. */
export const TAMANHO_APONTAMENTO_DIALOG = { width: '560px', maxWidth: '95vw' };

export interface DadosApontamentoDialog {
  /** Editar este apontamento. */
  apontamento?: Apontamento;
  /** Apontar nesta tarefa; sem ela, a pessoa escolhe projeto e tarefa. */
  tarefa?: TarefaFixa;
  /** Data sugerida (yyyy-MM-dd); padrão: hoje. */
  data?: string;
}

function duracaoValida(controle: AbstractControl<string>): ValidationErrors | null {
  if (!controle.value) {
    return null;
  }
  const minutos = lerDuracao(controle.value);
  if (minutos === null) {
    return { duracao: true };
  }
  return minutos > 24 * 60 ? { maximo: true } : null;
}

/** Aponta ou edita horas: data, duração (ex.: 1h30) e descrição. */
@Component({
  selector: 'app-apontamento-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './apontamento-dialog.html',
  styles: `
    .campos {
      display: grid;
      grid-template-columns: 1fr 1fr;
      column-gap: 12px;
      padding-top: 4px;
    }
    .inteiro {
      grid-column: 1 / -1;
    }
    .fixa {
      grid-column: 1 / -1;
      margin: 0 0 16px;
      color: var(--mat-sys-on-surface-variant);
    }
    .erro {
      padding: 12px;
      border-radius: 8px;
      background: var(--mat-sys-error-container);
      color: var(--mat-sys-on-error-container);
    }
    @media (max-width: 599px) {
      .campos {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class ApontamentoDialog implements OnInit {
  protected readonly dados = inject<DadosApontamentoDialog>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<ApontamentoDialog, Apontamento>);
  private readonly horas = inject(HorasService);
  private readonly projetosService = inject(ProjetosService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly hoje = hojeIso();
  protected readonly edicao = !!this.dados.apontamento;
  /** Tarefa já conhecida: a do apontamento em edição ou a recebida. */
  protected readonly tarefaFixa: TarefaFixa | null = this.dados.apontamento
    ? {
        id: this.dados.apontamento.tarefa.id,
        titulo: this.dados.apontamento.tarefa.titulo,
        projetoNome: this.dados.apontamento.tarefa.projetoNome,
      }
    : (this.dados.tarefa ?? null);

  protected readonly projetos = signal<ProjetoResumo[]>([]);
  protected readonly tarefas = signal<TarefaOpcao[]>([]);
  protected readonly carregandoTarefas = signal(false);
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly projetoId = new FormControl<number | null>(null);
  protected readonly form = inject(NonNullableFormBuilder).group({
    tarefaId: new FormControl<number | null>(this.tarefaFixa?.id ?? null, Validators.required),
    data: [this.dados.apontamento?.data ?? this.dados.data ?? this.hoje, Validators.required],
    duracao: [
      this.dados.apontamento ? formatarDuracao(this.dados.apontamento.minutos) : '',
      [Validators.required, duracaoValida],
    ],
    descricao: [this.dados.apontamento?.descricao ?? '', Validators.maxLength(500)],
  });

  ngOnInit(): void {
    if (this.tarefaFixa) {
      return;
    }
    this.projetosService.listar({ pagina: 0, tamanho: 100, ordenacao: 'nome,asc' }).subscribe({
      // Só projetos de que a pessoa participa: é onde ela pode apontar horas.
      next: (pagina) => this.projetos.set(pagina.conteudo.filter((p) => p.meuPapel !== null)),
      error: (erro: unknown) => this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar os projetos.')),
    });
    this.projetoId.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((id) => {
      this.form.controls.tarefaId.setValue(null);
      this.tarefas.set([]);
      if (id) {
        this.carregarTarefas(id);
      }
    });
  }

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.projetoId.markAsTouched();
      return;
    }
    const valor = this.form.getRawValue();
    const corpo = {
      data: valor.data,
      minutos: lerDuracao(valor.duracao)!,
      descricao: valor.descricao.trim() || null,
    };
    const requisicao = this.dados.apontamento
      ? this.horas.atualizar(this.dados.apontamento.id, corpo)
      : this.horas.registrar(valor.tarefaId!, corpo);
    this.salvando.set(true);
    this.erro.set(null);
    requisicao.subscribe({
      next: (apontamento) => this.ref.close(apontamento),
      error: (erro: unknown) => {
        this.erro.set(mensagemDeErro(erro, 'Não foi possível salvar as horas.'));
        this.salvando.set(false);
      },
    });
  }

  private carregarTarefas(projetoId: number): void {
    this.carregandoTarefas.set(true);
    this.horas.tarefasDoProjeto(projetoId).subscribe({
      next: (tarefas) => {
        this.tarefas.set(tarefas);
        this.carregandoTarefas.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar as tarefas.'));
        this.carregandoTarefas.set(false);
      },
    });
  }
}
