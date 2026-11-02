# School ID Card Ordering & Printing Platform

[![Backend Test Suite](https://img.shields.io/badge/Tests-152%20Passed-brightgreen.svg)]()
[![Java](https://img.shields.io/badge/Java-17%20LTS-blue.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)]()
[![React](https://img.shields.io/badge/Frontend-React%20%2B%20Vite%20%2B%20Tailwind-61dafb.svg)]()
[![Database](https://img.shields.io/badge/Database-PostgreSQL%2016%20%2B%20Flyway-336791.svg)]()

A production-grade, multi-tenant enterprise platform for automated school student identity card data intake, dynamic template design, order quotation, distributed printing agency fulfillment, quality control inspection, dispatch tracking, and financial reconciliation.

---

## 1. Architectural Pillars & Core Rules

This platform strictly enforces 8 non-negotiable architectural and domain invariants:

1. **Strict Tenant & Role Separation:** Schools must **never** see agency names, internal agency job IDs, or agency cost data. Agencies must **never** see school pricing, platform margins, or client customer details. Each role interacts exclusively with dedicated DTOs and role-scoped endpoints.
2. **Zero Client Trust for Tenant IDs:** `school_id` and `agency_id` are derived strictly from cryptographically verified JWT claims in `SecurityUtils`. Any client payload attempting to inject or spoof tenant identifiers is disregarded.
3. **Monetary Precision:** All currency amounts use `NUMERIC(14,2)` in PostgreSQL and `BigDecimal` in Java. School selling prices and agency manufacturing costs remain segregated and immutable once finalized.
4. **Named Business Actions Only:** State changes execute solely via explicit, named domain service actions (e.g., `approveOrder`, `assignAgency`, `startProduction`, `recordQcResult`, `dispatchShipment`, `confirmDelivery`, `generateInvoice`, `recordPayment`, `generateAgencySettlement`, `payAgencySettlement`), preventing generic updates.
5. **No Mock or Fake Data:** All modules operate on real database tables, Flyway versioned migrations (V1 to V10), verified JPA repositories, and authentic service workflows.
6. **Strict Pagination & Indexing:** All list endpoints are paginated via Spring Data `Pageable`. Every foreign key and query filter column is backed by explicit database indexes to eliminate full table scans and N+1 query bottlenecks.
7. **Unified Error Envelope:** All application exceptions and validation errors return a standardized response schema: `{ success: false, errorCode: "...", message: "...", timestamp: "..." }`. Stack traces are never exposed to clients.
8. **Jakarta Bean Validation:** All API inputs are strictly checked on the backend using Jakarta Bean Validation constraints (`@NotBlank`, `@NotNull`, `@Size`, `@DecimalMin`, `@Valid`).

---

## 2. Technology Stack

### Backend
- **Language & Runtime:** Java 17 LTS
- **Core Framework:** Spring Boot 3.2.5 (`web`, `security`, `data-jpa`, `validation`)
- **Database & Migration:** PostgreSQL 16 with Flyway Migrations (V1 to V10)
- **Security:** Spring Security with stateless HMAC-SHA256 JWT tokens & Refresh Tokens
- **Document & Media Generation:**
  - **Apache POI (5.2.5):** Robust Excel `.xlsx` / `.xls` roster ingestion & schema mapping
  - **Apache PDFBox (3.0.2):** High-precision vector card PDF rendering (CR80 standard 85.6mm × 54mm)
  - **ZXing (3.5.3):** Dynamic Code-128 barcode and 2D QR-code card symbology
- **API Documentation:** SpringDoc OpenAPI 2.5.0 with interactive Swagger UI
- **Testing:** JUnit 5, Mockito, Spring Boot Test, Spring Security Test (152 automated tests)

### Frontend
- **Framework & Tooling:** React 18, Vite 5.4, JavaScript (ES6+ `.jsx`)
- **Styling & Design Tokens:** Tailwind CSS 3.4 with accessible design tokens (tested at 360px, 768px, 1280px, 1920px)
- **Routing & State:** React Router 6, TanStack Query 5 (server-state cache), Zustand (auth & UI state)
- **HTTP Client:** Axios with JWT bearer interceptors and 401 automatic redirection
- **Icons & UI:** Lucide React icons, accessible modals, and custom interactive WYSIWYG canvas

---

## 3. System Architecture & Role Matrix

```text
┌────────────────┐     ┌────────────────┐     ┌──────────────────────┐
│  School Admin  │     │ Platform Admin │     │   Printing Agency    │
│ (Roster/Order) │     │ (Quote/Assign) │     │ (Batch/QC/Dispatch)  │
└───────┬────────┘     └───────┬────────┘     └──────────┬───────────┘
        │                      │                         │
        └──────────────────────┼─────────────────────────┘
                               │
                       ┌───────▼────────┐
                       │ React Frontend │ (Vite SPA + Nginx)
                       └───────┬────────┘
                               │ HTTPS / REST (JWT Auth)
                       ┌───────▼────────┐
                       │ Spring Boot 3  │ (Modular Monolith)
                       └───────┬────────┘
                               │
         ┌─────────────────────┼─────────────────────┐
         ▼                     ▼                     ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│  PostgreSQL 16  │   │  Local Storage  │   │   PDFBox Engine │
│ (Flyway V1-V10) │   │ (Uploads/Photos)│   │ (Print-Ready PDF│
└─────────────────┘   └─────────────────┘   └─────────────────┘
```

| Role | Portal Capabilities | Data Visibility Restrictions |
| :--- | :--- | :--- |
| **SUPER_ADMIN** | Tenant management, template approval, order quoting, agency job routing, invoice generation, agency payout settlement, operational & financial P&L reporting, audit logs. | Complete visibility across all schools and printing partners. |
| **SCHOOL** | Student/staff enrollment, Excel roster import with column mapping, ZIP photo upload & auto-matching, template preview, order placement, direct delivery confirmation, billing invoices, summary reports. | Strictly isolated to own school data. Zero visibility into agency names, agency quotes, or internal production metrics. |
| **PRINTING_AGENCY**| Assigned print job intake, batch scheduling, card status updates, QC defect logging, consignment dispatch (Model A / Model B), agency payment ledger. | Strictly isolated to own assigned jobs. Zero visibility into school customer identity, school selling price, or admin gross margins. |

---

## 4. Platform Modules & Completed Roadmap

All 13 implementation steps are 100% complete and fully verified:

- [x] **Step 0: Setup and Design System:** Responsive UI foundation, design tokens, error handling.
- [x] **Step 1: Auth, Roles & Organizations:** Multi-tenant JWT auth, User/School/Agency entities, `V1__init_auth_and_organizations.sql`.
- [x] **Step 2: Students & Enrolment:** Student directory, validation, manual photo upload, `V2__init_students.sql`.
- [x] **Step 3: Roster Ingestion & Photo Matching:** Excel/CSV POI parsing, column mapper, ZIP photo auto-matching, `V3__init_import_and_photos.sql`.
- [x] **Step 4: Interactive Template Designer:** Canvas WYSIWYG editor, versioning workflow, Apache PDFBox print generator, `V4__init_card_templates.sql`.
- [x] **Step 5: Orders & Quotation Management:** Order submission, admin pricing quotation, proof approvals, `V5__init_orders_and_quotations.sql`.
- [x] **Step 6: Agency Assignment & Job Allocation:** Multi-agency job distribution, cost negotiation, allocation limits, `V6__init_agency_assignments.sql`.
- [x] **Step 7: Production Tracking & Quality Control:** Batch tracking, card reprint lifecycle, 7-point QC inspection, `V7__init_production_and_qc.sql`.
- [x] **Step 8: Dispatch, Delivery & Chain of Custody:** Model A (Direct) & Model B (Via Admin), BlueDart/courier tracking, POD receipts, `V8__init_delivery_and_dispatch.sql`.
- [x] **Step 9: Finance, Invoices & Agency Settlements:** Dual-ledger accounting, school invoices, payment tracking, agency settlement payouts, `V9__init_finance_and_settlements.sql`.
- [x] **Step 10: In-App Notifications, Audit Logs & Analytics:** Live notification popover drawer, admin immutable audit trail, operational & financial P&L reporting, `V10__init_notifications_and_audit.sql`.
- [x] **Step 11: Security Hardening & Full E2E Testing:** Cross-tenant penetration tests, complete real-world lifecycle test harness (`FullPlatformE2EHardeningTest`, 152/152 tests passed).
- [x] **Step 12: Production Release Preparation:** Multi-stage Dockerfiles, Nginx SPA reverse proxy configuration, `docker-compose.yml`, production profile, documentation.

---

## 5. Quickstart & Local Development

### Prerequisites
- **Java 17 LTS** (OpenJDK / Eclipse Temurin)
- **Node.js 20+** and `npm`
- **PostgreSQL 16** (or Docker)

### 1. Database Setup
Create a PostgreSQL database named `school_id_platform`:
```sql
CREATE DATABASE school_id_platform;
```

### 2. Backend Setup
```bash
cd backend

# Run automated tests (152 tests)
mvn clean test

# Start the Spring Boot application (runs on http://localhost:8080)
mvn spring-boot:run
```
Flyway automatically applies all migrations (`V1` to `V10`) on startup.

### 3. Frontend Setup
```bash
cd frontend

# Install dependencies
npm install

# Build for production
npm run build

# Start local development server (runs on http://localhost:5173)
npm run dev
```

### 4. Default Seed Credentials
The system comes pre-seeded with a default Super Admin account:
- **Email:** `admin@platform.com`
- **Password:** `Password123!`
- **Role:** `SUPER_ADMIN`

---

## 6. Docker Compose Deployment

To deploy the entire production stack (PostgreSQL, Spring Boot Backend, and Nginx Frontend) in one command:

```bash
# Clone the repository
git clone https://github.com/Ahmad-web-Engineer/school-Id-card.git
cd school-Id-card

# Start all services in the background
docker-compose up --build -d
```

### Deployed Services & Endpoints
| Component | URL | Description |
| :--- | :--- | :--- |
| **Frontend Portal** | `http://localhost` | Single Page Application served via Nginx with reverse proxy |
| **Backend REST API** | `http://localhost:8080/api/v1` | Spring Boot 3 modular monolith API |
| **Swagger UI** | `http://localhost:8080/swagger-ui.html`| Interactive OpenAPI documentation |
| **OpenAPI Specification** | `http://localhost:8080/v3/api-docs` | Raw OpenAPI v3 JSON spec |
| **Health Check** | `http://localhost:8080/api/v1/health` | Service health status (`{"status":"UP"}`) |
| **PostgreSQL DB** | `localhost:5432` | Database container (`school_id_platform`) |

To stop services:
```bash
docker-compose down
```

---

## 7. Testing & Verification

The platform includes an extensive test suite verifying business logic, tenant isolation, and security constraints:

```bash
# Run all 152 automated backend tests
mvn -f backend/pom.xml test
```

### Test Suite Highlights
- **`FullPlatformE2EHardeningTest`:** Full real-world lifecycle simulation: student enrollment → photo mapping → order quote → approval → agency job assignment → batch production → QC inspection → dispatch → delivery confirmation → tax invoicing → school payment → agency settlement payout → financial P&L calculation.
- **Tenant Security Penetration:** Strict verification that school tokens cannot access agency or admin endpoints (403), cross-tenant IDs are rejected, negative payments are blocked by Jakarta constraints (400), and unauthenticated calls return standard 401 errors.
- **`FinanceSecurityTest` & `FinanceIntegrationTest`:** Dual-ledger balance integrity and settlement approval workflows.
- **`CardTemplateIntegrationTest`:** Dynamic Apache PDFBox card generation with barcodes and QR codes.

---

## 8. License

Copyright © 2026 School ID Card Platform. All rights reserved.
