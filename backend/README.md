# Taskly: back-end

API REST em Spring Boot 4 (Java 17).

## Executar

Suba o banco a partir da raiz (`docker compose up -d`) e rode:

```bash
./mvnw spring-boot:run
```

## Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | `localhost`, `5432`, `taskly`, `taskly`, `taskly` | Conexão com o PostgreSQL |
| `SERVER_PORT` | `8080` | Porta da API |
| `JWT_SECRET` | segredo de desenvolvimento | Segredo HMAC dos tokens (mínimo de 32 bytes). **Obrigatório em produção** |
| `JWT_ACCESS_VALIDADE` | `15m` | Validade do access token |
| `JWT_REFRESH_VALIDADE` | `7d` | Validade do refresh token |
| `TASKLY_ADMIN_EMAIL`, `TASKLY_ADMIN_SENHA` | vazio | Se definidos, criam o primeiro ADMIN na subida |

## Autenticação

A API é stateless. O login devolve um **access token** JWT (HS256, 15 min) e um **refresh token**
opaco (7 dias). O refresh token é de uso único: cada renovação devolve um par novo, e reapresentar um token
já usado revoga todas as sessões do usuário. No banco fica só o hash SHA-256 do refresh token.

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/api/auth/cadastro` | público | Cria um usuário com perfil MEMBRO |
| `POST` | `/api/auth/login` | público | Devolve access e refresh token |
| `POST` | `/api/auth/refresh` | público | Troca o refresh token por um par novo |
| `POST` | `/api/auth/logout` | público | Revoga o refresh token |
| `GET` | `/api/usuarios/me` | autenticado | Dados do usuário logado |
| `GET` | `/api/usuarios` | ADMIN | Lista os usuários |
| `PATCH` | `/api/usuarios/{id}/perfil` | ADMIN | Altera o perfil (ADMIN, GERENTE, MEMBRO) |

| `GET` | `/api/usuarios/busca?termo=` | autenticado | Até 10 usuários ativos por nome ou e-mail (para convidar a projetos) |

Envie o access token no header `Authorization: Bearer <token>`.

## Projetos e membros

Cada membro tem um papel no projeto, independente do perfil global: **GESTOR** (edita o projeto e
administra equipe, quadros e colunas) ou **COLABORADOR** (trabalha nas tarefas). Quem cria o projeto vira
gestor, e o projeto sempre mantém pelo menos um. ADMIN enxerga e administra todos os projetos; os demais
usuários só enxergam os projetos de que participam (para os outros, a API responde 404).

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/api/projetos` | autenticado | Lista paginada. Filtros: `status`, `busca` (nome ou cliente), `page`, `size`, `sort` |
| `POST` | `/api/projetos` | ADMIN, GERENTE | Cria o projeto |
| `GET` | `/api/projetos/{id}` | membro | Detalhes, membros e o seu papel |
| `PUT` | `/api/projetos/{id}` | gestor | Atualiza nome, descrição, cliente, datas e status |
| `DELETE` | `/api/projetos/{id}` | gestor | Exclui o projeto com quadros e tarefas |
| `GET` | `/api/projetos/{id}/membros` | membro | Lista os membros |
| `POST` | `/api/projetos/{id}/membros` | gestor | Adiciona um membro (`usuarioId`, `papel`) |
| `PATCH` | `/api/projetos/{id}/membros/{usuarioId}` | gestor | Altera o papel |
| `DELETE` | `/api/projetos/{id}/membros/{usuarioId}` | gestor | Remove o membro |

## Quadros, colunas e tarefas

Um projeto tem um ou mais quadros kanban. Um quadro novo já vem com as colunas *A fazer*, *Em andamento*
e *Concluído*. A ordem de colunas e tarefas é guardada em `posicao` (0 = primeira) e a API mantém as
posições contínuas a cada movimento.

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/api/projetos/{id}/quadros` | membro | Lista os quadros do projeto |
| `POST` | `/api/projetos/{id}/quadros` | gestor | Cria um quadro |
| `GET` | `/api/quadros/{id}` | membro | Quadro completo: colunas, tarefas e membros |
| `PUT` / `DELETE` | `/api/quadros/{id}` | gestor | Renomeia ou exclui o quadro |
| `POST` | `/api/quadros/{id}/colunas` | gestor | Adiciona uma coluna no fim |
| `PUT` | `/api/colunas/{id}` | gestor | Renomeia a coluna |
| `PATCH` | `/api/colunas/{id}/mover` | gestor | Muda a coluna de posição (`posicao`) |
| `DELETE` | `/api/colunas/{id}` | gestor | Exclui a coluna (precisa estar vazia) |
| `POST` | `/api/colunas/{id}/tarefas` | membro | Cria a tarefa no fim da coluna |
| `GET` / `PUT` | `/api/tarefas/{id}` | membro | Detalhes ou edição (título, descrição, responsável, prioridade, prazo) |
| `DELETE` | `/api/tarefas/{id}` | autor ou gestor | Exclui a tarefa |
| `PATCH` | `/api/tarefas/{id}/mover` | membro | Move para `colunaId` na `posicao` indicada (mesma coluna = reordenar) |
| `GET` / `POST` | `/api/tarefas/{id}/comentarios` | membro | Lista ou adiciona comentários |
| `DELETE` | `/api/comentarios/{id}` | autor ou ADMIN | Exclui o comentário |

O responsável por uma tarefa precisa ser membro do projeto. Prioridades: `BAIXA`, `MEDIA`, `ALTA` e `URGENTE`.

## Erros

Todos os erros seguem o formato Problem Details (RFC 9457), com `status` e `detail`:

| Status | Quando |
|---|---|
| 400 | Dados inválidos (com `erros` por campo), JSON malformado ou parâmetro inválido |
| 401 | Sem token, token inválido ou credenciais erradas |
| 403 | Sem permissão para a operação |
| 404 | Recurso inexistente ou que o usuário não pode ver |
| 409 | Conflito (e-mail já cadastrado, membro repetido) |
| 422 | Regra de negócio violada (ex.: remover o único gestor, excluir coluna com tarefas) |

## Testes

```bash
./mvnw test
```

Os testes de integração usam Testcontainers e são ignorados quando o Docker não está disponível.
