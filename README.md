# TrustVault

TrustVault is a secure document-sharing application. It provides per-document read/edit grants, private file storage, revocation, document search, and an HMAC-chained audit history.

## Current implementation

- `ADMIN` can inspect the verified global audit trail. Every signed-in account can upload documents; the uploader owns and manages that document. Access to other people’s documents follows explicit per-document grants, so an employee can own some documents and view or edit others. Admin status does not grant implicit document access.
- Every document read, download, update, replacement, deletion, grant, and revocation is authorized by the backend. Revocation is checked on each request, so later requests are denied immediately. A file already downloaded cannot be recalled.
- Search supports title, category, owner email, and a date range, over only the caller's accessible documents.
- Upload validation checks extension and file signature for PDF, PNG, and JPEG, with Spring multipart size limits (10 MB per file).
- Audit events include successful operations and relevant denied attempts. The HMAC chain is verified before audit views are returned. The key must remain secret and stable.
- Owner and viewer accounts appear in personal audit views, which contain only events recorded under the signed-in email.

## Requirements not yet satisfied

This repository does not currently include a deployed live URL, a five-minute demo video, or a completed submission write-up. The GitHub remote is configured, but public accessibility could not be verified from this environment. The Render deployment configuration is in [render.yaml](render.yaml); see the deployment instructions below. See [THREATS.md](THREATS.md) for known risks and [PROMPTS.md](PROMPTS.md) for AI usage notes.

## Run locally on Windows

Prerequisites: JDK 21+, Node.js/npm, and internet access the first time Maven downloads dependencies.

Open PowerShell in the repository root. The following retrieves the same Windows user-level key each run and creates one only if none exists. It validates the existing key and stops without replacing it if it is malformed or too short. Passwords are read as secure strings and are not echoed. Do not commit these values.

```powershell
$key = [Environment]::GetEnvironmentVariable('TRUSTVAULT_AUDIT_HMAC_KEY', 'User')
if (-not $key) {
  $bytes = [byte[]]::new(32)
  [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
  $key = [Convert]::ToBase64String($bytes)
  [Environment]::SetEnvironmentVariable('TRUSTVAULT_AUDIT_HMAC_KEY', $key, 'User')
}
try {
  $keyBytes = [Convert]::FromBase64String($key)
} catch {
  throw 'Saved TRUSTVAULT_AUDIT_HMAC_KEY is not valid Base64; it was not changed.'
}
if ($keyBytes.Length -lt 32) {
  [Array]::Clear($keyBytes, 0, $keyBytes.Length)
  throw 'Saved TRUSTVAULT_AUDIT_HMAC_KEY decodes to fewer than 32 bytes; it was not changed.'
}
[Array]::Clear($keyBytes, 0, $keyBytes.Length)
$env:TRUSTVAULT_AUDIT_HMAC_KEY = $key
$env:TRUSTVAULT_DEV_ADMIN_ENABLED = 'true'
$env:TRUSTVAULT_DEV_OWNER_ENABLED = 'true'
$env:TRUSTVAULT_DEV_VIEWER_ENABLED = 'true'
function Set-TrustVaultSecretFromPrompt([string]$Name, [string]$Prompt) {
  $secure = Read-Host $Prompt -AsSecureString
  if ($secure.Length -lt 12) { throw "$Name must be at least 12 characters." }
  $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
  try {
    $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    [Environment]::SetEnvironmentVariable($Name, $plain, 'Process')
  } finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    $secure.Dispose()
    Remove-Variable plain -ErrorAction SilentlyContinue
  }
}
Set-TrustVaultSecretFromPrompt 'TRUSTVAULT_DEV_ADMIN_PASSWORD' 'Local admin password (12+ characters)'
Set-TrustVaultSecretFromPrompt 'TRUSTVAULT_DEV_OWNER_PASSWORD' 'Local owner password (12+ characters)'
Set-TrustVaultSecretFromPrompt 'TRUSTVAULT_DEV_VIEWER_PASSWORD' 'Local viewer password (12+ characters)'
$env:SPRING_PROFILES_ACTIVE = 'dev'
```

