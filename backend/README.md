# Taskly: back-end

API REST em Spring Boot 4 (Java 17).

## Executar

Suba o banco a partir da raiz (`docker compose up -d`) e rode:

```bash
./mvnw spring-boot:run
```

Variáveis de ambiente aceitas: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT`.

## Testes

```bash
./mvnw test
```

Os testes de integração usam Testcontainers e são ignorados quando o Docker não está disponível.
