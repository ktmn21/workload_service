# Workload Service

A Spring Boot microservice for processing workload-related operations in the Gym CRM ecosystem. It separates workload processing from the main CRM application and supports asynchronous, message-driven communication.

## Related Project

- [Gym CRM](https://github.com/ktmn21/gym-project)

The Gym CRM application acts as the main business system, while this service is responsible for workload-specific processing and related asynchronous operations.

## Features

- Exposes workload-service functionality through a dedicated Spring Boot application.
- Separates workload processing from the core Gym CRM application.
- Supports asynchronous communication through message listeners.
- Uses a layered structure with controllers, services, DAOs, DTOs, and models.
- Handles application-specific exceptions in a dedicated exception package.
- Includes logging support for tracing message and request processing.
- Supports Docker Compose-based local development.
- Includes automated tests under `src/test`.

## Technology Stack

- **Language:** Java
- **Framework:** Spring Boot, Spring MVC
- **Build:** Maven
- **Messaging:** ActiveMQ and asynchronous message processing
- **Containerization:** Docker, Docker Compose
- **Testing:** JUnit and Spring test support
- **Logging:** Spring Boot logging with Logback configuration

## Architecture

The service is organized into separate layers:

```text
Message / HTTP Request
          ↓
Listener / Controller
          ↓
Service
          ↓
DAO / Data Access
          ↓
Persistence or External Integration
```

### Main packages

- `config` — application and messaging configuration.
- `controller` — HTTP endpoints exposed by the service.
- `dao` — data-access components.
- `dto` — request and response objects.
- `exception` — custom exceptions and error handling.
- `listener` — asynchronous message listeners.
- `logging` — logging and processing-trace support.
- `model` — domain models.
- `service` — workload business logic.

## Project Structure

```text
.
├── docker-compose.yml
├── mvnw
├── mvnw.cmd
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/crm/workloadservice/
    │   │   ├── config/
    │   │   ├── controller/
    │   │   ├── dao/
    │   │   ├── dto/
    │   │   ├── exception/
    │   │   ├── listener/
    │   │   ├── logging/
    │   │   ├── model/
    │   │   └── service/
    │   └── resources/
    │       ├── application.yml
    │       └── logback-spring.xml
    └── test/
```

## Prerequisites

- Java Development Kit compatible with the version configured in `pom.xml`.
- Docker Engine and Docker Compose for containerized execution.
- Maven, or use the included Maven Wrapper.
- Access to the messaging infrastructure required by the selected configuration.

## Configuration

Application configuration is located in:

```text
src/main/resources/application.yml
```

Review the configuration before starting the service and provide environment-specific values for messaging, database, application ports, and other integrations as required.

Do not commit passwords, tokens, private keys, or production connection strings to the repository. Prefer environment variables or an external configuration provider for deployed environments.

## Running with Maven

On Linux or macOS:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

If Maven is installed globally:

```bash
mvn spring-boot:run
```

## Running with Docker Compose

Clone the repository and start the configured services:

```bash
git clone https://github.com/ktmn21/workload_service.git
cd workload_service
docker compose up --build
```

Run in the background:

```bash
docker compose up --build -d
```

Stop the services:

```bash
docker compose down
```

## Testing

Run the test suite with the Maven Wrapper:

```bash
./mvnw test
```

On Windows PowerShell:

```powershell
.\mvnw.cmd test
```

Or with installed Maven:

```bash
mvn test
```

Tests are located under:

```text
src/test
```

When testing message-driven functionality, make sure the required messaging dependencies are available or configured for the test environment.

## Integration with Gym CRM

The service is designed to work as a separate backend component alongside Gym CRM:

```text
Gym CRM
   │
   │ asynchronous message
   ▼
ActiveMQ
   │
   ▼
Workload Service
   │
   ▼
Workload processing
```

This separation allows the main CRM application to delegate workload processing without coupling the request flow directly to the workload implementation.

## Development Guidelines

- Keep workload business rules in the service layer.
- Keep message-consumption logic in listener classes.
- Use DTOs at API and messaging boundaries.
- Keep persistence logic inside DAO components.
- Add tests when changing workload behavior or message contracts.
- Use clear message names and document payload expectations.
- Include correlation or transaction identifiers in logs when tracing asynchronous operations.
- Keep configuration environment-specific and free of secrets.

## Future Improvements

- Add complete message-contract documentation and sample payloads.
- Add integration tests using Testcontainers for the messaging broker.
- Add health checks and metrics for message-consumer status.
- Add retry and dead-letter queue documentation.
- Add API documentation if HTTP endpoints are exposed.
- Add CI checks for build, tests, and container startup.

## Author

**Kutman Mukarapov**

- GitHub: [@ktmn21](https://github.com/ktmn21)
- Repository: [workload_service](https://github.com/ktmn21/workload_service)
- Main project: [gym-project](https://github.com/ktmn21/gym-project)
