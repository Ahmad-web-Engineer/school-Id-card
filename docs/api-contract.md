# API Contract

> **Notice:** This document is written and maintained by the Orchestrator subagent for each step. Backend and Frontend both depend on this contract.

## Overview
- **Base URL:** `/api/v1`
- **Authentication:** Bearer JWT in `Authorization` header (`Authorization: Bearer <token>`)
- **Standard Success Envelope:**
  ```json
  {
    "success": true,
    "data": {}
  }
  ```
- **Standard Paginated List Envelope:**
  ```json
  {
    "success": true,
    "data": [],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 100,
      "totalPages": 5,
      "last": false
    }
  }
  ```
- **Standard Error Envelope:**
  ```json
  {
    "success": false,
    "errorCode": "STRING_CODE",
    "message": "Human-readable description",
    "timestamp": "2026-11-02T10:30:00Z"
  }
  ```
- **Standard Error Codes:**
  - `400 BAD_REQUEST`: Validation failed (`VALIDATION_FAILED`), malformed request syntax (`INVALID_REQUEST_PAYLOAD`).
  - `401 UNAUTHORIZED`: Missing or invalid credentials (`UNAUTHORIZED`, `TOKEN_EXPIRED`, `INVALID_CREDENTIALS`).
  - `403 FORBIDDEN`: Insufficient role or access to specific record denied (`ACCESS_DENIED`, `RECORD_ACCESS_FORBIDDEN`).
  - `404 NOT_FOUND`: Resource not found (`RESOURCE_NOT_FOUND`).
  - `409 CONFLICT`: Duplicate key or state conflict (`RESOURCE_CONFLICT`).
  - `422 UNPROCESSABLE_ENTITY`: Invalid business action or status transition (`INVALID_ACTION`).
  - `500 INTERNAL_SERVER_ERROR`: Unhandled server condition (`INTERNAL_SERVER_ERROR`).

---

## Step 0 Endpoints: System & Health

### 1. Health Check
- **Path:** `/api/v1/health`
- **Method:** `GET`
- **Role / Access:** Public
- **Description:** Verifies service availability and application health status.
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "status": "UP",
      "service": "school-id-card-platform",
      "version": "1.0.0",
      "timestamp": "2026-11-02T10:30:00Z"
    }
  }
  ```

---

## Step 1 Endpoints: Auth, Roles & Organizations

### 1. User Login
- **Path:** `/api/v1/auth/login`
- **Method:** `POST`
- **Role / Access:** Public
- **Request Body:**
  ```json
  {
    "email": "admin@platform.com",
    "password": "Password123!"
  }
  ```
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "accessToken": "eyJhbGciOi...",
      "refreshToken": "d8f8a1e2...",
      "tokenType": "Bearer",
      "expiresIn": 86400,
      "user": {
        "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "fullName": "Admin User",
        "email": "admin@platform.com",
        "mobile": "9876543210",
        "role": "SUPER_ADMIN",
        "schoolId": null,
        "agencyId": null,
        "designation": "System Administrator",
        "status": "ACTIVE"
      }
    }
  }
  ```
- **Error Responses:**
  - `401 UNAUTHORIZED`: `INVALID_CREDENTIALS` ("Invalid email or password" or "Account is deactivated").
  - `400 BAD_REQUEST`: `VALIDATION_FAILED` (missing email or password).

### 2. Refresh Token
- **Path:** `/api/v1/auth/refresh`
- **Method:** `POST`
- **Role / Access:** Public
- **Request Body:**
  ```json
  {
    "refreshToken": "d8f8a1e2..."
  }
  ```
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "accessToken": "eyJhbGciOi...",
      "refreshToken": "new_refresh_token_uuid",
      "tokenType": "Bearer",
      "expiresIn": 86400
    }
  }
  ```
- **Error Responses:**
  - `401 UNAUTHORIZED`: `TOKEN_EXPIRED` or `UNAUTHORIZED` ("Refresh token is invalid or expired").

### 3. User Logout
- **Path:** `/api/v1/auth/logout`
- **Method:** `POST`
- **Role / Access:** Authenticated (`SUPER_ADMIN`, `SCHOOL`, `PRINTING_AGENCY`)
- **Request Body:**
  ```json
  {
    "refreshToken": "d8f8a1e2..."
  }
  ```
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "message": "Logged out successfully"
    }
  }
  ```

### 4. Current Authenticated User (`/auth/me`)
- **Path:** `/api/v1/auth/me`
- **Method:** `GET`
- **Role / Access:** Authenticated (`SUPER_ADMIN`, `SCHOOL`, `PRINTING_AGENCY`)
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "fullName": "School Principal",
      "email": "principal@springfield.edu",
      "mobile": "9876543211",
      "role": "SCHOOL",
      "schoolId": "a1b2c3d4-0000-0000-0000-000000000001",
      "agencyId": null,
      "designation": "Principal",
      "status": "ACTIVE"
    }
  }
  ```

### 5. Admin: List Schools
- **Path:** `/api/v1/admin/schools`
- **Method:** `GET`
- **Role / Access:** `SUPER_ADMIN`
- **Query Parameters:** `page` (default 0), `size` (default 20), `search` (optional string), `status` (optional: `ACTIVE`, `INACTIVE`)
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": [
      {
        "id": "a1b2c3d4-0000-0000-0000-000000000001",
        "schoolName": "Springfield High School",
        "schoolCode": "SCH-001",
        "address": "123 Education Lane",
        "city": "Springfield",
        "state": "IL",
        "pincode": "62701",
        "email": "info@springfield.edu",
        "mobile": "9876543200",
        "status": "ACTIVE",
        "createdAt": "2026-11-02T10:00:00Z"
      }
    ],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1,
      "last": true
    }
  }
  ```

### 6. Admin: Register School
- **Path:** `/api/v1/admin/schools`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Request Body:**
  ```json
  {
    "schoolName": "Oakridge Academy",
    "schoolCode": "SCH-002",
    "address": "456 Learning Way",
    "city": "Oakridge",
    "state": "CA",
    "pincode": "94016",
    "email": "contact@oakridge.edu",
    "mobile": "9876543201"
  }
  ```
- **Success Response (201 Created):**
  ```json
  {
    "success": true,
    "data": {
      "id": "b2c3d4e5-0000-0000-0000-000000000002",
      "schoolName": "Oakridge Academy",
      "schoolCode": "SCH-002",
      "address": "456 Learning Way",
      "city": "Oakridge",
      "state": "CA",
      "pincode": "94016",
      "email": "contact@oakridge.edu",
      "mobile": "9876543201",
      "status": "ACTIVE",
      "createdAt": "2026-11-02T10:30:00Z"
    }
  }
  ```
- **Error Responses:**
  - `409 CONFLICT`: `RESOURCE_CONFLICT` ("School with code SCH-002 already exists" or "School email already in use").

### 7. Admin: Get School by ID
- **Path:** `/api/v1/admin/schools/{id}`
- **Method:** `GET`
- **Role / Access:** `SUPER_ADMIN`
- **Success Response (200 OK):** School details object with assigned user accounts.
- **Error Responses:** `404 NOT_FOUND` (`RESOURCE_NOT_FOUND`).

### 8. Admin: Update School
- **Path:** `/api/v1/admin/schools/{id}`
- **Method:** `PUT`
- **Role / Access:** `SUPER_ADMIN`
- **Request Body:**
  ```json
  {
    "schoolName": "Oakridge Academy Updated",
    "address": "456 Learning Way Suite 2",
    "city": "Oakridge",
    "state": "CA",
    "pincode": "94016",
    "email": "contact@oakridge.edu",
    "mobile": "9876543201",
    "status": "ACTIVE"
  }
  ```

### 9. Admin: Create School User
- **Path:** `/api/v1/admin/schools/{id}/users`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Request Body:**
  ```json
  {
    "fullName": "Jane Doe",
    "email": "jane.doe@springfield.edu",
    "mobile": "9876543222",
    "password": "Password123!",
    "designation": "Vice Principal"
  }
  ```
