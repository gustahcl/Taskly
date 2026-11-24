# Taskly: front-end

Aplicação web em Angular 21 com Angular Material.

## Executar

Com a API rodando em `http://localhost:8080`:

```bash
npm install
npm start          # http://localhost:4200
```

O servidor de desenvolvimento repassa as chamadas `/api` para a API (veja `proxy.conf.json`),
então não é preciso configurar CORS em desenvolvimento.

## Testes

```bash
npm test -- --watch=false
```

Os testes unitários rodam com Vitest.

## Estrutura

```
src/app/
├── core/auth/        # AuthService, interceptor do token, guards de rota
├── core/usuarios/    # Chamadas da API de usuários
├── layout/shell/     # Layout da área logada: menu lateral e cabeçalho
└── features/         # Telas: login, cadastro, início, usuários, acesso negado
```

## Autenticação

- O login guarda o access token e o refresh token no `localStorage`.
- O `authInterceptor` envia o token em toda chamada a `/api`. Se a API responder 401, ele renova o token
  uma vez e repete a requisição. Se a renovação falhar, a sessão é encerrada.
- Guards: `autenticadoGuard` (precisa estar logado), `visitanteGuard` (login e cadastro) e
  `perfilGuard(...perfis)` (restringe por perfil, por exemplo a tela de usuários é só para ADMIN).
