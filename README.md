# TrustVault

TrustVault is a secure document-sharing application. It provides per-document read/edit grants, private file storage, revocation, document search, and an HMAC-chained audit history.

## Current implementation

- Roles: `ADMIN` can inspect the verified global audit trail; `OWNER` can upload and manage documents they own; `VIEWER` can read or edit only documents with an explicit grant for the relevant permission. Admin status does not grant implicit document access.
- Every document read, download, update, replacement, deletion, grant, and revocation is authorized by the backend. Revocation is checked on each request, so later requests are denied immediately. A file already downloaded cannot be recalled.
- Search supports title, category, owner email, and a date range, over only the caller's accessible documents.
- Upload validation checks extension and file signature for PDF, PNG, and JPEG, with Spring multipart size limits (10 MB per file).
- Audit events include successful operations and relevant denied attempts. The HMAC chain is verified before audit views are returned. The key must remain secret and stable.
- Owner and viewer accounts appear in personal audit views, which contain only events recorded under the signed-in email.

## Requirements not yet satisfied

This repository does not currently include a deployed live URL, a five-minute demo video, or a completed submission write-up. The GitHub remote is configured, but public accessibility could not be verified from this environment. Deployment needs a persistent database and private file volume; the default H2 database is in-memory and loses data when the process stops. See [THREATS.md](THREATS.md) for known risks and [PROMPTS.md](PROMPTS.md) for AI usage notes.

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

## Database and API

The default profile uses in-memory H2 for local development. `database/schema.sql` documents the PostgreSQL schema for a persistent installation; `database/setup.sql` applies it with `psql`. Production starts with `SPRING_PROFILES_ACTIVE=prod`, uses PostgreSQL validation (schema must be applied first), disables H2 and development account initialization, and requires the database URL/username/password, private storage path, stable audit key, and frontend origin as environment values. Point the frontend build at the deployed API with `VITE_API_BASE_URL`. Never use development account initializers in production. Production account provisioning is not yet automated; do not expose a public deployment until an owner/admin bootstrap procedure has been added and verified.

Endpoint methods, roles, request fields, and CSRF expectations are documented in [API.md](API.md). For session-authenticated state-changing requests, first obtain the CSRF token from `GET /api/csrf` and send the returned token in `X-XSRF-TOKEN` with the session cookie.

## Repository layout

- `trustvault/`: Spring Boot backend and automated tests.
- `trustvault-frontend/`: React/Vite user interface.
- `database/`: PostgreSQL schema and setup script.
- `API.md`: endpoint reference.
- `THREATS.md`: threat model and accepted limitations.
- `PROMPTS.md`: AI-assisted development record.
- `SUBMISSION_DRAFT.md`: honest draft answers for the challenge submission form.
