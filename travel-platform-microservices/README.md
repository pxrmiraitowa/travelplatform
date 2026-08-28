# Travel Platform Microservices

This directory is the microservice skeleton for role D. It keeps the original monolith untouched and provides isolated modules for later controller, service, mapper, entity, dto, and vo migration.

## Modules

| Module | Port | Responsibility |
| --- | ---: | --- |
| `common-lib` | - | Shared result model, constants, common exception handling, and basic health VOs |
| `user-service` | 8101 | User, authentication, member profile, and admin user management |
| `product-service` | 8102 | Travel products, routes, hotels, tickets, inventory, and price query |
| `order-service` | 8103 | Order creation, order status, simulated payment, refund, and after-sales flow |
| `content-trip-service` | 8104 | Guides, notes, comments, favorites, itinerary, and recommendation-facing content |

## Public Endpoints

Each service currently exposes two baseline endpoints:

| Endpoint | Description |
| --- | --- |
| `GET /api/public/health` | Service health status |
| `GET /api/public/version` | Service name, version, and startup time |

## Build

Use JDK 17 for the normal build path:

```bash
mvn compile
```

On the current local machine, only JDK 24 is available. If the full Maven reactor build cannot resolve `common-lib` from `target/classes`, build through the local Maven repository first:

```bash
mvn install -N
mvn -pl common-lib install
mvn -pl user-service,product-service,order-service,content-trip-service compile
```

## Local Integration

Initialize local MySQL databases before starting the services:

```powershell
cd C:\Users\han\Desktop\travelplatform\travel-platform-microservices
powershell -ExecutionPolicy Bypass -File .\scripts\init-local-databases.ps1
```

Start the four business services and the gateway:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-local-services.ps1
```

Run the gateway smoke test:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-gateway.ps1
```

The smoke test uses `demo_user / 123456` and `admin / 123456` by default.

## Containerization and CI/CD

The GitHub Actions workflow at `.github/workflows/ci-cd.yml` runs for pushes to
`dev` and `codex/**`, pull requests targeting `dev`, and manual dispatches.

The pipeline performs the following stages:

1. Builds the microservice reactor and frontend.
2. Runs backend and frontend unit tests and uploads test evidence.
3. Builds and pushes versioned images for the four business services, gateway,
   and frontend to GitHub Container Registry.
4. Creates an isolated Kind cluster, initializes MySQL, deploys all components,
   checks their health, and runs the frontend API regression suite.
5. Uploads Kubernetes diagnostics when deployment verification fails.

Image publishing and deployment are skipped for pull requests. Push builds use
both an immutable `sha-<commit>` tag and a readable `0.1.<run-number>` tag.

## Next Migration Order

1. Move shared response, constants, and exceptions into `common-lib`.
2. Migrate user authentication and user profile APIs into `user-service`.
3. Migrate product query APIs into `product-service`.
4. Migrate order creation and order status APIs into `order-service`.
5. Migrate guides, comments, favorites, and itinerary APIs into `content-trip-service`.
