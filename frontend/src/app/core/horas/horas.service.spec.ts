import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { HorasService } from './horas.service';

describe('HorasService', () => {
  let service: HorasService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(HorasService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('aponta horas na tarefa', () => {
    service.registrar(7, { data: '2026-11-26', minutos: 90, descricao: 'Tela' }).subscribe();

    const req = http.expectOne('/api/tarefas/7/apontamentos');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ data: '2026-11-26', minutos: 90, descricao: 'Tela' });
    req.flush({});
  });

  it('busca as próprias horas no período', () => {
    service.minhas('2026-11-23', '2026-11-29').subscribe();

    const req = http.expectOne((r) => r.url === '/api/apontamentos/meus');
    expect(req.request.params.get('inicio')).toBe('2026-11-23');
    expect(req.request.params.get('fim')).toBe('2026-11-29');
    req.flush({});
  });

  it('só manda o projeto no consolidado quando há filtro', () => {
    service.consolidado('2026-11-01', '2026-11-30', null).subscribe();
    service.consolidado('2026-11-01', '2026-11-30', 3).subscribe();

    const [semProjeto, comProjeto] = http.match((r) => r.url === '/api/apontamentos/consolidado');
    expect(semProjeto.request.params.has('projetoId')).toBe(false);
    expect(comProjeto.request.params.get('projetoId')).toBe('3');
    semProjeto.flush({});
    comProjeto.flush({});
  });
});
