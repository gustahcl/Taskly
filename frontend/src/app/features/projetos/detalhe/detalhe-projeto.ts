import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectChange, MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';

import { AuthService } from '../../../core/auth/auth.service';
import { mensagemDeErro } from '../../../core/auth/mensagem-erro';
import {
  Membro,
  NOMES_PAPEL,
  NOMES_STATUS,
  PapelProjeto,
  Projeto,
  QuadroResumo,
  UsuarioResumo,
} from '../../../core/projetos/projetos.models';
import { ProjetosService } from '../../../core/projetos/projetos.service';
import { QuadrosService } from '../../../core/projetos/quadros.service';
import { ConfirmarDialog, DadosConfirmarDialog } from '../../../shared/dialogos/confirmar-dialog';
import { DadosNomeDialog, NomeDialog } from '../../../shared/dialogos/nome-dialog';
import { ProjetoDialog } from '../projeto-dialog/projeto-dialog';

/** Página do projeto: dados, quadros e equipe. O gestor edita tudo; os demais só consultam. */
@Component({
  selector: 'app-detalhe-projeto',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    MatAutocompleteModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTooltipModule,
  ],
  templateUrl: './detalhe-projeto.html',
  styleUrl: './detalhe-projeto.scss',
})
export class DetalheProjeto implements OnInit {
  /** Vem da rota (:id). */
  readonly id = input.required<string>();

  private readonly service = inject(ProjetosService);
  private readonly quadrosService = inject(QuadrosService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly idLogado = inject(AuthService).usuario()?.id;
  protected readonly nomesStatus = NOMES_STATUS;
  protected readonly nomesPapel = NOMES_PAPEL;
  protected readonly papeis: PapelProjeto[] = ['GESTOR', 'COLABORADOR'];

  protected readonly projeto = signal<Projeto | null>(null);
  protected readonly quadros = signal<QuadroResumo[]>([]);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);

  protected readonly convidado = new FormControl<string | UsuarioResumo>('', { nonNullable: true });
  protected readonly papelConvite = new FormControl<PapelProjeto>('COLABORADOR', { nonNullable: true });
  protected readonly sugestoes = signal<UsuarioResumo[]>([]);
  protected readonly selecionado = signal<UsuarioResumo | null>(null);

