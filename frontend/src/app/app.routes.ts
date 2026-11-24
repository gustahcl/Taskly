import { Routes } from '@angular/router';

import { autenticadoGuard, perfilGuard, visitanteGuard } from './core/auth/auth.guards';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [visitanteGuard],
    title: 'Entrar · Taskly',
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'cadastro',
    canActivate: [visitanteGuard],
    title: 'Criar conta · Taskly',
    loadComponent: () => import('./features/auth/cadastro/cadastro').then((m) => m.Cadastro),
  },
  {
    path: '',
    canActivate: [autenticadoGuard],
    loadComponent: () => import('./layout/shell/shell').then((m) => m.Shell),
    children: [
      {
        path: '',
        pathMatch: 'full',
        title: 'Início · Taskly',
        loadComponent: () => import('./features/inicio/inicio').then((m) => m.Inicio),
      },
      {
        path: 'usuarios',
        canActivate: [perfilGuard('ADMIN')],
        title: 'Usuários · Taskly',
        loadComponent: () => import('./features/usuarios/usuarios').then((m) => m.Usuarios),
      },
      {
        path: 'acesso-negado',
        title: 'Acesso negado · Taskly',
        loadComponent: () => import('./features/acesso-negado/acesso-negado').then((m) => m.AcessoNegado),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
