import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Coluna, Comentario, Quadro, QuadroResumo, Tarefa, TarefaRequest, TarefaResumo } from './projetos.models';

/** Quadros, colunas, tarefas e comentários. */
@Injectable({ providedIn: 'root' })
export class QuadrosService {
  private readonly http = inject(HttpClient);

  listar(projetoId: number): Observable<QuadroResumo[]> {
    return this.http.get<QuadroResumo[]>(`/api/projetos/${projetoId}/quadros`);
  }

  criar(projetoId: number, nome: string): Observable<Quadro> {
    return this.http.post<Quadro>(`/api/projetos/${projetoId}/quadros`, { nome });
  }

  buscar(id: number): Observable<Quadro> {
    return this.http.get<Quadro>(`/api/quadros/${id}`);
  }

  renomear(id: number, nome: string): Observable<QuadroResumo> {
    return this.http.put<QuadroResumo>(`/api/quadros/${id}`, { nome });
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`/api/quadros/${id}`);
  }

  adicionarColuna(quadroId: number, nome: string): Observable<Coluna> {
    return this.http.post<Coluna>(`/api/quadros/${quadroId}/colunas`, { nome });
  }

  renomearColuna(id: number, nome: string): Observable<Coluna> {
    return this.http.put<Coluna>(`/api/colunas/${id}`, { nome });
  }

  moverColuna(id: number, posicao: number): Observable<Coluna> {
    return this.http.patch<Coluna>(`/api/colunas/${id}/mover`, { posicao });
  }

  excluirColuna(id: number): Observable<void> {
    return this.http.delete<void>(`/api/colunas/${id}`);
  }

  criarTarefa(colunaId: number, tarefa: Partial<TarefaRequest> & { titulo: string }): Observable<Tarefa> {
    return this.http.post<Tarefa>(`/api/colunas/${colunaId}/tarefas`, tarefa);
  }

  buscarTarefa(id: number): Observable<Tarefa> {
    return this.http.get<Tarefa>(`/api/tarefas/${id}`);
  }

  atualizarTarefa(id: number, tarefa: TarefaRequest): Observable<Tarefa> {
    return this.http.put<Tarefa>(`/api/tarefas/${id}`, tarefa);
  }

  excluirTarefa(id: number): Observable<void> {
    return this.http.delete<void>(`/api/tarefas/${id}`);
  }

  moverTarefa(id: number, colunaId: number, posicao: number): Observable<TarefaResumo> {
    return this.http.patch<TarefaResumo>(`/api/tarefas/${id}/mover`, { colunaId, posicao });
  }

  comentarios(tarefaId: number): Observable<Comentario[]> {
    return this.http.get<Comentario[]>(`/api/tarefas/${tarefaId}/comentarios`);
  }

  comentar(tarefaId: number, texto: string): Observable<Comentario> {
    return this.http.post<Comentario>(`/api/tarefas/${tarefaId}/comentarios`, { texto });
  }

  excluirComentario(id: number): Observable<void> {
    return this.http.delete<void>(`/api/comentarios/${id}`);
  }
}
