# Taskly

Plataforma de gestão de projetos e tarefas com quadro kanban e apontamento de horas.

> **Status:** em desenvolvimento. Hoje a API já tem cadastro, login com JWT, refresh token
> e controle de acesso por perfil. O front-end ainda não foi iniciado.

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

Pré-requisitos: Java 17, Docker e Node.js 20+.

```bash
# 1. Banco de dados
cp .env.example .env        # troque a senha
docker compose up -d

# 2. API (http://localhost:8080)
cd backend
./mvnw spring-boot:run
```

Os testes de integração usam Testcontainers e precisam do Docker ligado:

```bash
cd backend
./mvnw test
```

## Roadmap

- [x] Autenticação com JWT e perfis (ADMIN, GERENTE, MEMBRO) no back-end
- [ ] Telas de login e layout no Angular
- [ ] Projetos e membros
- [ ] Quadros e tarefas com kanban (drag-and-drop)
- [ ] Apontamento de horas
- [ ] Dashboard
- [ ] Testes, CI e deploy
- [ ] Tempo real (WebSocket) e histórico de alterações

## Convenções

- Branches: `main` (estável), `dev` (integração) e `feature/*`
- Commits no padrão [Conventional Commits](https://www.conventionalcommits.org/pt-br/)
