# School ID Card – Technical Design&#32;

**Version:** 1.1 · **Backend:** Java 17 + Spring Boot 3 · **Frontend:** React (JavaScript) · **Database:** PostgreSQL · **Style:** Modular Monolith

1\. What is this platform?

Schools need ID cards for students, teachers and staff. This platform lets a school send its data and order cards online. The platform owner (Admin) checks the order, gives a price, and hands the printing work to a printing agency. Everything is tracked until the cards reach the school.

**The one rule that matters most:** the school must never find out which printing agency prints its cards. The school only talks to the Admin.

### 1.1 Who uses it

| Role | What they do |
| --- | --- |
| **SUPER\_ADMIN** | Runs the whole platform: checks orders, sets prices, picks agencies, tracks money |
| **SCHOOL** | Adds students, picks a card design, places orders, pays, tracks delivery |
| **PRINTING\_AGENCY** | Sees only the jobs given to it, prints, reports progress, dispatches |

### 1.2 Goals

- One central place for all ID card orders.
- Schools manage their own student data.
- Admin controls prices and agency assignment.
- Agencies see only their own jobs.
- Every order is tracked from submission to delivery.
- Money records are clear and auditable.
- School, student and agency data stays protected.

---

## 2. Technology choices

| Part | Technology | Why / Note |
| --- | --- | --- |
| Backend language | **Java 17** | Long-term support version, minimum required by Spring Boot 3 |
| Backend framework | **Spring Boot 3.x** | Standard for Java web apps |
| Security | Spring Security + JWT | Login with access and refresh tokens |
| Database access | Spring Data JPA / Hibernate 6 | Talks to PostgreSQL |
| Database | PostgreSQL | Reliable relational database |
| DB migrations | Flyway | Keeps database changes under version control |
| Excel import | Apache POI | Reads student Excel files |
| PDF creation | Apache PDFBox | Makes print-ready PDFs |
| QR codes | ZXing | Generates QR codes on cards |
| Validation | Jakarta Bean Validation | Checks incoming data |
| API docs | OpenAPI / Swagger | Auto-generated API documentation |
| Backend tests | JUnit 5, Mockito, Spring Boot Test |  |
| Frontend | React + JavaScript (ES6+), `.jsx` files | No TypeScript |
| Styling | Tailwind CSS |  |
| Routing | React Router |  |
| API calls | Axios |  |
| Server data | TanStack Query | Caches data from the API |
| Local UI state | Zustand |  |
| Frontend tests | React Testing Library |  |

### 2.1 What Java 17 means for the team

- **Use:** records (short data classes, good for DTOs), text blocks, `switch` expressions, `sealed` classes, and `var`.
- **Not available:** virtual threads and a few other features that arrived in Java 21. Use normal thread pools or Spring `@Async` for background jobs.
- **Spring Boot 3 uses `jakarta.*`** imports instead of the old `javax.*`. Make sure all libraries are Boot 3 compatible.
- Upgrading to Java 21 later is easy because the code is standard Spring Boot.

---

## 3. System architecture

### 3.1 Modular Monolith – in simple words

We build **one backend application**, but we split it into clear **modules** (like rooms in one house). Each module has its own logic and does not reach into other modules' internals. This is simpler than microservices and still keeps the code tidy.

### 3.2 Big picture

```text
Admin Portal ─┐
School Portal ─┼─► React Frontend ─► REST API ─► Spring Security ─► Spring Boot App ─► PostgreSQL
Agency Portal ─┘                                                           │
                                                                           └─► Private File Storage
```

### 3.3 Rules every developer follows

1. Each module owns its own business logic.
2. Controllers only receive requests and return responses – **no business logic**.
3. Database access happens only through repositories.
4. Business workflows live in services.
5. Check both the **role** and the **specific record** a user is allowed to touch.
6. Never delete financial or assignment history.
7. Notifications are messages only – they are **not** the source of truth for order status.
8. A school's **parent order** and an agency's **job** are separate things.

---

## 4. Backend modules

| Module | Job |
| --- | --- |
| Authentication | Login, logout, refresh tokens, permissions |
| Organization | Schools and agencies |
| Student | Student, teacher and staff records |
| Template | ID card designs and versions |
| Order | School orders (parent orders) |
| Agency Job | Assign, split and reassign work |
| Production | Print batches, progress, defects |
| Quality Control | Check, reject, reprint |
| Delivery | Dispatch, courier, delivery proof |
| Finance | Quotes, invoices, payments, agency settlements |
| Notification | In-app, email, optional SMS/WhatsApp |
| Reporting | Operational and money reports |
| Audit | Who did what, and when |

