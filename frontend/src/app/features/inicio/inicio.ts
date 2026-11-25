import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { NOMES_PERFIL } from '../../layout/shell/shell';

@Component({
  selector: 'app-inicio',
  imports: [RouterLink, MatButtonModule, MatIconModule],
  template: `
    @if (usuario(); as u) {
      <h1>Olá, {{ u.nome }}</h1>
      <p class="perfil">Você está logado como <strong>{{ nomesPerfil[u.perfil] }}</strong>.</p>
      <p class="aviso">Seus quadros e tarefas ficam dentro de cada projeto.</p>
      <a mat-flat-button routerLink="/projetos"><mat-icon>folder</mat-icon> Ver meus projetos</a>
    }
  `,
  styles: `
    h1 {
      margin: 0 0 8px;
      font: var(--mat-sys-headline-medium);
    }
    .perfil {
      margin: 0 0 24px;
    }
    .aviso {
      color: var(--mat-sys-on-surface-variant);
    }
  `,
})
export class Inicio {
  protected readonly usuario = inject(AuthService).usuario;
  protected readonly nomesPerfil = NOMES_PERFIL;
}
