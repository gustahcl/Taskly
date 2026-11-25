import { Coluna, Prioridade, TarefaResumo } from '../../../core/projetos/projetos.models';

/** Filtro de prazo do quadro. */
export type FiltroPrazo = 'atrasadas' | 'semana' | 'sem-prazo';

export interface FiltrosQuadro {
  busca: string;
  /** Id do responsável, 'nenhum' para tarefas sem responsável ou null para todos. */
  responsavel: number | 'nenhum' | null;
  prioridade: Prioridade | null;
  prazo: FiltroPrazo | null;
}

export const SEM_FILTROS: FiltrosQuadro = { busca: '', responsavel: null, prioridade: null, prazo: null };

export function temFiltro(filtros: FiltrosQuadro): boolean {
  return (
    filtros.busca.trim() !== '' || filtros.responsavel !== null || filtros.prioridade !== null || filtros.prazo !== null
  );
}

/** Data local no formato yyyy-MM-dd (o mesmo dos prazos que vêm da API). */
export function dataIso(data: Date): string {
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const dia = String(data.getDate()).padStart(2, '0');
  return `${data.getFullYear()}-${mes}-${dia}`;
}

export function atrasada(tarefa: TarefaResumo, hoje: string): boolean {
  return tarefa.prazo !== null && tarefa.prazo < hoje;
}

/** Diz se a tarefa passa nos filtros. {@code hoje} vem no formato yyyy-MM-dd. */
export function passaNoFiltro(tarefa: TarefaResumo, filtros: FiltrosQuadro, hoje: string): boolean {
  const busca = filtros.busca.trim().toLowerCase();
  if (busca && !tarefa.titulo.toLowerCase().includes(busca)) {
    return false;
  }
  if (filtros.responsavel === 'nenhum' && tarefa.responsavel !== null) {
    return false;
  }
  if (typeof filtros.responsavel === 'number' && tarefa.responsavel?.id !== filtros.responsavel) {
    return false;
  }
  if (filtros.prioridade && tarefa.prioridade !== filtros.prioridade) {
    return false;
  }
  switch (filtros.prazo) {
    case 'atrasadas':
      return atrasada(tarefa, hoje);
    case 'semana': {
      const limite = new Date(`${hoje}T00:00:00`);
      limite.setDate(limite.getDate() + 7);
      return tarefa.prazo !== null && tarefa.prazo >= hoje && tarefa.prazo <= dataIso(limite);
    }
    case 'sem-prazo':
      return tarefa.prazo === null;
    default:
      return true;
  }
}

/**
 * Converte o índice em que a tarefa foi solta na lista visível (filtrada) para a posição real
 * na coluna completa. As duas listas chegam sem a tarefa que está sendo movida.
 */
export function posicaoReal(completa: TarefaResumo[], visiveis: TarefaResumo[], indiceVisivel: number): number {
  if (visiveis.length === 0) {
    return completa.length;
  }
  if (indiceVisivel >= visiveis.length) {
    const ultima = visiveis[visiveis.length - 1];
    return completa.findIndex((t) => t.id === ultima.id) + 1;
  }
  return completa.findIndex((t) => t.id === visiveis[indiceVisivel].id);
}

/**
 * Move a tarefa para a coluna e posição indicadas e devolve um novo array de colunas com as posições
 * renumeradas, como o back-end faz. Não altera o array recebido.
 */
export function moverTarefa(colunas: Coluna[], tarefaId: number, destinoId: number, posicao: number): Coluna[] {
  const tarefa = colunas.flatMap((c) => c.tarefas).find((t) => t.id === tarefaId);
  if (!tarefa) {
    return colunas;
  }
  return colunas.map((coluna) => {
    const tarefas = coluna.tarefas.filter((t) => t.id !== tarefaId);
    if (coluna.id === destinoId) {
      tarefas.splice(Math.max(0, Math.min(posicao, tarefas.length)), 0, tarefa);
    } else if (tarefas.length === coluna.tarefas.length) {
      return coluna;
    }
    return {
      ...coluna,
      tarefas: tarefas.map((t, i) => ({ ...t, colunaId: coluna.id, posicao: i })),
    };
  });
}
