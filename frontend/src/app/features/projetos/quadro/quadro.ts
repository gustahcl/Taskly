import { CdkDrag, CdkDragDrop, CdkDragPlaceholder, CdkDropList, CdkDropListGroup } from '@angular/cdk/drag-drop';
import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';

import { mensagemDeErro } from '../../../core/auth/mensagem-erro';
import {
  Coluna,
  NOMES_PRIORIDADE,
  Prioridade,
  PRIORIDADES,
  Quadro as QuadroModelo,
  TarefaResumo,
} from '../../../core/projetos/projetos.models';
import { QuadrosService } from '../../../core/projetos/quadros.service';
import { ConfirmarDialog, DadosConfirmarDialog } from '../../../shared/dialogos/confirmar-dialog';
import { DadosNomeDialog, NomeDialog } from '../../../shared/dialogos/nome-dialog';
import { DadosTarefaDialog, ResultadoTarefaDialog, TarefaDialog } from '../tarefa-dialog/tarefa-dialog';
import {
  atrasada,
  dataIso,
  FiltroPrazo,
  FiltrosQuadro,
  moverTarefa,
  passaNoFiltro,
  posicaoReal,
  SEM_FILTROS,
  temFiltro,
} from './kanban';

/** Coluna como aparece na tela: as tarefas já passaram pelos filtros. */
interface ColunaVisivel {
  coluna: Coluna;
  tarefas: TarefaResumo[];
}

/** Quadro kanban: colunas, cards com drag-and-drop, filtros e criação rápida de tarefas. */
@Component({
  selector: 'app-quadro',
  imports: [
    CdkDrag,
    CdkDragPlaceholder,
    CdkDropList,
    CdkDropListGroup,
    DatePipe,
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTooltipModule,
  ],
  templateUrl: './quadro.html',
  styleUrl: './quadro.scss',
})
export class Quadro implements OnInit {
  /** Vêm da rota (:id e :quadroId). */
  readonly id = input.required<string>();
  readonly quadroId = input.required<string>();

  private readonly service = inject(QuadrosService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  protected readonly prioridades = PRIORIDADES;
  protected readonly nomesPrioridade = NOMES_PRIORIDADE;
  protected readonly hoje = dataIso(new Date());

  protected readonly quadro = signal<QuadroModelo | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);
  protected readonly filtros = signal<FiltrosQuadro>(SEM_FILTROS);
  protected readonly filtrando = computed(() => temFiltro(this.filtros()));
  /** Coluna com o campo de nova tarefa aberto. */
  protected readonly criandoEm = signal<number | null>(null);
  protected novoTitulo = '';

  protected readonly colunas = computed<ColunaVisivel[]>(() => {
    const filtros = this.filtros();
    return (this.quadro()?.colunas ?? []).map((coluna) => ({
      coluna,
      tarefas: coluna.tarefas.filter((t) => passaNoFiltro(t, filtros, this.hoje)),
    }));
  });
  protected readonly totalVisivel = computed(() => this.colunas().reduce((soma, c) => soma + c.tarefas.length, 0));
  protected readonly total = computed(() =>
    (this.quadro()?.colunas ?? []).reduce((soma, c) => soma + c.tarefas.length, 0),
  );

  ngOnInit(): void {
    this.recarregar();
  }

  protected filtrar<K extends keyof FiltrosQuadro>(campo: K, valor: FiltrosQuadro[K]): void {
    this.filtros.update((f) => ({ ...f, [campo]: valor }));
  }

  protected limparFiltros(): void {
    this.filtros.set(SEM_FILTROS);
  }

  protected atrasada(tarefa: TarefaResumo): boolean {
    return atrasada(tarefa, this.hoje);
  }

  /** Solta um card: atualiza a tela na hora e desfaz se a API recusar. */
  protected soltar(evento: CdkDragDrop<ColunaVisivel, ColunaVisivel, TarefaResumo>): void {
    const quadro = this.quadro();
    if (!quadro) {
      return;
    }
    const tarefa = evento.item.data;
    const destino = evento.container.data.coluna;
    const mesmaColuna = evento.previousContainer === evento.container;
    if (mesmaColuna && evento.previousIndex === evento.currentIndex) {
      return;
    }

    const semMovida = (lista: TarefaResumo[]) => lista.filter((t) => t.id !== tarefa.id);
    const posicao = posicaoReal(
      semMovida(destino.tarefas),
      semMovida(evento.container.data.tarefas),
      evento.currentIndex,
    );

    const anterior = quadro;
    this.quadro.set({ ...quadro, colunas: moverTarefa(quadro.colunas, tarefa.id, destino.id, posicao) });
    this.service.moverTarefa(tarefa.id, destino.id, posicao).subscribe({
      error: (erro: unknown) => {
        this.quadro.set(anterior);
        this.avisar(erro, 'Não foi possível mover a tarefa.');
      },
    });
  }

