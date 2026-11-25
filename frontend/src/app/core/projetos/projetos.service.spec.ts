import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ProjetosService } from './projetos.service';

describe('ProjetosService', () => {
  let service: ProjetosService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ProjetosService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista com paginação, filtros e ordenação nos parâmetros', () => {
    service
      .listar({ busca: '  portal ', status: 'EM_ANDAMENTO', pagina: 2, tamanho: 25, ordenacao: 'nome,desc' })
      .subscribe();

    const req = http.expectOne((r) => r.url === '/api/projetos');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('25');
    expect(req.request.params.get('busca')).toBe('portal');
    expect(req.request.params.get('status')).toBe('EM_ANDAMENTO');
    expect(req.request.params.get('sort')).toBe('nome,desc');
    req.flush({ conteudo: [], pagina: 2, tamanho: 25, totalElementos: 0, totalPaginas: 0 });
  });

  it('omite filtros vazios', () => {
    service.listar({ busca: '   ', status: null, pagina: 0, tamanho: 10 }).subscribe();

    const req = http.expectOne((r) => r.url === '/api/projetos');
    expect(req.request.params.keys().sort()).toEqual(['page', 'size']);
    req.flush({ conteudo: [], pagina: 0, tamanho: 10, totalElementos: 0, totalPaginas: 0 });
  });

  it('adiciona membro com o papel escolhido', () => {
    service.adicionarMembro(5, 9, 'GESTOR').subscribe();

    const req = http.expectOne('/api/projetos/5/membros');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ usuarioId: 9, papel: 'GESTOR' });
    req.flush({});
  });
});