- **Success Response (201 Created):** User response object with `role: "SCHOOL"`, `schoolId: "{id}"`.

### 10. Admin: List Agencies
- **Path:** `/api/v1/admin/agencies`
- **Method:** `GET`
- **Role / Access:** `SUPER_ADMIN`
- **Query Parameters:** `page`, `size`, `search`, `status`
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": [
      {
        "id": "c3d4e5f6-0000-0000-0000-000000000003",
        "agencyName": "Apex Print Solutions",
        "contactPerson": "Robert Smith",
        "email": "robert@apexprint.com",
        "mobile": "9876543300",
        "address": "789 Industrial Park",
        "productionCapacity": 5000,
        "capabilities": "PVC Thermal, UV Gloss, Barcode/QR",
        "status": "ACTIVE",
        "createdAt": "2026-11-02T10:00:00Z"
      }
    ],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1,
      "last": true
    }
  }
  ```

### 11. Admin: Register Agency
- **Path:** `/api/v1/admin/agencies`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Request Body:**
  ```json
  {
    "agencyName": "Apex Print Solutions",
    "contactPerson": "Robert Smith",
    "email": "robert@apexprint.com",
    "mobile": "9876543300",
    "address": "789 Industrial Park",
    "productionCapacity": 5000,
    "capabilities": "PVC Thermal, UV Gloss, Barcode/QR"
  }
  ```
- **Success Response (201 Created):** Agency response object.

### 12. Admin: Get Agency by ID
- **Path:** `/api/v1/admin/agencies/{id}`
- **Method:** `GET`
- **Role / Access:** `SUPER_ADMIN`
- **Success Response (200 OK):** Agency details with assigned users.

### 13. Admin: Update Agency
- **Path:** `/api/v1/admin/agencies/{id}`
- **Method:** `PUT`
- **Role / Access:** `SUPER_ADMIN`
- **Request Body:**
  ```json
  {
    "agencyName": "Apex Print Solutions",
    "contactPerson": "Robert Smith",
    "email": "robert@apexprint.com",
    "mobile": "9876543300",
    "address": "789 Industrial Park",
    "productionCapacity": 6000,
    "capabilities": "PVC Thermal, UV Gloss, Smartcard RFID",
    "status": "ACTIVE"
  }
  ```

### 14. Admin: Create Agency User
- **Path:** `/api/v1/admin/agencies/{id}/users`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Request Body:**
  ```json
  {
    "fullName": "Mark Davis",
    "email": "mark@apexprint.com",
    "mobile": "9876543311",
    "password": "Password123!",
    "designation": "Production Supervisor"
  }
  ```
- **Success Response (201 Created):** User response object with `role: "PRINTING_AGENCY"`, `agencyId: "{id}"`.

### 15. School: Get Own School Profile
- **Path:** `/api/v1/school/profile`
- **Method:** `GET`
- **Role / Access:** `SCHOOL`
- **Access Rule:** Uses `school_id` from the authenticated user token.
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "id": "a1b2c3d4-0000-0000-0000-000000000001",
      "schoolName": "Springfield High School",
      "schoolCode": "SCH-001",
      "address": "123 Education Lane",
      "city": "Springfield",
      "state": "IL",
      "pincode": "62701",
      "email": "info@springfield.edu",
      "mobile": "9876543200",
      "status": "ACTIVE"
    }
  }
  ```

### 16. Agency: Get Own Agency Profile
- **Path:** `/api/v1/agency/profile`
- **Method:** `GET`
- **Role / Access:** `PRINTING_AGENCY`
- **Access Rule:** Uses `agency_id` from the authenticated user token.
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "id": "c3d4e5f6-0000-0000-0000-000000000003",
      "agencyName": "Apex Print Solutions",
      "contactPerson": "Robert Smith",
      "email": "robert@apexprint.com",
      "mobile": "9876543300",
      "address": "789 Industrial Park",
      "productionCapacity": 5000,
      "capabilities": "PVC Thermal, UV Gloss, Barcode/QR",
      "status": "ACTIVE"
    }
  }
  ```


---

## Step 2 Endpoints: Student Management

### 1. School: List Students (Paginated & Filterable)
- **Path:** `/api/v1/school/students`
- **Method:** `GET`
- **Role / Access:** `SCHOOL`
- **Access Rule:** Uses `school_id` from the logged-in user's JWT token.
- **Query Parameters:**
  - `page` (integer, default `0`)
  - `size` (integer, default `20`)
  - `search` (optional string, matches student name or admission number)
  - `className` (optional string, e.g. "10th")
  - `section` (optional string, e.g. "A")
  - `status` (optional string: `ACTIVE`, `INACTIVE`)
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": [
      {
        "id": "e1f2a3b4-5678-90ab-cdef-1234567890ab",
        "schoolId": "a1b2c3d4-0000-0000-0000-000000000001",
        "admissionNumber": "ADM-2026-001",
        "studentName": "Aarav Sharma",
        "fatherName": "Rajesh Sharma",
        "motherName": "Pooja Sharma",
        "dateOfBirth": "2010-05-15",
        "gender": "MALE",
        "bloodGroup": "B+",
        "className": "10th",
        "section": "A",
        "rollNumber": "12",
        "address": "42 Lake View Apartments",
        "mobile": "9876543210",
        "emergencyContact": "9876543219",
        "photoFileId": null,
        "status": "ACTIVE",
        "createdAt": "2026-11-02T10:00:00Z"
      }
    ],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1,
      "last": true
    }
  }
  ```

### 2. School: Create Student
- **Path:** `/api/v1/school/students`
- **Method:** `POST`
- **Role / Access:** `SCHOOL`
- **Access Rule:** `school_id` is assigned strictly from the authenticated token.
- **Request Body:**
  ```json
  {
    "admissionNumber": "ADM-2026-001",
    "studentName": "Aarav Sharma",
    "fatherName": "Rajesh Sharma",
    "motherName": "Pooja Sharma",
    "dateOfBirth": "2010-05-15",
    "gender": "MALE",
    "bloodGroup": "B+",
    "className": "10th",
    "section": "A",
    "rollNumber": "12",
    "address": "42 Lake View Apartments",
    "mobile": "9876543210",
    "emergencyContact": "9876543219",
    "photoFileId": null,
    "status": "ACTIVE"
  }
  ```
- **Success Response (201 Created):** `StudentResponse` object.
- **Error Responses:**
  - `409 CONFLICT`: `RESOURCE_CONFLICT` ("Admission number ADM-2026-001 already exists in this school").
  - `400 BAD_REQUEST`: `VALIDATION_FAILED` (missing required fields).

### 3. School: Get Student Details
- **Path:** `/api/v1/school/students/{id}`
- **Method:** `GET`
- **Role / Access:** `SCHOOL`
- **Access Rule:** Checks that the student's `school_id` matches the logged-in user's school. If mismatched, returns 404 (does not disclose existence).
- **Success Response (200 OK):** `StudentResponse` object.
- **Error Responses:** `404 NOT_FOUND` (`RESOURCE_NOT_FOUND`).

### 4. School: Update Student Details
- **Path:** `/api/v1/school/students/{id}`
- **Method:** `PUT`
- **Role / Access:** `SCHOOL`
- **Access Rule:** Verifies student belongs to the logged-in user's school.
- **Request Body:** `StudentRequest`
- **Success Response (200 OK):** Updated `StudentResponse` object.
- **Error Responses:**
  - `404 NOT_FOUND`: `RESOURCE_NOT_FOUND`.
  - `409 CONFLICT`: `RESOURCE_CONFLICT` (if changing to an admission number that already exists in this school).

