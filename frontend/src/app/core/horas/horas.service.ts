import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  Apontamento,
  ApontamentoRequest,
  Consolidado,
  HorasDaTarefa,
  MinhasHoras,
  TarefaOpcao,
} from './horas.models';

/** Apontamento de horas: por tarefa, visão semanal e consolidado do gestor. */
@Injectable({ providedIn: 'root' })
export class HorasService {
  private readonly http = inject(HttpClient);

  daTarefa(tarefaId: number): Observable<HorasDaTarefa> {
    return this.http.get<HorasDaTarefa>(`/api/tarefas/${tarefaId}/apontamentos`);
  }

  registrar(tarefaId: number, apontamento: ApontamentoRequest): Observable<Apontamento> {
    return this.http.post<Apontamento>(`/api/tarefas/${tarefaId}/apontamentos`, apontamento);
  }

  atualizar(id: number, apontamento: ApontamentoRequest): Observable<Apontamento> {
    return this.http.put<Apontamento>(`/api/apontamentos/${id}`, apontamento);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`/api/apontamentos/${id}`);
  }

  /** Datas no formato yyyy-MM-dd. */
  minhas(inicio: string, fim: string): Observable<MinhasHoras> {
    return this.http.get<MinhasHoras>('/api/apontamentos/meus', { params: { inicio, fim } });
  }

  consolidado(inicio: string, fim: string, projetoId?: number | null): Observable<Consolidado> {
    let params = new HttpParams().set('inicio', inicio).set('fim', fim);
    if (projetoId) {
      params = params.set('projetoId', projetoId);
    }
    return this.http.get<Consolidado>('/api/apontamentos/consolidado', { params });
  }

  tarefasDoProjeto(projetoId: number): Observable<TarefaOpcao[]> {
    return this.http.get<TarefaOpcao[]>(`/api/projetos/${projetoId}/tarefas`);
  }
}
