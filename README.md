# 📚 Library Management System

A small but complete library management system built with **Spring Boot**, **Spring Data JPA** and an embedded database — **H2** (in-memory, default) or **SQLite** (file-based, persistent). Registered users borrow and return books; the system tracks due dates and automatically calculates **late-return fines** (default: **5,000 Toman per day** after a **14-day** loan period).

## Requirements

- Java 17 or newer
- Maven 3.6+ (no database installation needed — H2 and SQLite both run embedded)

## Run it

```bash
mvn spring-boot:run
```

This starts with the default **H2 in-memory** database (fresh seed data on every start).

### Run with SQLite (persistent, file-based)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=sqlite
# or with the packaged jar:
java -jar target/library-management-system-1.0.0.jar --spring.profiles.active=sqlite
```

- The database file **`library.db`** is created in the working directory on first start (with seed data) and **kept across restarts** — no re-seeding on later starts.
- Delete `library.db` to reset the database.
- The H2 web console does not work with SQLite; inspect `library.db` with [DB Browser for SQLite](https://sqlitebrowser.org/) or the `sqlite3` CLI instead.
- To make SQLite the default, add `spring.profiles.active=sqlite` to `src/main/resources/application.properties`.

Then open:

| URL | What it is |
|---|---|
| http://localhost:8080 | Web UI (users, books, borrow/return, reports) |
| http://localhost:8080/h2-console | H2 console (H2 mode only) — JDBC URL `jdbc:h2:mem:librarydb`, user `sa`, empty password |

The database is seeded on startup with 3 users, 4 books and 5 loans (including an overdue one, so fines are visible immediately).

## Configuration (`src/main/resources/application.properties`)

| Property | Default | Meaning |
|---|---|---|
| `library.loan.period-days` | `14` | Allowed loan period; the book is overdue from day 15 |
| `library.fine.daily-amount` | `5000` | Fixed fine per overdue day, in Toman |
| `spring.datasource.url` | `jdbc:h2:mem:librarydb` | H2 in-memory DB; use `jdbc:h2:file:./data/library` for a persistent file-based H2 |
| `spring.datasource.url` (sqlite profile) | `jdbc:sqlite:library.db` | SQLite file DB — created on first start, kept across restarts |

## How the fine is calculated

```
dueDate     = borrowDate + 14 days
overdueDays = ChronoUnit.DAYS.between(dueDate, referenceDate)   // only if positive
fine        = overdueDays × daily fine amount
```

- **Open loans**: `referenceDate` = *today*, so the fine you see is the amount accrued so far and it grows day by day (recomputed on every request, nothing stored).
- **Returned loans**: `referenceDate` = the actual `returnDate`; the final fine is computed once at return time and **stored** in `loan.fineAmount`.

Example: borrowed 2026-01-01 → due 2026-01-15 → returned 2026-01-18 → 3 days late → 3 × 5,000 = **15,000 Toman**.

## REST API

All dates are ISO-8601 (`yyyy-MM-dd`). Errors return a JSON body:
`{"timestamp": "...", "status": 409, "error": "Conflict", "message": "..."}` (bean-validation adds a `fieldErrors` map).

### Users

| Method | Path | Description |
|---|---|---|
| POST | `/api/users` | Register a user — `{"name":"Ali","email":"ali@x.com","phone":"0912...","joinedAt":"2026-01-01"}` (phone/joinedAt optional) |
| GET | `/api/users` | List all users (with active-loan count) |
| DELETE | `/api/users/{id}` | Delete a user (blocked with 409 while they have active loans) |
| GET | `/api/users/{id}/loans?active=true` | Loan history / active loans of one user |
| GET | `/api/users/{id}/fines` | Fine report: settled + accruing + total |

### Books

| Method | Path | Description |
|---|---|---|
| POST | `/api/books` | Add a book — `{"title":"Clean Code","author":"Robert C. Martin","isbn":"978-...","totalCopies":3}` (isbn optional) |
| GET | `/api/books` | Catalog with availability (`availableCopies` / `totalCopies`) |
| DELETE | `/api/books/{id}` | Delete a book (blocked with 409 while copies are on loan) |

### Loans

| Method | Path | Description |
|---|---|---|
| POST | `/api/loans` | Borrow — `{"userId":1,"bookId":2}`. Validates user/book exist, copy availability, and no duplicate active loan |
| PUT | `/api/loans/{id}/return` | Return — sets `returnDate`, stores the final fine, puts the copy back |
| GET | `/api/loans?active=true` | All loans / only open ones |
| GET | `/api/loans/overdue` | Overdue report with the fine accrued per loan |

### Example session

```bash
# register a user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Maryam Tehrani","email":"maryam@example.com","phone":"0912-333-4444"}'

# borrow book 1
curl -X POST http://localhost:8080/api/loans \
  -H "Content-Type: application/json" \
  -d '{"userId":4,"bookId":1}'

# return loan 5 (fine is computed and stored here)
curl -X PUT http://localhost:8080/api/loans/5/return

# overdue report and per-user fines
curl http://localhost:8080/api/loans/overdue
curl http://localhost:8080/api/users/1/fines
```

## Business rules enforced

- Only registered users can borrow (404 otherwise); unavailable books cannot be borrowed (409).
- A user cannot borrow the same book twice without returning it (409).
- Books/users with active loans cannot be deleted (409).
- Duplicate email registration is rejected (409).
- Request bodies are validated with Bean Validation (`@NotNull`, `@NotBlank`, `@Email`, `@Min`).

## Project structure (layered architecture)

```
src/main/java/com/example/library/
├── LibraryApplication.java        # entry point
├── config/
│   ├── LibraryProperties.java     # loan period + daily fine from properties
│   └── DataSeeder.java            # CommandLineRunner seed data
├── controller/                    # REST layer (UserController, BookController, LoanController)
├── service/                       # business logic + fine calculation (LoanService)
├── repository/                    # Spring Data JPA repositories
├── entity/                        # User, Book, Loan (JPA entities)
├── dto/                           # request/response records
└── exception/                     # NotFoundException, BusinessException, @RestControllerAdvice
src/main/resources/
├── application.properties           # H2 (default) + fine configuration
├── application-sqlite.properties    # 'sqlite' profile: persistent file-based DB
└── static/index.html                # single-page web UI
```
