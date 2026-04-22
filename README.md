# Finance Dashboard — Spring Boot Backend

A production-ready REST API backend for a multi-role finance dashboard system,
built with Spring Boot 3.2, Spring Security + JWT, JPA/H2, and OpenAPI/Swagger.

---

## Table of Contents
1. [Quick Start](#quick-start)
2. [Technology Stack](#technology-stack)
3. [Architecture Overview](#architecture-overview)
4. [Role & Access Control](#role--access-control)
5. [API Reference](#api-reference)
6. [Data Model](#data-model)
7. [Database](#database)
9. [Design Decisions & Tradeoffs](#design-decisions--tradeoffs)
10. [Switching to a Persistent Database](#switching-to-a-persistent-database)

---

## Quick Start

### Prerequisites
- Java 21
- Maven 3.8+

### Run the application

```bash
cd finance-dashboard
mvn spring-boot:run
```

The server starts on **http://localhost:8080**

### Explore the API

| URL | Description |
|-----|-------------|
| http://localhost:8080/swagger-ui.html | Interactive Swagger UI |
| http://localhost:8080/h2-console | H2 database console |
| http://localhost:8080/api-docs | Raw OpenAPI JSON |

### Important: Database Reset on Restart
By default, this application uses an H2 in-memory database. 
This means that every time you restart the backend, the database is completely erased. 
You will need to recreate your admin user on every fresh startup.

### First-time setup
After installing all dependencies and starting the server, 
follow these exact steps to initialize the system:

### 1-Create the Admin: 
Your first step is to create an admin user using the one-time setup endpoint. 
This requires no authentication.
--> Open the Interactive Swagger UI: http://localhost:8080/swagger-ui.html
--> Locate and execute the POST /api/v1/setup/admin endpoint.

### 2-Generate a Token: 
Once the admin is created, you need to authenticate to get your access token.
--> Still in Swagger UI, use POST /api/v1/auth/login with your new admin credentials.
--> Copy the token string from the response body.

### 3-Authorize Swagger:
Click the Authorize 🔒 button at the top of the Swagger UI page.
--> Enter Bearer <your-token> and click Authorize.

### 4-Explore the APIs: 
You can now use the Swagger UI to freely test all secured APIs, 
including User Management, Dashboard Summary, and Financial Records.

### 5-Connect the Frontend: 
--> Once you have generated data, log in via your React frontend.
--> You can view all records seamlessly on the frontend dashboard. 
--> Note that from the frontend UI, Admins have full access to create and manage data entries, 
while other roles (like Analysts) are restricted to view-only access.

### Authenticate via Swagger UI
1. Open `http://localhost:8080/swagger-ui.html`
2. Call `POST /api/auth/login` with one of the credentials above
3. Copy the `token` from the response
4. Click the **Authorize 🔒** button at the top
5. Enter `Bearer <your-token>` and click Authorize
6. All subsequent requests will include the JWT automatically

---

## Technology Stack

| Layer            | Technology                             |
|------------------|----------------------------------------|
| Framework        | Spring Boot 3.2, Java 21             |
| Security         | Spring Security 6, JWT (JJWT 0.12)    |
| Database         | H2 in-memory (swap-ready for MySQL/PG) |
| ORM              | Spring Data JPA / Hibernate            |
| Validation       | Jakarta Bean Validation 3.0            |
| API Docs         | SpringDoc OpenAPI 2 (Swagger UI)       |
| Password Hashing | BCrypt (strength 12)                   |
| Build            | Maven                                  |
| Utilities        | Lombok                                 |

---

## Architecture Overview

```
src/main/java/com/finance/dashboard/
│
├── config/
│   ├── SecurityConfig.java          # Filter chain, route-level RBAC, session (stateless)
│   ├── OpenApiConfig.java           # Swagger/OpenAPI 3 with JWT security scheme
│
├── controller/
│   ├── AuthController.java          # POST /api/auth/login
│   ├── UserController.java          # /api/users  (CRUD + /me)
│   ├── FinancialRecordController.java # /api/records (CRUD + filters)
│   └── DashboardController.java     # /api/dashboard/summary, /period, /monthly
│
├── service/
│   ├── AuthService.java             # Authenticate + generate JWT
│   ├── UserService.java             # User business logic, last-admin guard
│   ├── FinancialRecordService.java  # Record CRUD, audit trail, soft delete
│   └── DashboardService.java        # Analytics: totals, trends, breakdown
│
├── security/
│   ├── JwtUtil.java                 # Token sign / verify (HMAC-SHA256)
│   ├── JwtAuthenticationFilter.java # OncePerRequestFilter: extracts & validates JWT
│   ├── UserPrincipal.java           # UserDetails wrapper around User entity
│   └── CustomUserDetailsService.java # Loads user from DB for Spring Security
│
├── entity/
│   ├── User.java                    # Users table with soft-delete flag
│   └── FinancialRecord.java         # Records table with audit + soft-delete
│
├── repository/
│   ├── UserRepository.java          # JPA queries: active users, filter by role/status
│   └── FinancialRecordRepository.java # Dynamic filter, analytics JPQL queries
│
├── dto/
│   ├── request/                     # Validated inbound DTOs
│   └── response/                    # Outbound response shapes (no entity leakage)
│
├── enums/
│   ├── Role.java                    # VIEWER | ANALYST | ADMIN
│   ├── UserStatus.java              # ACTIVE | INACTIVE
│   ├── RecordType.java              # INCOME | EXPENSE
│   └── RecordCategory.java          # SALARY, RENT, GROCERIES, ... (14 categories)
│
└── exception/
    ├── GlobalExceptionHandler.java  # Translates all exceptions to structured JSON
    ├── ResourceNotFoundException.java
    ├── DuplicateResourceException.java
    └── BadRequestException.java
```

### Request Lifecycle

```
HTTP Request
    │
    ▼
JwtAuthenticationFilter          ← Validates Bearer token, sets SecurityContext
    │
    ▼
SecurityConfig (HttpSecurity)    ← Route-level role guards
    │
    ▼
Controller                       ← @PreAuthorize method-level guard (second layer)
    │
    ▼
Service                          ← Business logic, validation, DB access
    │
    ▼
Repository (JPA)                 ← H2 / RDBMS
    │
    ▼
GlobalExceptionHandler           ← Catches any exception → structured ErrorResponse
```

---

## Role & Access Control

Two layers of access control are enforced:

1. **Route-level** — in `SecurityConfig.java` via `HttpSecurity.authorizeHttpRequests()`
2. **Method-level** — via `@PreAuthorize("hasRole('ADMIN')")` annotations on controllers

### Permission Matrix

| Endpoint group                      | VIEWER | ANALYST | ADMIN |
|-------------------------------------|:------:|:-------:|:-----:|
| `POST /api/auth/login`              | ✅     | ✅      | ✅    |
| `GET /api/users/me`                 | ✅     | ✅      | ✅    |
| `GET /api/dashboard/**`             | ✅     | ✅      | ✅    |
| `GET /api/records/**`               | ❌     | ✅      | ✅    |
| `POST /api/records`                 | ❌     | ❌      | ✅    |
| `PUT /api/records/{id}`             | ❌     | ❌      | ✅    |
| `DELETE /api/records/{id}`          | ❌     | ❌      | ✅    |
| `GET /api/users`                    | ❌     | ❌      | ✅    |
| `GET /api/users/{id}`               | ❌     | ❌      | ✅    |
| `POST /api/users`                   | ❌     | ❌      | ✅    |
| `PUT /api/users/{id}`               | ❌     | ❌      | ✅    |
| `DELETE /api/users/{id}`            | ❌     | ❌      | ✅    |

---

## API Reference

### Authentication

#### `POST /api/auth/login`
Authenticate and receive a JWT token.

**Request body:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGci...",
    "tokenType": "Bearer",
    "username": "admin",
    "role": "ADMIN",
    "userId": 1
  },
  "timestamp": "2025-04-05T10:00:00"
}
```

---

### User Management _(Admin only, except /me)_

| Method | Path              | Description                        |
|--------|-------------------|------------------------------------|
| POST   | `/api/users`      | Create a new user                  |
| GET    | `/api/users`      | List users (paginated)             |
| GET    | `/api/users/me`   | Get own profile                    |
| GET    | `/api/users/{id}` | Get user by ID                     |
| PUT    | `/api/users/{id}` | Update user (partial)              |
| DELETE | `/api/users/{id}` | Soft-delete user                   |

**Query parameters for `GET /api/users`:**

| Param  | Type       | Description          |
|--------|------------|----------------------|
| page   | int        | Page number (0-based)|
| size   | int        | Page size (default 10)|
| role   | Role enum  | Filter by role       |
| status | UserStatus | Filter by status     |

**`POST /api/users` body:**
```json
{
  "username": "jsmith",
  "email": "jsmith@company.com",
  "password": "securepass1",
  "fullName": "John Smith",
  "role": "ANALYST"
}
```

---

### Financial Records _(Analyst + Admin read; Admin write)_

| Method | Path               | Description                        |
|--------|--------------------|------------------------------------|
| POST   | `/api/records`     | Create a record (Admin)            |
| GET    | `/api/records`     | List records (filtered, paginated) |
| GET    | `/api/records/{id}`| Get record by ID                   |
| PUT    | `/api/records/{id}`| Update record partially (Admin)    |
| DELETE | `/api/records/{id}`| Soft-delete record (Admin)         |

**Query parameters for `GET /api/records`:**

| Param     | Type            | Description                     |
|-----------|-----------------|---------------------------------|
| page      | int             | Page number (0-based)           |
| size      | int             | Page size (max 100)             |
| type      | INCOME/EXPENSE  | Filter by transaction type      |
| category  | RecordCategory  | Filter by category              |
| startDate | yyyy-MM-dd      | Filter from date (inclusive)    |
| endDate   | yyyy-MM-dd      | Filter to date (inclusive)      |
| search    | string          | Search in title or notes        |

**`POST /api/records` body:**
```json
{
  "amount": 85000.00,
  "type": "INCOME",
  "category": "SALARY",
  "date": "2025-04-01",
  "title": "April Salary",
  "notes": "Monthly salary payment"
}
```

**Available categories:** `SALARY`, `FREELANCE`, `INVESTMENT`, `BUSINESS`, `RENT`,
`UTILITIES`, `GROCERIES`, `TRANSPORT`, `HEALTHCARE`, `ENTERTAINMENT`,
`EDUCATION`, `INSURANCE`, `SAVINGS`, `OTHER`

---

### Dashboard _(All authenticated roles)_

| Method | Path                              | Description                               |
|--------|-----------------------------------|-------------------------------------------|
| GET    | `/api/dashboard/summary`          | Full snapshot with trends + recent activity|
| GET    | `/api/dashboard/summary/period`   | Custom date range summary                 |
| GET    | `/api/dashboard/summary/monthly`  | Calendar-month summary                    |

**`GET /api/dashboard/summary` response:**
```json
{
  "success": true,
  "data": {
    "totalIncome":   255000.00,
    "totalExpenses": 125400.00,
    "netBalance":    129600.00,
    "totalRecords":  20,
    "categoryBreakdown": {
      "incomeByCategory":  { "SALARY": 255000.00, "FREELANCE": 23000.00 },
      "expenseByCategory": { "RENT": 75000.00, "GROCERIES": 24100.00 }
    },
    "monthlyTrends": [
      { "year": 2025, "month": 1, "monthLabel": "Jan 2025",
        "income": 95000.00, "expenses": 43200.00, "net": 51800.00 }
    ],
    "recentActivity": [ /* last 10 records */ ]
  }
}
```

**`GET /api/dashboard/summary/period?startDate=2025-01-01&endDate=2025-03-31`**

**`GET /api/dashboard/summary/monthly?year=2025&month=3`**

---

### Standard Error Response

All errors follow a consistent shape:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "One or more fields are invalid",
  "path": "/api/records",
  "timestamp": "2025-04-05T10:15:00",
  "fieldErrors": {
    "amount": "Amount must be greater than 0",
    "date": "Date cannot be in the future"
  }
}
```

| Status | When                                     |
|--------|------------------------------------------|
| 400    | Validation failure or bad input          |
| 401    | Invalid / expired JWT or wrong password  |
| 403    | Insufficient role or inactive account    |
| 404    | Resource not found (or soft-deleted)     |
| 409    | Duplicate username or email              |
| 500    | Unexpected server error                  |

---

## Data Model

### `users` table

| Column     | Type        | Notes                            |
|------------|-------------|----------------------------------|
| id         | BIGINT PK   | Auto-generated                   |
| username   | VARCHAR(50) | Unique, not null                 |
| email      | VARCHAR(100)| Unique, not null                 |
| password   | VARCHAR     | BCrypt hashed                    |
| full_name  | VARCHAR(50) | Not null                         |
| role       | VARCHAR     | VIEWER / ANALYST / ADMIN         |
| status     | VARCHAR     | ACTIVE / INACTIVE                |
| deleted    | BOOLEAN     | Soft-delete flag (default false) |
| created_at | TIMESTAMP   | Auto-set on insert               |
| updated_at | TIMESTAMP   | Auto-set on update               |

### `financial_records` table

| Column     | Type          | Notes                               |
|------------|---------------|-------------------------------------|
| id         | BIGINT PK     | Auto-generated                      |
| amount     | DECIMAL(15,2) | Must be > 0                         |
| type       | VARCHAR       | INCOME / EXPENSE                    |
| category   | VARCHAR       | One of 14 categories                |
| date       | DATE          | Cannot be in the future             |
| title      | VARCHAR(100)  | Short description                   |
| notes      | VARCHAR(500)  | Optional long description           |
| created_by | BIGINT FK     | References users.id                 |
| updated_by | BIGINT FK     | References users.id (nullable)      |
| deleted    | BOOLEAN       | Soft-delete flag (default false)    |
| created_at | TIMESTAMP     | Auto-set on insert                  |
| updated_at | TIMESTAMP     | Auto-set on update                  |

---

## Database

**Default:** H2 in-memory — data is reset on every restart.
- H2 console: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:financedb`
- Username: `sa`, Password: _(empty)_

### Switching to a Persistent Database

**MySQL / MariaDB:**
```properties
# application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/financedb?createDatabaseIfNotExist=true
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
```

Add to pom.xml:
```xml
<dependency>
  <groupId>com.mysql</groupId>
  <artifactId>mysql-connector-j</artifactId>
  <scope>runtime</scope>
</dependency>
```

**PostgreSQL:**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/financedb
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=postgres
spring.datasource.password=yourpassword
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
```

---


## Design Decisions & Tradeoffs

### Soft Delete
Both `User` and `FinancialRecord` support soft deletion via a `deleted` boolean flag.
- Preserves audit history (who created what, even after deletion)
- Deleted records remain invisible via all queries (`WHERE deleted = false`)
- Tradeoff: Deleted data accumulates — a scheduled cleanup job could archive old records in production

### JWT (Stateless Auth)
- No server-side session storage — horizontally scalable out of the box
- Tokens expire after 24 hours (configurable via `app.jwt.expiration-ms`)
- Tradeoff: Tokens cannot be individually revoked before expiry; a token blacklist (Redis) would be needed for logout in production

### Two-Layer Access Control
Route-level rules in `SecurityConfig` act as a first gate; `@PreAuthorize` on
controllers acts as a second, method-level gate. This ensures access control works
correctly even if routes are accidentally misconfigured.

### H2 In-Memory Database
Simple to run — no external DB setup required. The JPQL queries are all
database-agnostic (no native SQL), so switching to MySQL or PostgreSQL requires
only a dependency swap and property change (see above).

### Partial Updates (PUT, not PATCH)
Update endpoints accept all fields as optional and only apply those that are
non-null. This gives PATCH-like behaviour while keeping the API surface simple.
A true PATCH implementation would require JSON Merge Patch or JSON Patch support.

### Page Size Cap
`GET /api/records` caps `size` at 100 to prevent inadvertently large queries
from overloading the server or returning unbounded result sets.

### Last-Admin Guard
`DELETE /api/users/{id}` checks whether the target is the last remaining admin
and rejects the deletion with a `400 Bad Request`, preventing the system from
being left without any administrative access.

### Audit Trail on Records
Each `FinancialRecord` stores both `createdBy` and `updatedBy` user references,
providing a full audit log of who created and who last modified every record.

---

## Assumptions

- A single currency is assumed (amounts are stored as `DECIMAL(15,2)` without a currency code)
- Users can only belong to one role at a time (no multi-role support)
- Soft-deleted users' financial records remain visible (ownership is preserved for audit purposes)
- The `date` field on records represents the transaction date, not the creation timestamp
