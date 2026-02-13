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

### Base URL
```
http://localhost:8081/api
```

---

### Terminal Endpoints

These endpoints are called by the Raspberry Pi timeclock terminal.

#### Get User Status
```
GET /terminal/user?rfid={rfidTag}
```

**Response 200:**
```json
{
  "userId": 1,
  "displayName": "Max Mustermann",
  "role": "USER",
  "clockedIn": false
}
```

**Response 404:** Unknown RFID tag

---

#### Punch Clock (In/Out)
```
POST /terminal/punch
Content-Type: application/json
```

**Request:**
```json
{
  "rfid": "ABC123",
  "breakMinutes": 30
}
```
`breakMinutes` is optional, used for clock-out.

**Response 200:**
```json
{
  "displayName": "Max Mustermann",
  "action": "CLOCK_IN",
  "timestamp": "2026-01-31T08:00:00",
  "breakMinutes": null,
  "message": "Willkommen, Max Mustermann!"
}
```

---

### Admin Endpoints

User management endpoints for the admin interface.

#### List Users
```
GET /admin/users
GET /admin/users?includeInactive=true
```

**Response 200:**
```json
[
  {
    "id": 1,
    "rfidTag": "ABC123",
    "displayName": "Max Mustermann",
    "role": "USER",
    "clockedIn": false,
    "active": true,
    "createdAt": "2026-01-31T06:00:00",
    "updatedAt": null
  }
]
```

---

#### Get User
```
GET /admin/users/{id}
```

**Response 200:** Single user object  
**Response 404:** User not found

---

#### Create User
```
POST /admin/users
Content-Type: application/json
```

**Request:**
```json
{
  "rfidTag": "XYZ789",
  "displayName": "Erika Musterfrau",
  "role": "ADMIN"
}
```
`role` is optional, defaults to `USER`.

**Response 201:** Created user object  
**Response 400:** RFID tag already exists

---

#### Update User
```
PUT /admin/users/{id}
Content-Type: application/json
```

**Request (all fields optional):**
```json
{
  "displayName": "New Name",
  "rfidTag": "NEWRFID",
  "role": "ADMIN",
  "active": true
}
```

**Response 200:** Updated user object  
**Response 404:** User not found

---

#### Delete User (Soft Delete)
```
DELETE /admin/users/{id}
```

**Response 204:** Success (no content)  
**Response 404:** User not found

---

### Health Check
```
GET /health
```

**Response 200:**
```json
{
  "status": "UP",
  "service": "rasptime-backend",
  "timestamp": "2026-01-31T08:00:00"
}
```

## Development

Access H2 Console at: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:timeclockdb`
- Username: `sa`
- Password: (empty)

## Install postgresql
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib

# Start the service
sudo service postgresql start

# Create database and user
sudo -u postgres psql
```
```bash
# SQL commands for the PostgreSQL shell:
CREATE DATABASE rasptime;
CREATE USER rasptime WITH ENCRYPTED PASSWORD 'rasptime';
GRANT ALL PRIVILEGES ON DATABASE rasptime TO rasptime;

-- Required for PostgreSQL 15+
\c rasptime
GRANT ALL ON SCHEMA public TO rasptime;

\q
```


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
ExecStart=/usr/bin/java -jar /home/admshaulov/projects/rasptime_backend/target/rasptime_backend-0.0.1-SNAPSHOT.jar --server.port=8081
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
