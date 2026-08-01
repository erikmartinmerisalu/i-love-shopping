# ESTValgus Backend

Spring Boot backend service for ESTValgus user authentication and registration.

## Prerequisites

- Java 17 or higher
- Maven 3.8+

## Build

```bash
cd lampify-backend
mvn clean package
```

## Run

Start PostgreSQL first:

```bash
cd backend
docker compose up -d postgres
```

Then start the backend:

```bash
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

> If you already started the backend once and Flyway has applied the first migration, you may need to recreate the database before rerunning. Use:
>
> ```bash
> docker compose down -v
> docker compose up -d postgres
> ```
> 
> Then run `mvn spring-boot:run` again.

## Database

- Uses PostgreSQL via Docker Compose
- Database URL: `jdbc:postgresql://localhost:5432/lampify_db`
- Username: `postgres`
- Password: `postgres`

## API Endpoints

### Register
```
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "confirmPassword": "password123"
}
```

### Login
```
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

### Response

Success (200):
```json
{
  "success": true,
  "message": "Account created successfully",
  "email": "user@example.com",
  "username": "user"
}
```

Error (400):
```json
{
  "success": false,
  "message": "Email already registered",
  "email": null,
  "username": null
}
```

## Security

- Passwords are hashed using BCrypt
- CORS enabled for `http://localhost:5173` (frontend development server)

## Features

- User registration with email and password validation
- User login with credentials verification
- Email uniqueness constraint
- Password encryption with BCrypt