The audit key must stay unchanged when reopening the app against the same database. With the default in-memory H2 database, audit data is lost on restart.

Start the backend in this terminal:

```powershell
cd .\trustvault
.\mvnw.cmd spring-boot:run
```

Start the frontend in a second PowerShell terminal from the repository root:

```powershell
cd .\trustvault-frontend
npm install
npm run dev
```

Open the Vite URL, normally `http://localhost:5173`. Development accounts are `admin@trustvault.local`, `owner@trustvault.local`, and `viewer@trustvault.local`; use the passwords you entered above. Health check: `http://localhost:8080/api/health`.

To run backend tests, open a terminal in `trustvault` and run:

```powershell
.\mvnw.cmd clean test
```

## Deploy on Render

The root `render.yaml` creates a free Docker web service that connects to your Neon PostgreSQL database and private Neon Object Storage bucket. The frontend is bundled into the Spring Boot app and uses the same origin for API calls and session cookies. Before creating the Render Blueprint, create a private bucket and a Neon Object Storage credential on the same branch as the database, with `storage:read` and `storage:write` scopes. Enter the pooled PostgreSQL connection URI (including `sslmode=require`), bucket endpoint/name, credential ID/secret, and signing region when Render prompts for the secrets. The credential ID is `TRUSTVAULT_STORAGE_S3_ACCESS_KEY`; its secret is `TRUSTVAULT_STORAGE_S3_SECRET_KEY`. Neon’s current free plan includes 1 GB of database storage and 5 GB of object storage.

1. Push the repository to GitHub and create a new Blueprint in the Render dashboard from that repository.
2. When Render prompts for `TRUSTVAULT_DATABASE_URL`, the five `TRUSTVAULT_STORAGE_S3_*` values, and the six role email/password values, enter the Neon values and unique role passwords of at least 12 characters. These are stored as Render secrets.
3. Let the first deploy finish. The production bootstrap creates those three accounts only if the database contains no users. Later deploys skip account creation. Keep the Neon database and the generated `TRUSTVAULT_AUDIT_HMAC_KEY`; the key must remain unchanged for the life of the audit history.
4. Open the service URL and sign in with the owner or viewer account. The admin account can view the global audit trail.

The service uses the `prod` profile, initializes and validates the PostgreSQL schema, disables H2 and development accounts, and marks session cookies secure. Uploaded documents are stored in the private Neon bucket; the SQL database keeps document metadata, grants, users, and audit events. The initial schema is in `trustvault/src/main/resources/database/schema.sql`; future schema changes should be delivered as explicit migrations before updating Hibernate validation expectations. Render's free service spins down after 15 minutes without inbound traffic and takes about a minute to wake on the next request.

## Database and API

The default profile uses in-memory H2 and local file storage for development. `database/schema.sql` documents the PostgreSQL schema for a persistent installation; `database/setup.sql` applies it with `psql`. Production starts with `SPRING_PROFILES_ACTIVE=prod`, initializes and validates the PostgreSQL schema, disables H2 and development account initializers, and requires a PostgreSQL URL, private object storage configuration, and stable audit key. The first-run production bootstrap is separately gated and creates the three configured role accounts only when the user table is empty. Never enable development account initializers in production. The Render setup bundles the frontend and API on one origin; other deployments can set `VITE_API_BASE_URL` when building the frontend.

Endpoint methods, roles, request fields, and CSRF expectations are documented in [API.md](API.md). For session-authenticated state-changing requests, first obtain the CSRF token from `GET /api/csrf` and send the returned token in `X-XSRF-TOKEN` with the session cookie.

## Repository layout

- `trustvault/`: Spring Boot backend and automated tests.
- `trustvault-frontend/`: React/Vite user interface.
- `database/`: PostgreSQL schema and setup script.
- `API.md`: endpoint reference.
- `THREATS.md`: threat model and accepted limitations.
- `PROMPTS.md`: AI-assisted development record.
- `SUBMISSION_DRAFT.md`: honest draft answers for the challenge submission form.