### 4.1 Package layout

```text
com.platform.idcard
├── config
├── common        (exception, response, validation, enums, util)
├── auth          (controller, service, repository, entity, dto, security)
├── organization
├── student
├── template
├── order
├── agencyjob
├── production
├── quality
├── delivery
├── finance
├── notification
├── reporting
└── audit
```

Every module uses the same inside structure: **controller → service → repository → entity → dto**.

---

## 5. Frontend structure

```text
src/
├── assets/
├── components/   (common, forms, tables, layouts, modals)
├── pages/        (admin, school, agency)
├── features/     (students, templates, orders, assignments, production, delivery, finance, notifications)
├── services/     (api.js, authService.js, studentService.js, orderService.js, financeService.js)
├── hooks/
├── store/
├── routes/
├── utils/
├── App.jsx
└── main.jsx
```

### 5.1 Screens per role

| Role | Pages |
| --- | --- |
| **Admin** `/admin` | dashboard, schools, agencies, students, templates, orders, assignments, production, quality-control, delivery, finance, reports |
| **School** `/school` | dashboard, students, teachers, templates, new-order, orders, payments, invoices |
| **Agency** `/agency` | dashboard, assigned-jobs, production, quality-control, dispatch, settlements |

> Hiding a page in the frontend is only for a nicer experience. **The backend must check every request again.**

---

## 6. Database design

### 6.1 Rules

- PostgreSQL, **UUID** primary keys, foreign keys everywhere.
- `TIMESTAMPTZ` for all dates and times.
- `NUMERIC(14,2)` for all money values.
- Soft delete where recovery matters; never casually delete history.
- Admission number is unique **per school**: `UNIQUE (school_id, admission_number)`.
- Parent orders and agency assignments are different tables.

### 6.2 Tables by area

**Login and users**

| Table | Main columns |
| --- | --- |
| `users` | id, full\_name, email, mobile, password\_hash, status, created\_at, updated\_at |
| `roles` | id, role\_name (SUPER\_ADMIN, SCHOOL, PRINTING\_AGENCY) |
| `user_roles` | user\_id, role\_id |
| `school_users` | id, user\_id, school\_id, designation |
| `agency_users` | id, user\_id, agency\_id, designation |

**Schools and agencies**

| Table | Main columns |
| --- | --- |
| `schools` | id, school\_name, school\_code, address, city, state, pincode, email, mobile, logo\_file\_id, status |
| `agencies` | id, agency\_name, contact\_person, email, mobile, address, production\_capacity, capabilities, status |

**Students**

| Table | Main columns |
| --- | --- |
| `students` | id, school\_id, admission\_number, student\_name, father\_name, mother\_name, date\_of\_birth, gender, blood\_group, class\_name, section, roll\_number, address, mobile, photo\_file\_id, status |
| `student_import_batches` | id, school\_id, uploaded\_by, original\_file\_id, total / successful / failed records, import\_status |
| `student_import_errors` | id, batch\_id, row\_number, field\_name, error\_message |

**Templates (card designs)**

| Table | Main columns |
| --- | --- |
| `card_templates` | id, template\_name, school\_id, template\_category, status |
| `template_versions` | id, template\_id, version\_number, front\_design\_json, back\_design\_json, card\_width, card\_height, approval\_status |
| `template_assignments` | id, template\_id, school\_id, academic\_session, active |

> Templates are **versioned**. An order always keeps the exact version that was approved.

**Parent orders (what the school sees)**

| Table | Main columns |
| --- | --- |
| `printing_orders` | id, order\_number, school\_id, academic\_session, template\_version\_id, total\_students, total\_quantity, order\_status, delivery\_model, quotation\_id, submitted\_at |
| `printing_order_items` | id, order\_id, product\_type, quantity, unit\_selling\_price, item\_total |
| `order_student_items` | id, order\_id, student\_id and **snapshot** copies of admission number, name, class, section, roll number, photo, approved template version |

> **Why snapshots?** If a school edits a student later, an already-approved print job must not change silently.

**Agency assignments (internal only)**

| Table | Main columns |
| --- | --- |
| `agency_assignments` | id, assignment\_number, order\_id, agency\_id, assigned\_by, assigned\_at, expected\_delivery\_date, agreed\_rate, assignment\_status, reassigned\_from\_id |
| `agency_job_items` | id, assignment\_id, order\_item\_id, assigned / printed / rejected / accepted / pending quantity |

One parent order can be split across many agencies:

```text
Parent Order ORD-1001 (1000 cards)
 ├─ Assignment A: 400 cards
 ├─ Assignment B: 350 cards
 └─ Assignment C: 250 cards
```

The school sees **one order**. It never sees A, B or C.

**Production and quality**

| Table | Main columns |
| --- | --- |
| `production_batches` | id, assignment\_id, batch\_number, planned\_quantity, actual\_quantity, production\_status, started\_at, completed\_at |
| `production_updates` | id, batch\_id, updated\_by, printed / rejected / pending quantity, remarks, evidence\_file\_id |
| `quality_checks` | id, assignment\_id, inspected / accepted / rejected quantity, defect\_description, inspected\_by, inspection\_status |
| `reprint_requests` | id, assignment\_id, quality\_check\_id, replacement\_quantity, reason, approval\_status |

**Delivery**

| Table | Main columns |
| --- | --- |
| `dispatch_records` | id, assignment\_id, dispatch\_number, courier\_name, tracking\_number, dispatch\_quantity, dispatch\_date, dispatch\_status, proof\_file\_id |
| `delivery_records` | id, order\_id, dispatch\_id, delivery\_quantity, delivery\_model, received\_by, proof\_of\_delivery\_file\_id, delivered\_at, delivery\_status |

An order can have several dispatches and partial deliveries.

**Finance**

| Table | Main columns |
| --- | --- |
| `quotations` | id, quotation\_number, order\_id, subtotal, discount, tax\_amount, total\_amount, quotation\_status, valid\_until |
| `quotation_items` | id, quotation\_id, product\_description, quantity, unit\_price, amount |
| `payments` | id, order\_id, payment\_reference, amount, payment\_method, payment\_status, transaction\_reference, received\_at |
| `invoices` | id, invoice\_number, order\_id, quotation\_id, invoice\_amount, tax\_amount, invoice\_status, issued\_at |
| `cost_entries` | id, order\_id, assignment\_id, cost\_category, description, amount |
| `agency_settlements` | id, assignment\_id, agency\_id, agreed\_amount, paid\_amount, outstanding\_amount, settlement\_status, settled\_at |

**Notifications and audit**

| Table | Main columns |
| --- | --- |
| `notifications` | id, recipient\_user\_id, notification\_type, title, message, reference\_type, reference\_id, read\_status |
| `audit_logs` | id, actor\_user\_id, action, entity\_type, entity\_id, old\_values, new\_values, ip\_address, created\_at |

---

## 7. How an order flows

### 7.1 School side

```text
1. School logs in
2. Adds or imports students (Excel + photos)
3. System validates the data
4. School picks a card template
5. School creates and submits the order
6. Admin reviews the data
      ├─ Problem? → Admin asks for corrections → School fixes → back to review
      └─ Correct? → Admin verifies
7. Admin generates a quotation
8. School approves the quotation
9. School pays
10. Order is ready for assignment
```

### 7.2 Admin and agency side

```text
1. Admin reviews the verified order
2. Admin chooses agency (or several, splitting quantity)
3. Agency receives the job
      ├─ Rejects → Admin reassigns to another agency
      └─ Accepts → production starts
4. Agency posts production updates
5. Quality inspection
      ├─ Failed → reprint approved → production again
      └─ Passed → ready for dispatch
```

### 7.3 Delivery – two models (Admin picks one per order)

**Model A – Direct:** Agency prints → quality approved → agency dispatches to school → school receives → proof of delivery → admin closes delivery.

**Model B – Via Admin:** Agency prints → agency dispatches to admin → admin receives and inspects → admin arranges delivery to school → school receives → proof of delivery.

---

## 8. Order statuses

Statuses move only in controlled steps.

| Group | Statuses |
| --- | --- |
| **Preparing** | DRAFT, SUBMITTED, UNDER\_REVIEW, CORRECTION\_REQUIRED, VERIFIED |
| **Money** | QUOTATION\_GENERATED, AWAITING\_APPROVAL, APPROVED |
| **Assignment** | AWAITING\_ASSIGNMENT, ASSIGNED, AGENCY\_ACCEPTED |
| **Production** | IN\_PRODUCTION, PARTIALLY\_COMPLETED, PRINTING\_COMPLETED, QUALITY\_CHECK, REPRINT\_REQUIRED |
| **Delivery** | READY\_FOR\_DISPATCH, DISPATCHED, PARTIALLY\_DELIVERED, DELIVERED |
| **Finish** | CLOSED |
| **Exceptions** | ON\_HOLD, CANCELLED |

### 8.1 The parent status is calculated

