import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';

import { filter } from 'rxjs';

import { mensagemDeErro } from '../../../core/auth/mensagem-erro';
import { DuracaoPipe } from '../../../core/horas/duracao.pipe';
import { Apontamento, HorasDaTarefa } from '../../../core/horas/horas.models';
import { HorasService } from '../../../core/horas/horas.service';
import {
  Comentario,
  Membro,
  NOMES_PRIORIDADE,
  Prioridade,
  PRIORIDADES,
  Tarefa,
} from '../../../core/projetos/projetos.models';
import { QuadrosService } from '../../../core/projetos/quadros.service';
import {
  ApontamentoDialog,
  DadosApontamentoDialog,
  TAMANHO_APONTAMENTO_DIALOG,
} from '../../horas/apontamento-dialog/apontamento-dialog';

export interface DadosTarefaDialog {
  tarefaId: number;
  membros: Membro[];
  projetoNome: string;
}

/** Como o modal terminou: o quadro recarrega se algo mudou. */
export type ResultadoTarefaDialog = 'alterada' | 'excluida' | undefined;

/** Detalhes da tarefa: edição dos campos, responsável, horas apontadas, comentários e exclusão. */
@Component({
  selector: 'app-tarefa-dialog',
  imports: [
    DatePipe,
    DuracaoPipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTooltipModule,
  ],
  templateUrl: './tarefa-dialog.html',
  styleUrl: './tarefa-dialog.scss',
})
export class TarefaDialog implements OnInit {
  protected readonly dados = inject<DadosTarefaDialog>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<TarefaDialog, ResultadoTarefaDialog>);
  private readonly service = inject(QuadrosService);
  private readonly horasService = inject(HorasService);
  private readonly dialog = inject(MatDialog);

  protected readonly prioridades = PRIORIDADES;
  protected readonly nomesPrioridade = NOMES_PRIORIDADE;

  protected readonly tarefa = signal<Tarefa | null>(null);
  protected readonly comentarios = signal<Comentario[]>([]);
  protected readonly horas = signal<HorasDaTarefa | null>(null);
  protected readonly erro = signal<string | null>(null);
  protected readonly salvando = signal(false);
  protected readonly confirmandoExclusao = signal(false);
  private alterada = false;

  protected readonly form = inject(NonNullableFormBuilder).group({
    titulo: ['', [Validators.required, Validators.maxLength(200), Validators.pattern(/\S/)]],
    descricao: ['', Validators.maxLength(5000)],
    responsavelId: new FormControl<number | null>(null),
    prioridade: ['MEDIA' as Prioridade],
    prazo: [''],
  });
  protected readonly novoComentario = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(2000), Validators.pattern(/\S/)],
  });

  ngOnInit(): void {
    this.service.buscarTarefa(this.dados.tarefaId).subscribe({
      next: (tarefa) => this.preencher(tarefa),
      error: (erro: unknown) => this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar a tarefa.')),
    });
    this.service.comentarios(this.dados.tarefaId).subscribe({
      next: (comentarios) => this.comentarios.set(comentarios),
    });
    this.carregarHoras();
  }

  protected apontarHoras(apontamento?: Apontamento): void {
    const tarefa = this.tarefa();
    if (!tarefa) {
      return;
    }
    this.dialog
      .open<ApontamentoDialog, DadosApontamentoDialog, Apontamento>(ApontamentoDialog, {
        ...TAMANHO_APONTAMENTO_DIALOG,
        data: apontamento
          ? { apontamento }
          : { tarefa: { id: tarefa.id, titulo: tarefa.titulo, projetoNome: this.dados.projetoNome } },
      })
      .afterClosed()
      .pipe(filter(Boolean))
      .subscribe(() => this.carregarHoras());
  }

  protected excluirHoras(apontamento: Apontamento): void {
    this.horasService.excluir(apontamento.id).subscribe({
      next: () => this.carregarHoras(),
      error: (erro: unknown) => this.erro.set(mensagemDeErro(erro, 'Não foi possível excluir as horas.')),
    });
  }

  private carregarHoras(): void {
    this.horasService.daTarefa(this.dados.tarefaId).subscribe({
      next: (horas) => this.horas.set(horas),
    });
  }

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const valor = this.form.getRawValue();
    this.salvando.set(true);
    this.erro.set(null);
    this.service
      .atualizarTarefa(this.dados.tarefaId, {
        titulo: valor.titulo.trim(),
        descricao: valor.descricao.trim() || null,
        responsavelId: valor.responsavelId,
        prioridade: valor.prioridade,
        prazo: valor.prazo || null,
      })
      .subscribe({
        next: () => this.ref.close('alterada'),
        error: (erro: unknown) => {
          this.erro.set(mensagemDeErro(erro, 'Não foi possível salvar a tarefa.'));
          this.salvando.set(false);
        },
      });
  }

  protected comentar(): void {
    if (this.novoComentario.invalid) {
      return;
    }
    this.service.comentar(this.dados.tarefaId, this.novoComentario.value.trim()).subscribe({
      next: (comentario) => {
        this.comentarios.update((lista) => [...lista, comentario]);
        this.novoComentario.reset();
        this.alterada = true;
      },
      error: (erro: unknown) => this.erro.set(mensagemDeErro(erro, 'Não foi possível comentar.')),
    });
  }

  protected excluirComentario(comentario: Comentario): void {
    this.service.excluirComentario(comentario.id).subscribe({
      next: () => {
        this.comentarios.update((lista) => lista.filter((c) => c.id !== comentario.id));
        this.alterada = true;
      },
      error: (erro: unknown) => this.erro.set(mensagemDeErro(erro, 'Não foi possível excluir o comentário.')),
    });
  }

  protected excluir(): void {
    this.service.excluirTarefa(this.dados.tarefaId).subscribe({
      next: () => this.ref.close('excluida'),
      error: (erro: unknown) => {
        this.confirmandoExclusao.set(false);
        this.erro.set(mensagemDeErro(erro, 'Não foi possível excluir a tarefa.'));
      },
    });
  }

  protected fechar(): void {
    this.ref.close(this.alterada ? 'alterada' : undefined);
  }

  private preencher(tarefa: Tarefa): void {
    this.tarefa.set(tarefa);
    this.form.setValue({
      titulo: tarefa.titulo,
      descricao: tarefa.descricao ?? '',
      responsavelId: tarefa.responsavel?.id ?? null,
      prioridade: tarefa.prioridade,
      prazo: tarefa.prazo ?? '',
    });
  }
}
