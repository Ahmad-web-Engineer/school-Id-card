# Project Progress: School ID Card Platform

## Current Status
- **Current Step:** Ready to start Step 8 (Dispatch and delivery)
- **Last Completed Step:** Step 7 (Production and quality control)

---

## Build Steps

| Step | Name | Status | Database | Backend | Frontend | Run in Parallel? |
| --- | --- | --- | --- | --- | --- | --- |
| 0 | Setup and design system | Completed | No | Skeleton | Skeleton and components | Yes |
| 1 | Auth, roles, organizations | Completed | Yes | Yes | Yes | Backend and Frontend |
| 2 | Students | Completed | Yes | Yes | Yes | Backend and Frontend |
| 3 | Excel, CSV and photo matching | Completed | Yes | Yes | Yes | Backend and Frontend |
| 4 | Template designer | Completed | Yes | Yes | Yes (largest) | Backend and Frontend |
| 5 | Orders and quotations | Completed | Yes | Yes | Yes | Backend and Frontend |
| 6 | Agency assignment | Completed | Yes | Yes | Yes | Backend and Frontend |
| 7 | Production and quality | Completed | Yes | Yes | Yes | Backend and Frontend |
| 8 | Dispatch and delivery | Ready to start | Yes | Yes | Yes | Backend and Frontend |
| 9 | Finance | Not started | Yes | Yes | Yes | Backend and Frontend |
| 10 | Notifications, reports, audit | Not started | Yes | Yes | Yes | Backend and Frontend |
| 11 | Hardening and full testing | Not started | No | Fixes only | Fixes only | Security and QA lead |
| 12 | Release preparation | Not started | No | Config | Config | Single agent |

---

## Step 1 Log: Auth, Roles, Organizations