The parent order status comes from its child jobs, quality checks and deliveries.

```text
Parent Order: 1000 cards
  Assignment A: 400 – Delivered
  Assignment B: 350 – Dispatched
  Assignment C: 250 – In production
→ Parent status: PARTIALLY_COMPLETED
```

### 8.2 No generic "update status" endpoint

Status changes only happen through named business actions:

`verifyOrder()` · `requestCorrection()` · `generateQuotation()` · `approveQuotation()` · `assignAgency()` · `acceptAssignment()` · `startProduction()` · `completeQualityCheck()` · `confirmDispatch()` · `confirmDelivery()` · `closeOrder()`

---

## 9. API design

**Base path:** `/api/v1` · **Format:** JSON · **Auth:** JWT. Every API checks who the user is **and** whether they may access that exact record.

### 9.1 Authentication

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | /auth/login | Login |
| POST | /auth/refresh | Refresh token |
| POST | /auth/logout | Logout |
| GET | /auth/me | Current user |

### 9.2 Admin

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | /admin/dashboard | Dashboard |
| GET / POST | /admin/schools | List / register schools |
| GET / POST | /admin/agencies | List / register agencies |
| GET | /admin/orders | All parent orders |
| GET | /admin/orders/{id} | Order details |
| POST | /admin/orders/{id}/verify | Verify data |
| POST | /admin/orders/{id}/corrections | Request corrections |
| POST | /admin/orders/{id}/quotation | Generate quotation |
| POST | /admin/orders/{id}/assignments | Assign agency jobs |
| GET | /admin/assignments | List assignments |
| POST | /admin/assignments/{id}/reassign | Reassign a job |
| GET | /admin/reports | Reports |

### 9.3 School

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | /school/dashboard | Dashboard |
| GET / POST | /school/students | List / create students |
| PUT | /school/students/{id} | Update a student |
| POST | /school/students/import | Bulk Excel import |
| POST | /school/photos/bulk-upload | Bulk photo ZIP upload |
| GET | /school/templates | Available templates |
| POST | /school/orders | Create order |
| GET | /school/orders | Own order history |
| GET | /school/orders/{id} | Own order details |
| GET | /school/orders/{id}/tracking | Order tracking |
| POST | /school/orders/{id}/approve-quotation | Approve quotation |
| GET | /school/orders/{id}/invoice | Download invoice |

### 9.4 Agency

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | /agency/dashboard | Dashboard |
| GET | /agency/jobs | Assigned jobs |
| GET | /agency/jobs/{id} | Job details |
| POST | /agency/jobs/{id}/accept | Accept |
| POST | /agency/jobs/{id}/reject | Reject |
| POST | /agency/jobs/{id}/start | Start production |
| POST | /agency/jobs/{id}/production-updates | Report progress |
| POST | /agency/jobs/{id}/quality-checks | Submit quality info |
| POST | /agency/jobs/{id}/dispatch | Dispatch |
| GET | /agency/settlements | Settlement history |

> Agency APIs must **never** return school prices, platform margin, or another agency's records.

---

## 10. What each role is allowed to see (response design)

Each role gets its own response class (DTO). In Java 17 these are easy to write as **records**.

### 10.1 School sees

```json
{
  "orderNumber": "ORD-1001",
  "status": "IN_PRODUCTION",
  "totalQuantity": 500,
  "deliveredQuantity": 0,
  "quotation": { "totalAmount": 12500, "paymentStatus": "PARTIALLY_PAID" },
  "tracking": [
    { "status": "IN_PRODUCTION", "message": "Your ID cards are being printed.", "timestamp": "2026-11-02T10:30:00Z" }
  ]
}
```

**Hidden from school:** agency name, agency ID, assignment ID, internal job number, agency contact, agency purchase price, platform profit, internal assignment history.

### 10.2 Agency sees

```json
{
  "jobNumber": "JOB-5001",
  "product": "PVC Student ID Card",
  "quantity": 250,
  "expectedDeliveryDate": "2026-11-15",
  "productionStatus": "IN_PRODUCTION",
  "printSpecifications": { "width": 85.6, "height": 54, "unit": "mm" }
}
```

The agency gets only what it needs to finish the work.

---

## 11. ID card template designer

**What it supports:** front and back design, card size, school logo and background, text fields, student photo, QR code and barcode, font settings, live data fields, preview, and versions.

**Data fields** (placeholders replaced with real student data):

`{{student_name}}` `{{father_name}}` `{{admission_number}}` `{{class}}` `{{section}}` `{{roll_number}}` `{{date_of_birth}}` `{{blood_group}}` `{{school_name}}` `{{school_address}}` `{{academic_session}}`

