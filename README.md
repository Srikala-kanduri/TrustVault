# TrustVault

**Secure document sharing with controlled access and an auditable activity history.**

TrustVault is a document-sharing application being developed for the Snovasys Fresher Hiring Challenge 2026. Its goal is to let users share documents securely through explicit permissions while maintaining a record of important activities.

## Project Status

TrustVault is under active development.

### Implemented so far

- Spring Boot backend foundation.
- React frontend with a login interface.
- Spring Security form-based authentication.
- User roles: `ADMIN`, `OWNER`, and `VIEWER`.
- BCrypt password hashing.
- Database-backed user accounts using JPA.
- Development admin initialization.
- CSRF protection and local frontend CORS configuration.
- Initial automated application-context test configuration.

### Planned features

- Document upload, download, and metadata management.
- Document ownership and per-document read/write permissions.
- Permission granting and revocation.
- Server-side authorization for every protected document operation.
- Search and filtering of documents.
- Audit events for document and permission activity.
- Protection against unauthorized audit record modification.
- Comprehensive authorization and security tests.
- Production database and deployment configuration.

Features listed as planned are not yet claimed to be implemented.

## Technology Stack

**Frontend**
- React
- Vite
- JavaScript
- CSS

**Backend**
- Java
- Spring Boot
- Spring Security
- Spring Data JPA
- Maven Wrapper

**Database**
- H2 for local development and tests
- PostgreSQL dependency included for planned deployment configuration

## Project Structure

```text
TrustVault/
├── .gitignore
├── README.md
├── PROMPTS.md
├── THREATS.md
├── trustvault/              # Spring Boot backend
│   ├── pom.xml
│   ├── mvnw
│   └── src/
└── trustvault-frontend/     # React frontend
    ├── package.json
    └── src/
```

## Prerequisites

Install the following before running the application:

- Java 21 or a compatible supported JDK
- Node.js and npm
- Git

The project includes the Maven Wrapper, so a separate global Maven installation should not be necessary.

## Running the Backend

Open a terminal in the backend directory:

```bash
cd trustvault
```

Configure the development admin password using an environment variable before starting the application. Use a strong password of at least 12 characters and never commit it to Git.

In Windows Command Prompt:

```bat
set TRUSTVAULT_DEV_ADMIN_PASSWORD=YOUR_LOCAL_PASSWORD
```

Run the tests:

```bat
mvnw.cmd test
```

Start the backend:

```bat
mvnw.cmd spring-boot:run
```

The health endpoint is available locally at:

```text
http://localhost:8080/api/health
```

## Running the Frontend

Open a second terminal in the project root:

```bash
cd trustvault-frontend
npm install
npm run dev
```

Open the local URL printed by Vite, normally:

```text
http://localhost:5173
```

The current frontend login integration is configured to communicate with the local backend.

## Security Documentation

- [`THREATS.md`](THREATS.md) — threat model, mitigations, and outstanding security work.
- [`PROMPTS.md`](PROMPTS.md) — AI-assisted development log.

## Testing

Run backend tests with:

```bash
cd trustvault
mvnw.cmd test
```

The initial application-context test has completed successfully in the development environment. Comprehensive tests for document authorization, permission revocation, and audit integrity remain to be implemented.

## Deployment

Production deployment instructions, database configuration, secret management, persistent file storage, and a live application URL will be added after those components are implemented and verified.

## Project Goal

The goal is to build a secure document-sharing system in which access is checked by the backend, permissions can be revoked, and important activities can be audited. Security features will be described as complete only after implementation and verification.
