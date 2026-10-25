# TrustVault

TrustVault is a secure document-sharing application with per-document permissions and an HMAC-chained audit trail.

## Live demo

- Application: <https://trustvault-6vsk.onrender.com>
- Repository: <https://github.com/Srikala-kanduri/TrustVault>
- The Render free web service may sleep when idle. The first page load can take about a minute; wait for it to wake before retrying.
- The application uses seeded `ADMIN`, `OWNER`, and `VIEWER` accounts. Give evaluators the email/password pairs for these demo accounts in the submission's private credential field. Do not use production or personal passwords. If the submission requires the credentials to be public in this README, first rotate the accounts to disposable evaluator-only passwords and replace the entries below. **Do not submit this README with the placeholders unchanged.**

| Account | Email | Password |
|---|---|---|
| Admin | `admin@trustvault.com` | `Admin123456@@` |
| Owner | `owner@trustvault.com` | `Owner123456@@` |
| Viewer | `viewer@trustvault.com` | `Viewer123456@@` |

The role labels identify the seeded demo accounts. All signed-in users can upload documents and manage the documents they upload. Owners can grant `READ` or `EDIT` access to another registered account. A user's access to documents uploaded by others comes only from an active grant. Admin can inspect the global audit trail, but admin status does not grant access to document contents. Every non-admin account sees only audit events attributed to its own email.

### Evaluator walkthrough

1. Sign in as `owner@trustvault.com`, upload a small PDF or image, then open **Share a document** and grant `READ` to `viewer@trustvault.com`.
2. Sign out and sign in as the viewer. Confirm the shared document is listed and can be opened/downloaded. Try an unshared document ID if available; the server should deny the request.
3. Sign back in as the owner and revoke the grant. Subsequent viewer requests for that document should be denied. A previously downloaded copy cannot be recalled.
4. Open **Audit trail** as the viewer and confirm it shows only viewer-attributed activity. Sign in as admin and confirm the global audit view includes events from all accounts.
5. Try the protected API without a session (`GET /api/documents` should return `401`) and try the admin audit endpoint as a non-admin (should return `403`).

## Authorization and audit behavior

- Authentication uses session cookies and BCrypt-encoded passwords. CSRF protection applies to state-changing requests.
- Access is checked server-side on every document request. Listing and search return only documents the caller owns or has an active grant for.
- Every signed-in user can upload; the uploader becomes the owner. Only that document's owner can grant or revoke access.
- `READ` grants allow reading/downloading. `EDIT` grants also allow metadata/file updates. Neither grants deletion or grant-management rights.
- Revocation blocks later server requests. It cannot recall bytes already downloaded or copied.
- Admin can read the global audit trail. Other accounts can read only events whose actor matches their signed-in email. Audit views are returned only after HMAC-chain verification.

See [API.md](trustvault/API.md) for endpoint methods, permissions, parameters, and CSRF steps. See [THREATS.md](THREATS.md) for defenses, limits, and accepted risks. See [PROMPTS.md](PROMPTS.md) for the AI-assisted development record.

## Local development on Windows

Prerequisites: JDK 21+, Node.js/npm, and internet access for the first Maven dependency download. Open PowerShell in the repository root.

Set a persistent local audit key once per Windows user, or preserve your existing key. Never change it while using the same audit database. In PowerShell:

```powershell
$key = [Environment]::GetEnvironmentVariable('TRUSTVAULT_AUDIT_HMAC_KEY', 'User')
if (-not $key) {
  $bytes = [byte[]]::new(32)
  [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
  $key = [Convert]::ToBase64String($bytes)
  [Environment]::SetEnvironmentVariable('TRUSTVAULT_AUDIT_HMAC_KEY', $key, 'User')
}
try { $decoded = [Convert]::FromBase64String($key) }
catch { throw 'Saved audit key is not valid Base64; it was not changed.' }
if ($decoded.Length -lt 32) { throw 'Saved audit key is shorter than 32 bytes; it was not changed.' }
$env:TRUSTVAULT_AUDIT_HMAC_KEY = $key
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:TRUSTVAULT_DEV_ADMIN_ENABLED = 'true'
$env:TRUSTVAULT_DEV_OWNER_ENABLED = 'true'
$env:TRUSTVAULT_DEV_VIEWER_ENABLED = 'true'
```

Set local role passwords in this same PowerShell session (12 or more characters):