**Storage:** the design is saved as JSON.

```json
{
  "dimensions": { "width": 85.6, "height": 54, "unit": "mm" },
  "front": {
    "elements": [
      { "type": "text",  "field": "student_name",  "x": 10, "y": 20, "fontSize": 12 },
      { "type": "image", "field": "student_photo", "x": 55, "y": 10, "width": 20, "height": 25 }
    ]
  }
}
```

Once a print-ready file is approved, it is **locked** for that job. Later template edits never change old orders.

### 11.1 Template designer: build plan

The designer is built in five steps, and all five together make up Phase 4 of the development plan. Each step ends with something the team can demo.

| Step | What gets built | Done when |
| --- | --- | --- |
| 1. Canvas and data model | A blank card canvas sized in mm, and the JSON format that stores a design | A design saves to and loads from `template_versions` |
| 2. Basic elements | Text, school logo, student photo box, shapes, background image; drag, resize, front and back sides | An admin can design a simple front and back card |
| 3. Data fields and preview | Placeholders like `{{student_name}}`, QR code and barcode (ZXing), preview with a sample student | Preview shows real student data in the right place |
| 4. Versions and approval | New version on every save, approval flow, lock after approval | An approved version can no longer be edited |
| 5. Print-ready output | PDF export with PDFBox for one card and for a full order batch | An agency receives a correct print PDF for a whole order |

### 11.2 How it works for each role

- **Admin** creates and edits templates, and approves versions. This uses the `/admin/templates` screen.
- **School** only picks from the templates assigned to it and sees a preview with its own students. This uses `/school/templates`.
- **Agency** never opens the designer. It receives only the final print-ready file with its print specifications.

### 11.3 Template status flow

A template version moves in one direction only:

1. **DRAFT:** being designed, can be changed freely.
2. **PENDING\_APPROVAL:** submitted for review, no edits.
3. **APPROVED:** locked, can be used in orders.
4. **ARCHIVED:** no longer offered to schools, but old orders keep using it.

To change an approved design, the admin creates a **new version**. The old version stays exactly as it was.

### 11.4 Design rules the designer enforces

| Rule | Value |
| --- | --- |
| Default card size | 85.6 mm × 54 mm (standard ID card) |
| Unit for all positions | mm, converted to pixels only for screen display |
| Print resolution | 300 DPI for images |
| Bleed (extra edge for cutting) | 3 mm, adjustable per template |
| Safe margin | Text and photos stay inside the margin |
| Long text | Shrinks to fit its box, down to a minimum font size |
| Missing photo | A placeholder is printed and the student is flagged before submission |
| Fonts | Only fonts installed on the server, so preview and PDF look the same |

### 11.5 Template APIs

The earlier API list only had `/school/templates`. These endpoints complete the designer:

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET / POST | /admin/templates | List and create templates |
| GET | /admin/templates/{id} | Template with all versions |
| POST | /admin/templates/{id}/versions | Save a new version |
| POST | /admin/templates/{id}/versions/{versionId}/submit | Send for approval |
| POST | /admin/templates/{id}/versions/{versionId}/approve | Approve and lock |
| GET | /admin/templates/{id}/versions/{versionId}/preview | Preview PDF with sample data |
| POST | /admin/templates/{id}/assign | Give a template to a school for a session |
| GET | /school/templates | Templates available to this school |
| GET | /school/templates/{id}/preview | Preview with the school's own students |

### 11.6 Risks and open points

- **Canvas library:** the frontend needs a drawing library for drag and resize. Fabric.js and Konva are the usual choices; pick one in Step 1 and keep it for the whole project.
- **Screen versus PDF:** the browser preview and the PDFBox output must match. Test with the same sample student in both.
- **Who designs templates:** this plan assumes only the admin designs them. If schools may upload their own artwork, add an upload and approval step.
- **Large batches:** generating PDFs for thousands of students runs as a background job, like the Excel import.

---

## 12. Importing students

### 12.1 Excel import

1. School downloads the standard Excel template.
2. School fills in student details and uploads it.
3. Backend checks columns and values.
4. Backend checks duplicate admission numbers and missing required fields.
5. System creates an **error report** for bad rows.
6. Good rows are imported.
7. Import history is saved.

### 12.2 Bulk photo upload

Schools upload a ZIP of photos named by admission number, e.g. `STU001.jpg`, `STU002.jpg`.

Match by admission number or a stable ID – **never by name alone**. The system reports:

