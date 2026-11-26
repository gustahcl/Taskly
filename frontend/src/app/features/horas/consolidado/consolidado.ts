import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { debounceTime } from 'rxjs';

import { AuthService } from '../../../core/auth/auth.service';
import { mensagemDeErro } from '../../../core/auth/mensagem-erro';
import { fimDoMes, hojeIso, inicioDaSemana, inicioDoMes, somarDias } from '../../../core/horas/duracao';
import { DuracaoPipe } from '../../../core/horas/duracao.pipe';
import { Consolidado as Dados } from '../../../core/horas/horas.models';
import { HorasService } from '../../../core/horas/horas.service';
import { ProjetoResumo } from '../../../core/projetos/projetos.models';
import { ProjetosService } from '../../../core/projetos/projetos.service';

export type Atalho = 'semana' | 'semana-passada' | 'mes' | 'mes-passado';

/** Período de cada atalho, a partir de hoje (yyyy-MM-dd). */
export function periodoDoAtalho(atalho: Atalho, hoje: string): { inicio: string; fim: string } {
  const segunda = inicioDaSemana(hoje);
  switch (atalho) {
    case 'semana':
      return { inicio: segunda, fim: somarDias(segunda, 6) };
    case 'semana-passada':
      return { inicio: somarDias(segunda, -7), fim: somarDias(segunda, -1) };
    case 'mes':
      return { inicio: inicioDoMes(hoje), fim: fimDoMes(hoje) };
    case 'mes-passado': {
      const ultimoDoMesPassado = somarDias(inicioDoMes(hoje), -1);
      return { inicio: inicioDoMes(ultimoDoMesPassado), fim: ultimoDoMesPassado };
    }
  }
}

/** Visão do gestor: horas por pessoa e por projeto no período, nos projetos que ele gere. */
@Component({
  selector: 'app-consolidado',
  imports: [
    DatePipe,
    DecimalPipe,
    DuracaoPipe,
    ReactiveFormsModule,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './consolidado.html',
  styleUrl: './consolidado.scss',
})
export class Consolidado implements OnInit {
  private readonly service = inject(HorasService);
  private readonly projetosService = inject(ProjetosService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly admin = inject(AuthService).temPerfil('ADMIN');

  protected readonly colunas = ['projeto', 'pessoa', 'horas', 'parcela'];
  protected readonly atalho = new FormControl<Atalho | null>('mes');
  protected readonly filtro = inject(NonNullableFormBuilder).group({
    inicio: [periodoDoAtalho('mes', hojeIso()).inicio],
    fim: [periodoDoAtalho('mes', hojeIso()).fim],
    projetoId: new FormControl<number | null>(null),
  });

  protected readonly projetos = signal<ProjetoResumo[]>([]);
  protected readonly dados = signal<Dados | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);

  protected readonly maiorPorPessoa = computed(() => Math.max(1, ...(this.dados()?.porPessoa.map((p) => p.minutos) ?? [])));
  protected readonly maiorPorProjeto = computed(() =>
    Math.max(1, ...(this.dados()?.porProjeto.map((p) => p.minutos) ?? [])),
  );
  /** Total de cada projeto, para a parcela de cada pessoa na tabela. */
  private readonly totalDoProjeto = computed(
    () => new Map((this.dados()?.porProjeto ?? []).map((p) => [p.projetoId, p.minutos])),
  );

  ngOnInit(): void {
    this.projetosService.listar({ pagina: 0, tamanho: 100, ordenacao: 'nome,asc' }).subscribe({
      next: (pagina) => this.projetos.set(pagina.conteudo.filter((p) => this.admin || p.meuPapel === 'GESTOR')),
    });
    this.atalho.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((atalho) => {
      if (atalho) {
        this.filtro.patchValue(periodoDoAtalho(atalho, hojeIso()), { emitEvent: false });
        this.carregar();
      }
    });
    this.filtro.controls.projetoId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.carregar());
    // Datas digitadas à mão: espera terminar de digitar e desmarca o atalho.
    for (const campo of [this.filtro.controls.inicio, this.filtro.controls.fim]) {
      campo.valueChanges.pipe(debounceTime(400), takeUntilDestroyed(this.destroyRef)).subscribe(() => {
        this.atalho.setValue(null, { emitEvent: false });
        this.carregar();
      });
    }
    this.carregar();
  }

  protected parcela(projetoId: number, minutos: number): number {
    const total = this.totalDoProjeto().get(projetoId) ?? 0;
    return total ? minutos / total : 0;
  }

  private carregar(): void {
    const { inicio, fim, projetoId } = this.filtro.getRawValue();
    if (!inicio || !fim) {
      return;
    }
    if (fim < inicio) {
      this.erro.set('O fim do período não pode ser anterior ao início.');
      return;
    }
    this.carregando.set(true);
    this.erro.set(null);
    this.service.consolidado(inicio, fim, projetoId).subscribe({
      next: (dados) => {
        this.dados.set(dados);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar o consolidado.'));
        this.carregando.set(false);
      },
    });
  }
}
