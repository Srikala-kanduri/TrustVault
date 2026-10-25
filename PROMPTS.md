# TrustVault â€” AI Usage Log

This log records representative development and review prompts, AI assistance, corrections made after checking the repository or running the app, and the resulting code commits. AI suggestions were treated as proposals; the human developer chose the authorization model and verified behavior. Current date for this log: 25 October 2026.

## 1. Account roles and document ownership

**Date:** 25 October 2026
**Prompt summary:** â€œA person may need to upload and share documents and also view documents shared with them. We cannot assign a fixed owner/viewer role to an employee.â€

- **AI output:** Initially reasoned in terms of the existing fixed `OWNER`/`VIEWER` role distinction and suggested making upload depend on role.
- **What was wrong:** That would prevent one employee from acting as owner on their own upload and grantee on another user's document. It did not match the requested per-document model.
- **Human decision/correction:** Keep the account role labels for the seeded demo accounts, allow every authenticated account to upload, make the uploader the owner, and enforce access to other documents through explicit per-document `READ`/`EDIT` grants. Only the document owner can share or revoke that document.
- **Evidence/commit:** [02817ec](https://github.com/Srikala-kanduri/TrustVault/commit/02817ec) enabled upload for all signed-in users; [7b86b11](https://github.com/Srikala-kanduri/TrustVault/commit/7b86b11) added regression coverage for authorization and audit behavior.

## 2. Personal and global audit visibility

**Date:** 25 October 2026
**Prompt summary:** â€œAdmin should see every audit; every other user should see only their personal audit. The owner and viewer profile audit panels show no data.â€

- **AI output:** Identified that the UI called `/api/audit-logs` while the backend lacked the personal-feed route in the version being checked.
- **What was wrong/incomplete:** A UI change alone would not establish that actor filtering was enforced server-side, and it would not fix a missing backend route.
- **Human decision/correction:** Add an authenticated personal endpoint filtered to the normalized current email, keep a separate admin-only global endpoint, and cover both scopes and unauthenticated access with tests.
- **Evidence/commit:** [7b86b11](https://github.com/Srikala-kanduri/TrustVault/commit/7b86b11).

## 3. Audit integrity and failure handling

**Date:** 22â€“25 October 2026
**Prompt summary:** â€œReview the audit trail against the requirements, including chain tampering, deletion/reordering, startup verification, and failed audit writes.â€

- **AI output:** Helped enumerate chain-integrity cases and transaction boundaries, and proposed tests for audit failures.
- **What was incomplete:** Passing unit or service tests does not prove that a live database history is tamper-evident, and a first-time seal cannot establish that legacy rows were never changed before sealing.
- **Human decision/correction:** Keep the documented threat-model limitation, verify history at startup and before returning audit views, add regression cases for modified fields, deletion, reordering, truncation, and chain-head changes, and reject unsupported legacy history instead of silently blessing it.
- **Evidence/commit:** [aa6c309](https://github.com/Srikala-kanduri/TrustVault/commit/aa6c309) addressed chain verification in a writable transaction; [7b86b11](https://github.com/Srikala-kanduri/TrustVault/commit/7b86b11) contains audit/access review fixes. External anchoring remains unimplemented.

## 4. Broken document access after direct URL navigation

**Date:** 25 October 2026
**Prompt summary:** â€œChanging a document URL should show access denied; opening a document should include its document ID and keep the user signed in.â€

- **AI output:** Helped trace document navigation, session restoration, and access-denied handling across React routes and backend endpoints.
- **What was wrong/incomplete:** The UI had been treating route/session state inconsistently; redirecting every failed detail fetch to login would hide a real `403` and make it appear that access control signed the user out.
- **Human decision/correction:** Put the document identifier in the details route, restore the existing session on direct navigation, and preserve the distinction between unauthenticated `401` and authenticated-but-forbidden `403`.
- **Evidence/commits:** [2d76171](https://github.com/Srikala-kanduri/TrustVault/commit/2d76171), [6a617d0](https://github.com/Srikala-kanduri/TrustVault/commit/6a617d0), and [1750b26](https://github.com/Srikala-kanduri/TrustVault/commit/1750b26).

## 5. Deployment and account initialization

**Date:** 25 October 2026
**Prompt summary:** â€œDeploy the app with Render and Neon, retain the audit key, initialize the role accounts, and guide an evaluator through the live app.â€

- **AI output:** Helped prepare a Render Blueprint, production profile, Neon database/object-storage configuration, and first-run account bootstrap.
- **What was wrong/incomplete:** Early documentation described a non-deployed/local-only state and account instructions did not explain that bootstrap is one-time. Changing Render bootstrap password variables after accounts exist does not reset those passwords.
- **Human decision/correction:** Document the live URL, exact environment-variable names, one-time bootstrap behavior, same-origin session/CSRF behavior, and the requirement to use dedicated evaluator accounts. Keep database/storage credentials and the audit key out of Git.
- **Evidence/commits:** [6876e5a](https://github.com/Srikala-kanduri/TrustVault/commit/6876e5a) added production deployment configuration; [f6ea97c](https://github.com/Srikala-kanduri/TrustVault/commit/f6ea97c) added Neon storage support.

## 6. Search, rate limiting, and verifier security claims

**Date:** 25 October 2026
**Prompt summary:** â€œCheck search behavior and security controls; add a defense against request floods and make the public documentation match the implementation.â€

- **AI output:** Helped review the search query wiring and implement per-IP token-bucket limits for login and API requests.
- **What was wrong/incomplete:** A prior threat-model draft said there was no rate limiter, and a previous search UI could issue stale/incomplete filter values. In-process throttling also does not provide shared limits across multiple instances or prove DoS resistance.
- **Human decision/correction:** Keep the rate limit bounded and configurable, return `429` with `Retry-After`, update docs to state the defaults and single-instance limitation, and do not flood-test the public free-tier service. Search filters were corrected and documented as constrained to accessible documents.
- **Evidence/commits:** [2adf04a](https://github.com/Srikala-kanduri/TrustVault/commit/2adf04a) corrected search filter construction; [61ea960](https://github.com/Srikala-kanduri/TrustVault/commit/61ea960) added request rate limits. The implementation has not been load/DoS tested.

## Verification record and limits

Earlier development work was verified with the Maven Wrapper tests, a Vite production build, and manual browser checks of login, upload, grant, read/download, revocation, and audit visibility. Verification results are point-in-time evidence and should be refreshed against the exact commit being submitted. Do not claim a new test or live-environment check based only on this historical log.

The human developer reviewed AI suggestions, selected the access model, corrected inaccurate assumptions, and owns the final submission. Known limitations are listed in [THREATS.md](THREATS.md); verifier steps and disposable demo-account setup are in [README.md](README.md).
