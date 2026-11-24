# Taskly

Plataforma de gestão de projetos e tarefas com quadro kanban e apontamento de horas.

> **Status:** em desenvolvimento. Já funcionam cadastro, login com JWT, refresh token, controle de
> acesso por perfil e a gestão de usuários (ADMIN), na API e no front-end.

## Stack

| Camada | Tecnologias |
|---|---|
| Back-end | Java 17, Spring Boot, Spring Security (JWT), Spring Data JPA, Flyway |
| Front-end | Angular, Angular Material, Angular CDK |
| Banco | PostgreSQL 16 |
| Infra | Docker, Docker Compose, GitHub Actions |
| Testes | JUnit, Mockito, Testcontainers |

## Estrutura

```
Taskly/
├── backend/            # API REST (Spring Boot)
├── frontend/           # Aplicação web (Angular)
├── docker-compose.yml  # PostgreSQL para desenvolvimento
└── .env.example        # Variáveis de ambiente
```

## Como rodar

Pré-requisitos: Java 17, Docker e Node.js 22.12+ ou 24+.

```bash
# 1. Banco de dados
cp .env.example .env        # troque a senha
docker compose up -d

# 2. API (http://localhost:8080)
cd backend
./mvnw spring-boot:run

# 3. Front-end (http://localhost:4200), em outro terminal
cd frontend
npm install
npm start
```

Para ter um ADMIN logo na primeira subida, defina `TASKLY_ADMIN_EMAIL` e `TASKLY_ADMIN_SENHA`
antes de iniciar a API. Os demais usuários se cadastram pela tela e entram como Membro.

Testes:

```bash
cd backend && ./mvnw test                     # usa Testcontainers: precisa do Docker ligado
cd frontend && npm test -- --watch=false      # Vitest
```

## Roadmap

- [x] Autenticação com JWT e perfis (ADMIN, GERENTE, MEMBRO) no back-end
- [x] Telas de login, cadastro e layout no Angular
- [ ] Projetos e membros
- [ ] Quadros e tarefas com kanban (drag-and-drop)
- [ ] Apontamento de horas
- [ ] Dashboard
- [ ] Testes, CI e deploy
- [ ] Tempo real (WebSocket) e histórico de alterações

## Convenções

- Branches: `main` (estável), `develop` (integração) e `feature/*`
- Commits no padrão [Conventional Commits](https://www.conventionalcommits.org/pt-br/)
