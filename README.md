# 🎓 Campus Event Management System

A full-stack **event management platform** built for colleges and universities. Students can browse events, register with one click, and download PDF entry passes with QR codes. Administrators get a powerful dashboard to manage events, track registrations, and export reports.

Built with **Spring Boot 3.5**, **Thymeleaf**, **Spring Security**, **PostgreSQL**, and **OpenPDF + ZXing**.

---

## ✨ Features

### 🎓 Student Portal
- **Browse Events** — View all upcoming campus events with banners, dates, venues, and seat availability
- **One-Click Registration** — Register for events instantly (capacity-enforced)
- **PDF Entry Pass** — Download a styled entry pass with a **QR code** for gate verification
- **Cancellation & Re-registration** — Cancel tickets and re-register anytime if seats are available
- **Personal Dashboard** — View all your registrations with status badges

### 🔐 Admin Dashboard
- **Event CRUD** — Create, edit, and delete events with banner images
- **Registration Management** — View all registrations, filter by name/department/event, update statuses
- **Data Export** — Export registration data as **CSV**, **Excel (.xlsx)**, or **PDF** reports
- **Analytics Overview** — Total events, registrations, departments, and capacity utilization

### 🔒 Security
- **Role-Based Access Control** — Separate student and admin roles with Spring Security
- **BCrypt Password Hashing** — All passwords are securely hashed
- **Session-Based Authentication** — Secure login/logout with CSRF protection
- **Ownership Validation** — Students can only cancel their own tickets

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Spring Boot 3.5, Spring MVC, Spring Security, Spring Data JPA |
| **Frontend** | Thymeleaf, HTML5, CSS3 (Glassmorphism UI), Font Awesome |
| **Database** | PostgreSQL (production), H2 (testing) |
| **PDF Generation** | OpenPDF (reports + entry passes) |
| **QR Codes** | ZXing (Zebra Crossing) |
| **Excel Export** | Apache POI |
| **Build Tool** | Maven |
| **Deployment** | Docker, Render.com |

---

## 📁 Project Structure

```
src/main/java/com/college/eventreg/
├── config/
│   └── DataInitializer.java        # Seeds default admin account
├── controller/
│   └── EventController.java        # All HTTP route handlers
├── model/
│   ├── User.java                   # Student/Admin entity
│   ├── Event.java                  # Event entity
│   ├── EventRegistration.java      # Registration bridge entity
│   └── RegistrationStatus.java     # CONFIRMED / CANCELLED enum
├── repository/
│   ├── UserRepository.java
│   ├── EventRepository.java
│   └── EventRegistrationRepository.java
├── security/
│   ├── SecurityConfig.java         # Spring Security configuration
│   └── CustomUserDetailsService.java
└── service/
    ├── RegistrationService.java    # Business logic (register, cancel, capacity)
    └── ExportService.java          # CSV, Excel, PDF, Event Pass generation
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** or higher
- **Maven 3.9+** (or use the included `mvnw` wrapper)
- **PostgreSQL 14+** (for local development)

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/event-management-system.git
cd event-management-system
```

### 2. Set Up the Database

Create a PostgreSQL database:
```sql
CREATE DATABASE eventdb;
```

### 3. Configure Environment

Update `src/main/resources/application.properties` or set environment variables:

```properties
DATABASE_URL=jdbc:postgresql://localhost:5432/eventdb
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_password
```

### 4. Run the Application

```bash
# Using Maven wrapper (recommended)
./mvnw spring-boot:run

# Or on Windows
.\mvnw spring-boot:run
```

The app starts at **http://localhost:8080**

### 5. Default Admin Account

On first startup, a default admin account is created:
- **Email:** `admin@college.edu`
- **Password:** `admin123`

> ⚠️ **Change this password** after your first login in a production environment.

---

## 🧪 Running Tests

The test suite includes **unit tests** and **integration tests** that run against an in-memory H2 database (no PostgreSQL required).

```bash
# Run all tests
./mvnw test

# Run with verbose output
./mvnw test -Dtest.verbose=true
```

### Test Coverage

| Test Class | Tests | Description |
|-----------|-------|-------------|
| `RegistrationServiceTest` | 7 | Registration, duplicate prevention, capacity limits, cancellation, re-registration, authorization |
| `ExportServiceTest` | 3 | PDF pass generation, CSV export, PDF report generation |
| `EventControllerTest` | 5 | Public page access, auth redirects, signup, duplicate signup rejection |

---

## 🐳 Docker

Build and run with Docker:

```bash
docker build -t event-management-system .
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/eventdb \
  -e DATABASE_USERNAME=postgres \
  -e DATABASE_PASSWORD=your_password \
  event-management-system
```
---

## 📊 API Routes

| Method | Route | Auth | Description |
|--------|-------|------|-------------|
| GET | `/` | Public | Landing page |
| GET | `/login` | Public | Login page |
| GET/POST | `/signup` | Public | Student registration |
| GET | `/schedule` | Authenticated | Browse all events |
| GET | `/user/home` | Student | Student dashboard |
| POST | `/user/register/{eventId}` | Student | Register for event |
| POST | `/user/cancel/{regId}` | Student | Cancel registration |
| GET | `/user/registrations/{regId}/pass` | Student | Download PDF entry pass |
| GET | `/admin/home` | Admin | Admin dashboard |
| GET/POST | `/admin/events/new` | Admin | Create event |
| GET/POST | `/admin/events/edit/{id}` | Admin | Edit event |
| POST | `/admin/events/delete/{id}` | Admin | Delete event |
| GET | `/admin/registrations` | Admin | View all registrations |
| POST | `/admin/registrations/status/{id}` | Admin | Update registration status |
| GET | `/admin/export/csv` | Admin | Export as CSV |
| GET | `/admin/export/excel` | Admin | Export as Excel |
| GET | `/admin/export/pdf` | Admin | Export as PDF report |

---

## 📝 License

This project is open source and available under the [MIT License](LICENSE).