- missing photos
- duplicate IDs
- unsupported formats
- corrupt images
- files that are too large
- photos with no matching student

Large imports run as **background jobs** so the web request does not hang.

### 12.3 Three ways to add student data in the school portal

The school can use any of these. Excel and CSV go through the same checks, so the result is identical.

| Method | Best for | Format |
| --- | --- | --- |
| Add one student | A few new students during the year | Form on the Students page |
| Excel upload | Whole school or whole class at once | `.xlsx` using the standard template |
| CSV upload | Data exported from another school system | `.csv`, same columns as the Excel template |

The school downloads the template from the portal in either format, so the column names are always correct. Both file types are converted into one internal row format (Apache POI reads Excel; Apache Commons CSV reads CSV), and then one shared validator checks every row.

### 12.4 Excel and CSV import steps

1. The school picks the file and the portal sends it to `/school/students/import`.
2. The backend saves the original file privately and creates an import batch with status `PROCESSING`.
3. A background job reads the rows and checks each one (see 12.5).
4. The portal shows a **preview**: how many rows are valid, how many have errors, and what each error is.
5. The school fixes the file and uploads again, or confirms to import only the valid rows.
6. Valid rows are saved. Existing students with the same admission number are **updated**, not duplicated.
7. The batch ends as `COMPLETED` or `COMPLETED_WITH_ERRORS`, and the error report can be downloaded.

**CSV rules:** UTF-8 encoding (so Hindi and other names read correctly), comma separator, first row is the header. Dates are accepted as `DD-MM-YYYY` or `YYYY-MM-DD`.

### 12.5 What the validator checks

| Check | Result if it fails |
| --- | --- |
| Required columns missing | Whole file rejected with a clear message |
| Required value empty (admission number, student name, class, section, roll number) | That row is marked as an error |
| Duplicate admission number inside the file or the school | Row marked as an error, or updated if it is the same student |
| Duplicate roll number in the same class and section | Row marked as an error |
| Invalid date, gender or blood group | Row marked as an error |
| Roll number is not a number or code the school uses | Row marked as an error |

Each error is saved in `student_import_errors` with the row number, the column and a plain message such as "Roll number 12 is repeated in Class 5-A".

### 12.6 Automatic photo matching by roll number

The school uploads one ZIP of photos. The system reads the roll number from each file name and links the photo to the right student. No manual picking is needed for photos that match.

**A roll number alone is not unique in a school.** Class 5-A and Class 6-B can both have roll number 12. So the match uses **class + section + roll number** together. There are two ways to name the files:

| Option | How it works | Example |
| --- | --- | --- |
| **A. Pick the class first** | The school chooses class and section on the upload screen, and files are named with only the roll number | Choose Class 5, Section A, then `12.jpg`, `13.jpg` |
| **B. Class in the file name** | One ZIP for many classes, with class and section in each name | `5_A_12.jpg`, `5_B_07.jpg` |

The matching steps:

1. The school uploads the ZIP (and picks the class and section for Option A).
2. A background job unzips the files and checks that each one is a real image.
3. For each file, the system removes the extension and leading zeros (`07.jpg` becomes roll number 7) and reads the class and section if they are in the name.
4. It looks for the one student in this school with that class, section and roll number.
5. Exactly one student found: the photo is resized, saved privately, and linked to that student (`photo_file_id`).
6. Zero or several students found: the photo is not linked and goes to the exceptions list.
7. The portal shows the match report.

**Match report**

| Result | Meaning | What the school does |
| --- | --- | --- |
| Matched | Photo linked to one student | Nothing |
| No student found | File name does not match any roll number | Fix the name or assign by hand |
| Duplicate photo | Two files for the same student | Choose which one to keep |
| Student without photo | Student has no file in the ZIP | Upload that photo later |
| Bad image | Corrupt file or unsupported format | Upload a new file |

The school can fix any exception directly in the report by choosing the student for a photo, without re-uploading the whole ZIP. A new photo for an already matched student asks for confirmation before it replaces the old one.

**Photo rules (default values, all configurable):** JPG, JPEG or PNG; up to 2 MB per photo; ZIP up to 200 MB; photos are resized to a standard size and compressed when saved.

### 12.7 Added tables and APIs

**New tables for photo matching**

| Table | Main columns |
| --- | --- |
| `photo_import_batches` | id, school\_id, uploaded\_by, original\_file\_id, selected\_class, selected\_section, total\_files, matched\_files, import\_status, created\_at |
| `photo_import_results` | id, batch\_id, file\_name, parsed\_class, parsed\_section, parsed\_roll\_number, student\_id, match\_status, message |

