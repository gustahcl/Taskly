import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  FiltroProjetos,
  Membro,
  Pagina,
  PapelProjeto,
  Projeto,
  ProjetoRequest,
  ProjetoResumo,
  UsuarioResumo,
} from './projetos.models';

@Injectable({ providedIn: 'root' })
export class ProjetosService {
  private readonly http = inject(HttpClient);

  listar(filtro: FiltroProjetos): Observable<Pagina<ProjetoResumo>> {
    let params = new HttpParams().set('page', filtro.pagina).set('size', filtro.tamanho);
    const busca = filtro.busca?.trim();
    if (busca) {
      params = params.set('busca', busca);
    }
    if (filtro.status) {
      params = params.set('status', filtro.status);
    }
    if (filtro.ordenacao) {
      params = params.set('sort', filtro.ordenacao);
    }
    return this.http.get<Pagina<ProjetoResumo>>('/api/projetos', { params });
  }

  buscar(id: number): Observable<Projeto> {
    return this.http.get<Projeto>(`/api/projetos/${id}`);
  }

  criar(projeto: ProjetoRequest): Observable<Projeto> {
    return this.http.post<Projeto>('/api/projetos', projeto);
  }

  atualizar(id: number, projeto: ProjetoRequest): Observable<Projeto> {
    return this.http.put<Projeto>(`/api/projetos/${id}`, projeto);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`/api/projetos/${id}`);
  }

  adicionarMembro(projetoId: number, usuarioId: number, papel: PapelProjeto): Observable<Membro> {
    return this.http.post<Membro>(`/api/projetos/${projetoId}/membros`, { usuarioId, papel });
  }

  alterarPapel(projetoId: number, usuarioId: number, papel: PapelProjeto): Observable<Membro> {
    return this.http.patch<Membro>(`/api/projetos/${projetoId}/membros/${usuarioId}`, { papel });
  }

  removerMembro(projetoId: number, usuarioId: number): Observable<void> {
    return this.http.delete<void>(`/api/projetos/${projetoId}/membros/${usuarioId}`);
  }

  /** Usuários ativos para convidar ao projeto (a API exige pelo menos 2 letras). */
  buscarUsuarios(termo: string): Observable<UsuarioResumo[]> {
    return this.http.get<UsuarioResumo[]>('/api/usuarios/busca', { params: { termo } });
  }
}
