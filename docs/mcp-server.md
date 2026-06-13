# MCP Server

This document describes the server-side MCP surface at a client-neutral level. The project may be used by different MCP clients, but the backend should not depend on one specific client implementation.

## Server responsibility

The MCP server exposes selected backend workflows as capabilities. It is responsible for:

- registering tools, resources and prompts;
- routing capability calls to the correct backend behavior;
- validating inputs and producing structured outcomes;
- applying transport, security, concurrency and runtime policies;
- keeping domain logic inside domain services rather than inside client adapters.

## Transports

The backend is configured around HTTP-based MCP access. Streamable HTTP is the primary local transport surface, and WebSocket support exists as a transport option but is not the default local path.

Transport code belongs to foundation-level infrastructure. Domain modules should not know which MCP client or transport was used to invoke a workflow.

## Tools

Tools expose executable server behavior. Current tool responsibilities include:

- mission proposal input conversion;
- candidate profile inspection;
- match discovery inspection and score breakdown;
- match notification preview and send workflows;
- candidate presentation generation preparation and result recording;
- company identity lookup;
- operational health checks.

A tool should be thin: it validates and routes a capability call, while domain/application services own the actual business behavior.

## Resources

Resources expose readable server information. The backend includes resource providers for runtime overview, recent audit data, tool catalogs, MCP resource manifests and server capability metadata.

Resources should describe server state or contracts without becoming a second implementation of business logic.

## Prompts

Prompt infrastructure belongs to the MCP server foundation. Prompt definitions, rendering and argument schemas are handled as server-side contracts, not as client-owned behavior.

Domain-specific prompt usage should stay tied to the workflow that needs it, while shared prompt registration and rendering support remains in foundation.

## Capability lifecycle

A typical MCP capability flow is:

1. A client sends a capability request through the active transport.
2. Foundation-level routing identifies the target tool, resource or prompt.
3. Runtime policy checks validation, concurrency, cancellation and error mapping rules.
4. The capability delegates to the correct application/domain service.
5. The server records operational signals and returns a structured result.

This lifecycle keeps MCP protocol concerns separate from consultant-platform workflow logic.

## Exposed workflow areas

The MCP surface is centered on workflows that are meaningful without assuming a specific UI:

- candidate profile inspection;
- mission proposal intake support;
- candidate-to-mission matching inspection;
- match notification preview and delivery;
- candidate presentation generation support;
- runtime and operations inspection.

## Client neutrality

MCP clients are consumers of server capabilities. A client may provide chat, automation, local AI runtime integration or UI-driven orchestration, but the server should continue to own:

- capability contracts;
- validation and authorization boundaries;
- domain workflow behavior;
- persistence and catalog usage;
- operational logging and observability.

Client-specific setup belongs outside the domain model. If a client integration needs reusable infrastructure, it should be isolated behind foundation-level adapters rather than embedded into a domain workflow.