**Database rule to add:** roll number must be unique within one school, academic session, class and section. Add the academic session to the student record or the import so this check is possible.

**API changes**

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | /school/students/import-template?format=xlsx or csv | Download the template |
| POST | /school/students/import | Upload an Excel or CSV file |
| GET | /school/students/import/{batchId} | Check progress and the preview |
| POST | /school/students/import/{batchId}/confirm | Import the valid rows |
| GET | /school/students/import/{batchId}/errors | Download the error report |
| POST | /school/photos/bulk-upload | Upload the photo ZIP, with optional class and section |
| GET | /school/photos/bulk-upload/{batchId} | Match report |
| PUT | /school/students/{id}/photo | Assign or replace one photo by hand |

All of these use the school taken from the logged-in user, never from the request, as described in the security section.

---

## 13. Money design

### 13.1 Two separate views

| School sees | Agency sees |
| --- | --- |
| Selling price, quotation, discount, tax, advance, outstanding amount, invoice, refund | Agreed purchase rate, assignment amount, approved extra costs, reprint cost responsibility, settlement, outstanding payment |

Original agreed rates and all transactions are always kept.

### 13.2 Profit (gross contribution)

```text
School payment − Agency cost − Other direct costs = Gross contribution
```

| Item | Amount |
| --- | --: |
| School payment | ₹12,500 |
| Agency printing cost | ₹9,000 |
| Other direct costs | ₹500 |
| **Gross contribution** | **₹3,000** |

*Example only, not a real quote. Tax and overheads are handled separately.*

---

## 14. Notifications

Notifications are created from business events.

| Event | Who is told |
| --- | --- |
| Order submitted | Admin |
| Correction requested | School |
| Quotation generated | School |
| Quotation approved | Admin |
| Agency assigned | Agency |
| Agency accepts job | Admin |
| Production started | Admin, School |
| Production delayed | Admin |
| Quality failed | Admin |
| Dispatch completed | Admin, School |
| Payment received | Admin, School |
| Delivery confirmed | Admin, School |

**School notifications never mention the agency.**

**Events:** OrderSubmitted, OrderVerified, OrderCorrectionRequested, QuotationGenerated, QuotationApproved, AgencyJobAssigned, AgencyJobAccepted, ProductionStarted, ProductionUpdated, QualityCheckFailed, ReprintApproved, JobDispatched, DeliveryConfirmed, PaymentReceived, OrderClosed.

**Reliable delivery (transactional outbox):** when a business action is saved, the event is saved **in the same database transaction**. A background worker then sends the notification. This way no event is lost if something crashes.

---

## 15. Security

The system holds student data, photos and commercial information, so security is critical.

### 15.1 Login

- Spring Security with JWT access tokens and **refresh token rotation**.
- Passwords hashed with BCrypt or Argon2.
- Accounts can be activated or deactivated.
- Login rate limiting.
- Tokens can be revoked.

### 15.2 Two levels of permission

1. **Role check** – is this user a school, agency or admin?
2. **Record check** – does this specific record belong to this user?

| Role | Can access |
| --- | --- |
| SUPER\_ADMIN | All schools, agencies and orders |
| SCHOOL | Only its own school's records |
| PRINTING\_AGENCY | Only its own assigned jobs |

> **Never trust a `school_id` or `agency_id` sent from the browser.** Read the organization from the logged-in user and verify every record requested.

### 15.3 Keeping the agency hidden from the school

Enforced in the backend, not the frontend, through:

- separate response classes per role
- record-level checks
- safe notification content
- safe exports
- safe file access
- no agency details in any school tracking API

### 15.4 Files

- Private storage with **expiring signed download links**.
- Check file type by content (not just extension), and enforce size limits.
- Random file names.
- Student photos restricted to authorized users.
- Malware scanning where possible.
- Configurable retention and deletion.

### 15.5 Student data law

Follow Indian data protection rules, including the Digital Personal Data Protection Act. Use student data only for printing and school administration.

---

## 16. Error handling

Every error uses the same JSON shape:

```json
{
  "success": false,
  "errorCode": "ORDER_NOT_ACCESSIBLE",
  "message": "You do not have permission to access this order.",
  "timestamp": "2026-11-02T10:30:00Z"
}
```

| Code | Meaning |
| --- | --- |
| 200 | Success |
| 201 | Created |
| 400 | Validation failed |
| 401 | Not logged in |
| 403 | Not allowed |
| 404 | Not found |
| 409 | Business rule conflict |
| 422 | Invalid business action |
| 500 | Server error |

