# booking-api

Spring Boot (Java) + PostgreSQL service, built with Gradle.

Required environment variables (see `.env.example`; no defaults): `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.

- Build: `gradle bootJar`
- Unit tests (JUnit 5): `gradle test`
- E2E (REST Assured + Testcontainers, needs Docker): `gradle e2e`
- Run: `gradle bootRun`; `GET /status` returns service status.
