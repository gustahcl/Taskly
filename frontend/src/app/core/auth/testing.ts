import { TokenResponse, Usuario } from './auth.models';

export const USUARIO_MEMBRO: Usuario = {
  id: 1,
  nome: 'Ana',
  email: 'ana@taskly.dev',
  perfil: 'MEMBRO',
  ativo: true,
  criadoEm: '2026-11-24T12:00:00Z',
};

export function tokens(sufixo = '1'): TokenResponse {
  return {
    accessToken: `access-${sufixo}`,
    refreshToken: `refresh-${sufixo}`,
    tipo: 'Bearer',
    accessTokenExpiraEm: '2026-11-24T12:15:00Z',
  };
}

/** Simula uma sessão já salva no navegador. */
export function salvarSessao(usuario: Usuario = USUARIO_MEMBRO): void {
  localStorage.setItem('taskly.accessToken', 'access-salvo');
  localStorage.setItem('taskly.refreshToken', 'refresh-salvo');
  localStorage.setItem('taskly.usuario', JSON.stringify(usuario));
}
