# TrustVault API Reference

The deployed UI and API share one origin. All `/api/**` endpoints except health and CSRF bootstrap require an authenticated session, unless a row below says otherwise. `POST`, `PUT`, `PATCH`, and `DELETE` requests require the session's CSRF token in `X-XSRF-TOKEN`; obtain the token from `GET /api/csrf` and retain the session/CSRF cookies. Responses use JSON unless noted. The backend checks access on every request.

## Authentication, health, and audit

| Method and path | Access | Behavior |
|---|---|---|
| `GET /api/health` | Public | Liveness response. |
| `GET /api/csrf` | Public/session bootstrap | Returns CSRF token and sets CSRF cookie. |
| `POST /login` | Public + CSRF | Spring form login; form fields `username` (email) and `password`. Returns JSON; successful login establishes session. |
| `POST /logout` | Authenticated + CSRF | Ends the session. |
| `GET /api/me` | Any authenticated user | Returns current account information and authorities. |
| `GET /api/audit-logs` | Any authenticated user | Returns only events attributed to the caller's normalized email, after verifying the HMAC chain. |
| `GET /api/admin/audit-logs` | `ADMIN` | Returns the global audit history after verifying the HMAC chain. |

Authentication failures return `401`; authenticated callers without permission receive `403`. Missing resources return `404`; rate-limit responses return `429` with `Retry-After`.

## Documents

All signed-in users may upload. The uploader becomes the owner regardless of the account's seeded role label. A user can access a document they do not own only through an active explicit grant.

| Method and path | Access | Parameters / behavior |
|---|---|---|
| `GET /api/documents` | Authenticated | List documents owned by or actively shared with caller. |
| `GET /api/documents/search` | Authenticated | Optional `title`, `category`, `ownerEmail`, `fromDate`, `toDate` (ISO-8601 timestamps). Searches only accessible documents. |
| `POST /api/documents` | Authenticated | Multipart fields `title`, `category`, `file`; creates owned document, returns `201`. Supported file signatures: PDF, PNG, JPEG. Maximum 10 MB per file (11 MB request). |
| `GET /api/documents/{id}` | Owner or any active grant | Read document metadata; records activity. |
| `GET /api/documents/{id}/download` | Owner or any active grant | Downloads the bytes after a current access check; records activity. |
| `PATCH /api/documents/{id}` | Owner or explicit `EDIT` grant | JSON `{ "title": "...", "category": "..." }`; title max 200 characters and category max 100. |
| `PUT /api/documents/{id}/file` | Owner or explicit `EDIT` grant | Multipart field `file`; replaces stored bytes after validation. |
| `DELETE /api/documents/{id}` | Owner only | Deletes the document and related grants; returns `204`. |
| `GET /api/documents/{id}/my-permissions` | Owner or active grantee | Reports whether caller owns the document and whether caller can edit it. |

## Sharing and permissions

Only the document owner may manage its grants. The recipient email must match an existing account. A `READ` grant permits reading/downloading; `EDIT` also permits metadata/file updates. Neither permission allows deletion or further sharing.

| Method and path | Access | Parameters / behavior |
|---|---|---|
| `GET /api/documents/{documentId}/permissions` | Document owner | Lists active grants. |
| `POST /api/documents/{documentId}/permissions` | Document owner | JSON `{ "email": "user@example.com", "permissionLevel": "READ" }`; creates or updates a grant (`READ` or `EDIT`). |
| `DELETE /api/documents/{documentId}/permissions?email=user%40example.com` | Document owner | Revokes the target account's grant; returns `204`. Later requests use the updated grant state. |
| `GET /api/permissions/my-grants` | Any authenticated user | Lists active grants received by caller. |

Successful and denied protected operations are audited where implemented. UI visibility is not an access control. Never use a document identifier as proof of authorization.

## Data and deployment

PostgreSQL schema initialization is in `src/main/resources/database/schema.sql`; standalone setup files are in the repository `database/` directory. Production uses the `prod` Spring profile, PostgreSQL, private Neon Object Storage, and a stable `TRUSTVAULT_AUDIT_HMAC_KEY`. Render deployment instructions, account bootstrap variables, and verifier walkthrough are in the root [`README.md`](../README.md). Do not expose object storage paths or map private storage as static web content.
