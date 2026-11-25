import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorIntl, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { Router, RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { AuthService } from '../../../core/auth/auth.service';
import { mensagemDeErro } from '../../../core/auth/mensagem-erro';
import {
  NOMES_PAPEL,
  NOMES_STATUS,
  Projeto,
  ProjetoResumo,
  STATUS,
  StatusProjeto,
} from '../../../core/projetos/projetos.models';
import { PaginadorPtBr } from '../../../core/paginador-pt-br';
import { ProjetosService } from '../../../core/projetos/projetos.service';
import { ProjetoDialog } from '../projeto-dialog/projeto-dialog';

/** Lista paginada dos projetos visíveis ao usuário, com busca e filtro por status. */
@Component({
  selector: 'app-lista-projetos',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    MatSortModule,
    MatTableModule,
  ],
  providers: [{ provide: MatPaginatorIntl, useClass: PaginadorPtBr }],
  templateUrl: './lista-projetos.html',
  styleUrl: './lista-projetos.scss',
})
export class ListaProjetos implements OnInit {
  private readonly service = inject(ProjetosService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly podeCriar = inject(AuthService).temPerfil('ADMIN', 'GERENTE');
  protected readonly statusDisponiveis = STATUS;
  protected readonly nomesStatus = NOMES_STATUS;
  protected readonly colunas = ['nome', 'cliente', 'status', 'dataFim', 'membros', 'papel'];

  protected readonly busca = new FormControl('', { nonNullable: true });
  protected readonly status = new FormControl<StatusProjeto | null>(null);

  protected readonly projetos = signal<ProjetoResumo[]>([]);
  protected readonly total = signal(0);
  protected readonly pagina = signal(0);
  protected readonly tamanho = signal(10);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);
  private ordenacao = 'nome,asc';

  ngOnInit(): void {
    this.busca.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.recarregarDoInicio());
    this.status.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.recarregarDoInicio());
    this.carregar();
  }

  protected rotuloStatus(projeto: ProjetoResumo): string {
    return NOMES_STATUS[projeto.status];
  }

  /** ADMIN pode ver projetos de que não participa: nesse caso não há papel. */
  protected rotuloPapel(projeto: ProjetoResumo): string {
    return projeto.meuPapel ? NOMES_PAPEL[projeto.meuPapel] : 'Administrador';
  }

  protected mudarPagina(evento: PageEvent): void {
    this.pagina.set(evento.pageIndex);
    this.tamanho.set(evento.pageSize);
    this.carregar();
  }

  protected ordenar(sort: Sort): void {
    this.ordenacao = sort.direction ? `${sort.active},${sort.direction}` : 'nome,asc';
    this.recarregarDoInicio();
  }

  protected novoProjeto(): void {
    this.dialog
      .open<ProjetoDialog, Projeto | null, Projeto>(ProjetoDialog, { data: null, width: '600px' })
      .afterClosed()
      .subscribe((criado) => {
        if (criado) {
          this.router.navigate(['/projetos', criado.id]);
        }
      });
  }

  private recarregarDoInicio(): void {
    this.pagina.set(0);
    this.carregar();
  }

  private carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.service
      .listar({
        busca: this.busca.value,
        status: this.status.value,
        pagina: this.pagina(),
        tamanho: this.tamanho(),
        ordenacao: this.ordenacao,
      })
      .subscribe({
        next: (resultado) => {
          this.projetos.set(resultado.conteudo);
          this.total.set(resultado.totalElementos);
          this.carregando.set(false);
        },
        error: (erro: unknown) => {
          this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar os projetos.'));
          this.carregando.set(false);
        },
      });
  }
}
