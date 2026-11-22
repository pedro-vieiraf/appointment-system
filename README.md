# Appointment System

Sistema de gerenciamento de agendamentos desenvolvido com Spring Boot.

O objetivo do projeto é construir uma API REST seguindo práticas utilizadas em ambientes profissionais, incluindo testes automatizados, CI/CD, Docker e boas práticas de arquitetura.

## Tecnologias

- Java 21
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Docker
- Maven
- GitHub Actions

## Arquitetura

O projeto segue uma arquitetura em camadas:

```
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

## Executando localmente

### Pré-requisitos

Antes de executar o projeto, certifique-se de possuir:

- Java 21
- Docker
- Docker Compose

### Subindo o banco de dados

```bash
docker compose up -d --build
```

Caso seja da sua preferência, a aplicação também pode ser executada pelo container Docker.

### Executando a aplicação

```bash
./mvnw spring-boot:run
```

A API estará disponível em:

```
http://localhost:8080
```

## Testes

Para executar os testes automatizados:

```bash
./mvnw test
```

### Coverage

With the test database running, use `./mvnw verify` to generate the JaCoCo report.
Open `target/site/jacoco/index.html` to review line and branch coverage.
Use `./mvnw clean verify` when you need a fresh report after switching branches
or changing the code under test.

## CI/CD

O projeto utiliza GitHub Actions para:

- Executar testes automaticamente;
- Validar o build da aplicação;
- Garantir que Pull Requests estejam funcionando corretamente antes do merge.

## Explore a API no Swagger

Com o PostgreSQL disponível em `localhost:5432` e o banco `appointment_system` criado, inicie
a aplicação com o perfil de desenvolvimento:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Especificação OpenAPI: `http://localhost:8080/v3/api-docs`

A configuração padrão desabilita a documentação. O perfil `dev` habilita a interface e
a especificação sem exigir login para consultá-las.

1. Crie uma conta com `POST /auth/register` e faça login com `POST /auth/login`.
2. Copie o valor de `accessToken` e cole-o em **Authorize** no Swagger UI. A interface
   envia o cabeçalho `Authorization: Bearer <token>`.
3. Experimente `GET /appointments`. Use `title` para buscar por parte do título e
   `startDate` e `endDate` para limitar o período. As datas são inclusivas e devem
   incluir a hora local, por exemplo `2026-12-01T09:00:00`.
   A primeira página é `page=0`. O padrão é `size=10` e `sort=appointmentDate,asc`.

Um agendamento nasce como `PENDING`. Use `PATCH /appointments/{id}/confirm` para
confirmá-lo, `PATCH /appointments/{id}/cancel` para cancelar um agendamento pendente
ou confirmado e `PATCH /appointments/{id}/complete` para concluir um agendamento
confirmado. Transições inválidas retornam `409 Conflict`.
