# TrustVault API

All `/api` endpoints except `GET /api/health` require a signed-in session. State-changing calls require the session's CSRF token in `X-XSRF-TOKEN`. Obtain it from `GET /api/csrf` and include the session cookie. Requests and responses use JSON unless noted. Access checks are performed by the backend on each request.

## Authentication, health, and audit

| Method and path | Access | Purpose |
|---|---|---|
| `GET /api/health` | Public | Liveness response. |
| `GET /api/csrf` | Public/session bootstrap | Returns CSRF token and sets the CSRF cookie. |
| `GET /api/me` | Any authenticated role | Current user and authorities. |
| `GET /api/audit-logs` | Any authenticated role | Only events attributed to the caller's normalized email. |
| `GET /api/admin/audit-logs` | `ADMIN` | Full audit history, only after chain integrity verifies. |

Login uses Spring Security's form login endpoint (`POST /login`) with `username` (email) and `password`, plus CSRF. Logout uses `POST /logout` with CSRF.

## Documents

| Method and path | Access | Purpose / parameters |
|---|---|---|
| `GET /api/documents` | Authenticated | Documents owned by caller or explicitly granted to caller. |
| `GET /api/documents/search` | Authenticated | Optional `title`, `category`, `ownerEmail`, `fromDate`, `toDate` (ISO-8601 timestamps); searches only accessible documents. |
| `POST /api/documents` | `OWNER` | Multipart `title`, `category`, and `file`; creates document. Allowed file signatures: PDF, PNG, JPEG; max file 10 MB. Returns `201`. |
| `GET /api/documents/{id}` | Owner or any explicit grant | Read metadata; records a read event. |
| `GET /api/documents/{id}/download` | Owner or any explicit grant | Downloads bytes after current access check; records download. |
| `PATCH /api/documents/{id}` | Owner or explicit `EDIT` grant | JSON `{ "title": "...", "category": "..." }`; title max 200, category max 100. |
| `PUT /api/documents/{id}/file` | Owner or explicit `EDIT` grant | Multipart `file`; replaces stored file after validation. |
| `DELETE /api/documents/{id}` | Owner only | Deletes document and grants; returns `204`. |
| `GET /api/documents/{id}/my-permissions` | Owner or any explicit grant | Reports whether caller owns or can edit document. |

Denied access returns `403`; missing resources return `404`; invalid request data is rejected. Exact error JSON is provided by Spring's exception handling.

## Permissions

Only the document owner may manage grants. A target email must belong to an existing account. Permission levels are `READ` and `EDIT`.

| Method and path | Access | Purpose / parameters |
|---|---|---|
| `GET /api/documents/{documentId}/permissions` | Document owner | Lists active grants. |
| `POST /api/documents/{documentId}/permissions` | Document owner | JSON `{ "email": "viewer@example.com", "permissionLevel": "READ" }`; grant or update permission. |
| `DELETE /api/documents/{documentId}/permissions?email=...` | Document owner | Revokes that user's grant; subsequent requests are checked against current grants. Returns `204`. |
| `GET /api/permissions/my-grants` | Any authenticated role | Lists active grants received by caller. |

Successful and denied protected operations are audited where applicable. Each access decision is server-side; hiding a frontend action is not an authorization control.

## Data and deployment

PostgreSQL schema and initial setup are in `database/schema.sql` and `database/setup.sql`. Production uses the `prod` profile with Hibernate validation. Create the database and apply the schema first, then configure JDBC credentials, a private persistent `TRUSTVAULT_STORAGE_DIR`, a stable random `TRUSTVAULT_AUDIT_HMAC_KEY`, and `TRUSTVAULT_CORS_ALLOWED_ORIGIN`. Build the frontend with `VITE_API_BASE_URL`. The application currently has no production user-provisioning endpoint or CLI; create production accounts through a controlled, reviewed bootstrap procedure before exposing the service. Do not expose stored file paths or map the storage directory as static web content.
