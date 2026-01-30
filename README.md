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

## Production build

### Create systemd-Service (Linux/Ubuntu)
```bash
sudo nano /etc/systemd/system/rasptime_backend.service
```

Edit paths as needed
```
[Unit]
Description=Rasptime Spring Boot Backend
After=network.target docker.service

[Service]
User=admshaulov
WorkingDirectory=/home/admshaulov/projects/rasptime_backend
ExecStart=/usr/bin/java -jar /home/admshaulov/projects/rasptime_backend/target/rasptime_nackend-0.0.1-SNAPSHOT.jar --server.port=8081
SuccessExitStatus=143
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```
### Activate service 

```bash
# Daemon neu laden
sudo systemctl daemon-reload

# Service aktivieren (startet automatisch bei Boot)
sudo systemctl enable rasptime

# Service starten
sudo systemctl start rasptime
```
