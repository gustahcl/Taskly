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

Envie o access token no header `Authorization: Bearer <token>`. Os erros seguem o formato
Problem Details (RFC 9457).

## Testes

```bash
./mvnw test
```

Os testes de integração usam Testcontainers e são ignorados quando o Docker não está disponível.
