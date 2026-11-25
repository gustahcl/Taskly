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
├── core/projetos/    # Modelos e chamadas da API de projetos, quadros e tarefas
├── layout/shell/     # Layout da área logada: menu lateral e cabeçalho
├── shared/dialogos/  # Diálogos reutilizáveis (nome, confirmação)
└── features/
    ├── projetos/     # Módulo carregado sob demanda: lista, detalhe, quadro kanban e tarefa
    └── ...           # Login, cadastro, início, usuários, acesso negado
```

## Autenticação

- O login guarda o access token e o refresh token no `localStorage`.
- O `authInterceptor` envia o token em toda chamada a `/api`. Se a API responder 401, ele renova o token
  uma vez e repete a requisição. Se a renovação falhar, a sessão é encerrada.
- Guards: `autenticadoGuard` (precisa estar logado), `visitanteGuard` (login e cadastro) e
  `perfilGuard(...perfis)` (restringe por perfil, por exemplo a tela de usuários é só para ADMIN).

## Projetos e kanban

O módulo `features/projetos` só é baixado quando o usuário entra em `/projetos` (lazy loading):

- `/projetos`: lista paginada com busca, filtro por status e ordenação feitos na API.
- `/projetos/:id`: dados do projeto, quadros e equipe. O gestor edita o projeto, convida pessoas
  (busca por nome ou e-mail) e muda o papel de cada membro.
- `/projetos/:id/quadros/:quadroId`: o quadro kanban. Os cards são arrastados entre colunas com o
  Angular CDK; a tela é atualizada na hora e volta ao estado anterior se a API recusar o movimento.
  Filtros por texto, responsável, prioridade e prazo (atrasadas, próximos 7 dias, sem prazo) funcionam
  no navegador e continuam permitindo arrastar: a posição solta na lista filtrada é convertida para a
  posição real na coluna (`kanban.ts`). Clicar num card abre o modal com edição, responsável e comentários.