```powershell
function Set-TrustVaultPassword([string]$Name, [string]$Prompt) {
  $secure = Read-Host $Prompt -AsSecureString
  if ($secure.Length -lt 12) { throw 'Passwords must be at least 12 characters.' }
  $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
  try {
    [Environment]::SetEnvironmentVariable(
      $Name, [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer), 'Process')
  } finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    $secure.Dispose()
  }
}
Set-TrustVaultPassword 'TRUSTVAULT_DEV_ADMIN_PASSWORD' 'Admin123456@@'
Set-TrustVaultPassword 'TRUSTVAULT_DEV_OWNER_PASSWORD' 'Owner123456@@'
Set-TrustVaultPassword 'TRUSTVAULT_DEV_VIEWER_PASSWORD' 'Viewer123456@@'
```

The prompt masks typing; the password is briefly converted for the child process environment. Start the backend in one terminal:

```powershell
cd .\trustvault
.\mvnw.cmd spring-boot:run
```

Start the frontend in a second terminal from the repository root:

```powershell
cd .\trustvault-frontend
npm install
npm run dev
```

Open the Vite URL, normally <http://localhost:5173>. Local account emails default to `admin@trustvault.local`, `owner@trustvault.local`, and `viewer@trustvault.local`, with the passwords set above. The local default database is in-memory H2, so its data is lost when the backend stops. Health endpoint: <http://localhost:8080/api/health>.

Run backend tests from `trustvault`:

```powershell
.\mvnw.cmd clean test
```

Build the frontend from `trustvault-frontend`:

```powershell
npm run build
```

## Render and Neon deployment

The repository includes a Render Blueprint in [`render.yaml`](render.yaml). It builds the frontend into the Spring Boot app and serves UI and API on the same origin. Production uses PostgreSQL on Neon and private Neon Object Storage for uploaded file bytes. The database schema is in [`trustvault/src/main/resources/database/schema.sql`](trustvault/src/main/resources/database/schema.sql); the standalone PostgreSQL setup scripts are in [`database/`](database/).

The first production bootstrap is enabled by the Blueprint. Supply these Render environment variables using the Render dashboard's secret inputs:

- `TRUSTVAULT_DATABASE_URL`
- `TRUSTVAULT_STORAGE_S3_ENDPOINT`, `TRUSTVAULT_STORAGE_S3_BUCKET`, `TRUSTVAULT_STORAGE_S3_ACCESS_KEY`, `TRUSTVAULT_STORAGE_S3_SECRET_KEY`, `TRUSTVAULT_STORAGE_S3_REGION`
- `TRUSTVAULT_BOOTSTRAP_ADMIN_EMAIL`, `TRUSTVAULT_BOOTSTRAP_ADMIN_PASSWORD`
- `TRUSTVAULT_BOOTSTRAP_OWNER_EMAIL`, `TRUSTVAULT_BOOTSTRAP_OWNER_PASSWORD`
- `TRUSTVAULT_BOOTSTRAP_VIEWER_EMAIL`, `TRUSTVAULT_BOOTSTRAP_VIEWER_PASSWORD`
- `TRUSTVAULT_AUDIT_HMAC_KEY` (generated by the Blueprint; preserve it)

Passwords must be at least 12 characters. The production bootstrap creates the three accounts only when the database has no users. It skips creation on later starts, so changing these environment variables does **not** change existing account passwords. To provision new verifier credentials, use a fresh demo database or a reviewed password-reset procedure; do not delete a database that contains submission data without a backup. Never put the database URI, storage secrets, or audit HMAC key in the repository or share them with evaluators.

For submission, use disposable evaluator accounts and share their login pairs only where the submission instructions specify. If credentials are to be placed in this public README, rotate them first, ensure the accounts contain only synthetic demo data, then replace the three password placeholders above. Do not commit real operational credentials.

Post-deployment checks: open the live URL, sign in with each evaluator account, follow the evaluator walkthrough, and check the Render deploy log if an endpoint fails. A `401` means no valid session; `403` means the session is authenticated but lacks permission. The deployed in-process rate limiter allows up to 10 login requests and 120 API requests per client IP per minute; see [THREATS.md](THREATS.md) for its limits.

## Repository map

- `trustvault/`: Spring Boot backend, PostgreSQL schema, and tests.
- `trustvault-frontend/`: React/Vite frontend.
- `database/`: PostgreSQL schema and setup scripts.
- `trustvault/API.md`: endpoint reference.
- `THREATS.md`: threat model and limitations.
- `PROMPTS.md`: AI usage log.
- `SUBMISSION_DRAFT.md`: draft responses for the challenge submission.
