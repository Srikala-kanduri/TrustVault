# TrustVault submission draft

Copy these answers into the matching submission form fields and replace the final link placeholders only after verifying them from a clean browser session.

**Project title:** TrustVault

**One-line description:** Secure document sharing with per-file permissions, immediate request-time revocation, and a tamper-evident audit trail.

**GitHub repository:** https://github.com/Srikala-kanduri/TrustVault

**Live website:** Not deployed yet. Do not submit a localhost URL; deploy and verify a stranger can open it first.

## 1. What problem does it solve?

TrustVault lets an owner share an individual document with named accounts at read or edit level, then revoke the grant. The backend checks the current grant for each document request. It also records who uploaded, viewed, downloaded, changed, deleted, granted, revoked, or was denied access, and protects the ordered log with an HMAC chain.

## 2. What works right now?

Local login supports ADMIN, OWNER, and VIEWER accounts. Owners upload and manage their documents; viewers access only documents explicitly shared with them; admins see the global audit feed, while other users see personal events. Document list/search, metadata updates, file replacement/download/deletion, per-document read/edit grants, revocation, signature-based PDF/PNG/JPEG validation, and audit integrity checks are implemented. Latest backend suite: 86 tests passed on 25 October 2026. Frontend production build passed. The default development database is in-memory and resets on restart.

## 3. How can the evaluator run it?

Use the PowerShell setup and exact commands in [README.md](README.md). It creates and validates a stable local HMAC key in the Windows user environment, prompts for masked local role passwords, enables all three development initializers under the `dev` profile, starts the backend, and starts Vite in a second terminal. Backend tests run with `.\mvnw.cmd clean test` from `trustvault`.

## 4. Which AI tools were used?

ChatGPT was used for implementation suggestions, UI changes, security review, debugging, documentation, and test review. `PROMPTS.md` records the dated development tasks and corrections.

## 5. What did AI get wrong, and what was corrected?

During review, the audit UI/backend route was out of sync and returned 404; denied deletion could skip audit when read authorization threw first; upload authorization existed in the interface but not in the service; a download test expected obsolete audit details; and the Windows Maven wrapper indexed a missing `.m2` link target. These were corrected with a personal-audit integration test, deletion denial audit test, OWNER-only service check, corrected download fixture, and safe wrapper link detection. The fixes are in [commit 7b86b11](https://github.com/Srikala-kanduri/TrustVault/commit/7b86b11); push the local commit before submitting this link.

## 6. What is unfinished?

There is no live deployment or demo video yet. Production requires a persistent database and file volume, HTTPS/host configuration, a tested production account bootstrap procedure, a stable secret-manager HMAC key, and end-to-end validation on the chosen host. The GitHub remote is configured, but public visibility was not verifiable from this environment. Audit head anchoring outside the database, malware scanning, rate limiting, backup/restore testing, and recovery/version history are not implemented.