### 5. School: Deactivate / Delete Student
- **Path:** `/api/v1/school/students/{id}`
- **Method:** `DELETE`
- **Role / Access:** `SCHOOL`
- **Access Rule:** Verifies student belongs to the logged-in user's school.
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "message": "Student record deactivated successfully"
    }
  }
  ```

### 6. Admin: List Students for a School
- **Path:** `/api/v1/admin/schools/{schoolId}/students`
- **Method:** `GET`
- **Role / Access:** `SUPER_ADMIN`
- **Query Parameters:** `page`, `size`, `search`, `className`, `section`, `status`
- **Success Response (200 OK):** Paginated `PagedResponse<StudentResponse>`.

### 7. Admin: Get Student by ID
- **Path:** `/api/v1/admin/schools/{schoolId}/students/{id}`
- **Method:** `GET`
- **Role / Access:** `SUPER_ADMIN`
- **Success Response (200 OK):** `StudentResponse` object.

---

## Step 3 Endpoints: Excel, CSV Import & Photo Matching

### 1. School: Download Import Template
- **Path:** `/api/v1/school/students/import-template`
- **Method:** `GET`
- **Role / Access:** `SCHOOL`
- **Query Parameters:**
  - `format` (optional string, `xlsx` or `csv`, default `xlsx`)
- **Headers Returned:**
  - `Content-Type`: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` (for xlsx) or `text/csv; charset=UTF-8` (for csv)
  - `Content-Disposition`: `attachment; filename="student_import_template.[xlsx|csv]"`
- **Response Content:** Standard spreadsheet containing header columns (`Admission Number*`, `Student Name*`, `Class*`, `Section*`, `Roll Number*`, `Father's Name`, `Mother's Name`, `Date of Birth`, `Gender`, `Blood Group`, `Mobile`, `Emergency Contact`, `Address`) and 2 example rows.

### 2. School: Upload Student Import File (Excel / CSV)
- **Path:** `/api/v1/school/students/import`
- **Method:** `POST`
- **Role / Access:** `SCHOOL`
- **Access Rule:** Uses `school_id` from the logged-in user JWT session.
- **Request Format:** `multipart/form-data`
  - `file`: Multipart file (must be `.xlsx` or `.csv`, max 10MB)
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "batchId": "b1b2b3b4-1111-2222-3333-444455556666",
      "fileName": "students_class_10.xlsx",
      "fileType": "XLSX",
      "totalRows": 45,
      "validRows": 42,
      "errorRows": 3,
      "importStatus": "VALIDATED",
      "previewRows": [
        {
          "rowNumber": 2,
          "admissionNumber": "ADM-2026-001",
          "studentName": "Aarav Sharma",
          "className": "10th",
          "section": "A",
          "rollNumber": "1",
          "gender": "MALE",
          "bloodGroup": "B+",
          "mobile": "9876543210",
          "validationStatus": "VALID",
          "errorMessage": null
        },
        {
          "rowNumber": 3,
          "admissionNumber": "ADM-2026-002",
          "studentName": "",
          "className": "10th",
          "section": "A",
          "rollNumber": "1",
          "gender": "UNKNOWN",
          "bloodGroup": "Z+",
          "mobile": "123",
          "validationStatus": "ERROR",
          "errorMessage": "Student name is required; Roll number 1 is duplicate in Class 10th-A; Invalid gender; Invalid blood group"
        }
      ],
      "createdAt": "2026-11-02T10:30:00Z"
    }
  }
  ```
- **Error Responses:**
  - `400 BAD_REQUEST`: `INVALID_FILE_TYPE` ("Only .xlsx and .csv files are supported")
  - `400 BAD_REQUEST`: `EMPTY_FILE` ("Uploaded file is empty")
  - `400 BAD_REQUEST`: `MISSING_REQUIRED_COLUMNS` ("Required column missing: Admission Number")

### 3. School: Get Import Batch Progress & Details
- **Path:** `/api/v1/school/students/import/{batchId}`
- **Method:** `GET`
- **Role / Access:** `SCHOOL`
- **Access Rule:** Batch must belong to the logged-in user's `school_id`.
- **Query Parameters:**
  - `page` (optional integer, default `0`)
  - `size` (optional integer, default `20`)
  - `status` (optional string filter: `ALL`, `VALID`, `ERROR`)
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "batchId": "b1b2b3b4-1111-2222-3333-444455556666",
      "fileName": "students_class_10.xlsx",
      "fileType": "XLSX",
      "totalRows": 45,
      "validRows": 42,
      "errorRows": 3,
      "importedRows": 0,
      "importStatus": "VALIDATED",
      "createdAt": "2026-11-02T10:30:00Z",
      "completedAt": null,
      "rows": [
        {
          "id": "r1r2r3r4-0000-0000-0000-000000000001",
          "rowNumber": 2,
          "admissionNumber": "ADM-2026-001",
          "studentName": "Aarav Sharma",
          "className": "10th",
          "section": "A",
          "rollNumber": "1",
          "validationStatus": "VALID",
          "errorMessage": null
        }
      ]
    },
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 45,
      "totalPages": 3,
      "last": false
    }
  }
  ```
- **Error Responses:** `404 NOT_FOUND`: `RESOURCE_NOT_FOUND` ("Import batch not found").

### 4. School: Confirm and Execute Bulk Student Import
- **Path:** `/api/v1/school/students/import/{batchId}/confirm`
- **Method:** `POST`
- **Role / Access:** `SCHOOL`
- **Access Rule:** Strictly checks that the batch belongs to the school of the logged-in user.
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "batchId": "b1b2b3b4-1111-2222-3333-444455556666",
      "totalRows": 45,
      "importedCount": 42,
      "errorCount": 3,
      "importStatus": "COMPLETED_WITH_ERRORS",
      "message": "Successfully imported 42 students. 3 rows were skipped due to errors."
    }
  }
  ```
- **Error Responses:**
  - `404 NOT_FOUND`: `RESOURCE_NOT_FOUND`.
  - `400 BAD_REQUEST`: `INVALID_STATE` ("Batch is already imported or no valid rows available").

### 5. School: Download Import Errors Report
- **Path:** `/api/v1/school/students/import/{batchId}/errors`
- **Method:** `GET`
- **Role / Access:** `SCHOOL`
- **Headers Returned:**
  - `Content-Type`: `text/csv; charset=UTF-8`
  - `Content-Disposition`: `attachment; filename="import_errors_{batchId}.csv"`
- **Response Content:** CSV listing `Row Number`, `Admission Number`, `Student Name`, `Field Name`, `Error Description`.

### 6. School: Bulk Upload Student Photos (ZIP Archive)
- **Path:** `/api/v1/school/photos/bulk-upload`
- **Method:** `POST`
- **Role / Access:** `SCHOOL`
- **Access Rule:** Uses `school_id` from the logged-in user JWT session.
- **Request Format:** `multipart/form-data`
  - `file`: ZIP file archive (max 200MB)
  - `className`: (optional string, e.g. "10th")
  - `section`: (optional string, e.g. "A")
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "batchId": "p1p2p3p4-5555-6666-7777-888899990000",
      "totalFiles": 45,
      "matchedCount": 40,
      "unmatchedCount": 3,
      "duplicateCount": 2,
      "importStatus": "COMPLETED",
      "results": [
        {
          "fileName": "01.jpg",
          "parsedClass": "10th",
          "parsedSection": "A",
          "parsedRollNumber": "1",
          "studentId": "e1f2a3b4-5678-90ab-cdef-1234567890ab",
          "studentName": "Aarav Sharma",
          "admissionNumber": "ADM-2026-001",
          "matchStatus": "MATCHED",
          "message": "Matched by class 10th-A roll number 1"
        },
        {
          "fileName": "99.jpg",
          "parsedClass": "10th",
          "parsedSection": "A",
          "parsedRollNumber": "99",
          "studentId": null,
          "studentName": null,
          "admissionNumber": null,
          "matchStatus": "NO_STUDENT_FOUND",
          "message": "No student found with roll number 99 in Class 10th-A"
        }
      ]
    }
  }
  ```