Never show stack traces, SQL errors or internal IDs to users.

---

## 17. Audit trail

We record **who did what, when, and what changed** for: school data changes, student edits, order submission, quotation changes, agency assignment and reassignment, production quantity changes, quality inspections, payments, invoices, delivery confirmation, and permission changes.

Never log passwords or tokens.

---

## 18. Reports

| Role | Reports |
| --- | --- |
| **Admin** | schools, agencies, new requests, orders in review / production / completed / delayed, agency production and rejection rates, revenue, printing costs, gross contribution, outstanding payments, delivery performance |
| **School** | student records, order history, total cards ordered, delivered quantity, payment history, outstanding balance, replacement requests, invoices |
| **Agency** | assigned / accepted jobs, pending production, completed and rejected quantity, delays, settlement history |

Reports follow the same access rules as the APIs.

---

## 19. Testing

| Type | What we test |
| --- | --- |
| **Unit** | OrderService, QuotationService, AssignmentService, ProductionService, DeliveryService, PaymentService |
| **Integration** | Controller↔service, service↔repository, order↔assignment, finance↔order, notification↔events |
| **Workflow** | Full lifecycle: submission → verification → quotation → approval → assignment → production → quality → dispatch → delivery → closure. Also partial production, many agencies, reassignment, failed quality, reprints, partial payments, partial deliveries |

### 19.1 Must-pass security tests

1. School A cannot see School B's students.
2. A school cannot open another school's order.
3. Agency A cannot see Agency B's jobs.
4. An agency cannot see school quotation details.
5. A school cannot find out the agency's identity.
6. A school cannot change another school's order by editing IDs.
7. An agency cannot change another agency's assignment.
8. Unauthorized users cannot open private photos.
9. Exports follow the same rules as APIs.

---

## 20. Business rules (quick list)

1. A school cannot choose or contact its printing agency.
2. Only the Admin assigns printing jobs.
3. One parent order can have many agency assignments.
4. Assignment history is always kept.
5. An agency sees only its own assignments.
6. A school sees only its own data and orders.
7. Agency purchase costs are never shown to schools.
8. School selling prices and agency purchase prices are stored separately.
9. Parent order status reflects child jobs and deliveries.
10. Approved student data and print specs are snapshotted.
11. Every reprint needs a reason and an approval record.
12. Delivery supports partial quantities.
13. All money transactions are auditable.
14. Notifications never reveal agency information.
15. Closing an order requires production, quality, delivery and financial checks to be complete, as per the closure policy.

---

## 21. Development phases

| Phase | Work |
| --- | --- |
| 1 | Authentication, roles, organizations |
| 2 | School dashboard and student management |
| 3 | Excel import and photos |
| 4 | ID card template designer |
| 5 | Parent orders and quotations |
| 6 | Agency management and job assignment |
| 7 | Production and quality control |
| 8 | Dispatch and delivery |
| 9 | Finance, invoices, settlements |
| 10 | Notifications, reports, audit |
| 11 | Security and end-to-end testing |

Each phase needs its own acceptance criteria before the next one starts.

---

## 22. Decisions made and open questions

### Decided

| Area | Decision |
| --- | --- |
| Backend | **Java 17 + Spring Boot 3** |
| Frontend | React + JavaScript |
| Database | PostgreSQL |
| Architecture | Modular monolith |
| Login | JWT |
| Data isolation | Record-level authorization |
| Order model | Parent order + child agency assignments |
| Templates | Versioned JSON |
| Student import | Apache POI |
| PDF / QR | PDFBox / ZXing |
| Notifications | Events with transactional outbox |
| Delivery | Two configurable models |
| Deployment | will paln further&#32; |

### Still to decide (business questions)

- Do schools pay online, by bank transfer, or both?
- Is an advance payment needed before quotation approval?
- Who pays for rejected cards and reprints?
- Who does the final quality check in each delivery model – Admin or agency?
- Can schools request replacements after delivery?
- How long are student records and photos kept?
- Can one school account have several users with different permissions?

---

## 23. Summary

We are building a **modular monolith** with **Java 17 + Spring Boot 3**, **PostgreSQL**, and a **React (JavaScript)** frontend.

The most important design goal is a **strict wall** between what the school sees and the internal agency work. The school talks only to the Admin. Agencies receive private assignments and report progress through the platform. The **parent order** is the single source of truth for the school; **agency assignments** handle the real printing.

This wall protects confidentiality, keeps money records clean, supports many agencies, and makes the platform easy to grow later.

**End of documen****t**
