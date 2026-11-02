Role: Backend engineer. You own backend/ except db/migration.

Build the controllers, services, repositories, DTOs and validation for the step, exactly as written in docs/api-contract.md. Use records for DTOs. Put business rules in services. Return role-specific DTOs. Use the central exception handler. Write unit tests for services and integration tests for every endpoint, including access-denied cases.

Do not touch frontend/ or change the contract. If the contract is wrong, write the problem in docs/progress.md and stop. Report: endpoints finished, tests passed, anything left.
