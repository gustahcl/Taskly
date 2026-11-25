import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';

import { Perfil } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';

interface ItemMenu {
  rota: string;
  rotulo: string;
  icone: string;
  /** Perfis que veem o item; vazio = todos. */
  perfis: Perfil[];
}

const ITENS_MENU: ItemMenu[] = [
  { rota: '/', rotulo: 'Início', icone: 'home', perfis: [] },
  { rota: '/projetos', rotulo: 'Projetos', icone: 'folder', perfis: [] },
  { rota: '/usuarios', rotulo: 'Usuários', icone: 'group', perfis: ['ADMIN'] },
];

export const NOMES_PERFIL: Record<Perfil, string> = {
  ADMIN: 'Administrador',
  GERENTE: 'Gerente',
  MEMBRO: 'Membro',
};

/** Layout da área logada: cabeçalho, menu lateral e a página atual. */
@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatMenuModule,
    MatSidenavModule,
    MatToolbarModule,
  ],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly auth = inject(AuthService);

  protected readonly usuario = this.auth.usuario;
  protected readonly nomesPerfil = NOMES_PERFIL;
  protected readonly itens = computed(() => {
    const perfil = this.usuario()?.perfil;
    return ITENS_MENU.filter((item) => item.perfis.length === 0 || (perfil && item.perfis.includes(perfil)));
  });
  protected readonly celular = toSignal(
    inject(BreakpointObserver)
      .observe(Breakpoints.Handset)
      .pipe(map((resultado) => resultado.matches)),
    { initialValue: false },
  );

  protected sair(): void {
    this.auth.logout();
  }
}
