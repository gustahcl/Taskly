import { Routes } from '@angular/router';

/** Rotas do módulo de projetos, carregadas sob demanda a partir de /projetos. */
export const PROJETOS_ROUTES: Routes = [
  {
    path: '',
    title: 'Projetos · Taskly',
    loadComponent: () => import('./lista/lista-projetos').then((m) => m.ListaProjetos),
  },
  {
    path: ':id',
    title: 'Projeto · Taskly',
    loadComponent: () => import('./detalhe/detalhe-projeto').then((m) => m.DetalheProjeto),
  },
  {
    path: ':id/quadros/:quadroId',
    title: 'Quadro · Taskly',
    loadComponent: () => import('./quadro/quadro').then((m) => m.Quadro),
  },
];
