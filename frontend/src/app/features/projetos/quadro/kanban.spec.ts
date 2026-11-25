import { Coluna, TarefaResumo } from '../../../core/projetos/projetos.models';
import { dataIso, FiltrosQuadro, moverTarefa, passaNoFiltro, posicaoReal, SEM_FILTROS, temFiltro } from './kanban';

function tarefa(id: number, extra: Partial<TarefaResumo> = {}): TarefaResumo {
  return {
    id,
    colunaId: 1,
    titulo: `Tarefa ${id}`,
    prioridade: 'MEDIA',
    prazo: null,
    posicao: 0,
    responsavel: null,
    totalComentarios: 0,
    ...extra,
  };
}

function colunas(): Coluna[] {
  return [
    { id: 1, nome: 'A fazer', posicao: 0, tarefas: [tarefa(1), tarefa(2, { posicao: 1 }), tarefa(3, { posicao: 2 })] },
    { id: 2, nome: 'Feito', posicao: 1, tarefas: [tarefa(4, { colunaId: 2 })] },
  ];
}

const titulos = (coluna: Coluna) => coluna.tarefas.map((t) => t.id);
const posicoes = (coluna: Coluna) => coluna.tarefas.map((t) => t.posicao);

describe('moverTarefa', () => {
  it('move para outra coluna e renumera as duas', () => {
    const original = colunas();
    const resultado = moverTarefa(original, 2, 2, 0);

    expect(titulos(resultado[0])).toEqual([1, 3]);
    expect(posicoes(resultado[0])).toEqual([0, 1]);
    expect(titulos(resultado[1])).toEqual([2, 4]);
    expect(posicoes(resultado[1])).toEqual([0, 1]);
    expect(resultado[1].tarefas[0].colunaId).toBe(2);
    expect(titulos(original[0])).toEqual([1, 2, 3]);
  });

  it('reordena dentro da mesma coluna', () => {
    const resultado = moverTarefa(colunas(), 1, 1, 2);

    expect(titulos(resultado[0])).toEqual([2, 3, 1]);
    expect(posicoes(resultado[0])).toEqual([0, 1, 2]);
  });

  it('posição além do fim vai para o fim e colunas intocadas são reaproveitadas', () => {
    const original = colunas();
    const resultado = moverTarefa(original, 4, 2, 99);

    expect(titulos(resultado[1])).toEqual([4]);
    expect(resultado[0]).toBe(original[0]);
  });

  it('ignora tarefa inexistente', () => {
    const original = colunas();
    expect(moverTarefa(original, 42, 1, 0)).toBe(original);
  });
});

describe('posicaoReal', () => {
  const completa = [tarefa(1), tarefa(2), tarefa(3), tarefa(4)];

  it('sem filtro, o índice visível é o real', () => {
    expect(posicaoReal(completa, completa, 2)).toBe(2);
  });

  it('com filtro, solta antes da tarefa visível que ocupava o índice', () => {
    expect(posicaoReal(completa, [tarefa(2), tarefa(4)], 1)).toBe(3);
  });

  it('soltar depois da última visível coloca logo após ela', () => {
    expect(posicaoReal(completa, [tarefa(1), tarefa(2)], 2)).toBe(2);
  });

  it('coluna sem nada visível recebe a tarefa no fim', () => {
    expect(posicaoReal(completa, [], 0)).toBe(4);
  });
});

describe('filtros do quadro', () => {
  const hoje = '2026-11-25';
  const filtro = (extra: Partial<FiltrosQuadro>): FiltrosQuadro => ({ ...SEM_FILTROS, ...extra });
  const ana = { id: 7, nome: 'Ana', email: 'ana@taskly.dev' };

  it('sem filtros tudo passa', () => {
    expect(temFiltro(SEM_FILTROS)).toBe(false);
    expect(passaNoFiltro(tarefa(1), SEM_FILTROS, hoje)).toBe(true);
  });

  it('busca pelo título sem diferenciar maiúsculas', () => {
    expect(passaNoFiltro(tarefa(1, { titulo: 'Login social' }), filtro({ busca: 'LOGIN' }), hoje)).toBe(true);
    expect(passaNoFiltro(tarefa(1, { titulo: 'Relatório' }), filtro({ busca: 'login' }), hoje)).toBe(false);
  });

  it('filtra por responsável e por tarefas sem responsável', () => {
    const comAna = tarefa(1, { responsavel: ana });
    expect(passaNoFiltro(comAna, filtro({ responsavel: 7 }), hoje)).toBe(true);
    expect(passaNoFiltro(comAna, filtro({ responsavel: 8 }), hoje)).toBe(false);
    expect(passaNoFiltro(comAna, filtro({ responsavel: 'nenhum' }), hoje)).toBe(false);
    expect(passaNoFiltro(tarefa(2), filtro({ responsavel: 'nenhum' }), hoje)).toBe(true);
  });

  it('filtra por prioridade', () => {
    expect(passaNoFiltro(tarefa(1, { prioridade: 'ALTA' }), filtro({ prioridade: 'ALTA' }), hoje)).toBe(true);
    expect(passaNoFiltro(tarefa(1), filtro({ prioridade: 'ALTA' }), hoje)).toBe(false);
  });

  it('filtra por prazo: atrasadas, próximos 7 dias e sem prazo', () => {
    const ontem = tarefa(1, { prazo: '2026-11-24' });
    const hojeMesmo = tarefa(2, { prazo: hoje });
    const emUmaSemana = tarefa(3, { prazo: '2026-12-02' });
    const depois = tarefa(4, { prazo: '2026-12-03' });
    const semPrazo = tarefa(5);

    const atrasadas = filtro({ prazo: 'atrasadas' });
    expect([ontem, hojeMesmo, semPrazo].map((t) => passaNoFiltro(t, atrasadas, hoje))).toEqual([true, false, false]);

    const semana = filtro({ prazo: 'semana' });
    expect([ontem, hojeMesmo, emUmaSemana, depois].map((t) => passaNoFiltro(t, semana, hoje))).toEqual([
      false,
      true,
      true,
      false,
    ]);

    expect(passaNoFiltro(semPrazo, filtro({ prazo: 'sem-prazo' }), hoje)).toBe(true);
    expect(passaNoFiltro(ontem, filtro({ prazo: 'sem-prazo' }), hoje)).toBe(false);
  });

  it('formata a data local como yyyy-MM-dd', () => {
    expect(dataIso(new Date(2026, 0, 5))).toBe('2026-01-05');
  });
});
