import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectChange, MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { Perfil, Usuario } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { mensagemDeErro } from '../../core/auth/mensagem-erro';
import { UsuariosService } from '../../core/usuarios/usuarios.service';
import { NOMES_PERFIL } from '../../layout/shell/shell';

/** Gestão de usuários (somente ADMIN): lista e troca de perfil. */
@Component({
  selector: 'app-usuarios',
  imports: [DatePipe, MatFormFieldModule, MatProgressBarModule, MatSelectModule, MatTableModule],
  templateUrl: './usuarios.html',
  styleUrl: './usuarios.scss',
})
export class Usuarios implements OnInit {
  private readonly service = inject(UsuariosService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly idLogado = inject(AuthService).usuario()?.id;
  protected readonly perfis: Perfil[] = ['ADMIN', 'GERENTE', 'MEMBRO'];
  protected readonly nomesPerfil = NOMES_PERFIL;
  protected readonly colunas = ['nome', 'email', 'criadoEm', 'perfil'];
  protected readonly usuarios = signal<Usuario[]>([]);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);

  ngOnInit(): void {
    this.service.listar().subscribe({
      next: (usuarios) => {
        this.usuarios.set(usuarios);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erro.set(mensagemDeErro(erro, 'Não foi possível carregar os usuários.'));
        this.carregando.set(false);
      },
    });
  }

  protected alterarPerfil(usuario: Usuario, evento: MatSelectChange<Perfil>): void {
    const anterior = usuario.perfil;
    this.service.alterarPerfil(usuario.id, evento.value).subscribe({
      next: (atualizado) => {
        this.usuarios.update((lista) => lista.map((u) => (u.id === atualizado.id ? atualizado : u)));
        this.snackBar.open(`${atualizado.nome} agora é ${NOMES_PERFIL[atualizado.perfil]}.`, 'OK', { duration: 3000 });
      },
      error: (erro: unknown) => {
        evento.source.value = anterior;
        this.snackBar.open(mensagemDeErro(erro, 'Não foi possível alterar o perfil.'), 'OK', { duration: 5000 });
      },
    });
  }
}
