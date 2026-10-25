# TrustVault Threat Model

## Assets and authorization model

Protected assets are user credentials, document metadata and file bytes, per-document grants, audit events, and the audit HMAC key. Authentication uses BCrypt-backed credentials and session authentication with CSRF protection. `OWNER` can manage documents they own; a named `VIEWER` can read or edit only when an explicit per-document `READ` or `EDIT` grant exists; `ADMIN` can view the global audit trail but has no automatic document access. Backend authorization is evaluated for each request.

## Threats, controls, and remaining exposure

| Threat | Current controls | Remaining risk / accepted limitation |
|---|---|---|
| Unauthenticated or guessed API requests | Authentication is required for application APIs; CSRF is enabled for session-backed writes. | No account lockout or rate limiting is implemented. A compromised password can be used until changed. |
| IDOR / access to another user's document | Read, edit, download, delete, and permission services check owner/grant rights server-side; listing/search are restricted to accessible documents. | A missed check in a future endpoint would be serious; keep authorization tests for every new route. |
| Viewer privilege escalation | Upload is OWNER-only; document mutation requires owner or explicit edit grant; only the owner grants/revokes. | Admin does not have implicit document access. Administrative document recovery is not implemented. |
| Stale grant after revocation | Each document request checks the current grant in the database; revocation applies to subsequent requests. | Bytes previously downloaded or copied cannot be recalled. |
| Unauthorized upload or malicious file | Upload size caps and extension plus file-signature validation allow PDF, PNG, and JPEG. Storage uses server-generated keys outside static web assets. | No antivirus scanning or content disarm is implemented. Signature checks identify common formats but do not prove a file is safe. |
| Path traversal or public file access | Generated storage keys and a private filesystem directory; download goes through an authorization-checked endpoint. | Production must mount the directory with least privilege, backups protected, and no web-server static mapping. |
| Audit alteration or deletion | Audit entries are HMAC-chained and the chain head is separately authenticated; startup and audit responses verify the chain. Automated tests cover changed fields, deletion, reordering, truncation, modified chain heads, and rejection of a modified chain at startup. Application routes provide no audit edit/delete operation. | Compromise of the app host or HMAC key permits forgery. The chain head is in the same database; there is no external append-only anchor. Unsealed legacy rows cannot be verified, so startup now refuses to seal or modify them; they require a separately reviewed migration. |
| Audit omission on denied requests | Relevant read/download/update/delete/permission denials are recorded; audit failure does not turn a denial into access. | Infrastructure failure can prevent an audit write; critical operations are designed to fail closed where success audit participates in the transaction. Review each new route. |
| Secret leakage | Password hashes use BCrypt; keys/passwords are configuration values rather than source constants. | Operators must keep environment values out of logs, shell history, and source control. Rotate secrets after suspected exposure. |
| Data loss / database compromise | Database constraints and transactional updates protect consistency. | Default development H2 is in-memory, not a production store. No tested production deployment, disaster-recovery plan, or persistent file backup is supplied. |
| Cross-site request forgery / origin abuse | CSRF protection remains enabled; development CORS is limited to the local frontend origin. | Production HTTPS, cookie attributes, trusted origins, and reverse-proxy settings must be configured and tested before deployment. |
| Resource exhaustion | Multipart upload limit is 10 MB per file and 11 MB per request. | No general API throttling, quota, or storage-capacity enforcement is implemented. |

## Security tests

Backend tests exercise document authorization, direct download, revocation, upload validation, audit integrity, personal/global audit separation, failed audit writes, and deletion behavior. Run them from `trustvault` with `cmd.exe /c mvnw.cmd test`. The tests are regression evidence for covered cases, not proof that every possible route is safe. A useful mutation check is to remove an authorization call and confirm the corresponding negative test fails.

## Deployment decisions and unfinished work

The local app uses H2 in-memory storage and a local private-file directory. This is deliberately convenient for development but unsuitable for a live service. Before a public evaluation, configure a persistent production database and private persistent file volume, provision accounts securely, set a stable random HMAC key through a secret manager, use HTTPS, and test restore and revocation behavior. No deployed URL or demo video is included in this repository yet.

The product does not implement public links, folder-level inheritance, external identity providers, malware scanning, version history, soft-delete recovery, retention policies, or externally anchored audit heads. These are out of scope for the current build and should not be implied by the UI or documentation.
