# Appointment System

A Java 21 and Spring Boot REST API for independent professionals to manage their agenda
and share available appointment times. It is a portfolio project focused on reliable
scheduling, tenant isolation, and tests against a real PostgreSQL database.

## Project status

The latest published release is [v0.6.0](https://github.com/pedro-vieiraf/appointment-system/releases/tag/v0.6.0).
It introduced configurable email reminders. This development snapshot also includes weekly
availability, per-appointment duration, protection against overlapping appointments,
revocable sharing links, and direct guest requests with temporary holds.

## Capabilities

- **Private agenda.** Professionals can register, authenticate with JWT, manage their
  profile, and create, search, reschedule, confirm, cancel, complete, or delete their
  own appointments. Listing supports date and title filters, sorting, and pagination.
- **Availability.** Professionals set a time zone, a default slot duration, and
  multiple working windows per weekday. The free-slot endpoint subtracts intervals
  occupied by their non-cancelled appointments.
- **Booking consistency.** PostgreSQL rejects overlapping intervals for the same
  professional, including concurrent writes. Consecutive intervals may touch at
  their endpoints. Cancellation releases the slot, and optimistic locking prevents
  concurrent changes from silently overwriting each other.
- **Email reminders.** Confirmed appointments can send reminders to their owner using
  PostgreSQL and SMTP. The default is 24 hours before the appointment. Multiple
  reminder offsets and an explicit opt-out are supported.
- **Public requests.** Guests can view free slots without logging in
  and request a slot without an email verification step. The professional can approve or
  decline the request using the existing appointment actions. The guest receives the
  decision by email when delivery is enabled.

## Run locally

Requirements are Java 21, a PostgreSQL 17 instance, and network access to Maven
repositories on the first build. The Maven Wrapper is included. Docker Compose is
an option for running PostgreSQL.

```bash
docker compose up -d postgres
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Run these commands from the repository root. An existing PostgreSQL installation works
too. The `dev` profile expects the `appointment_system` database at `localhost:5432`.
Override the connection with `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
and `SPRING_DATASOURCE_PASSWORD` as needed. Use a strong, deployment-specific
`JWT_SECRET` outside local development.

- [Swagger UI](http://localhost:8080/swagger-ui/index.html)
- [OpenAPI document](http://localhost:8080/v3/api-docs)

Swagger is enabled by the `dev` profile. Register with `POST /auth/register`, log in
with `POST /auth/login`, and paste the returned `accessToken` into **Authorize** to
try protected routes.

The appointment list supports `title`, inclusive `startDate` and `endDate`,
and the standard `page`, `size`, and `sort` parameters. The default is
`page=0&size=10&sort=appointmentDate,asc`.

### Configure email

Reminders and guest decision emails are independently opt-in. Configure the SMTP
server with `SPRING_MAIL_HOST` and `SPRING_MAIL_PORT`. Set credentials and TLS through
the Spring Boot mail configuration if your provider requires them. Use a local SMTP
capture server when demonstrating the flow.

| Setting | Purpose |
| --- | --- |
| `REMINDERS_DELIVERY_ENABLED=true` | Enables the reminder dispatcher. |
| `REMINDERS_FROM` | Sender address for reminder emails. |
| `REMINDERS_POLL_INTERVAL` | PostgreSQL polling interval. Defaults to `PT1M`. |
| `BOOKING_REQUESTS_ENABLED=true` | Enables direct guest requests. |
| `BOOKING_REQUESTS_HOLD_TTL` | Maximum review window. Defaults to `PT2H`. |
| `BOOKING_REQUESTS_RATE_LIMIT` | Maximum attempts per client address and agenda. Defaults to 5. |
| `BOOKING_REQUESTS_RATE_WINDOW` | Fixed limit window from the first attempt. Defaults to `PT15M`. |
| `BOOKING_NOTIFICATIONS_ENABLED=true` | Delivers decision emails. |
| `BOOKING_NOTIFICATIONS_FROM` | Sender address for decision emails. |
| `BOOKING_NOTIFICATIONS_POLL_INTERVAL` | Decision queue polling interval. Defaults to `PT1M`. |

Use ISO 8601 durations for the timing settings. With reminders enabled, a failed
SMTP submission is retried while the appointment remains confirmed and in the future.
If SMTP accepts a message and the database transaction fails before commit, a later
poll can send it again.

## Demonstrate the workflow

### 1. Configure the professional's availability

Send an authenticated `PUT /users/me/availability` request:

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

Use a future Monday for the examples below. A weekday with no windows has no slots.
Read the saved schedule with `GET /users/me/availability`. Use
`GET /users/me/availability/candidate-slots?date=<local-date>` to inspect the weekly
grid and `GET /users/me/availability/slots?date=<local-date>` to see the intervals
left after subtracting appointments. Both return UTC start and end instants.

### 2. Create or share a booking

**Manual appointment.** Use authenticated `POST /appointments` with a title and an
`appointmentDate`. The timestamp has no offset in this API and is interpreted as
UTC for slot calculations. Set `durationMinutes` to a whole number from 1 to 1,440.
The default is 30 minutes, including for older records without a stored duration.
When editing, omission preserves the current duration.
Appointment responses include `appointmentStart` with an explicit offset and
`appointmentTimeZone` for display. The existing `appointmentDate` field remains in UTC
for API clients. Guest decision emails use the booking time zone.

If an interval overlaps another appointment for the same professional, the API
returns `409 Conflict` with a `message` that explains what to change. For reminders,
pass `reminderOffsetsMinutes`, such as `[1440, 120]`. Omission on creation defaults
to 24 hours, omission on editing preserves the settings, and `[]` turns reminders off.

**Guest request.** Send authenticated
`PUT /users/me/availability/share` to obtain a `bookingPath` and a `slotsPath`.
Open `bookingPath` on the app host to choose a date and time and submit a request
with a name and email address. The page uses the professional's time zone and
allows browsing when guest requests are disabled.

For API clients, append
`?date=<local-date>` and open the path on the same API host without authentication.
The response contains only start and end times. The owner can retrieve the link
with `GET /users/me/availability/share` or invalidate it with
`DELETE /users/me/availability/share`. Enabling sharing again creates a new link.

With guest requests enabled, send `POST /public/availability/{linkId}/requests` with
the guest's `name`, `email`, local `date`, and the exact UTC `start` from the slot endpoint.
A successful request returns `201 Created` and holds the slot until the review deadline
or the appointment start, whichever comes first. An address can have only one active
request per agenda. Requests above the rate limit return `429` and `Retry-After`.
The rate limiter uses the network address reported by the server. Configure trusted
proxy handling at deployment when traffic passes through a reverse proxy.

### 3. Decide the request and check the agenda

Use `GET /appointments` as the professional to find the pending appointment and
its guest details. Use `PATCH /appointments/{id}/confirm` to approve it,
`PATCH /appointments/{id}/cancel` to decline or cancel it, and
`PATCH /appointments/{id}/complete` after a confirmed appointment takes place.
Expired holds release their intervals and are cancelled by a scheduled job. With
decision delivery enabled, the guest receives approval or decline by email. The
decision and its notification are saved in one database transaction. The dispatcher
retries failed SMTP submissions after one minute.

To exercise the conflict guarantee, try creating an overlapping appointment for
the same professional with a different start time. It should return `409`. Try an
interval starting at the first appointment's exact end and verify that it succeeds.

## Architecture and engineering decisions

Spring MVC controllers expose DTOs. Services own the application workflows and
transaction boundaries. Domain entities enforce status transitions and working-hour
rules. Spring Data JPA and PostgreSQL provide persistence, while Spring Security
uses JWT for protected routes. Public endpoints expose availability without access
to private appointment or customer records.

PostgreSQL enforces interval exclusion using `btree_gist`. Appointment writes for
one professional are coordinated in the database, and entity version checks catch
stale updates. The Spring scheduler polls the persistent reminder table and uses
row-level locking to coordinate multiple dispatchers.

### Upgrade an existing database

Hibernate updates the tables and the SQL initializer adds the interval constraint.
The PostgreSQL role needs permission to create `btree_gist`, or an administrator
must install the extension beforehand. If the 30-minute fallback reveals overlapping
legacy appointments, constraint installation fails and preserves those records and
the previous index. After the duration column exists, locate the conflicts before
retrying startup:

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

## Tests and CI

Run with a dedicated PostgreSQL test database and a test-only JWT secret:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/appointment_system_test
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export JWT_SECRET=local-test-jwt-key-at-least-32-bytes-long
./mvnw clean verify checkstyle:check
```

Create the test database before running the command. The suite combines unit tests,
MockMvc controller tests, and PostgreSQL integration tests for ownership, overlapping
writes, reminder concurrency, and public booking. The automated email tests use a
simulated SMTP transport. The [GitHub Actions workflow](.github/workflows/ci.yml)
runs Maven verification with PostgreSQL 17 and checks style on pushes and PRs
targeting `develop`. Open `target/site/jacoco/index.html` for the coverage report.

## Roadmap

| Priority | Remaining work |
| --- | --- |
| **1. Prepare for public deployment** | Document deployment configuration, database migrations, and operational checks. |

Potential follow-ups include date-specific availability overrides and one-step
confirmation for appointments booked manually by the professional.
