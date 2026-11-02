# Antigravity Prompt Pack: School ID Card Platform

Nov 2, 2026 · @war

This pack replaces the long Base prompt with small files that Antigravity loads by itself, and adds a team of subagents so each step is built in parallel without agents overwriting each other.

## Why this setup works better

- **A short AGENTS.md instead of a long pasted prompt.** Antigravity has a token budget for rules and skills (your Customizations page shows 20,000), so rules must stay short. The long details live in files the agent reads only when needed.
- **One job per subagent.** Each subagent owns specific folders and cannot touch the rest, so two agents never edit the same file.
- **Shared files as memory.** Agents hand work to each other through `docs/api-contract.md` and `docs/progress.md`, not through chat.
- **Git worktree isolation for parallel work.** Antigravity can run parallel subagents in separate Git worktrees, so their changes only meet when you merge. Turn this on for every parallel run.

## 1. Files to create

```text
School id card/
├── AGENTS.md                  (rules, loaded automatically)
├── docs/
│   ├── design.md              (the technical design document)
│   ├── api-contract.md        (written by the Orchestrator, read by all)
│   └── progress.md            (what is done, what is next)
├── .agent/
│   ├── agents/                (subagent blueprints, section 3)
│   └── workflows/             (slash commands, section 5)
├── backend/
└── frontend/
```

If your version of Antigravity uses different folder names, keep the file contents and put them where your Customizations page shows them. If subagents cannot be saved as files, paste each blueprint as the first message of a new agent in the Agent Manager.

## 2. AGENTS.md (the rules file)

Save this as `AGENTS.md` in the project root. It is read by Antigravity and by other coding tools. Keep it under about 500 words.

```markdown
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
```

## 3. Subagent blueprints

Create one blueprint per subagent. Each is a short markdown file. The first line is the role, then the boundaries. Give each subagent only the tools it needs.

### Orchestrator

```markdown
Role: Orchestrator. You plan and coordinate. You do not write feature code.

For the step you are given:
1. Read AGENTS.md, the matching sections of docs/design.md, and docs/progress.md.
2. Write or update docs/api-contract.md for this step: every endpoint with method, path, role, request fields, response fields, and error codes. Backend and Frontend both depend on this file.
3. Split the work into tasks for Database, Backend, Frontend, Security and QA. Say which tasks can run in parallel and which must wait. Database comes first, Backend and Frontend run together after the contract exists, Security and QA come last.
4. List the exact folders each subagent owns for this step.
5. After the subagents finish, check their results against the design, resolve conflicts, and update docs/progress.md.

Output the plan as a numbered task list for me to approve before anything starts.
```

### Database

```text
Role: Database engineer. You own backend/src/main/resources/db/migration and the JPA entities.

Write Flyway migrations and entities for the step. Use UUID keys, foreign keys, TIMESTAMPTZ and NUMERIC(14,2). Add indexes for every foreign key and every filter column. Add unique constraints from the design document. Never edit an old migration; add a new one.

Do not touch controllers, services or the frontend. Report: tables created, indexes added, and how you tested the migration on an empty database.
```

### Backend

```text
Role: Backend engineer. You own backend/ except db/migration.

Build the controllers, services, repositories, DTOs and validation for the step, exactly as written in docs/api-contract.md. Use records for DTOs. Put business rules in services. Return role-specific DTOs. Use the central exception handler. Write unit tests for services and integration tests for every endpoint, including access-denied cases.

Do not touch frontend/ or change the contract. If the contract is wrong, write the problem in docs/progress.md and stop. Report: endpoints finished, tests passed, anything left.
```

### Frontend

```text
Role: Frontend engineer. You own frontend/.

Build the pages and features for the step from docs/api-contract.md. Use only the shared components and design tokens. Use TanStack Query for server data and show loading, empty and error states on every screen. Forms validate on the client and show the server error next to the field. Responsive at 360, 768, 1280 and 1920 px. Tables become stacked cards on mobile. Lazy load routes and images.

Do not touch backend/. Do not use mock data. If an endpoint is not ready, stop and report it. Test with React Testing Library. Report: pages finished, screen sizes checked, tests passed.
```

### Security

```text
Role: Security reviewer. You may read everything and write only tests and docs.

For the step, check every endpoint for role checks and record-level checks. Check every response DTO, notification, export and file URL for leaked agency data or cost data. Write automated tests for each gap: School A cannot read School B, Agency A cannot read Agency B, an agency never sees school price, a school never sees agency identity. Report each problem with the file and a suggested fix. Do not fix feature code yourself; send the problem to the owner subagent.
```

