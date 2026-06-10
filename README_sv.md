# Java MCP Consultant Platform

Ett Java/Spring Boot-projekt för affärsoperationer inom en konsultförmedlingsplattform. Backend är centraliserat kring en Model Context Protocol-server (MCP), och användargränssnittet är ett React-baserat UI för hela plattformen.

Engelsk version: [README.md](README.md)

Mer detaljerad dokumentation är tillgänglig i [`docs/`](docs/).

## Projektöversikt

- Backenddesign med Java 21 och Spring Boot.
- MCP-serveryta med tools, resources, prompts och HTTP-transportstöd.
- Affärsflöden för kunder och kandidater.
- Manifeststyrda JSON-kataloger för semantisk data, referensdata och runtime-kontrakt.
- React Router-baserat operationsgränssnitt för public intake, operator review och runtime visibility.

## Repo-översikt

| Sökväg        | Ansvar                                                                |
| ------------- | --------------------------------------------------------------------- |
| `mcp-server/` | Spring Boot-backendmodul och MCP-serverimplementation.                |
| `web-app/`    | React Router-frontend för public intake och operator workflows.       |
| `catalogs/`   | Manifeststyrda JSON-kataloger som används av backendens runtime-ytor. |

## Backend

Backenden är organiserad kring domänspecifika verksamhetsflöden och vilar på en infrastrukturellt grundläggande yta för att köra MCP-servern.

### foundation/

Foundation-delen täcker allt kring MCP-servern; MCP transport och serverförmågor, säkerhet, observabilitet, loggning, serverprocessbeteende och Spring integration.

### domain/

På hög nivå hanterar domain-sidan:

- uppdragsgivare och uppdragsintag;
- uppdragsförslag, uppdragsplatser och granskningsflöden;
- kandidatintag, kandidatprofiler och CV-baserade detaljer;
- matchning mellan kandidater och uppdrag samt inspektion av matchningspoäng;
- förhandsgranskning och leverans av matchningsnotifieringar;
- kandidatpresentationsartefakter;
- gemensam referensdata som kompetenser och roller;
- systemvyer för operatörer.

## Frontend

Frontenden är en React Router-applikation med publika ingångar för customer/candidate intake och en operator portal för overview, diagnostics, triage, matching, mission review, candidate review, match notification previews och candidate presentation artifacts.

## Tech stack

| Yta             | Stack                                                                                     |
| --------------- | ----------------------------------------------------------------------------------------- |
| Backend         | Java 21, Spring Boot, Maven, Spring MVC, JPA, Flyway, Actuator, Micrometer, OpenTelemetry |
| Persistence     | PostgreSQL för lokal utveckling/runtime, H2 tillgängligt för in-memory mode               |
| Frontend        | React 19, React Router 7, TypeScript, Vite, Tailwind CSS                                  |
| Runtime support | Docker, Docker Compose, Keycloak, JSON-kataloger                                          |

## Verifiering

Backendtester:

```bash
mvn -B -pl mcp-server -am test
```

Frontend typecheck och build:

```bash
npm --prefix web-app run typecheck
npm --prefix web-app run build
```

## Lokal konfiguration

Lokal runtime-konfiguration är miljövariabelstyrd. Använd `.env.example` som referens för backend HTTP, PostgreSQL, operator development auth, MCP transport, AI-generation runtime, notification mail och valfria Keycloak-inställningar.

## Dokumentation

- [Architecture](docs/architecture.md)
- [MCP server](docs/mcp-server.md)

## Projektstatus

Det här projektet är under aktiv utveckling.