- **Error Responses:**
  - `400 BAD_REQUEST`: `INVALID_FILE_TYPE` ("Only .zip files are supported").
  - `400 BAD_REQUEST`: `ZIP_EXTRACTION_FAILED` ("Corrupt archive or security traversal attempt").

### 7. School: Get Photo Import Match Report
- **Path:** `/api/v1/school/photos/bulk-upload/{batchId}`
- **Method:** `GET`
- **Role / Access:** `SCHOOL`
- **Access Rule:** Strictly checked against logged-in user's `school_id`.
- **Success Response (200 OK):** `PhotoImportBatchResponse` object.

### 8. School: Assign / Replace Single Student Photo
- **Path:** `/api/v1/school/students/{id}/photo`
- **Method:** `PUT`
- **Role / Access:** `SCHOOL`
- **Access Rule:** Verifies student belongs to the logged-in user's school.
- **Request Format:** `multipart/form-data`
  - `file`: Single image file (.jpg, .jpeg, .png, max 5MB)
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "studentId": "e1f2a3b4-5678-90ab-cdef-1234567890ab",
      "photoFileId": "f1f2f3f4-0000-0000-0000-000000000001",
      "photoUrl": "/api/v1/files/f1f2f3f4-0000-0000-0000-000000000001",
      "message": "Photo uploaded and assigned successfully"
    }
  }
  ```
- **Error Responses:**
  - `404 NOT_FOUND`: `RESOURCE_NOT_FOUND`.
  - `400 BAD_REQUEST`: `INVALID_IMAGE` ("Unsupported image format or corrupt file").

### 9. Common: View / Download Stored File
- **Path:** `/api/v1/files/{fileId}`
- **Method:** `GET`
- **Role / Access:** `SCHOOL`, `SUPER_ADMIN`, `PRINTING_AGENCY`
- **Access Rule:**
  - Super Admin can view all files.
  - School can only view files belonging to its own institution.
  - Agency can only view files tied to jobs assigned to that agency.
- **Headers Returned:**
  - `Content-Type`: `image/jpeg` or file MIME type
  - `Cache-Control`: `private, max-age=86400`
- **Success Response (200 OK):** Binary file stream.
- **Error Responses:**
  - `404 NOT_FOUND`: `RESOURCE_NOT_FOUND`.
  - `403 FORBIDDEN`: `ACCESS_DENIED`.

---

## Step 4 Endpoints: ID Card Template Designer

### 1. Admin: List Templates
- **Path:** `/api/v1/admin/templates`
- **Method:** `GET`
- **Role / Access:** `SUPER_ADMIN`
- **Query Parameters:**
  - `page` (optional integer, default `0`)
  - `size` (optional integer, default `10`)
  - `search` (optional string, matches template name)
  - `category` (optional string, e.g. `STUDENT`, `STAFF`)
  - `status` (optional string, e.g. `ACTIVE`, `INACTIVE`)
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": [
      {
        "id": "t1t2t3t4-1111-2222-3333-444455556666",
        "templateName": "Classic Horizontal Student Card",
        "templateCategory": "STUDENT",
        "schoolId": null,
        "schoolName": "Global Platform Template",
        "status": "ACTIVE",
        "latestVersionNumber": 2,
        "activeVersionApprovalStatus": "APPROVED",
        "createdAt": "2026-11-02T10:00:00Z"
      }
    ],
    "pagination": {
      "page": 0,
      "size": 10,
      "totalElements": 1,
      "totalPages": 1,
      "last": true
    }
  }
  ```

### 2. Admin: Create Template Master
- **Path:** `/api/v1/admin/templates`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Request Body:**
  ```json
  {
    "templateName": "Classic Horizontal Student Card",
    "templateCategory": "STUDENT",
    "schoolId": null,
    "cardWidthMm": 85.6,
    "cardHeightMm": 54.0,
    "frontDesignJson": "{\"elements\":[{\"type\":\"text\",\"field\":\"student_name\",\"x\":10,\"y\":20,\"fontSize\":12}]}",
    "backDesignJson": "{\"elements\":[{\"type\":\"text\",\"field\":\"school_address\",\"x\":10,\"y\":15,\"fontSize\":9}]}"
  }
  ```
- **Success Response (201 Created):** `CardTemplateDetailResponse` containing template metadata and Version 1 in `DRAFT` status.
- **Error Responses:**
  - `400 BAD_REQUEST`: `VALIDATION_FAILED` (missing templateName or invalid dimensions).
  - `409 CONFLICT`: `RESOURCE_CONFLICT` (template with identical name already exists).

### 3. Admin: Get Template by ID with Version History
- **Path:** `/api/v1/admin/templates/{id}`
- **Method:** `GET`
- **Role / Access:** `SUPER_ADMIN`
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "id": "t1t2t3t4-1111-2222-3333-444455556666",
      "templateName": "Classic Horizontal Student Card",
      "templateCategory": "STUDENT",
      "schoolId": null,
      "status": "ACTIVE",
      "versions": [
        {
          "id": "v1v2v3v4-0000-0000-0000-000000000001",
          "versionNumber": 1,
          "cardWidthMm": 85.6,
          "cardHeightMm": 54.0,
          "bleedMarginMm": 3.0,
          "frontDesignJson": "{\"elements\":[]}",
          "backDesignJson": "{\"elements\":[]}",
          "approvalStatus": "APPROVED",
          "approvedAt": "2026-11-02T11:00:00Z"
        }
      ]
    }
  }
  ```
- **Error Responses:** `404 NOT_FOUND`: `RESOURCE_NOT_FOUND`.

### 4. Admin: Save New Template Version
- **Path:** `/api/v1/admin/templates/{id}/versions`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Request Body:**
  ```json
  {
    "cardWidthMm": 85.6,
    "cardHeightMm": 54.0,
    "bleedMarginMm": 3.0,
    "frontDesignJson": "{\"elements\":[]}",
    "backDesignJson": "{\"elements\":[]}"
  }
  ```
- **Success Response (201 Created):** `TemplateVersionResponse` with incremented `versionNumber` in `DRAFT` status.

### 5. Admin: Submit Version for Approval
- **Path:** `/api/v1/admin/templates/{id}/versions/{versionId}/submit`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Success Response (200 OK):** `TemplateVersionResponse` with status updated to `PENDING_APPROVAL`.
- **Error Responses:** `400 BAD_REQUEST`: `INVALID_ACTION` ("Only DRAFT versions can be submitted").

### 6. Admin: Approve and Lock Template Version
- **Path:** `/api/v1/admin/templates/{id}/versions/{versionId}/approve`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Success Response (200 OK):** `TemplateVersionResponse` with status `APPROVED`. Any previously `APPROVED` version moves to `ARCHIVED`.
- **Error Responses:** `400 BAD_REQUEST`: `INVALID_ACTION` ("Version is already approved or archived").

### 7. Admin: Render PDF Preview for Template Version
- **Path:** `/api/v1/admin/templates/{id}/versions/{versionId}/preview`
- **Method:** `GET`
- **Role / Access:** `SUPER_ADMIN`
- **Headers Returned:**
  - `Content-Type`: `application/pdf`
  - `Content-Disposition`: `inline; filename="template_preview_v{versionNumber}.pdf"`
- **Success Response (200 OK):** High-resolution PDF stream rendered with sample student data, front and back canvas pages, and ZXing barcode/QR codes.

### 8. Admin: Assign Template to School
- **Path:** `/api/v1/admin/templates/{id}/assign`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Request Body:**
  ```json
  {
    "schoolId": "a1b2c3d4-0000-0000-0000-000000000001",
    "academicSession": "2026-2027",
    "active": true
  }
  ```
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "assignmentId": "as1as2-0000-0000-0000-000000000001",
      "templateId": "t1t2t3t4-1111-2222-3333-444455556666",
      "schoolId": "a1b2c3d4-0000-0000-0000-000000000001",
      "academicSession": "2026-2027",
      "message": "Template assigned to school successfully"
    }
  }
  ```

