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

## Appointment reminders

Supply `reminderOffsetsMinutes` on create or update, for example `[1440, 120]`.
Omitting the field on create uses 24 hours. On update, omission preserves the current
settings. Send `[]` to disable reminders for the appointment.

Email delivery is opt-in. Set `REMINDERS_DELIVERY_ENABLED=true`, `REMINDERS_FROM`,
and `SPRING_MAIL_HOST` before starting the application. Set `SPRING_MAIL_PORT` and
configure credentials and TLS using Spring Boot mail properties as required by your
SMTP server. The sender polls PostgreSQL every minute by default
(`REMINDERS_POLL_INTERVAL=PT1M`).

The sender retries a failed submission on later polls while the appointment is confirmed
and still in the future. If SMTP accepts a message and the database transaction fails
before committing, a later poll may send the message again.

## Appointment slot conflicts

Non-cancelled appointments for the same account cannot overlap. Creation, rescheduling,
and duration changes return `409 Conflict` when the requested interval is occupied.
Intervals that only touch at an endpoint are allowed. Cancelling releases the interval
and preserves the appointment record.

On startup, the application installs the PostgreSQL exclusion constraint after Hibernate
updates the tables. The database needs the `btree_gist` extension. The application can
install it if its database role has `CREATE` permission, or an administrator can install
it beforehand. If older records overlap using the 30-minute fallback, the constraint
installation fails and leaves the records and prior index intact. Use this query to
identify conflicts and resolve them before retrying startup.

```sql
WITH occupied AS (
    SELECT id, user_id, tsrange(
        appointment_date,
        appointment_date + COALESCE(duration_minutes, 30) * INTERVAL '1 minute',
        '[)'
    ) AS slot
    FROM appointments
    WHERE status <> 'CANCELLED'
)
SELECT a.user_id, a.id AS first_appointment_id, b.id AS second_appointment_id
FROM occupied a
JOIN occupied b ON b.user_id = a.user_id AND b.id > a.id AND a.slot && b.slot;
```

## Weekly availability preview

Use the authenticated `PUT /users/me/availability` endpoint to replace your weekly schedule.
For example:

```json
{
  "timeZone": "America/Sao_Paulo",
  "durationMinutes": 45,
  "windows": [
    {"dayOfWeek": "MONDAY", "start": "10:00", "end": "12:00"},
    {"dayOfWeek": "MONDAY", "start": "13:00", "end": "18:00"}
  ]
}
```

A weekday with no windows has no candidates. Use `GET /users/me/availability` to read
the schedule and `GET /users/me/availability/candidate-slots?date=2030-01-07` to preview
future intervals that fit fully within a window. The date uses the configured time zone,
and the response contains UTC instants. The preview does not account for existing
appointments and is not a public booking endpoint.

Use `GET /users/me/availability/slots?date=2030-01-07` to exclude intervals occupied
by your non-cancelled appointments. The candidate preview remains available for checking
the weekly schedule on its own.

Use `durationMinutes` (1 to 1440 whole minutes) when creating or editing an appointment.
Creation defaults to 30 minutes, and appointments saved before the duration field was
added are treated as 30 minutes. Omitting the field when editing preserves the current
duration. Appointment start timestamps are interpreted as UTC for slot calculations.
Intervals that only touch at their endpoints do not overlap.