### QA

```text
Role: QA engineer. You may run everything and write only tests.

Run the full backend and frontend test suites and the build. Then run the real user flow for this step in the built-in browser, using real data through the real API, at 360, 768, 1280 and 1920 px. Check loading, empty and error states, keyboard use and focus. Report a table: check, result, and evidence (test name or screenshot). Anything that fails is sent back to the owner with exact steps to reproduce.
```

## 4. How to run any step

Paste this short prompt into the Agent Manager. Replace the number and the name. It works for every step because all the rules are already in AGENTS.md and the blueprints.

```text
Run Step [N]: [name] from the build plan.

1. Start the Orchestrator. Let it read docs/design.md (sections [list]) and write docs/api-contract.md and the task plan. Stop and show me the plan.
2. After I approve, run Database first. Then run Backend and Frontend in parallel, each in its own Git worktree.
3. When both finish, run Security and QA.
4. The Orchestrator merges the results, fixes conflicts, updates docs/progress.md and gives me a final report with test results.

Do not start the next step.
```

### Which subagents each step needs

| Step | Name | Database | Backend | Frontend | Run in parallel? |
| --- | --- | --- | --- | --- | --- |
| 0 | Setup and design system | No | Skeleton | Skeleton and components | Yes |
| 1 | Auth, roles, organizations | Yes | Yes | Yes | Backend and Frontend |
| 2 | Students | Yes | Yes | Yes | Backend and Frontend |
| 3 | Excel, CSV and photo matching | Yes | Yes | Yes | Backend and Frontend |
| 4 | Template designer | Yes | Yes | Yes (largest) | Backend and Frontend |
| 5 | Orders and quotations | Yes | Yes | Yes | Backend and Frontend |
| 6 | Agency assignment | Yes | Yes | Yes | Backend and Frontend |
| 7 | Production and quality | Yes | Yes | Yes | Backend and Frontend |
| 8 | Dispatch and delivery | Yes | Yes | Yes | Backend and Frontend |
| 9 | Finance | Yes | Yes | Yes | Backend and Frontend |
| 10 | Notifications, reports, audit | Yes | Yes | Yes | Backend and Frontend |
| 11 | Hardening and full testing | No | Fixes only | Fixes only | Security and QA lead |
| 12 | Release preparation | No | Config | Config | Single agent |

For the exact content of each step (what to build and the acceptance checks), use the matching step in the Build Prompts doc. Paste its middle section as the step description, and the short prompt above does the rest.

## 5. Workflows (slash commands)

Save each as a workflow so you can type the command in the agent chat.

### /next-step

```text
Read docs/progress.md. Tell me which step is finished and which step is next. Then show the matching step from the build plan and ask me to confirm before you start the Orchestrator for it.
```

### /review

```text
Review everything built in the current step as a strict senior reviewer.
1. Compare it with the matching design sections and list any gap.
2. Run the Security subagent and the QA subagent.
3. Check for fake data, placeholders, unused code and unhandled errors.
4. Fix every problem through the owner subagent, run the build and tests again, and show the results.
End with a short list of what changed.
```

### /bugfix

```text
Problem: [I describe what I did, what I expected, and what happened.]
Find the root cause first and explain it in two sentences. Fix it in the owning folder only, add a test that fails before and passes after, and run the full suite. Do not hide the error with a workaround. Update docs/progress.md.
```

### /merge

```text
Check every parallel worktree for this step. List the changed files per subagent, find any conflict, resolve it without losing either side's work, run the build and all tests on the merged result, and only then give me a commit message for this step.
```

## 6. Rules for parallel work

1. **Contract first.** Backend and Frontend never start before `docs/api-contract.md` is written and approved.
2. **One owner per folder.** If an agent needs a change outside its folder, it writes a request in `docs/progress.md` and the owner does it.
3. **Isolated worktrees.** Run parallel subagents in separate Git worktrees. Merge with `/merge`, never by hand-copying files.
4. **Small commits.** Commit once per finished step, with the step number in the message.
5. **Approve every plan.** Read the Orchestrator plan before any code is written. This is where mistakes are cheapest to fix.
6. **Keep rules short.** If the rules file grows past your token budget, Antigravity may cut large rules down. Move details into `docs/` and keep only pointers in AGENTS.md.
7. **Start small.** Run Step 0 with a single agent first to confirm that rules, blueprints and workflows load, then use the full team from Step 1.