### 9. School: List Assigned Templates
- **Path:** `/api/v1/school/templates`
- **Method:** `GET`
- **Role / Access:** `SCHOOL`
- **Access Rule:** Uses `school_id` from token. Returns templates assigned to this school (or global templates) that have an `APPROVED` version.
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": [
      {
        "id": "t1t2t3t4-1111-2222-3333-444455556666",
        "templateName": "Classic Horizontal Student Card",
        "templateCategory": "STUDENT",
        "approvedVersionId": "v1v2v3v4-0000-0000-0000-000000000001",
        "versionNumber": 1,
        "cardWidthMm": 85.6,
        "cardHeightMm": 54.0,
        "frontDesignJson": "{\"elements\":[]}",
        "backDesignJson": "{\"elements\":[]}"
      }
    ],
    "pagination": {
      "page": 0,
      "size": 10,
      "totalElements": 1,
      "totalPages": 1,
      "last": true
    }
  }
  ```

### 10. School: Preview Card with Real Enrolled Student
- **Path:** `/api/v1/school/templates/{id}/preview`
- **Method:** `GET`
- **Role / Access:** `SCHOOL`
- **Query Parameters:**
  - `studentId` (optional UUID, must belong to the school; defaults to first active student or sample data)
- **Headers Returned:**
  - `Content-Type`: `application/pdf`
  - `Content-Disposition`: `inline; filename="card_preview.pdf"`
- **Success Response (200 OK):** High-resolution PDF stream rendering the student's actual photo, name, admission number, class, section, QR code, and school details.
- **Error Responses:**
  - `404 NOT_FOUND`: `RESOURCE_NOT_FOUND` (if template not assigned to school or student not found).

---

## Step 5 Endpoints: Orders and Quotations

### 1. School: Create Draft Order
- **Path:** `/api/v1/school/orders`
- **Method:** `POST`
- **Role / Access:** `SCHOOL`
- **Access Rule:** Uses `school_id` from token. Template version must be `APPROVED` and assigned to school. Student IDs must belong to the school.
- **Request Body:**
  ```json
  {
    "templateVersionId": "v1v2v3v4-0000-0000-0000-000000000001",
    "academicSession": "2026-2027",
    "deliveryModel": "DIRECT_TO_SCHOOL",
    "deliveryAddress": "123 School Lane, City, State 500001",
    "contactPerson": "John Doe",
    "contactMobile": "9876543210",
    "remarks": "Please print urgent cards first",
    "studentIds": [
      "s1s2s3s4-0000-0000-0000-000000000001",
      "s1s2s3s4-0000-0000-0000-000000000002"
    ],
    "additionalItems": [
      {
        "productType": "LANYARD",
        "quantity": 2
      },
      {
        "productType": "CARD_HOLDER",
        "quantity": 2
      }
    ]
  }
  ```
- **Success Response (201 Created):** `SchoolOrderDetailResponse` with `DRAFT` status and snapshots created.
- **Error Responses:**
  - `400 BAD_REQUEST`: `VALIDATION_FAILED` (missing template, empty students).
  - `404 NOT_FOUND`: `RESOURCE_NOT_FOUND` (student or template version not found).

### 2. School: List Own Orders
- **Path:** `/api/v1/school/orders`
- **Method:** `GET`
- **Role / Access:** `SCHOOL`
- **Query Parameters:** `page`, `size`, `search`, `status`
- **Success Response (200 OK):** Paginated `SchoolOrderSummaryResponse`.

### 3. School: Get Order Details by ID
- **Path:** `/api/v1/school/orders/{id}`
- **Method:** `GET`
- **Role / Access:** `SCHOOL`
- **Access Rule:** Checks `order.school_id == caller.school_id`. No agency data in response.
- **Success Response (200 OK):** `SchoolOrderDetailResponse` with order items, snapshot students, and quotation (if generated).

### 4. School: Submit Order for Review
- **Path:** `/api/v1/school/orders/{id}/submit`
- **Method:** `POST`
- **Role / Access:** `SCHOOL`
- **Success Response (200 OK):** Status transitioned to `SUBMITTED`.
- **Error Responses:** `400 BAD_REQUEST`: `INVALID_ACTION` ("Only DRAFT or CORRECTION_REQUIRED orders can be submitted").

### 5. School: Approve Quotation
- **Path:** `/api/v1/school/orders/{id}/approve-quotation`
- **Method:** `POST`
- **Role / Access:** `SCHOOL`
- **Success Response (200 OK):** Quotation status updated to `APPROVED`, order status updated to `APPROVED`.
- **Error Responses:** `400 BAD_REQUEST`: `INVALID_ACTION` ("No pending quotation to approve").

### 6. School: Cancel Draft Order
- **Path:** `/api/v1/school/orders/{id}/cancel`
- **Method:** `POST`
- **Role / Access:** `SCHOOL`
- **Success Response (200 OK):** Status updated to `CANCELLED`.

### 7. Admin: List All Orders
- **Path:** `/api/v1/admin/orders`
- **Method:** `GET`
- **Role / Access:** `SUPER_ADMIN`
- **Query Parameters:** `page`, `size`, `schoolId`, `search`, `status`
- **Success Response (200 OK):** Paginated `AdminOrderSummaryResponse`.

### 8. Admin: Get Order Details by ID
- **Path:** `/api/v1/admin/orders/{id}`
- **Method:** `GET`
- **Role / Access:** `SUPER_ADMIN`
- **Success Response (200 OK):** `AdminOrderDetailResponse` with school info, snapshot students, order items, quotation details, and audit history.

### 9. Admin: Start Review
- **Path:** `/api/v1/admin/orders/{id}/start-review`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Success Response (200 OK):** Status updated to `UNDER_REVIEW`.

### 10. Admin: Verify Order
- **Path:** `/api/v1/admin/orders/{id}/verify`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Success Response (200 OK):** Status updated to `VERIFIED`.

### 11. Admin: Request Correction
- **Path:** `/api/v1/admin/orders/{id}/corrections`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Request Body:**
  ```json
  {
    "remarks": "Missing photos for 5 students in Class 10-A. Please re-upload photos."
  }
  ```
- **Success Response (200 OK):** Status updated to `CORRECTION_REQUIRED`.

### 12. Admin: Generate Quotation
- **Path:** `/api/v1/admin/orders/{id}/quotation`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Request Body:**
  ```json
  {
    "discount": 100.00,
    "taxRate": 18.00,
    "validDays": 15,
    "notes": "Standard PVC card printing with glossy finish",
    "items": [
      {
        "productDescription": "PVC ID Card Printing",
        "quantity": 100,
        "unitPrice": 45.00
      },
      {
        "productDescription": "Standard 16mm Lanyard",
        "quantity": 100,
        "unitPrice": 15.00
      }
    ]
  }
  ```
- **Success Response (201 Created):** `QuotationResponse` with subtotal, tax amount, and total amount calculated. Order status updated to `QUOTATION_GENERATED`.



---

## Step 6 Endpoints: Agency Management and Job Assignment

### 1. Admin: Assign Order to Agency (with quantity splitting)
- **Path:** `/api/v1/admin/orders/{id}/assignments`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Access Rule:** Order must be in `APPROVED` or `AWAITING_ASSIGNMENT` status. Split quantities must not exceed available order quantities.
- **Request Body:**
  ```json
  {
    "assignments": [
      {
        "agencyId": "a1a2a3a4-0000-0000-0000-000000000001",
        "expectedDeliveryDate": "2026-11-20",
        "agreedRate": 25.00,
        "notes": "Urgent print run",
        "jobItems": [
          {
            "orderItemId": "o1o2o3o4-0000-0000-0000-000000000001",
            "assignedQuantity": 300
          }
        ]
      }
    ]
  }
  ```
- **Success Response (201 Created):**
  ```json
  {
    "success": true,
    "data": [
      {
        "id": "as1as2as3-0000-0000-0000-000000000001",
        "assignmentNumber": "JOB-2026-0001",
        "orderId": "ord1ord2-0000-0000-0000-000000000001",
        "orderNumber": "ORD-2026-0001",
        "agencyId": "a1a2a3a4-0000-0000-0000-000000000001",
        "agencyName": "Metro Print Works",
        "assignedBy": "Super Admin",
        "assignedAt": "2026-11-02T10:30:00Z",
        "expectedDeliveryDate": "2026-11-20",
        "agreedRate": 25.00,
        "assignmentStatus": "ASSIGNED",
        "reassignedFromId": null,
        "notes": "Urgent print run",
        "totalAssignedQuantity": 300,
        "jobItems": [
          {
            "id": "ji1ji2ji3-0000-0000-0000-000000000001",
            "orderItemId": "o1o2o3o4-0000-0000-0000-000000000001",
            "productType": "PVC_ID_CARD",
            "assignedQuantity": 300,
            "printedQuantity": 0,
            "rejectedQuantity": 0,
            "acceptedQuantity": 0,
            "pendingQuantity": 300
          }
        ]
      }
    ],
    "message": "Order jobs successfully assigned to agencies.",
    "timestamp": "2026-11-02T10:30:00Z"
  }
  ```
- **Error Responses:**
  - `400 BAD_REQUEST`: `INVALID_ACTION` ("Only APPROVED orders can be assigned"), `VALIDATION_FAILED` ("Assigned quantity exceeds available order quantity").
  - `404 NOT_FOUND`: `RESOURCE_NOT_FOUND` ("Order or Agency not found").

### 2. Admin: List All Agency Assignments
- **Path:** `/api/v1/admin/assignments`
- **Method:** `GET`
- **Role / Access:** `SUPER_ADMIN`
- **Query Parameters:** `page`, `size`, `orderId`, `agencyId`, `status`, `search`
- **Success Response (200 OK):** Paginated `AdminAgencyAssignmentResponse` list.

### 3. Admin: Get Assignment Details by ID
- **Path:** `/api/v1/admin/assignments/{id}`
- **Method:** `GET`
- **Role / Access:** `SUPER_ADMIN`
- **Success Response (200 OK):** `AdminAgencyAssignmentDetailResponse` including agency details, agreed rate, total agreed amount, line items, and reassignment history.

### 4. Admin: Reassign Job
- **Path:** `/api/v1/admin/assignments/{id}/reassign`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Request Body:**
  ```json
  {
    "newAgencyId": "a2a3a4a5-0000-0000-0000-000000000002",
    "expectedDeliveryDate": "2026-11-25",
    "agreedRate": 26.50,
    "reassignmentReason": "Original agency rejected due to machine maintenance"
  }
  ```
- **Success Response (200 OK):** `AdminAgencyAssignmentResponse` for newly created assignment. Old assignment marked as `REASSIGNED`.
- **Error Responses:**
  - `400 BAD_REQUEST`: `INVALID_ACTION` ("Only REJECTED or pending ASSIGNED jobs can be reassigned").

### 5. Agency: List Own Assigned Jobs
- **Path:** `/api/v1/agency/jobs`
- **Method:** `GET`
- **Role / Access:** `PRINTING_AGENCY`
- **Access Rule:** Resolves `agency_id` strictly from JWT session. Hidden: school pricing, platform profit, client customer identity.
- **Query Parameters:** `page`, `size`, `status`, `search`
- **Success Response (200 OK):** Paginated `AgencyJobSummaryResponse`:
  ```json
  {
    "success": true,
    "data": {
      "content": [
        {
          "id": "as1as2as3-0000-0000-0000-000000000001",
          "jobNumber": "JOB-2026-0001",
          "academicSession": "2026-2027",
          "totalQuantity": 300,
          "expectedDeliveryDate": "2026-11-20",
          "assignmentStatus": "ASSIGNED",
          "assignedAt": "2026-11-02T10:30:00Z"
        }
      ],
      "pageNumber": 0,
      "pageSize": 10,
      "totalElements": 1,
      "totalPages": 1,
      "first": true,
      "last": true
    },
    "message": "Agency jobs retrieved successfully.",
    "timestamp": "2026-11-02T10:30:00Z"
  }
  ```

### 6. Agency: Get Job Details
- **Path:** `/api/v1/agency/jobs/{id}`
- **Method:** `GET`
- **Role / Access:** `PRINTING_AGENCY`
- **Access Rule:** Checks `assignment.agency_id == caller.agency_id`. Returns print specs (card dimensions, bleed, front/back JSON or render specs), job items, quantities, and expected delivery date.
- **Success Response (200 OK):** `AgencyJobDetailResponse`.
- **Error Responses:**
  - `404 NOT_FOUND`: `RESOURCE_NOT_FOUND` (if job doesn't exist or belongs to another agency).

### 7. Agency: Accept Assigned Job
- **Path:** `/api/v1/agency/jobs/{id}/accept`
- **Method:** `POST`
- **Role / Access:** `PRINTING_AGENCY`
- **Access Rule:** Checks `assignment.agency_id == caller.agency_id`. Transitions status to `AGENCY_ACCEPTED`.
- **Success Response (200 OK):** Updated `AgencyJobDetailResponse`.
- **Error Responses:**
  - `400 BAD_REQUEST`: `INVALID_ACTION` ("Only ASSIGNED jobs can be accepted").

### 8. Agency: Reject Assigned Job
- **Path:** `/api/v1/agency/jobs/{id}/reject`
- **Method:** `POST`
- **Role / Access:** `PRINTING_AGENCY`
- **Access Rule:** Checks `assignment.agency_id == caller.agency_id`. Transitions status to `REJECTED`.
- **Request Body:**
  ```json
  {
    "rejectionReason": "Insufficient machine capacity during the delivery window"
  }
  ```
- **Success Response (200 OK):** Updated `AgencyJobDetailResponse`.
- **Error Responses:**
  - `400 BAD_REQUEST`: `INVALID_ACTION` ("Only ASSIGNED jobs can be rejected").

---

## 7. Step 7: Production and Quality Control APIs

### 1. Agency: Start Production
- **Path:** `/api/v1/agency/jobs/{id}/start`
- **Method:** `POST`
- **Role / Access:** `PRINTING_AGENCY`
- **Access Rule:** Checks `assignment.agency_id == caller.agency_id`. Assignment must be in `AGENCY_ACCEPTED` status.
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "assignmentId": "as1as2as3-0000-0000-0000-000000000001",
      "jobNumber": "JOB-2026-0001",
      "assignmentStatus": "IN_PRODUCTION",
      "message": "Production started successfully"
    },
    "timestamp": "2026-11-02T11:00:00Z"
  }
  ```
