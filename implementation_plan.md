# TicketTide — Implementation Plan

Eventbrite-style ticketing platform built as **five Spring Boot microservices** (auth, events,
ticketing, payments, notifications), containerized with Docker and deployed to AWS
(EC2 for compute, RDS PostgreSQL for data, S3 for event media, API Gateway for routing).

## Key design decisions

| Concern | Decision |
|---|---|
| Language / framework | Java 21, Spring Boot 3.3, Maven multi-module build |
| Service boundaries | `auth-service`, `event-service`, `ticket-service`, `payment-service`, `notification-service`, plus a small `common` library |
| Data | Database-per-service on one RDS PostgreSQL instance (separate logical DBs); schema managed with Flyway |
| AuthN / AuthZ | `auth-service` issues HS256 JWTs (claims: userId, email, role). Every other service validates the token locally with the shared secret — no per-request call back to auth. Roles: `ATTENDEE`, `ORGANIZER`, `ADMIN` |
| Service-to-service | Synchronous REST via Spring `RestClient` on `/internal/**` endpoints protected by a shared `X-Internal-Api-Key` header (never routed through API Gateway) |
| Inventory | Owned by `event-service`. Reservations use a single conditional `UPDATE ... WHERE available >= :qty` so overselling is impossible without distributed locks |
| Order lifecycle | `PENDING_PAYMENT` (10-min hold) → `CONFIRMED` / `EXPIRED` / `CANCELLED` / `REFUNDED`. A scheduled sweeper expires holds and releases seats |
| Payments | `PaymentProcessor` interface with a simulated card processor (deterministic test tokens); `Idempotency-Key` header de-duplicates retries |
| Notifications | Persisted in-app inbox + pluggable `EmailSender` (logging sender by default) |
| Media | Event cover images uploaded to S3 via AWS SDK v2; local dev uses LocalStack |
| Routing | AWS API Gateway (HTTP API) in prod; an nginx container mirrors the same route table locally |
| Infra as code | Terraform for VPC, EC2 (Docker host), RDS, S3, API Gateway |

## Phase 1 — Build skeleton, common library, auth-service
Deliverables:
- Maven parent POM with dependency management; `common` module with JWT verification,
  security filter, `AuthenticatedUser` principal, internal-API-key filter, error model.
- `auth-service`: user registration, login, `/me`, BCrypt passwords, JWT issuance, Flyway schema,
  admin role promotion endpoint.
- Unit tests for token issuance/verification and auth flow.

Files: `pom.xml`, `common/**`, `auth-service/**`.

## Phase 2 — event-service
Deliverables:
- Events (draft → published → cancelled) owned by organizers; search/filter published events with paging.
- Ticket types (name, price, capacity, available) per event.
- Internal reserve/release inventory endpoints with atomic conditional updates.
- S3 cover image upload (presigned GET URLs on read).
- Tests for inventory reservation and event ownership rules.

Files: `event-service/**`.

## Phase 3 — ticket-service
Deliverables:
- Orders with price snapshot, 10-minute hold, cancel, and scheduled expiry sweeper.
- Internal endpoints used by payments: fetch order, mark paid (issues unique ticket codes), mark refunded.
- Attendee ticket wallet; organizer check-in endpoint (single-use ticket codes).
- REST clients for event-service and notification-service.
- Tests for order lifecycle.

Files: `ticket-service/**`.

## Phase 4 — payment-service and notification-service
Deliverables:
- `payment-service`: charge an order with idempotency keys, simulated processor, refunds, payment history.
- `notification-service`: internal send endpoint, persisted inbox, unread count, mark-read, pluggable email sender.
- Tests for payment idempotency/failure paths and notification inbox.

Files: `payment-service/**`, `notification-service/**`.

## Phase 5 — Containers, gateway, AWS infrastructure, docs
Deliverables:
- Multi-stage Dockerfile per service, `docker-compose.yml` (PostgreSQL, LocalStack S3, nginx gateway, five services), `.env.example`.
- AWS: Terraform for VPC, EC2 Docker host (user-data runs compose), RDS PostgreSQL, S3 bucket, API Gateway HTTP API routes.
- `.gitignore`, README with architecture diagram, run and test instructions.

Files: `**/Dockerfile`, `docker-compose.yml`, `gateway/nginx.conf`, `infra/**`, `scripts/**`, `.env.example`, `.gitignore`, `README.md`.
