# Project: School ID Card Ordering and Printing Platform
Full design: docs/design.md. API contract: docs/api-contract.md. Status: docs/progress.md.
Read the design sections relevant to your task before you write code.

## Stack (fixed)
Backend: Java 17, Spring Boot 3, Spring Security + JWT, Spring Data JPA, PostgreSQL, Flyway, OpenAPI, JUnit 5, Mockito.
Frontend: React + JavaScript (.jsx only, no TypeScript), Vite, Tailwind CSS, React Router, Axios, TanStack Query, Zustand.
Architecture: modular monolith. Controllers have no business logic. Services hold workflows. Repositories hold data access.

## Non-negotiable rules
1. The school must never see any agency data. Use separate response DTOs per role.
2. Never trust school_id or agency_id from a request. Take them from the logged-in user. Check access to every record.
3. Money uses NUMERIC(14,2) and is never overwritten. School price and agency cost stay separate.
4. Status changes only through named business actions, never a generic update.
5. No fake data, no mock responses, no TODO placeholders, no half-finished features.
6. Every list endpoint is paginated. Every foreign key and filter column has an index. No N+1 queries.
7. One error format: success, errorCode, message, timestamp. Never expose stack traces.
8. Validate all input on the backend with Jakarta Bean Validation.

## UI rules
Clean, modern, minimal, professional. Fully responsive: check 360, 768, 1280, 1920 px. Use only the shared components and design tokens. Consistent spacing and type scale. No decorative animation, no clutter, no oversized elements. Every screen has loading, empty and error states. Accessible: semantic HTML, labels, keyboard use, visible focus, good contrast.

## How to work
- Plan first. Do only the task you were given. Touch only the folders you own.
- Write tests with the code. Run build and tests before you say you are done.
- When done: update docs/progress.md with what was built, how it was verified, and what is next.
- If the design document is unclear, write the question in docs/progress.md and stop. Do not guess.