- **Error Responses:**
  - `400 BAD_REQUEST`: `INVALID_ACTION` ("Only AGENCY_ACCEPTED jobs can start production").

### 2. Agency: Create Production Batch
- **Path:** `/api/v1/agency/jobs/{id}/batches`
- **Method:** `POST`
- **Role / Access:** `PRINTING_AGENCY`
- **Access Rule:** Checks `assignment.agency_id == caller.agency_id`. Assignment must be in `IN_PRODUCTION`.
- **Request Body:**
  ```json
  {
    "plannedQuantity": 150,
    "notes": "Class 10th - Sections A and B"
  }
  ```
- **Success Response (201 CREATED):**
  ```json
  {
    "success": true,
    "data": {
      "id": "pb1pb2pb3-0000-0000-0000-000000000001",
      "assignmentId": "as1as2as3-0000-0000-0000-000000000001",
      "batchNumber": "BATCH-2026-0001",
      "plannedQuantity": 150,
      "actualQuantity": 0,
      "productionStatus": "IN_PROGRESS",
      "startedAt": "2026-11-02T11:05:00Z",
      "completedAt": null,
      "notes": "Class 10th - Sections A and B"
    },
    "timestamp": "2026-11-02T11:05:00Z"
  }
  ```

### 3. Agency: List Batches for Job
- **Path:** `/api/v1/agency/jobs/{id}/batches`
- **Method:** `GET`
- **Role / Access:** `PRINTING_AGENCY`
- **Access Rule:** Checks `assignment.agency_id == caller.agency_id`. Returns paginated batches.
- **Success Response (200 OK):** Paged list of `AgencyProductionBatchResponse`.

