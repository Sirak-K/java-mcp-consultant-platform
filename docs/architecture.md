# Architecture

This project is a Java/Spring Boot backend with a React operations UI and a manifest-driven catalog surface. The backend is the main system boundary: it owns the domain workflows, persistence integration, MCP capability surface and operational infrastructure.

## System overview

| Area                    | Role                                                                   |
| ----------------------- | ---------------------------------------------------------------------- |
| `mcp-server/`           | Spring Boot backend and MCP server.                                    |
| `web-app/`              | React Router UI for intake, review and operations workflows.           |
| `catalogs/`             | JSON catalogs for semantic data, reference data and runtime contracts. |
| `compose.keycloak.yaml` | Optional local Keycloak service for OAuth2/OIDC development.           |

## Backend structure

The backend is split into `domain/` and `foundation/`.

`domain/` owns marketplace-specific behavior and language:

- `customers` - customer records used by mission intake.
- `missions` - mission proposals, mission slots and review workflows.
- `candidate_profiles` - candidate intake, profile records and CV-derived details.
- `matching` - candidate-to-mission-slot match scoring and inspection.
- `match_notifications` - match notification preview and delivery workflows.
- `candidate_presentation` - candidate presentation artifact preparation, generation support and result recording.
- `reference_data` - shared skills, roles and lookup data used by several domains.
- `system_operations` - operator-facing runtime and operational views.
- `shared_kernel` - small shared domain primitives.

`foundation/` owns reusable server infrastructure:

- MCP transport, tool, resource and prompt interfaces.
- Spring integration and server process orchestration.
- Security, control-plane, audit, logging and observability support.
- AI generation integration points that are not tied to a single domain workflow.

## Frontend structure

The React app has public intake routes for customers and candidates, plus an operations portal for runtime overview, diagnostics, triage, matching, mission review, candidate review, match notification previews and candidate presentation artifacts.

The frontend is a consumer of backend behavior. It should not own backend business rules, persistence semantics or MCP capability contracts.

## Catalogs

The `catalogs/` directory contains structured JSON data loaded by backend runtime services. `catalogs/catalog_manifest.json` is the top-level manifest that records catalog ownership, required root fields, runtime consumers and MCP exposure where relevant.

Catalog data is used for semantic data, reference data and runtime-contract information. Executable behavior stays in Java code; stable meaning and catalog-like semantics stay in catalog files when practical.

## Persistence

The backend uses JPA repositories and Flyway migrations, with PostgreSQL as the normal local development/runtime database. H2 is available for in-memory mode where that is useful for local development or tests.

Persistence belongs behind domain/application services. Domain workflows should not depend on frontend state or a specific MCP client.

## Security and operations

Security is environment-driven and supports local development auth, OAuth2/OIDC integration and optional Keycloak-based local identity setup. Operational surfaces include health, metrics, logging, audit and system-operations views.

These areas support the server; they are not separate product domains.

## Core workflow shape

Most runtime flows follow this shape:

1. A UI route, HTTP endpoint or MCP request enters the backend.
2. Spring integration maps the request into an application/domain service.
3. Domain logic uses repositories, catalogs and foundation services as needed.
4. The backend persists state, emits operational signals and returns a response or MCP capability outcome.

MCP-specific behavior is described in [mcp-server.md](mcp-server.md).