  protected abrirNovaTarefa(coluna: Coluna): void {
    this.novoTitulo = '';
    this.criandoEm.set(coluna.id);
  }

  protected criarTarefa(coluna: Coluna): void {
    const titulo = this.novoTitulo.trim();
    if (!titulo) {
      this.criandoEm.set(null);
      return;
    }
    this.service.criarTarefa(coluna.id, { titulo }).subscribe({
      next: () => {
        this.novoTitulo = '';
        this.recarregar();
      },
      error: (erro: unknown) => this.avisar(erro, 'Não foi possível criar a tarefa.'),
    });
  }

  protected abrirTarefa(tarefa: TarefaResumo): void {
    const quadro = this.quadro()!;
    this.dialog
      .open<TarefaDialog, DadosTarefaDialog, ResultadoTarefaDialog>(TarefaDialog, {
        data: { tarefaId: tarefa.id, membros: quadro.membros, projetoNome: quadro.projetoNome },
        width: '720px',
        maxWidth: '95vw',
        autoFocus: 'dialog',
      })
      .afterClosed()
      .subscribe((resultado) => {
        if (resultado === 'excluida') {
          this.snackBar.open('Tarefa excluída.', 'OK', { duration: 3000 });
        }
        // Mesmo fechado pelo Esc, o modal pode ter mudado comentários: recarrega sempre.
        this.recarregar();
      });
  }

  protected novaColuna(): void {
    this.pedirNome({ titulo: 'Nova coluna', rotulo: 'Nome da coluna', maximo: 60, confirmar: 'Adicionar' })
      .subscribe((nome) => {
        if (nome) {
          this.executar(this.service.adicionarColuna(this.quadro()!.id, nome), 'Não foi possível criar a coluna.');
        }
      });
  }

  protected renomearColuna(coluna: Coluna): void {
    this.pedirNome({ titulo: 'Renomear coluna', rotulo: 'Nome da coluna', valor: coluna.nome, maximo: 60 })
      .subscribe((nome) => {
        if (nome && nome !== coluna.nome) {
          this.executar(this.service.renomearColuna(coluna.id, nome), 'Não foi possível renomear a coluna.');
        }
      });
  }

  protected moverColuna(coluna: Coluna, deslocamento: number): void {
    this.executar(
      this.service.moverColuna(coluna.id, coluna.posicao + deslocamento),
      'Não foi possível mover a coluna.',
    );
  }

  protected excluirColuna(coluna: Coluna): void {
    if (coluna.tarefas.length > 0) {
      this.snackBar.open('Mova ou exclua as tarefas antes de excluir a coluna.', 'OK', { duration: 4000 });
      return;
    }
    this.executar(this.service.excluirColuna(coluna.id), 'Não foi possível excluir a coluna.');
  }

  protected renomearQuadro(): void {
    const quadro = this.quadro()!;
    this.pedirNome({ titulo: 'Renomear quadro', rotulo: 'Nome do quadro', valor: quadro.nome, maximo: 120 })
      .subscribe((nome) => {
        if (nome && nome !== quadro.nome) {
          this.executar(this.service.renomear(quadro.id, nome), 'Não foi possível renomear o quadro.');
        }
      });
  }

  protected excluirQuadro(): void {
    const quadro = this.quadro()!;
    this.dialog
      .open<ConfirmarDialog, DadosConfirmarDialog, boolean>(ConfirmarDialog, {
        data: {
          titulo: 'Excluir quadro',
          mensagem: `"${quadro.nome}" será excluído com todas as tarefas. Esta ação não pode ser desfeita.`,
          confirmar: 'Excluir',
        },
      })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) {
          this.service.excluir(quadro.id).subscribe({
            next: () => this.router.navigate(['/projetos', quadro.projetoId]),
            error: (erro: unknown) => this.avisar(erro, 'Não foi possível excluir o quadro.'),
          });
        }
      });
  }

  protected filtroPrazo(valor: FiltroPrazo | null): void {
    this.filtrar('prazo', valor);
  }

  protected filtroPrioridade(valor: Prioridade | null): void {
    this.filtrar('prioridade', valor);
  }

  private recarregar(): void {
    this.service.buscar(Number(this.quadroId())).subscribe({
      next: (quadro) => {
        this.quadro.set(quadro);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar o quadro.'));
        this.carregando.set(false);
      },
    });
  }

  private executar(chamada: Observable<unknown>, mensagemErro: string): void {
    chamada.subscribe({
      next: () => this.recarregar(),
      error: (erro: unknown) => this.avisar(erro, mensagemErro),
    });
  }

  private pedirNome(dados: DadosNomeDialog): Observable<string | undefined> {
    return this.dialog.open<NomeDialog, DadosNomeDialog, string>(NomeDialog, { data: dados }).afterClosed();
  }

  private avisar(erro: unknown, padrao: string): void {
    this.snackBar.open(mensagemDeErro(erro, padrao), 'OK', { duration: 5000 });
  }
}
