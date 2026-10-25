# TrustVault Threat Model

## Scope and security boundaries

TrustVault protects account credentials, document metadata and bytes, sharing grants, audit records, and the HMAC signing key. The browser is untrusted: hiding a button is not authorization. The Spring Boot backend authenticates each request and applies current document ownership/grant rules. Production uses HTTPS through Render, session cookies marked `Secure` and `SameSite=Lax`, CSRF protection for state-changing session requests, PostgreSQL on Neon, and a private object-storage bucket.

Every signed-in user can upload a document and becomes owner of that upload. Any user may therefore be an owner for one document and a grantee/viewer for another. The seeded account labels (`ADMIN`, `OWNER`, `VIEWER`) do not give an account implicit access to other users' document contents. Only the owner of a document can create or revoke that document's grants. `READ` permits read/download; `EDIT` additionally permits updates. Admin's special privilege is access to the global audit view, not document access. Non-admin audit views are scoped to events attributed to the authenticated user's normalized email.

## Threats, controls, and accepted risks

| Threat | Implemented controls | Remaining risk / accepted limitation |
|---|---|---|
| Unauthenticated or guessed API requests | Protected APIs require an authenticated session. Login failures return a generic message. CSRF protection applies to state-changing session requests. | No MFA, account lockout, or identity-provider integration. Password reuse or credential theft remains a risk. |
| Brute-force login and request bursts | In-process token-bucket throttling returns `429` and `Retry-After`; defaults are 10 `/login` requests and 120 `/api/**` requests per client IP per minute. The client IP parser accepts an IP literal from the first `X-Forwarded-For` hop, as expected behind Render. | Limits are per process, memory-only, and reset on restart. They are not coordinated across multiple service instances and are not a substitute for edge/network DDoS protection. The application has not been stress-tested against DoS traffic. |
| IDOR / URL tampering | Each document detail, download, update, replace, delete, and permission-management operation checks ownership or an active grant server-side. Listing and search are scoped to the caller's accessible documents. A guessed or altered ID without permission should be denied. | A missing check in a future endpoint could expose data. Keep negative authorization tests for every protected endpoint and re-check authorization when adding routes. |
| Unauthorized privilege escalation | Any authenticated user can upload and own their own documents; sharing is authorized against that document's owner. A grantee cannot re-share, delete, or administer grants. Admin status grants no document-content access. | Role labels are coarse seeded account labels; document capabilities derive from per-document ownership/grants. There is no separate organization or tenant boundary. |
| Access after revocation | The backend checks current grant state on each request. A revoked grant denies subsequent reads/downloads and the document disappears from accessible listings. | Revocation cannot recall data that was already downloaded, photographed, or copied. Existing in-flight responses may finish. |
| Malicious or oversized uploads | Uploads are capped at 10 MB per file and 11 MB per multipart request. PDF, PNG, and JPEG are checked by extension and file signature. Storage uses generated keys; files are served only through an authorized download route. Production requires private object storage. | There is no malware scanning, content disarm, or per-user storage quota. A valid file signature does not prove benign contents. |
| Path traversal / public file exposure | Client filenames are not used as storage paths; server-generated keys address files. Production uses a private Neon Object Storage bucket and the backend mediates downloads. | Operators must preserve bucket privacy and access-key secrecy. No independent bucket-policy penetration test is claimed. |
| Audit editing, deletion, truncation, or reordering | Audit events are HMAC chained. The separately authenticated chain-head record is checked at startup and before audit views are returned. Tests cover changed fields, deletion/reordering/truncation, chain-head tampering, and rejection of tampered history. There are no application endpoints to edit audit events. | The chain head is stored in the same database. A database administrator able to rewrite all rows and obtain the HMAC key, or a compromised app host/key, can forge a new valid history. There is no external append-only anchor. Legacy unsealed audit rows cannot prove historical integrity; startup refuses to silently seal them. |
| Audit omission / operation-audit mismatch | Sensitive database changes and their success events share a transaction where implemented. Reads, downloads, and denied attempts have explicit audit handling. Audit integrity is verified before returning audit data. Admin sees the global stream; other users see only their own actor events. | File-store operations cannot participate in a PostgreSQL transaction and use cleanup/compensation paths. An unavailable audit database may prevent an operation or prevent recording a denial. Logs are not externally anchored. |
| Secret disclosure | Passwords are stored as BCrypt hashes. Production passwords, database/storage credentials, and the HMAC key are configured as Render environment secrets; they do not belong in Git. | Anyone with access to Render secrets or the running host can misuse them. Rotate secrets after exposure. The audit key must stay unchanged while preserving a database's audit history; rotation requires a planned migration. |
| Database or file loss | Production metadata is stored in Neon PostgreSQL and file bytes in private Neon Object Storage. PostgreSQL constraints and transactional updates protect consistency. | Backups, point-in-time restore, and disaster recovery must be verified by the operator. Render's free service can sleep; temporary local storage is not authoritative. Do not claim tested recovery without performing a restore exercise. |
| Availability exhaustion | The app limits request bursts and upload size; Render provides platform-level protections. | No application-level concurrency cap, storage quota, shared rate-limit store, or load/DoS test is included. Free-tier capacity and cold starts affect availability. Do not run flood tests against the public Render service. |

## Verifier checks

Use the demo accounts and steps in the root [README](README.md). On the live deployment, test only normal, low-volume requests:

1. Without a session, `GET /api/documents` should return `401`.
2. As a non-admin, `GET /api/admin/audit-logs` should return `403`; `GET /api/audit-logs` should show only that user's events.
3. Have an owner grant a viewer `READ`, verify access, revoke it, then verify a later direct detail/download request is denied (`403`).
4. Try a document ID owned by another account with no grant. The backend must deny it even if the UI hides the document.
5. Send repeated normal login/API requests only if needed to observe throttling; stop at a small number above the configured limit and do not use concurrency, large payloads, or flooding on the live deployment.

Run local backend regression tests in PowerShell from `trustvault`:

```powershell
.\mvnw.cmd clean test
```

The tests are evidence for exercised cases, not proof of security for every input or deployment configuration. To test that an authorization check is security-critical, temporarily remove the corresponding service authorization check locally and confirm its negative test fails; never make that mutation on the live service. API route/access details are in [`trustvault/API.md`](trustvault/API.md).

## Out of scope

The application does not implement MFA, public share links, folder inheritance, external identity providers, malware scanning, document version history, soft-delete recovery, retention policy, organization-level tenancy, or external audit anchoring. These omissions should be treated as limitations, not implied capabilities.
