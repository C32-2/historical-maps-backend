# Runtime Configuration

This project uses environment variables as the runtime source of truth for the application.

There are still two layers to understand:

1. `docker-compose.local.yml` / `docker-compose.prod.yml` configure containers, ports, and volume mounts.
2. The application reads its own runtime settings from environment variables.

`src/main/resources/application.yaml` remains only as minimal Ktor bootstrap config for module wiring.

## Host And Container Paths

`RUNTIME_DIR` points to the runtime root on the host machine.

With the current compose setup:

- PostgreSQL data is stored at `${RUNTIME_DIR}/postgres-data`
- Uploaded map files are stored at `${RUNTIME_DIR}/storage`

The application container mounts `${RUNTIME_DIR}/storage` to `${BASE_MAP_DIRECTORY}` and uses:

- `BASE_MAP_DIRECTORY=/app/data`

Map uploads are then saved as:

- `/app/data/maps/{slug}.pmtiles` inside the container
- `${RUNTIME_DIR}/storage/maps/{slug}.pmtiles` on the host

Example host path:

- `/opt/historical-maps-backend/runtime/storage/maps/test-map.pmtiles`

## Environment Variables

### Compose / host variables

- `RUNTIME_DIR`: host directory used for persistent runtime data
- `POSTGRES_DB`: database name for the Postgres container
- `POSTGRES_USER`: database user for the Postgres container
- `POSTGRES_PASSWORD`: database password for the Postgres container

### Application variables

- `DB_ENABLED`: enables database integration
- `DB_JDBC_URL`: JDBC URL used by the application
- `DB_USER`: database user used by the application
- `DB_PASSWORD`: database password used by the application
- `DB_DRIVER_CLASS_NAME`: JDBC driver class
- `DB_FLYWAY_ENABLED`: enables Flyway migrations on startup
- `DB_FLYWAY_LOCATIONS`: comma-separated Flyway locations
- `BASE_MAP_DIRECTORY`: storage root inside the application container
- `ADMIN_TOKEN`: admin token required for protected endpoints

## Source Of Truth

Use this rule when reasoning about configuration:

- `docker-compose.local.yml` and `docker-compose.prod.yml` are the source of truth for container wiring and mounted directories.
- `.env.local` and production env values are the source of truth for runtime settings.
- `src/main/resources/application.yaml` is not used for database, storage, or admin-token runtime values.