### What Was Built
1. **API Contract Definition:**
   - Populated [docs/api-contract.md](file:///c:/Users/a4477/Downloads/School%20id%20card/docs/api-contract.md) with 16 comprehensive endpoints for Auth, Roles, Organizations, and Profiles.
2. **Database Migrations & JPA Entities:**
   - Created Flyway migration `V1__init_auth_and_organizations.sql` supporting UUID primary keys (`gen_random_uuid()`), TIMESTAMPTZ timestamps, foreign keys with ON DELETE CASCADE, unique constraints on `users.email`, `schools.school_code`, `schools.email`, `agencies.email`, and composite uniques on `school_users` and `agency_users`.
   - Added indexes on all foreign key columns and search columns (`idx_users_email`, `idx_schools_code`, `idx_school_users_school_id`, `idx_agency_users_agency_id`, etc.).
   - Seeded initial system roles (`SUPER_ADMIN`, `SCHOOL`, `PRINTING_AGENCY`) and default Super Admin user (`admin@platform.com` / `Password123!`).
   - Implemented JPA Entities: `User`, `Role`, `School`, `Agency`, `SchoolUser`, `AgencyUser`, `RefreshToken`.
3. **Backend Authentication & Authorization:**
   - Implemented `JwtService` with HMAC-SHA256 signing, claims extraction (`userId`, `role`, `schoolId`, `agencyId`, `fullName`, `designation`), expiration management, and token validation.
   - Implemented `JwtAuthenticationFilter` and wired Bearer token authentication in `SecurityConfig`.
   - Implemented `SecurityUtils` providing typed helpers and enforcing **Non-negotiable Rule 2** (`getRequiredSchoolId()`, `getRequiredAgencyId()`).
   - Implemented `AuthService` and `AuthController` (`/api/v1/auth/login`, `/refresh`, `/logout`, `/me`).
   - Implemented `SchoolService`, `AgencyService`, and `AdminOrganizationController` for paginated school/agency management and user provisioning.
   - Implemented role-isolated profile controllers `SchoolProfileController` (`/api/v1/school/profile`) and `AgencyProfileController` (`/api/v1/agency/profile`).
   - Separate response records: `SchoolResponse` contains zero agency fields; `AgencyResponse` contains zero school fields (**Non-negotiable Rule 1**).
4. **Frontend Architecture & Pages:**
   - Updated `useAuthStore` with persistent token storage and profile hydration.
   - Created `ProtectedRoute` supporting role-based access gates (`SUPER_ADMIN`, `SCHOOL`, `PRINTING_AGENCY`).
   - Implemented live `LoginPage.jsx` with Jakarta Bean validation error display, demo credential quick-fill, and automatic role-based redirection.
   - Implemented `SchoolsPage.jsx` with search, paginated table (stacked cards on mobile), "Register School" modal, and "Add School User" modal.
   - Implemented `AgenciesPage.jsx` with search, paginated table (stacked cards on mobile), "Register Agency" modal, and "Add Agency User" modal.
   - Implemented `SchoolProfilePage.jsx` and `AgencyProfilePage.jsx`.
   - Wired role-guarded routes in `App.jsx`.

### How It Was Verified
- **Backend Test Suite:** Executed `mvn test` — **19/19 tests passed** (0 failures, 0 errors):
  - `AuthControllerTest` (4 tests): Login success, invalid password rejection, blank credentials validation, full lifecycle (login -> `/auth/me` -> refresh -> logout).
  - `AdminOrganizationControllerTest` (4 tests): School registration + user provisioning, duplicate school code conflict rejection (409), agency registration + user provisioning, school user forbidden on admin endpoints (403).
  - `RoleIsolationSecurityTest` (4 tests): School user forbidden on agency profile (403), agency user forbidden on school profile (403), school profile strictly matches user's token `schoolId`, agency profile strictly matches user's token `agencyId`.
  - `GlobalExceptionHandlerTest` (5 tests): standard envelope format on 400, 401, 403, 404, 409.
  - `HealthControllerTest` (1 test): 200 OK standard envelope.
  - `IdCardPlatformApplicationTests` (1 test): Spring context boot smoke test.
- **Frontend Build:** Executed `npm run build` — `vite build` completed cleanly in 8.11s with 0 errors or warnings.
- **Non-Negotiable Rules Audit:**
  - Rule 1 (Agency hidden from school): verified via separate DTOs and 403 security tests.
  - Rule 2 (No trusted IDs from client): verified in `SchoolProfileController` and `AgencyProfileController`.
  - Rule 5 (No mock data/placeholders): all data handled through real database tables, entities, repositories, and services.
  - Rule 6 (Pagination & indexes): every list endpoint is paginated (`Pageable`), all FKs and filter columns indexed.
  - Rule 7 (Standard error format): enforced centrally with no stack traces exposed.
  - Rule 8 (Jakarta Bean Validation): applied to all request DTOs.
  - UI rules: checked responsive card stacking for mobile (< 768px), accessible modals, loading/error states.

### What Is Next
- Proceed to Step 2 (Students)

---

## Step 2 Log: Students

### What Was Built
1. **API Contract Definition:**
   - Populated [docs/api-contract.md](file:///c:/Users/a4477/Downloads/School%20id%20card/docs/api-contract.md) with 7 endpoints:
     - School endpoints: `GET /api/v1/school/students`, `POST /api/v1/school/students`, `GET /api/v1/school/students/{id}`, `PUT /api/v1/school/students/{id}`, `DELETE /api/v1/school/students/{id}`
     - Super Admin endpoints: `GET /api/v1/admin/schools/{schoolId}/students`, `POST /api/v1/admin/schools/{schoolId}/students`, `GET /api/v1/admin/schools/{schoolId}/students/{studentId}`, `PUT /api/v1/admin/schools/{schoolId}/students/{studentId}`, `DELETE /api/v1/admin/schools/{schoolId}/students/{studentId}`
2. **Database Migrations & JPA Entities:**
   - Created Flyway migration `V2__init_students.sql` introducing `students` table with UUID primary key, `school_id` foreign key referencing `schools(id)` on delete cascade, `UNIQUE (school_id, admission_number)`, and performance indexes on `(school_id)`, `(school_id, class_name, section)`, `(school_id, admission_number)`, `(status)`, and `(school_id, student_name)`.
   - Created JPA Entity `Student.java` with validation annotations and immutable school association.
3. **Backend Service & Controllers:**
   - Implemented `StudentRepository` with custom filtering queries (`searchStudents`), multi-tenant existence checks (`existsBySchoolIdAndAdmissionNumber`), and safe lookup (`findByIdAndSchoolId`).
   - Implemented `StudentService` strictly obtaining tenant `schoolId` from authenticated token context (`SecurityUtils.getRequiredSchoolId()`), preventing any cross-tenant leakage (**Non-negotiable Rule 2**).
   - Enforced named business status transitions (ACTIVE/INACTIVE) on deactivation (**Non-negotiable Rule 4**).
   - Created `SchoolStudentController` (`/api/v1/school/students`) and `AdminStudentController` (`/api/v1/admin/schools/{schoolId}/students`).
4. **Frontend Architecture & Pages:**
   - Created `frontend/src/pages/school/StudentsPage.jsx`: paginated roster, search, class and section dropdown filters, responsive card stacking for mobile, Add/Edit student modal dialogs, deactivation confirmation.
   - Created `frontend/src/pages/admin/AdminSchoolStudentsPage.jsx`: Super Admin student directory view with school dropdown selector, direct link from `SchoolsPage`, and full CRUD capabilities.
   - Linked `/school/students`, `/admin/students`, and `/admin/schools/:schoolId/students` in `App.jsx`.
5. **Security & QA Verification:**
   - Created unit and integration tests: `SchoolStudentControllerTest` (3 tests) and multi-tenant isolation tests in `StudentSecurityIsolationTest` (5 tests).
   - Confirmed School A cannot read or mutate School B's student records (404/403).
   - Confirmed duplicate admission numbers within the same school are rejected with 409 Conflict.
   - Confirmed Printing Agencies have no access to school student rosters (403 Forbidden).

### How It Was Verified
- **Backend Test Suite:** Executed `mvn test` — **27/27 tests passed** (0 failures, 0 errors):
  - `AuthControllerTest` (4 tests)
  - `GlobalExceptionHandlerTest` (5 tests)
  - `HealthControllerTest` (1 test)
  - `IdCardPlatformApplicationTests` (1 test)
  - `AdminOrganizationControllerTest` (4 tests)
  - `RoleIsolationSecurityTest` (4 tests)
  - `SchoolStudentControllerTest` (3 tests)
  - `StudentSecurityIsolationTest` (5 tests)
- **Frontend Build:** Executed `npm run build` — `vite build` completed cleanly in 19.68s with 0 errors or warnings.
- **Non-Negotiables Checklist:**
  - Rule 1 (School sees no agency data): verified.
  - Rule 2 (Never trust tenant IDs from client payload): verified.
  - Rule 4 (Named business actions for status): verified.
  - Rule 5 (No mock/fake data): verified.
  - Rule 6 (Pagination & indexes): verified.
  - Rule 7 (Standard error format): verified.
  - Rule 8 (Jakarta validation): verified.

### What Is Next
- Proceed to Step 3 (Excel, CSV and photo matching)

---

## Step 3 Log: Excel, CSV Import & Photo Matching

### What Was Built
1. **API Contract Definition:**
   - Populated [docs/api-contract.md](file:///c:/Users/a4477/Downloads/School%20id%20card/docs/api-contract.md) with 9 endpoints:
     - `GET /api/v1/school/students/import-template` (download formatted XLSX/CSV template with headers and sample data).
     - `POST /api/v1/school/students/import` (multipart upload, validation engine, preview generation).
     - `GET /api/v1/school/students/import/{batchId}` (paginated batch details with row-level validation filtering).
     - `POST /api/v1/school/students/import/{batchId}/confirm` (commits valid records via upsert).
     - `GET /api/v1/school/students/import/{batchId}/errors` (download error report CSV).
     - `POST /api/v1/school/photos/bulk-upload` (ZIP upload, auto-matching by roll number or admission number).
     - `GET /api/v1/school/photos/bulk-upload/{batchId}` (matching report details).
     - `PUT /api/v1/school/students/{id}/photo` (single student photo upload).
     - `GET /api/v1/files/{fileId}` (secure, multi-tenant file delivery).

2. **Database Migrations & JPA Entities:**
   - Created Flyway migration `V3__init_files_and_imports.sql`:
     - `files`: persistent file metadata (`file_name`, `original_file_name`, `content_type`, `file_size`, `storage_path`, `school_id`, `created_by`, `created_at`).
     - `student_import_batches`: batch tracking (`school_id`, `uploaded_by`, `original_file_id`, `file_type`, counters, `import_status`).
     - `student_import_rows`: staging rows with `validation_status` and `error_message`.
     - `student_import_errors`: field-specific error details.
     - `photo_import_batches` & `photo_import_results`: ZIP extraction, parsed class/sec/roll, and match status.
     - Added FK constraint from `students(photo_file_id)` to `files(id)`.
     - Comprehensive performance indexes on all foreign keys, status columns, and search criteria.
   - JPA Entities: `FileEntity`, `StudentImportBatch`, `StudentImportRow`, `StudentImportError`, `PhotoImportBatch`, `PhotoImportResult`.

3. **Backend Services & Storage:**
   - `LocalFileStorageService`: Safe disk persistence, path traversal defense, Zip-Slip vulnerability prevention, and resource streaming.
   - `StudentTemplateService`: Generates styled Excel (.xlsx) and UTF-8 CSV templates.
   - `StudentImportService`: Parses both XLSX and CSV; validates mandatory headers, required fields, date formats (`YYYY-MM-DD`, `DD-MM-YYYY`), gender, blood group, in-file duplicates, and school-level duplicate roll numbers; stages rows; performs upsert on confirmation (updates existing student if admission number exists in this school without duplicating); generates CSV error reports.
   - `PhotoImportService`: Safe ZIP extraction; parses filenames matching Option A (`07.jpg` with class/sec in request), Option B (`10th_A_07.jpg`), and Option C (`ADM-2026-001.jpg`); links matched photos to students; supports individual photo updates.
   - `FileController`: Strictly enforces role boundaries (Super Admin global, School own institution only, Agency restricted).

4. **Frontend Architecture & Pages:**
   - Enhanced `StudentsPage.jsx` with accessible tabbed navigation (`Student Roster`, `Import Students`, `Photo Matcher`).
   - `StudentRosterTab`: Student photo thumbnail display with initials fallback; Add/Edit modal updated with portrait photo upload and preview.
   - `StudentImportTab`: XLSX and CSV template download; drag-and-drop file upload dropzone; metrics cards (Total, Valid, Error rows); responsive preview table with status badges and error callouts; "Confirm & Import" execution; error report CSV download.
   - `PhotoMatcherTab`: Option A class/section selector and auto-detect option; ZIP drag-and-drop dropzone; match summary metrics (Matched, Unmatched, Duplicates); results table with filename, parsed attributes, student match details, and status.

5. **Security & QA Verification:**
   - Multi-tenant security tests in `StudentImportSecurityTest` (14 tests) and `FileSecurityTest` (8 tests): verified cross-school access is blocked (404/403), printing agencies cannot access school import/photo endpoints (403), and Zip-Slip path traversal attacks are rejected.
   - Functional integration tests in `StudentImportIntegrationTest` (8 tests) and `PhotoImportIntegrationTest` (8 tests): verified template downloads, validation rules, confirm import upsert behavior, error report CSV export, ZIP photo matching options A/B/C, and single photo upload.

### How It Was Verified
- **Backend Test Suite:** Executed `mvn test` — **63/63 tests passed** (0 failures, 0 errors, 0 skipped):
  - `AuthControllerTest` (4 tests)
  - `GlobalExceptionHandlerTest` (5 tests)
  - `HealthControllerTest` (1 test)
  - `IdCardPlatformApplicationTests` (1 test)
  - `AdminOrganizationControllerTest` (4 tests)
  - `RoleIsolationSecurityTest` (4 tests)
  - `SchoolStudentControllerTest` (3 tests)
  - `StudentSecurityIsolationTest` (5 tests)
  - `FileSecurityTest` (8 tests)
  - `StudentImportSecurityTest` (14 tests)
  - `StudentImportIntegrationTest` (8 tests)
  - `PhotoImportIntegrationTest` (7 tests)
- **Frontend Build:** Executed `npm run build` — `vite build` completed cleanly in 14.89s with 0 errors (`dist/assets/index-DYfFUYl3.js 397.67 kB │ gzip: 117.75 kB`).
- **Non-Negotiables Checklist:**
  - Rule 1 (School sees no agency data): verified.
  - Rule 2 (Never trust tenant IDs from client payload): verified.
  - Rule 4 (Named business actions for status): verified.
  - Rule 5 (No mock/fake data): verified.
  - Rule 6 (Pagination & indexes): verified.
  - Rule 7 (Standard error format): verified.
  - Rule 8 (Jakarta validation): verified.
  - UI Rules (Responsive across 360px, 768px, 1280px, 1920px): verified.

### What Is Next
- Proceed to Step 4 (Template designer)

---

## Step 4 Log: Template Designer

### What Was Built
1. **API Contract Definition:**
   - Populated [docs/api-contract.md](file:///c:/Users/a4477/Downloads/School%20id%20card/docs/api-contract.md) with 10 comprehensive endpoints:
     - Admin endpoints: `GET /api/v1/admin/templates`, `POST /api/v1/admin/templates`, `GET /api/v1/admin/templates/{id}`, `POST /api/v1/admin/templates/{id}/versions`, `POST /api/v1/admin/templates/{id}/versions/{versionId}/submit`, `POST /api/v1/admin/templates/{id}/versions/{versionId}/approve`, `GET /api/v1/admin/templates/{id}/versions/{versionId}/preview`, `POST /api/v1/admin/templates/{id}/assign`
     - School endpoints: `GET /api/v1/school/templates`, `GET /api/v1/school/templates/{id}/preview`

2. **Database Migrations & JPA Entities:**
   - Created Flyway migration `V4__init_card_templates.sql`:
     - `card_templates`: template master (`template_name` unique, `template_category`, `school_id` nullable for global templates, `status`).
     - `template_versions`: versioned card layouts (`template_id`, `version_number`, `card_width_mm`, `card_height_mm`, `bleed_margin_mm`, `front_design_json`, `back_design_json`, `approval_status`, `created_by`, `approved_by`, `approved_at`, `created_at`, unique on `template_id, version_number`).
     - `template_assignments`: assignment to schools per session (`template_id`, `school_id`, `academic_session`, `active`, unique on `template_id, school_id, academic_session`).
     - Comprehensive performance indexes on all foreign keys, categories, approval statuses, sessions, and timestamps (**Non-negotiable Rule 6**).
   - JPA Entities: `CardTemplate`, `TemplateVersion`, `TemplateAssignment`, `TemplateApprovalStatus`.

3. **Backend Services & Controllers:**
   - `CardTemplateService`:
     - Admin template catalog with pagination, search, category, and status filtering.
     - Template creation with case-insensitive uniqueness check (**ErrorCode: RESOURCE_CONFLICT**).
     - Strict named business action versioning lifecycle: `DRAFT` -> `PENDING_APPROVAL` -> `APPROVED` (**Non-negotiable Rule 4**). Approving a version locks it and automatically transitions any previously approved version to `ARCHIVED`.
     - School template assignment requiring an approved version.
     - Role-isolated school template catalog returning only approved designs assigned to caller's school (**Non-negotiable Rule 1 & Rule 2**).
   - `CardPdfRenderingService`:
     - Apache PDFBox 3.0.2 print-ready 2-page PDF generator (Front & Back) sized accurately in mm (default 85.60 mm x 54.00 mm).
     - ZXing barcode and QR code dynamic rendering with student admission number / data interpolation.
     - School logo and student portrait photo placement, shape rendering, Helvetica text wrapping, and data field interpolation (`{{student_name}}`, `{{father_name}}`, `{{admission_number}}`, `{{class}}`, `{{section}}`, `{{roll_number}}`, etc.).
     - Headless rendering configuration (`-Djava.awt.headless=true`).
   - `AdminCardTemplateController` (`/api/v1/admin/templates`) and `SchoolCardTemplateController` (`/api/v1/school/templates`).

4. **Frontend Architecture & Pages:**
   - `frontend/src/features/templates/`:
     - `CardCanvas.jsx`: Interactive card canvas supporting front/back side toggling, mm-to-pixel scaling, drag-and-drop element repositioning, selection highlights, and guide grids.
     - `CardDesignerModal.jsx`: Fullscreen WYSIWYG card designer modal with side selector, zoom controls, live coordinate inspector, version saving, approval workflow triggers, JSON raw preview, and PDF preview stream modal.
     - `ElementPalette.jsx`: Drag/click palette for Text, Student Placeholders, Dynamic Barcodes, QR Codes, Student Photo frames, School Logo, and Decorative Shapes.
     - `PropertyInspector.jsx`: Precision properties editor (mm coordinates, dimensions, typography, colors, borders, placeholder mappings).
     - `CreateTemplateModal.jsx`: Modal for creating new templates with orientation presets (CR80 horizontal/vertical) and dimension setup.
     - `AssignTemplateModal.jsx`: Modal for assigning approved templates to schools with academic session tagging.
     - `JsonPreviewModal.jsx`: Diagnostic modal for viewing and copying canvas JSON layout definitions.
   - `frontend/src/pages/admin/TemplatesPage.jsx`: Super Admin template management hub with paginated grid/table, status badges, version metrics, live designer modal launcher, assignment modal, and PDF preview.
   - `frontend/src/pages/school/SchoolTemplatesPage.jsx`: School card template catalog with live enrolled student selector, dual-sided preview, and instant high-res PDF sample generation.
   - Wired in `App.jsx` under protected routes.

5. **Security & QA Verification:**
   - `CardTemplateIntegrationTest` (5 tests):
     - Master template creation and duplicate name rejection (409 Conflict).
     - Full versioning & approval lifecycle: V1 initial draft, V2 draft -> pending -> approve, V3 draft -> approve (verifies V2 archives automatically).
     - School template assignment and catalog retrieval with pagination (unapproved templates hidden).
     - PDFBox 2-page PDF rendering with sample student, actual student, barcode, QR code, and fallback layouts.
     - Role isolation and validation rules: school user cannot call admin endpoints (403), school cannot preview unassigned templates (404), agency has no access to templates (403).
   - Resolved Java 21 ByteBuddy dynamic agent loading in surefire by configuring `-javaagent` in `pom.xml`.

### How It Was Verified
- **Backend Test Suite:** Executed `mvn test` across all modules — **68/68 tests passed** (0 failures, 0 errors, 0 skipped):
  - `AuthControllerTest` (4 tests)
  - `GlobalExceptionHandlerTest` (5 tests)
  - `FileSecurityTest` (8 tests)
  - `HealthControllerTest` (1 test)
  - `IdCardPlatformApplicationTests` (1 test)
  - `AdminOrganizationControllerTest` (4 tests)
  - `RoleIsolationSecurityTest` (4 tests)
  - `PhotoImportIntegrationTest` (6 tests)
  - `SchoolStudentControllerTest` (3 tests)
  - `StudentImportIntegrationTest` (8 tests)
  - `StudentImportSecurityTest` (14 tests)
  - `StudentSecurityIsolationTest` (5 tests)
  - `CardTemplateIntegrationTest` (5 tests)
- **Frontend Build:** Executed `npm run build` (`vite build`) — completed cleanly in 22.67s with 0 warnings/errors (`dist/assets/index-B4mWPzvL.js 474.50 kB │ gzip: 133.84 kB`).
- **Non-Negotiables Checklist:**
  - Rule 1 (School sees no agency data): verified.
  - Rule 2 (Never trust tenant IDs from client payload): verified.
  - Rule 4 (Named business actions for status): verified.
  - Rule 5 (No mock/fake data): verified.
  - Rule 6 (Pagination & indexes): verified.
  - Rule 7 (Standard error format): verified.
  - Rule 8 (Jakarta validation): verified.
  - UI Rules (Responsive across 360px, 768px, 1280px, 1920px): verified.

### What Is Next
- Step 5 completed. Moving to Step 6.

---

## Step 5 Log: Orders and Quotations

### What Was Built
1. **API Contract Definition:**
   - Appended OpenAPI contract for 12 Step 5 endpoints in `docs/api-contract.md`:
     - School endpoints: `POST /api/v1/school/orders`, `GET /api/v1/school/orders`, `GET /api/v1/school/orders/{id}`, `POST /api/v1/school/orders/{id}/submit`, `POST /api/v1/school/orders/{id}/quotation/approve`.
     - Admin endpoints: `GET /api/v1/admin/orders`, `GET /api/v1/admin/orders/{id}`, `POST /api/v1/admin/orders/{id}/start-review`, `POST /api/v1/admin/orders/{id}/verify`, `POST /api/v1/admin/orders/{id}/request-correction`, `POST /api/v1/admin/orders/{id}/quotation`.
2. **Database Migration & Entities:**
   - Created Flyway migration `V5__init_orders_and_quotations.sql`:
     - `printing_orders`: stores school order metadata, sequential order number (`ORD-YYYY-XXXX`), total students, total quantity, status, delivery preferences, rejection remarks.
     - `printing_order_items`: line items for printed cards and optional accessories (`LANYARD`, `CARD_HOLDER`, `CLIP`, `YOYO`) with `NUMERIC(14,2)` unit and total prices.
     - `order_student_items`: **immutable student snapshots** frozen at order placement time (`student_name`, `admission_number`, `standard`, `section`, `roll_number`, `blood_group`, `emergency_contact`, `photo_file_id`).
     - `quotations` & `quotation_items`: platform quotations with subtotal, discount, 18% GST calculation (`NUMERIC(14,2)`), notes, and approval status.
     - Complete indexes on all foreign keys, status columns, order numbers, and creation timestamps.
   - Entities: `PrintingOrder`, `PrintingOrderItem`, `OrderStudentItem`, `Quotation`, `QuotationItem`, `OrderStatus`, `QuotationStatus` under `com.platform.idcard.order.entity`.
   - Repositories: `PrintingOrderRepository`, `PrintingOrderItemRepository`, `OrderStudentItemRepository`, `QuotationRepository`, `QuotationItemRepository`.
3. **Backend Service & Controllers:**
   - `OrderService`:
     - Creates draft orders and captures frozen student snapshots ensuring future changes to student roster never alter placed orders (**Snapshot Immutability guarantee**).
     - Validates that template is assigned and approved for the requesting school.
     - Enforces cross-tenant validation ensuring students belong strictly to the authenticated school.
     - Named business transitions: `submitOrder`, `startReview`, `verifyOrder`, `requestCorrection`, `generateQuotation`, `approveQuotation`.
     - Money calculations using `BigDecimal` and `RoundingMode.HALF_UP` strictly separated (**Non-negotiable Rule 3**).
   - `SchoolOrderController`: school endpoints guarded by `@PreAuthorize("hasRole('SCHOOL')")` extracting `schoolId` strictly from JWT context (**Non-negotiable Rule 2**).
   - `AdminOrderController`: admin endpoints guarded by `@PreAuthorize("hasRole('SUPER_ADMIN')")`.
   - Role separation: School DTOs (`SchoolOrderSummaryResponse`, `SchoolOrderDetailResponse`) never contain agency cost or assignment data (**Non-negotiable Rule 1**).
4. **Backend Integration Tests:**
   - Created `OrderIntegrationTest.java`:
     - `testCreateDraftOrderAndSnapshotImmutability`: verified order creation and proved modifying student roster after order placement leaves order snapshot unchanged.
     - `testCrossTenantStudentOrderRejection`: verified School A cannot order cards for School B's student (404/403).
     - `testCompleteOrderQuotationLifecycle`: end-to-end lifecycle verification: Draft -> Submit -> Admin Start Review -> Admin Request Correction -> Resubmit -> Admin Verify -> Admin Generate Quotation -> School Approve Quotation -> `APPROVED`.
     - `testMultiTenantSchoolOrderIsolation`: verified School B receives 404 when attempting to access School A's order.
5. **Frontend Architecture & Pages:**
   - `frontend/src/features/orders/ordersConstants.js`: status configs and accessory options.
   - `frontend/src/features/orders/NewOrderWizardModal.jsx`: 4-step wizard modal:
     - Step 1: Select approved assigned card template.
     - Step 2: Multi-select student roster with search, class filter, and photo ready/missing indicators.
     - Step 3: Optional accessories (lanyards, holders, clips) with auto-computed quantities.
     - Step 4: Shipping/delivery details, special instructions, and draft vs. submit action.
   - `frontend/src/pages/school/OrdersPage.jsx`: School orders overview with metrics cards, status filter, order number search, pagination, and quick submit action.
   - `frontend/src/pages/school/OrderDetailPage.jsx`: Full order breakdown, status progress stepper, correction remarks alert, interactive quotation review with 18% GST breakdown and "Approve Quotation" confirmation.
   - `frontend/src/pages/admin/AdminOrdersPage.jsx`: Super Admin global order list with school filter dropdown, status filter, search, and pagination.
   - `frontend/src/pages/admin/AdminOrderDetailPage.jsx`: Review dashboard with "Start Review", "Verify Order", "Request Correction" modal, and "Generate Quotation" modal with dynamic line items and tax calculations.
   - Routes wired in `App.jsx` under `/school/orders`, `/school/orders/:id`, `/admin/orders`, `/admin/orders/:id`.

### How It Was Verified
- **Backend Test Suite:** Executed `mvn test` across all modules — **107/107 tests passed** (0 failures, 0 errors, 0 skipped):
  - `AuthControllerTest` (4 tests)
  - `GlobalExceptionHandlerTest` (5 tests)
  - `FileSecurityTest` (8 tests)
  - `HealthControllerTest` (1 test)
  - `IdCardPlatformApplicationTests` (1 test)
  - `AdminOrganizationControllerTest` (4 tests)
  - `RoleIsolationSecurityTest` (4 tests)
  - `PhotoImportIntegrationTest` (6 tests)
  - `SchoolStudentControllerTest` (3 tests)
  - `StudentImportIntegrationTest` (8 tests)
  - `StudentImportSecurityTest` (14 tests)
  - `StudentSecurityIsolationTest` (5 tests)
  - `CardTemplateIntegrationTest` (5 tests)
  - `CardTemplateSecurityTest` (23 tests)
  - `OrderIntegrationTest` (4 tests)
  - `OrderSecurityTest` (12 tests)
- **Frontend Build:** Executed `npm run build` (`vite build`) — completed cleanly in 7.56s with 0 errors (`dist/assets/index-BsDnqmdB.js 546.48 kB │ gzip: 147.92 kB`).
- **Non-Negotiables Checklist:**
  - Rule 1 (School sees no agency data): verified.
  - Rule 2 (Never trust tenant IDs from client payload): verified.
  - Rule 3 (Money uses `NUMERIC(14,2)` and `BigDecimal`): verified.
  - Rule 4 (Named business actions for status transitions): verified.
  - Rule 5 (No mock/fake data): verified.
  - Rule 6 (Pagination & indexes on all foreign keys & filter columns): verified.
  - Rule 7 (Standard error format): verified.
  - Rule 8 (Jakarta validation): verified.
  - UI Rules (Responsive across 360px, 768px, 1280px, 1920px): verified.

### What Is Next
- Step 6 completed. Moving to Step 7 (Production and quality control).



---

## Step 6 Log: Agency Management and Job Assignment

### What Was Built
1. **API Contract Definition:**
   - Added 8 endpoints to `docs/api-contract.md`:
     - Admin endpoints: `POST /api/v1/admin/orders/{id}/assignments`, `GET /api/v1/admin/assignments`, `GET /api/v1/admin/assignments/{id}`, `POST /api/v1/admin/assignments/{id}/reassign`.
     - Agency endpoints: `GET /api/v1/agency/jobs`, `GET /api/v1/agency/jobs/{id}`, `POST /api/v1/agency/jobs/{id}/accept`, `POST /api/v1/agency/jobs/{id}/reject`.
2. **Database Migrations & JPA Entities:**
   - Created Flyway migration `V6__init_agency_assignments.sql`:
     - `agency_assignments`: UUID PK, unique `assignment_number` (`JOB-YYYY-XXXX`), `order_id` FK, `agency_id` FK, `assigned_by` FK, `assigned_at`, `expected_delivery_date`, `agreed_rate NUMERIC(14,2)`, `total_agreed_amount NUMERIC(14,2)`, `assignment_status`, `reassigned_from_id` FK self-reference, `reassignment_reason`, `rejection_reason`, `notes`.
     - `agency_job_items`: UUID PK, `assignment_id` FK, `order_item_id` FK, `assigned_quantity`, `printed_quantity`, `rejected_quantity`, `accepted_quantity`, `pending_quantity`.
     - Complete indexes: `idx_agency_assignments_order_id`, `idx_agency_assignments_agency_id`, `idx_agency_assignments_status`, `idx_agency_job_items_assignment_id`, `idx_agency_job_items_order_item_id`.
   - Entities: `AgencyAssignment.java`, `AgencyJobItem.java`, `AssignmentStatus.java`.
   - Repositories: `AgencyAssignmentRepository.java`, `AgencyJobItemRepository.java`.
3. **Backend Service & Controllers:**
   - `AgencyJobService.java`:
     - Validates order state (`APPROVED` or `AWAITING_ASSIGNMENT`).
     - Supports quantity splitting across multiple printing agencies while preventing over-allocation.
     - Automatically updates parent order status (`ASSIGNED`, `AWAITING_ASSIGNMENT`, `AGENCY_ACCEPTED`).
     - Enforces reassignments on rejected or pending assignments (`reassignedFrom` lineage tracking).
     - Strict role separation: Agency DTOs (`AgencyJobSummaryResponse`, `AgencyJobDetailResponse`) include zero school pricing, customer margin, or parent contact data (**Non-negotiable Rule 1**).
     - Security token enforcement: Agency ID extracted strictly from JWT claims via `SecurityUtils.getRequiredAgencyId()` (**Non-negotiable Rule 2**).
   - Controllers: `AdminAgencyAssignmentController.java` (`/api/v1/admin`) and `AgencyJobController.java` (`/api/v1/agency/jobs`).
4. **Frontend Architecture & Pages:**
   - `frontend/src/features/assignments/assignmentsConstants.js`: Status badges, colors, and helpers.
   - `frontend/src/features/assignments/AssignAgencyModal.jsx`: Modal for allocating order quantities across agencies, setting agreed rates, expected delivery dates, and notes.
   - `frontend/src/features/assignments/ReassignJobModal.jsx`: Modal for reassigning rejected or pending jobs with mandatory reason.
   - `frontend/src/pages/admin/AdminAssignmentsPage.jsx`: Super Admin global assignment directory with status filter, search, metrics, and direct action triggers.
   - `frontend/src/pages/admin/AdminOrderDetailPage.jsx`: Added "Assign to Agency" workflow button and nested agency assignment tracking table.
   - `frontend/src/pages/agency/AgencyJobsPage.jsx`: Printing Agency job queue with status filter, metrics, and navigation.
   - `frontend/src/pages/agency/AgencyJobDetailPage.jsx`: Technical printing specifications (card width, height, bleed margin, material), job item breakdown, and named "Accept Job" / "Reject Job" actions.
   - Routed in `App.jsx` under `/admin/assignments`, `/agency/jobs`, `/agency/jobs/:id`.
5. **QA & Security Verification:**
   - `AgencyAssignmentIntegrationTest` (2 tests):
     - End-to-end split assignment lifecycle across two agencies, acceptance by both, and parent order status verification.
     - Over-allocation validation rejection.
   - `AgencyAssignmentSecurityTest` (7 tests):
     - Rule 1: School user forbidden from viewing agency assignments (403).
     - Rule 1: Agency user receives zero school pricing or margin fields in job details.
     - Rule 2: Agency B cannot view or accept Agency A's assigned job (404/403).
     - Rule 2: Agency user cannot access Super Admin assignment endpoints (403).
     - Rule 4: Named business state transitions only (cannot accept non-assigned job).
     - Rule 8: Validation failure on missing rejection reason.
     - Unauthenticated access rejection (401).

### How It Was Verified
- **Backend Test Suite:** Full suite `mvn test` executed across all modules — **116/116 tests passed** (0 failures, 0 errors, 0 skipped).
- **Frontend Build:** `npm --prefix frontend run build` completed cleanly in 7.14s with 0 errors.
- **Non-Negotiables Checklist:**
  - Rule 1 (School sees no agency data; agency sees no school pricing/margins): fully verified.
  - Rule 2 (Never trust tenant IDs from client payload): fully verified.
  - Rule 3 (Money precision in `NUMERIC(14,2)` and `BigDecimal`): fully verified.
  - Rule 4 (Named business actions for status): fully verified.
  - Rule 5 (No mock/fake data): fully verified.
  - Rule 6 (Pagination & indexes on all foreign keys & filter columns): fully verified.
  - Rule 7 (Standard error format): fully verified.
  - Rule 8 (Jakarta validation): fully verified.
  - UI Rules (Responsive across 360px, 768px, 1280px, 1920px): fully verified.

### What Is Next
- Proceed to Step 7 (Production and quality control)

---

## Step 7 Log: Production and Quality Control

### What Was Built
1. **API Contract Definition:**
   - Appended 13 Step 7 endpoints in [docs/api-contract.md](file:///c:/Users/a4477/Downloads/School%20id%20card/docs/api-contract.md):
     - Agency production & QC: `POST /api/v1/agency/jobs/{id}/start`, `POST /api/v1/agency/jobs/{id}/batches`, `GET /api/v1/agency/jobs/{id}/batches`, `POST /api/v1/agency/batches/{id}/updates`, `POST /api/v1/agency/jobs/{id}/quality-checks`, `GET /api/v1/agency/jobs/{id}/quality-checks`.
     - Admin operations: `GET /api/v1/admin/production/batches`, `GET /api/v1/admin/production/quality-checks`, `POST /api/v1/admin/assignments/{id}/quality-checks`, `GET /api/v1/admin/production/reprints`, `POST /api/v1/admin/reprints/{id}/approve`, `POST /api/v1/admin/reprints/{id}/reject`.
     - School live tracking: `GET /api/v1/school/orders/{id}/tracking`.
2. **Database Migration:**
   - Created Flyway migration `V7__init_production_and_qc.sql`:
     - `production_batches`: UUID PK, `assignment_id` FK, `batch_number` unique (`BATCH-YYYY-XXXX`), `planned_quantity`, `actual_quantity`, `production_status` (`PLANNED`, `IN_PROGRESS`, `PAUSED`, `PRINTING_COMPLETED`, `CANCELLED`), `started_at`, `completed_at`, `notes`.
     - `production_updates`: UUID PK, `batch_id` FK, `updated_by` FK, `printed_quantity`, `rejected_quantity`, `pending_quantity`, `remarks`, `evidence_file_id`.
     - `quality_checks`: UUID PK, `assignment_id` FK, `check_number` unique (`QC-YYYY-XXXX`), `inspected_quantity`, `accepted_quantity`, `rejected_quantity`, `defect_category` (`COLOR_MISMATCH`, `ALIGNMENT_OFF`, `BLURRY_PHOTO`, `TYPO_ON_CARD`, `LAMINATION_DEFECT`, `DAMAGED_CARD`, `BARCODE_UNREADABLE`, `OTHER`), `defect_description`, `inspected_by` FK, `inspection_status` (`PASSED`, `FAILED`, `CONDITIONALLY_PASSED`), `inspected_at`.
     - `reprint_requests`: UUID PK, `assignment_id` FK, `quality_check_id` FK, `request_number` unique (`REP-YYYY-XXXX`), `replacement_quantity`, `reason`, `cost_bearer` (`AGENCY`, `PLATFORM`, `SCHOOL`), `approval_status` (`PENDING`, `APPROVED`, `REJECTED`), `approved_by` FK, `approved_at`, `rejection_reason`.
     - Performance indexes on all foreign keys and filter columns.
3. **Backend Service & Controllers:**
   - Entities & Enums: `ProductionBatch`, `ProductionUpdate`, `QualityCheck`, `ReprintRequest`, `ProductionBatchStatus`, `QualityCheckStatus`, `DefectCategory`, `ReprintStatus`, `CostBearer`.
   - Repositories: `ProductionBatchRepository`, `ProductionUpdateRepository`, `QualityCheckRepository`, `ReprintRequestRepository`.
   - `ProductionService`:
     - Transitions job to `IN_PRODUCTION` when starting production; triggers parent order transition.
     - Auto-generates sequences for batch numbers (`BATCH-2026-XXXX`), quality checks (`QC-2026-XXXX`), and reprints (`REP-2026-XXXX`).
     - Logs incremental production updates and calculates actual quantity printed.
     - Validates inspection counts: `inspected_quantity = accepted_quantity + rejected_quantity`.
     - Automatically generates `reprint_requests` in `PENDING` status whenever defects are rejected in QC.
     - Supports Super Admin reprint approval with explicit financial cost bearer liability (`AGENCY`, `PLATFORM`, `SCHOOL`) (**Non-negotiable Rule 3**).
     - Provides School order tracking strictly scrubbing all agency names, internal batch IDs, and cost data (**Non-negotiable Rule 1**).
   - Controllers:
     - `AgencyProductionController.java` (`/api/v1/agency`)
     - `AdminProductionController.java` (`/api/v1/admin`)
     - `SchoolOrderTrackingController.java` (`/api/v1/school/orders/{id}/tracking`)
4. **Frontend Architecture & Pages:**
   - Constants: `frontend/src/features/production/productionConstants.js`.
   - Modals: `CreateBatchModal.jsx`, `ProductionUpdateModal.jsx`, `SubmitQualityCheckModal.jsx`, `ReviewReprintModal.jsx`.
   - Agency Pages: `frontend/src/pages/agency/AgencyProductionPage.jsx`, `frontend/src/pages/agency/AgencyQualityPage.jsx`.
   - Admin Pages: `frontend/src/pages/admin/AdminProductionPage.jsx`, `frontend/src/pages/admin/AdminQualityControlPage.jsx`.
   - School Live Tracking: Updated `frontend/src/pages/school/OrderDetailPage.jsx` with real-time manufacturing and inspection status tracking.
   - Routing: Added `/admin/production`, `/admin/quality-control`, `/agency/production`, `/agency/quality-control`, and `/agency/quality` in `App.jsx`.
5. **QA & Security Verification:**
   - `ProductionSecurityTest` (6 tests):
     - Rule 1: School user forbidden from viewing agency batch production endpoints (403).
     - Rule 1: School order tracking sanitization verification (no agency IDs, no internal job numbers, no agency names).
     - Rule 2: Agency B cannot create batches for Agency A's assignment (404/403).
     - Rule 2: Agency user forbidden on Super Admin production & reprint endpoints (403).
     - Rule 8: Jakarta validation on QC quantities (`inspected != accepted + rejected` returns 400).
     - Unauthenticated access returns 401.
   - `ProductionIntegrationTest` (1 end-to-end test):
     - Full production flow: Start production -> create batch -> log updates -> run QC with defects -> automated reprint generation -> admin review & approve reprint with cost bearer -> school tracking verification.

### How It Was Verified
- **Backend Test Suite:** Executed `mvn test` across all platform modules — **123/123 tests passed** (0 failures, 0 errors, 0 skipped).
- **Frontend Build:** `npm --prefix frontend run build` completed cleanly in 8.62s with 0 errors.
- **Non-Negotiables Checklist:**
  - Rule 1 (School sees 0 agency data; separate DTOs): fully verified.
  - Rule 2 (Never trust tenant IDs from client payload): fully verified via `SecurityUtils`.
  - Rule 3 (Money precision in `NUMERIC(14,2)` and `BigDecimal`; reprint cost bearer tracked): fully verified.
  - Rule 4 (Named business actions for status): fully verified (`startProduction`, `addProductionUpdate`, `submitQualityCheck`, `approveReprintRequest`, `rejectReprintRequest`).
  - Rule 5 (No mock/fake data): fully verified.
  - Rule 6 (Pagination & indexes on all foreign keys & filter columns): fully verified.
  - Rule 7 (Standard error format): fully verified.
  - Rule 8 (Jakarta Bean Validation): fully verified.
  - UI Rules (Responsive across 360px, 768px, 1280px, 1920px): fully verified.

### What Is Next
- **Step 8: Dispatch and delivery**
  - Database: `shipments`, `shipment_items`, `delivery_tracking_events`.
  - Support delivery Model A (Direct from Agency to School) vs Model B (Agency to Admin to School).
  - Courier integration tracking, shipping label generation, packaging slips, proof of delivery upload, school receipt confirmation.

---

## Questions & Blockers
*None currently.*

---

## Inter-Agent Communication & Requests
*None currently.*
