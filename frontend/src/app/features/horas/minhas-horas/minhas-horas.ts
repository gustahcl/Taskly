import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';

import { mensagemDeErro } from '../../../core/auth/mensagem-erro';
import { hojeIso, inicioDaSemana, nomeDoDia, somarDias } from '../../../core/horas/duracao';
import { DuracaoPipe } from '../../../core/horas/duracao.pipe';
import { Apontamento, MinhasHoras as Semana } from '../../../core/horas/horas.models';
import { HorasService } from '../../../core/horas/horas.service';
import { ConfirmarDialog, DadosConfirmarDialog } from '../../../shared/dialogos/confirmar-dialog';
import {
  ApontamentoDialog,
  DadosApontamentoDialog,
  TAMANHO_APONTAMENTO_DIALOG,
} from '../apontamento-dialog/apontamento-dialog';

interface Dia {
  data: string;
  nome: string;
  minutos: number;
  /** Fração do limite diário, de 0 a 1 (para a barra). */
  proporcao: number;
  hoje: boolean;
  futuro: boolean;
  apontamentos: Apontamento[];
}

/** Apontamento semanal: total de cada dia, lista de horas e atalhos para apontar, editar e excluir. */
@Component({
  selector: 'app-minhas-horas',
  imports: [DatePipe, RouterLink, DuracaoPipe, MatButtonModule, MatIconModule, MatProgressBarModule, MatTooltipModule],
  templateUrl: './minhas-horas.html',
  styleUrl: './minhas-horas.scss',
})
export class MinhasHoras implements OnInit {
  private readonly service = inject(HorasService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly hoje = hojeIso();
  protected readonly inicio = signal(inicioDaSemana(this.hoje));
  protected readonly fim = computed(() => somarDias(this.inicio(), 6));
  protected readonly semanaAtual = computed(() => this.inicio() === inicioDaSemana(this.hoje));
  protected readonly semana = signal<Semana | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);

  protected readonly dias = computed<Dia[]>(() => {
    const semana = this.semana();
    if (!semana) {
      return [];
    }
    return semana.dias.map((d) => ({
      data: d.data,
      nome: nomeDoDia(d.data),
      minutos: d.minutos,
      proporcao: Math.min(d.minutos / semana.limiteDiarioMinutos, 1),
      hoje: d.data === this.hoje,
      futuro: d.data > this.hoje,
      apontamentos: semana.apontamentos.filter((a) => a.data === d.data),
    }));
  });

  /** Total da semana por projeto, do maior para o menor. */
  protected readonly porProjeto = computed(() => {
    const totais = new Map<string, number>();
    for (const a of this.semana()?.apontamentos ?? []) {
      totais.set(a.tarefa.projetoNome, (totais.get(a.tarefa.projetoNome) ?? 0) + a.minutos);
    }
    return [...totais.entries()].map(([nome, minutos]) => ({ nome, minutos })).sort((a, b) => b.minutos - a.minutos);
  });

  ngOnInit(): void {
    this.carregar();
  }

  protected mudarSemana(semanas: number): void {
    this.inicio.update((inicio) => somarDias(inicio, semanas * 7));
    this.carregar();
  }

  protected irParaHoje(): void {
    this.inicio.set(inicioDaSemana(this.hoje));
    this.carregar();
  }

  protected apontar(data?: string): void {
    const sugestao = data ?? (this.semanaAtual() ? this.hoje : this.inicio());
    this.abrir({ data: sugestao }, 'Horas apontadas.');
  }

  protected editar(apontamento: Apontamento): void {
    this.abrir({ apontamento }, 'Horas atualizadas.');
  }

  protected excluir(apontamento: Apontamento): void {
    this.dialog
      .open<ConfirmarDialog, DadosConfirmarDialog, boolean>(ConfirmarDialog, {
        data: {
          titulo: 'Excluir horas',
          mensagem: `Excluir o apontamento em "${apontamento.tarefa.titulo}"?`,
          confirmar: 'Excluir',
        },
      })
      .afterClosed()
      .pipe(
        filter(Boolean),
        switchMap(() => this.service.excluir(apontamento.id)),
      )
      .subscribe({
        next: () => {
          this.snackBar.open('Apontamento excluído.', 'OK', { duration: 3000 });
          this.carregar();
        },
        error: (erro: unknown) =>
          this.snackBar.open(mensagemDeErro(erro, 'Não foi possível excluir.'), 'OK', { duration: 5000 }),
      });
  }

  private abrir(dados: DadosApontamentoDialog, mensagem: string): void {
    this.dialog
      .open<ApontamentoDialog, DadosApontamentoDialog, Apontamento>(ApontamentoDialog, { ...TAMANHO_APONTAMENTO_DIALOG, data: dados })
      .afterClosed()
      .pipe(filter(Boolean))
      .subscribe((apontamento) => {
        this.snackBar.open(mensagem, 'OK', { duration: 3000 });
        // Se a data caiu em outra semana, mostra aquela semana.
        this.inicio.set(inicioDaSemana(apontamento.data));
        this.carregar();
      });
  }

  private carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.service.minhas(this.inicio(), this.fim()).subscribe({
      next: (semana) => {
        this.semana.set(semana);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar suas horas.'));
        this.carregando.set(false);
      },
    });
  }
}
