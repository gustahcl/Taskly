import { UsuarioResumo } from '../projetos/projetos.models';

/** Onde as horas foram apontadas. */
export interface TarefaApontada {
  id: number;
  titulo: string;
  quadroId: number;
  projetoId: number;
  projetoNome: string;
}

export interface Apontamento {
  id: number;
  /** yyyy-MM-dd */
  data: string;
  minutos: number;
  descricao: string | null;
  usuario: UsuarioResumo;
  tarefa: TarefaApontada;
  podeEditar: boolean;
  criadoEm: string;
}

export interface ApontamentoRequest {
  data: string;
  minutos: number;
  descricao: string | null;
}

export interface HorasDaTarefa {
  totalMinutos: number;
  apontamentos: Apontamento[];
}

export interface TotalDoDia {
  data: string;
  minutos: number;
}

export interface MinhasHoras {
  inicio: string;
  fim: string;
  limiteDiarioMinutos: number;
  totalMinutos: number;
  dias: TotalDoDia[];
  apontamentos: Apontamento[];
}

export interface TotalPorPessoa {
  usuarioId: number;
  nome: string;
  minutos: number;
}

export interface TotalPorProjeto {
  projetoId: number;
  nome: string;
  cliente: string | null;
  minutos: number;
}

export interface LinhaConsolidado {
  projetoId: number;
  projetoNome: string;
  usuarioId: number;
  usuarioNome: string;
  minutos: number;
}

export interface Consolidado {
  inicio: string;
  fim: string;
  totalMinutos: number;
  porPessoa: TotalPorPessoa[];
  porProjeto: TotalPorProjeto[];
  linhas: LinhaConsolidado[];
}

/** Tarefa do projeto para escolher ao apontar horas. */
export interface TarefaOpcao {
  id: number;
  titulo: string;
  quadroId: number;
  quadroNome: string;
  colunaNome: string;
}
