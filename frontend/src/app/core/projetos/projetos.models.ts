export type StatusProjeto = 'PLANEJADO' | 'EM_ANDAMENTO' | 'PAUSADO' | 'CONCLUIDO' | 'CANCELADO';
export type PapelProjeto = 'GESTOR' | 'COLABORADOR';
export type Prioridade = 'BAIXA' | 'MEDIA' | 'ALTA' | 'URGENTE';

export const NOMES_STATUS: Record<StatusProjeto, string> = {
  PLANEJADO: 'Planejado',
  EM_ANDAMENTO: 'Em andamento',
  PAUSADO: 'Pausado',
  CONCLUIDO: 'Concluído',
  CANCELADO: 'Cancelado',
};

export const NOMES_PAPEL: Record<PapelProjeto, string> = {
  GESTOR: 'Gestor',
  COLABORADOR: 'Colaborador',
};

export const NOMES_PRIORIDADE: Record<Prioridade, string> = {
  BAIXA: 'Baixa',
  MEDIA: 'Média',
  ALTA: 'Alta',
  URGENTE: 'Urgente',
};

export const STATUS: StatusProjeto[] = ['PLANEJADO', 'EM_ANDAMENTO', 'PAUSADO', 'CONCLUIDO', 'CANCELADO'];
export const PRIORIDADES: Prioridade[] = ['BAIXA', 'MEDIA', 'ALTA', 'URGENTE'];

/** Página de resultados devolvida pela API. */
export interface Pagina<T> {
  conteudo: T[];
  pagina: number;
  tamanho: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface UsuarioResumo {
  id: number;
  nome: string;
  email: string;
}

export interface Membro {
  usuarioId: number;
  nome: string;
  email: string;
  papel: PapelProjeto;
  adicionadoEm: string;
}

export interface ProjetoResumo {
  id: number;
  nome: string;
  cliente: string | null;
  status: StatusProjeto;
  dataInicio: string | null;
  dataFim: string | null;
  totalMembros: number;
  meuPapel: PapelProjeto | null;
}

export interface Projeto {
  id: number;
  nome: string;
  descricao: string | null;
  cliente: string | null;
  status: StatusProjeto;
  dataInicio: string | null;
  dataFim: string | null;
  criadoEm: string;
  meuPapel: PapelProjeto | null;
  podeGerenciar: boolean;
  membros: Membro[];
}

export interface ProjetoRequest {
  nome: string;
  descricao: string | null;
  cliente: string | null;
  dataInicio: string | null;
  dataFim: string | null;
  status: StatusProjeto;
}

export interface FiltroProjetos {
  busca?: string;
  status?: StatusProjeto | null;
  pagina: number;
  tamanho: number;
  /** Ex.: "nome,asc". */
  ordenacao?: string;
}

export interface QuadroResumo {
  id: number;
  nome: string;
  criadoEm: string;
}

export interface TarefaResumo {
  id: number;
  colunaId: number;
  titulo: string;
  prioridade: Prioridade;
  /** Data no formato yyyy-MM-dd. */
  prazo: string | null;
  posicao: number;
  responsavel: UsuarioResumo | null;
  totalComentarios: number;
}

export interface Coluna {
  id: number;
  nome: string;
  posicao: number;
  tarefas: TarefaResumo[];
}

export interface Quadro {
  id: number;
  nome: string;
  projetoId: number;
  projetoNome: string;
  podeGerenciar: boolean;
  colunas: Coluna[];
  membros: Membro[];
}

export interface Tarefa {
  id: number;
  quadroId: number;
  colunaId: number;
  colunaNome: string;
  titulo: string;
  descricao: string | null;
  prioridade: Prioridade;
  prazo: string | null;
  posicao: number;
  responsavel: UsuarioResumo | null;
  criadoPor: UsuarioResumo;
  criadoEm: string;
  atualizadoEm: string;
  podeExcluir: boolean;
}

export interface TarefaRequest {
  titulo: string;
  descricao: string | null;
  responsavelId: number | null;
  prioridade: Prioridade;
  prazo: string | null;
}

export interface Comentario {
  id: number;
  autor: UsuarioResumo;
  texto: string;
  criadoEm: string;
  podeExcluir: boolean;
}
