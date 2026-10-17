# Piste and Skier CRUD integration tests (Phase 4)

Classes: `PisteCrudIntegrationTest` and `SkierCrudIntegrationTest` in `src/test/java/com/example/gestionstationskii/services/`.

Each class tests create, read, update and delete through the real service and JPA repository, without Mockito. The `crud-test` profile uses a test-scoped H2 in-memory database, never `skidb`. Each test rolls back; flush/clear checks ensure assertions read persisted state rather than cached entities. This does not replace MySQL-specific or concurrent integration testing.

From `backend/`, run only these eight tests (MySQL not required):

```bash
export JAVA_HOME="$HOME/.jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"
mvn -Dtest=PisteCrudIntegrationTest,SkierCrudIntegrationTest test
```

For the complete suite, the existing `contextLoads` test still requires MySQL and the regular database credentials:

```bash
set -a
source .env
set +a
export PATH="$JAVA_HOME/bin:$PATH"
mvn clean test
```

## Mockito CRUD unit tests (Phase 5)

`PisteCrudMockitoTest` and `SkierCrudMockitoTest` each cover create, read (by ID and list), update and delete. They use `@ExtendWith(MockitoExtension.class)`, `@Mock` and `@InjectMocks`, with real service logic and mocked dependencies. Assertions check returned values and changed fields; `verify` checks repository calls. Deletes verify `delete(entity)`, matching the actual services. No Spring context or database is started.

Run only the eight Mockito tests:

```bash
mvn -Dtest=PisteCrudMockitoTest,SkierCrudMockitoTest test
```

Run all sixteen Piste/Skier CRUD tests without MySQL:

```bash
mvn -Dtest=PisteCrudIntegrationTest,SkierCrudIntegrationTest,PisteCrudMockitoTest,SkierCrudMockitoTest test
```

Use Java 17 as shown above. Verified: all eight Mockito tests pass and `mvn clean test` passes all 26 project tests. These happy-path tests do not prove transaction/concurrency behavior or cover every error branch.

## JaCoCo coverage (Phase 6)

JaCoCo attaches its agent before tests and generates HTML/XML/CSV reports during the Maven `test` phase. No production dependencies or coverage exclusions were added. The report includes the entire backend; inspect your own services in the `com.example.gestionstationskii.services` package.

Load the Java/database environment as above, then:

```bash
mvn clean test
xdg-open target/site/jacoco/index.html
```

The XML report for later SonarQube integration is `target/site/jacoco/jacoco.xml`.

Verified with all 26 tests passing:

| Service | Line coverage | Branch coverage |
| --- | --- | --- |
| PisteServicesImpl | 42/44 (95.5%) | 7/18 (38.9%) |
| SkierServicesImpl | 54/75 (72.0%) | 10/34 (29.4%) |

Coverage measures exercised code, not correctness. Error paths, subscription/course assignments and other non-CRUD behavior remain only partially covered. No minimum coverage gate is configured. A targeted test run regenerates the report for only that run; run the full suite again before taking your final screenshot. Keep a screenshot of the services coverage page as course evidence.

Next: DevOps tool containers (Phase 7), then SonarQube (Phase 8).
