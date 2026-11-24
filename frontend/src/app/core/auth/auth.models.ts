export type Perfil = 'ADMIN' | 'GERENTE' | 'MEMBRO';

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  perfil: Perfil;
  ativo: boolean;
  criadoEm: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tipo: string;
  accessTokenExpiraEm: string;
}

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface CadastroRequest {
  nome: string;
  email: string;
  senha: string;
}

/** Corpo de erro no formato Problem Details (RFC 9457) devolvido pela API. */
export interface ProblemDetail {
  status: number;
  detail?: string;
  erros?: Record<string, string>;
}