  ngOnInit(): void {
    this.carregar();
    this.convidado.valueChanges
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        switchMap((valor) => {
          if (typeof valor !== 'string') {
            return of([]);
          }
          this.selecionado.set(null);
          return valor.trim().length < 2 ? of([]) : this.service.buscarUsuarios(valor.trim());
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((usuarios) => {
        const membros = new Set(this.projeto()?.membros.map((m) => m.usuarioId));
        this.sugestoes.set(usuarios.filter((u) => !membros.has(u.id)));
      });
  }

  protected nomeDoUsuario(usuario: UsuarioResumo | string | null): string {
    return typeof usuario === 'string' ? usuario : (usuario?.nome ?? '');
  }

  protected escolher(evento: MatAutocompleteSelectedEvent): void {
    this.selecionado.set(evento.option.value as UsuarioResumo);
  }

  protected convidar(): void {
    const projeto = this.projeto();
    const usuario = this.selecionado();
    if (!projeto || !usuario) {
      return;
    }
    this.service.adicionarMembro(projeto.id, usuario.id, this.papelConvite.value).subscribe({
      next: (membro) => {
        this.atualizarMembros((membros) => [...membros, membro]);
        this.convidado.setValue('');
        this.selecionado.set(null);
        this.snackBar.open(`${membro.nome} entrou no projeto.`, 'OK', { duration: 3000 });
      },
      error: (erro: unknown) => this.avisar(erro, 'Não foi possível adicionar o membro.'),
    });
  }

  protected alterarPapel(membro: Membro, evento: MatSelectChange<PapelProjeto>): void {
    const projeto = this.projeto()!;
    this.service.alterarPapel(projeto.id, membro.usuarioId, evento.value).subscribe({
      next: (atualizado) => {
        this.atualizarMembros((membros) => membros.map((m) => (m.usuarioId === atualizado.usuarioId ? atualizado : m)));
        if (atualizado.usuarioId === this.idLogado) {
          this.carregar();
        }
      },
      error: (erro: unknown) => {
        evento.source.value = membro.papel;
        this.avisar(erro, 'Não foi possível alterar o papel.');
      },
    });
  }

  protected removerMembro(membro: Membro): void {
    const projeto = this.projeto()!;
    this.confirmar({
      titulo: 'Remover membro',
      mensagem: `${membro.nome} deixará de ver o projeto e suas tarefas.`,
      confirmar: 'Remover',
    }).subscribe((ok) => {
      if (!ok) {
        return;
      }
      this.service.removerMembro(projeto.id, membro.usuarioId).subscribe({
        next: () => {
          if (membro.usuarioId === this.idLogado) {
            this.router.navigate(['/projetos']);
            return;
          }
          this.atualizarMembros((membros) => membros.filter((m) => m.usuarioId !== membro.usuarioId));
        },
        error: (erro: unknown) => this.avisar(erro, 'Não foi possível remover o membro.'),
      });
    });
  }

  protected editar(): void {
    this.dialog
      .open<ProjetoDialog, Projeto, Projeto>(ProjetoDialog, { data: this.projeto()!, width: '600px' })
      .afterClosed()
      .subscribe((salvo) => salvo && this.projeto.set(salvo));
  }

  protected excluir(): void {
    const projeto = this.projeto()!;
    this.confirmar({
      titulo: 'Excluir projeto',
      mensagem: `"${projeto.nome}" será excluído com todos os quadros e tarefas. Esta ação não pode ser desfeita.`,
      confirmar: 'Excluir',
    }).subscribe((ok) => {
      if (ok) {
        this.service.excluir(projeto.id).subscribe({
          next: () => this.router.navigate(['/projetos']),
          error: (erro: unknown) => this.avisar(erro, 'Não foi possível excluir o projeto.'),
        });
      }
    });
  }

  protected novoQuadro(): void {
    const projeto = this.projeto()!;
    this.dialog
      .open<NomeDialog, DadosNomeDialog, string>(NomeDialog, {
        data: { titulo: 'Novo quadro', rotulo: 'Nome do quadro', maximo: 120, confirmar: 'Criar quadro' },
      })
      .afterClosed()
      .subscribe((nome) => {
        if (nome) {
          this.quadrosService.criar(projeto.id, nome).subscribe({
            next: (quadro) => this.router.navigate(['/projetos', projeto.id, 'quadros', quadro.id]),
            error: (erro: unknown) => this.avisar(erro, 'Não foi possível criar o quadro.'),
          });
        }
      });
  }

  private carregar(): void {
    const id = Number(this.id());
    this.carregando.set(true);
    this.service.buscar(id).subscribe({
      next: (projeto) => {
        this.projeto.set(projeto);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar o projeto.'));
        this.carregando.set(false);
      },
    });
    this.quadrosService.listar(id).subscribe({
      next: (quadros) => this.quadros.set(quadros),
      error: () => this.quadros.set([]),
    });
  }

  private atualizarMembros(mudanca: (membros: Membro[]) => Membro[]): void {
    this.projeto.update((p) => (p ? { ...p, membros: mudanca(p.membros) } : p));
  }

  private confirmar(dados: DadosConfirmarDialog) {
    return this.dialog.open<ConfirmarDialog, DadosConfirmarDialog, boolean>(ConfirmarDialog, { data: dados }).afterClosed();
  }

  private avisar(erro: unknown, padrao: string): void {
    this.snackBar.open(mensagemDeErro(erro, padrao), 'OK', { duration: 5000 });
  }
}