### 4. Agency: Post Production Update
- **Path:** `/api/v1/agency/batches/{batchId}/updates`
- **Method:** `POST`
- **Role / Access:** `PRINTING_AGENCY`
- **Access Rule:** Checks batch's assignment belongs to caller's agency.
- **Request Body:**
  ```json
  {
    "printedQuantity": 150,
    "rejectedQuantity": 5,
    "remarks": "5 cards rejected due to ink streak defect",
    "evidenceFileId": "uploads/defects/streak-evidence.jpg"
  }
  ```
- **Success Response (200 OK):** Updated `AgencyProductionBatchResponse`.

### 5. Agency: Submit Quality Check
- **Path:** `/api/v1/agency/jobs/{id}/quality-checks`
- **Method:** `POST`
- **Role / Access:** `PRINTING_AGENCY`
- **Access Rule:** Checks `assignment.agency_id == caller.agency_id`.
- **Request Body:**
  ```json
  {
    "inspectedQuantity": 150,
    "acceptedQuantity": 145,
    "rejectedQuantity": 5,
    "defectCategory": "PRINT_ALIGNMENT",
    "defectDescription": "5 cards misaligned by >2mm along vertical edge",
    "notes": "Need replacement print"
  }
  ```
- **Success Response (201 CREATED):** `QualityCheckResponse`. If `rejectedQuantity > 0`, automated reprint request is generated.

### 6. Agency: List Quality Checks for Job
- **Path:** `/api/v1/agency/jobs/{id}/quality-checks`
- **Method:** `GET`
- **Role / Access:** `PRINTING_AGENCY`
- **Success Response (200 OK):** Paged list of `QualityCheckResponse`.

### 7. Admin: List All Production Batches
- **Path:** `/api/v1/admin/production/batches`
- **Method:** `GET`
- **Role / Access:** `SUPER_ADMIN`
- **Query Params:** `assignmentId`, `agencyId`, `status`, `page`, `size`
- **Success Response (200 OK):** Paginated `AdminProductionBatchResponse`.

### 8. Admin: List All Quality Checks
- **Path:** `/api/v1/admin/production/quality-checks`
- **Method:** `GET`
- **Role / Access:** `SUPER_ADMIN`
- **Query Params:** `assignmentId`, `status`, `page`, `size`
- **Success Response (200 OK):** Paginated `AdminQualityCheckResponse`.

### 9. Admin: Perform Inspection (Model B Delivery)
- **Path:** `/api/v1/admin/assignments/{id}/quality-checks`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Request Body:** Same as Submit Quality Check.
- **Success Response (201 CREATED):** `AdminQualityCheckResponse`.

### 10. Admin: List Reprint Requests
- **Path:** `/api/v1/admin/production/reprints`
- **Method:** `GET`
- **Role / Access:** `SUPER_ADMIN`
- **Query Params:** `assignmentId`, `status`, `page`, `size`
- **Success Response (200 OK):** Paginated `AdminReprintRequestResponse`.

### 11. Admin: Approve Reprint Request
- **Path:** `/api/v1/admin/reprints/{id}/approve`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Request Body:**
  ```json
  {
    "costBearer": "AGENCY",
    "notes": "Approved for agency reprint due to printer misalignment defect"
  }
  ```
- **Success Response (200 OK):** `AdminReprintRequestResponse` with status `APPROVED`.

### 12. Admin: Reject Reprint Request
- **Path:** `/api/v1/admin/reprints/{id}/reject`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Request Body:**
  ```json
  {
    "rejectionReason": "Card defect within acceptable 1mm tolerance margin"
  }
  ```
- **Success Response (200 OK):** `AdminReprintRequestResponse` with status `REJECTED`.

### 13. School: Order Production Tracking
- **Path:** `/api/v1/school/orders/{id}/tracking`
- **Method:** `GET`
- **Role / Access:** `SCHOOL`
- **Access Rule:** Checks `order.school_id == caller.school_id`.
- **Rule 1 Guarantee:** Returns overall cards printed count, quality inspected count, and percentage completion. Zero agency name, cost, or assignment split is leaked.
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "orderId": "ord1ord2-0000-0000-0000-000000000001",
      "orderNumber": "ORD-2026-0001",
      "orderStatus": "IN_PRODUCTION",
      "totalCards": 300,
      "cardsPrinted": 150,
      "cardsVerified": 145,
      "percentComplete": 50.0,
      "currentStage": "PRINTING",
      "stageDescription": "Your ID cards are actively in production at our certified printing facility."
    },
    "timestamp": "2026-11-02T11:15:00Z"
  }
  ```

---

## Step 8 Endpoints: Dispatch and Delivery (Model A & Model B)

### 1. Agency: Create Dispatch Record
- **Path:** `/api/v1/agency/jobs/{id}/dispatch`
- **Method:** `POST`
- **Role / Access:** `PRINTING_AGENCY`
- **Access Rule:** Checks `assignment.agency_id == caller.agency_id`. Verifies assignment is in `AGENCY_ACCEPTED`, `IN_PRODUCTION`, or `PRINTING_COMPLETED`.
- **Request Body:**
  ```json
  {
    "courierName": "BlueDart Express",
    "trackingNumber": "BLUD-884920491",
    "dispatchQuantity": 300,
    "dispatchNotes": "Packed in 3 sealed boxes with bubble wrap protection"
  }
  ```
- **Success Response (201 Created):**
  ```json
  {
    "success": true,
    "data": {
      "id": "dsp1-0000-0000-0000-000000000001",
      "assignmentId": "asgn-0000-0000-0000-000000000001",
      "jobNumber": "JOB-2026-0001",
      "dispatchNumber": "DSP-2026-0001",
      "courierName": "BlueDart Express",
      "trackingNumber": "BLUD-884920491",
      "dispatchQuantity": 300,
      "dispatchStatus": "IN_TRANSIT",
      "dispatchDate": "2026-11-02T12:00:00Z",
      "dispatchNotes": "Packed in 3 sealed boxes with bubble wrap protection"
    }
  }
  ```

### 2. Agency: List Own Dispatches
- **Path:** `/api/v1/agency/dispatches`
- **Method:** `GET`
- **Role / Access:** `PRINTING_AGENCY`
- **Query Params:** `page` (default 0), `size` (default 10), `status` (optional)
- **Access Rule:** Scoped strictly to authenticated `caller.agency_id`.

### 3. Agency: List Dispatches for a Specific Job
- **Path:** `/api/v1/agency/jobs/{id}/dispatches`
- **Method:** `GET`
- **Role / Access:** `PRINTING_AGENCY`

### 4. Admin: List All Global Dispatches
- **Path:** `/api/v1/admin/delivery/dispatches`
- **Method:** `GET`
- **Role / Access:** `SUPER_ADMIN`
- **Query Params:** `page`, `size`, `agencyId`, `status`, `deliveryModel`
- **Success Response (200 OK):** `PagedResponse<AdminDispatchResponse>` containing full agency and school information.

### 5. Admin: Receive Consignment at Central Hub (Model B)
- **Path:** `/api/v1/admin/delivery/dispatches/{id}/receive-hub`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Request Body:**
  ```json
  {
    "receivedQuantity": 300,
    "conditionStatus": "EXCELLENT",
    "hubNotes": "All 3 boxes received sealed. Quality audit passed at hub."
  }
  ```
- **Success Response (200 OK):** `AdminDispatchResponse` with `dispatchStatus` = `DELIVERED_TO_HUB`.

### 6. Admin: Dispatch From Hub to School (Model B Secondary Dispatch)
- **Path:** `/api/v1/admin/orders/{id}/dispatch-to-school`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Request Body:**
  ```json
  {
    "courierName": "Delhivery Secure",
    "trackingNumber": "DELH-9923184",
    "dispatchQuantity": 300,
    "notes": "Secondary dispatch from Central Hub to School premises"
  }
  ```
- **Success Response (201 Created):** `AdminDispatchResponse`.

### 7. School: View Order Deliveries & Dispatches
- **Path:** `/api/v1/school/orders/{id}/deliveries`
- **Method:** `GET`
- **Role / Access:** `SCHOOL`
- **Access Rule:** Checks `order.school_id == caller.school_id`.
- **Rule 1 Guarantee:** Returns courier name, tracking number, quantity dispatched, dispatch date, and delivery receipts. ZERO agency name, cost, or internal job number is returned.

### 8. School: Confirm Delivery Receipt
- **Path:** `/api/v1/school/orders/{id}/confirm-delivery`
- **Method:** `POST`
- **Role / Access:** `SCHOOL`
- **Access Rule:** Checks `order.school_id == caller.school_id`.
- **Request Body:**
  ```json
  {
    "receivedQuantity": 300,
    "receivedBy": "Principal Dr. Rajesh Sharma",
    "receivedMobile": "+91 98765 43210",
    "deliveryNotes": "Package inspected and all 300 student cards received in perfect condition."
  }
  ```
- **Success Response (201 Created):**
  ```json
  {
    "success": true,
    "data": {
      "id": "del1-0000-0000-0000-000000000001",
      "orderId": "ord1-0000-0000-0000-000000000001",
      "orderNumber": "ORD-2026-0001",
      "deliveryNumber": "DEL-2026-0001",
      "deliveryModel": "DIRECT_TO_SCHOOL",
      "deliveryQuantity": 300,
      "receivedBy": "Principal Dr. Rajesh Sharma",
      "receivedMobile": "+91 98765 43210",
      "deliveryStatus": "CONFIRMED",
      "deliveredAt": "2026-11-02T14:30:00Z",
      "deliveryNotes": "Package inspected and all 300 student cards received in perfect condition."
    }
  }
  ```

### 9. Admin: List All Delivery Records
- **Path:** `/api/v1/admin/delivery/records`
- **Method:** `GET`
- **Role / Access:** `SUPER_ADMIN`
- **Query Params:** `page`, `size`, `schoolId`, `status`

### 10. Admin: Close Delivery Record
- **Path:** `/api/v1/admin/delivery/records/{id}/close`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Request Body:**
  ```json
  {
    "closureNotes": "Verified receipt with school principal. Delivery cycle concluded."
  }
  ```
- **Success Response (200 OK):** `AdminDeliveryResponse` with status `CLOSED`.

---

## Step 9 Endpoints: Finance, Invoices, Payments & Settlements

### 1. School: Financial Summary & Ledger
- **Path:** `/api/v1/school/finance/summary`
- **Method:** `GET`
- **Role / Access:** `SCHOOL`
- **Access Rule:** Scoped strictly to authenticated `caller.school_id`.
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "totalInvoiced": 75000.00,
      "totalPaid": 50000.00,
      "outstandingBalance": 25000.00,
      "invoiceCount": 3,
      "paymentCount": 4
    }
  }
  ```

