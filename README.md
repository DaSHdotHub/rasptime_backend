# RaspTime Backend

Backend service for RFID-based timeclock terminal system.

## Tech Stack

- Java 21
- Spring Boot 4.0.1
- Spring Data JPA
- H2 (development) / PostgreSQL (production)

## Quick Start

```bash
# Run the application
./mvnw spring-boot:run

# Or build and run
./mvnw clean package
java -jar target/timeclock-backend-0.0.1-SNAPSHOT.jar
```

## API Endpoints

| Method | Endpoint      | Description        |
|--------|---------------|--------------------|
| GET    | /api/health   | Health check       |

## Development

Access H2 Console at: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:timeclockdb`
- Username: `sa`
- Password: (empty)