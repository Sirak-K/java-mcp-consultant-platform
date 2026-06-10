# Java MCP Consultant Platform

A Java/Spring Boot project for consultant marketplace workflows. The backend is a Model Context Protocol (MCP) server, and the frontend is a React-based UI for domain-specific and user-initiated operations.

Swedish version: [README_sv.md](README_sv.md)

More detailed documentation is available in [`docs/`](docs/).

## Highlights

- Java 21 and Spring Boot backend design.
- MCP server surface with tools, resources, prompts and HTTP transport support.
- Business flows for customers and candidates.
- Manifest-driven JSON catalogs for semantic data, reference data and runtime contracts.
- React Router operations UI for public intake, operator review and runtime visibility.

## Repository Overview

| Path          | Responsibility                                                  |
| ------------- | --------------------------------------------------------------- |
| `mcp-server/` | Spring Boot backend module and MCP server implementation.       |
| `web-app/`    | React Router frontend for public intake and operator workflows. |
| `catalogs/`   | Manifest-driven JSON catalogs used by backend runtime surfaces. |

## Backend

The backend is organized around domain-specific business flows and exists on top of a infrastructurally fundamental surface for running the MCP Server.

### foundation/

The foundation layer covers the core components of the MCP server; MCP transport and capability infrastructure, security, observability, logging, server process behavior and Spring integration.

### domain/

At a high level, the domain side handles:

- customer and mission intake;
- mission proposals, mission slots and review flows;
- candidate intake, candidate profiles and CV-derived details;
- candidate-to-mission matching and score inspection;
- match notification preview and delivery;
- candidate presentation artifacts;
- shared reference data such as skills and roles;
- operator-facing system views.

## Frontend

The frontend is a React Router application with public entry points for customer/candidate intake and an operator portal for overview, diagnostics, triage, matching, mission review, candidate review, match notification previews and candidate presentation artifacts.

## Tech stack

| Area            | Stack                                                                                     |
| --------------- | ----------------------------------------------------------------------------------------- |
| Backend         | Java 21, Spring Boot, Maven, Spring MVC, JPA, Flyway, Actuator, Micrometer, OpenTelemetry |
| Persistence     | PostgreSQL for local development/runtime, H2 available for in-memory mode                 |
| Frontend        | React 19, React Router 7, TypeScript, Vite, Tailwind CSS                                  |
| Runtime support | Docker, Docker Compose, Keycloak, JSON catalogs                                           |

## Verification

Backend tests:

```bash
mvn -B -pl mcp-server -am test
```

Frontend typecheck and build:

```bash
npm --prefix web-app run typecheck
npm --prefix web-app run build
```

## Local configuration

Local runtime configuration is environment-driven. Use `.env.example` as the reference for backend HTTP, PostgreSQL, operator development auth, MCP transport, AI-generation runtime, notification mail and optional Keycloak settings.

## Documentation

- [Architecture](docs/architecture.md)
- [MCP server](docs/mcp-server.md)

## Project status

This repository is under active development.