### 2. School: List Invoices
- **Path:** `/api/v1/school/finance/invoices`
- **Method:** `GET`
- **Role / Access:** `SCHOOL`
- **Query Params:** `page` (default 0), `size` (default 10), `status` (optional)
- **Success Response (200 OK):** `PagedResponse<SchoolInvoiceResponse>`. Zero agency data leaked.

### 3. School: Get Invoice Details
- **Path:** `/api/v1/school/finance/invoices/{id}`
- **Method:** `GET`
- **Role / Access:** `SCHOOL`
- **Access Rule:** Checks `invoice.school_id == caller.school_id`.

### 4. School: Record Payment
- **Path:** `/api/v1/school/orders/{id}/payments`
- **Method:** `POST`
- **Role / Access:** `SCHOOL`
- **Access Rule:** Checks `order.school_id == caller.school_id`.
- **Request Body:**
  ```json
  {
    "amount": 25000.00,
    "paymentMethod": "UPI",
    "transactionReference": "UPI/2026/89823104",
    "paymentNotes": "Advance 50% payment for Order ORD-2026-0001"
  }
  ```
- **Success Response (201 Created):** `SchoolPaymentResponse`.

### 5. Agency: Financial Summary
- **Path:** `/api/v1/agency/finance/summary`
- **Method:** `GET`
- **Role / Access:** `PRINTING_AGENCY`
- **Access Rule:** Scoped strictly to authenticated `caller.agency_id`.
- **Rule 1 Guarantee:** Returns only agency agreed payouts, deductions, and settled amounts. ZERO school selling prices or platform margins are exposed.
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "totalAgreedAmount": 60000.00,
      "totalDeductions": 1200.00,
      "totalPayableAmount": 58800.00,
      "totalPaidAmount": 40000.00,
      "outstandingPayable": 18800.00,
      "settlementCount": 5
    }
  }
  ```

### 6. Agency: List Settlements
- **Path:** `/api/v1/agency/finance/settlements`
- **Method:** `GET`
- **Role / Access:** `PRINTING_AGENCY`
- **Query Params:** `page`, `size`, `status`
- **Success Response (200 OK):** `PagedResponse<AgencySettlementResponse>`.

### 7. Admin: Global Financial Metrics (Platform P&L)
- **Path:** `/api/v1/admin/finance/summary`
- **Method:** `GET`
- **Role / Access:** `SUPER_ADMIN`
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "grossRevenue": 250000.00,
      "collectedRevenue": 190000.00,
      "outstandingReceivables": 60000.00,
      "agencyDirectCost": 120000.00,
      "agencySettledPayouts": 90000.00,
      "agencyPendingPayables": 30000.00,
      "grossContribution": 130000.00,
      "grossMarginPercentage": 52.00
    }
  }
  ```

### 8. Admin: Generate Order Invoice
- **Path:** `/api/v1/admin/orders/{id}/invoices`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Request Body:**
  ```json
  {
    "dueDate": "2026-11-20",
    "notes": "Official tax invoice with 18% GST"
  }
  ```
- **Success Response (201 Created):** `AdminInvoiceResponse`.

### 9. Admin: List Global Invoices
- **Path:** `/api/v1/admin/finance/invoices`
- **Method:** `GET`
- **Role / Access:** `SUPER_ADMIN`
- **Query Params:** `page`, `size`, `schoolId`, `status`

### 10. Admin: Record Manual Payment
- **Path:** `/api/v1/admin/orders/{id}/payments`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Request Body:**
  ```json
  {
    "amount": 25000.00,
    "paymentMethod": "BANK_TRANSFER",
    "transactionReference": "UTR9923847192",
    "paymentNotes": "Cheque cleared into HDFC main account"
  }
  ```
- **Success Response (201 Created):** `AdminPaymentResponse`.

### 11. Admin: Generate Agency Settlement
- **Path:** `/api/v1/admin/assignments/{id}/settlement`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Request Body:**
  ```json
  {
    "deductionAmount": 500.00,
    "penaltyAmount": 0.00,
    "notes": "Deduction for 10 defective cards reprinted under agency liability"
  }
  ```
- **Success Response (201 Created):** `AdminSettlementResponse`.

### 12. Admin: Pay Agency Settlement
- **Path:** `/api/v1/admin/finance/settlements/{id}/pay`
- **Method:** `POST`
- **Role / Access:** `SUPER_ADMIN`
- **Request Body:**
  ```json
  {
    "amount": 18800.00,
    "paymentReference": "BANK-NEFT-8839210",
    "notes": "Final payout via NEFT transfer"
  }
  ```
- **Success Response (200 OK):** `AdminSettlementResponse` with status `SETTLED`.


