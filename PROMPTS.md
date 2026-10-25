# TrustVault — AI Usage Log

## Introduction

TrustVault is a secure document-sharing application designed to control access to documents and maintain an audit history of important actions. During development, I used ChatGPT as a development assistant to understand technical concepts, troubleshoot errors, review security behavior, and improve implementation and configuration.

I reviewed the suggestions, applied changes to the project, and used Maven tests, frontend builds, and manual browser testing to verify results. This log records the main development tasks involving AI assistance, the problems encountered, the fixes applied, and the verification performed.

AI assistance was used as a development aid. The implementation, test results, and remaining limitations are documented separately rather than assuming that a suggested solution is automatically correct.

---

## 1. Git Setup and `.gitignore`

**Date:** 20 October 2026
**Tools:** ChatGPT, VS Code, Git

### Task and AI assistance

I asked ChatGPT to review the `.gitignore` configuration for a project containing a Spring Boot backend and a React frontend.

### Problem encountered

I needed to ensure that generated build files, dependency folders, environment files, and private uploaded documents would not accidentally be committed. Git also displayed line-ending warnings while processing some files.

### Fix implemented

I reviewed the root `.gitignore` and added or checked rules for Maven build output, Node.js dependencies, environment files, IDE-generated files, and the private document-storage directory.

### Verification

I inspected the Git status and reviewed the files selected for committing. The initial TrustVault authentication foundation was committed.

**Commit:** `f972743` — Initial TrustVault authentication foundation.

---

## 2. Correcting Whitespace in `pom.xml`

**Date:** 20 October 2026
**Tools:** ChatGPT, VS Code, Git

### Task and AI assistance

I asked ChatGPT to help identify and correct trailing whitespace in the Maven `pom.xml` configuration.

### Problem encountered

Git reported trailing whitespace when I ran `git diff --cached --check`.

### Fix implemented

I reviewed the affected dependency declarations, corrected the formatting, and staged the updated file.

### Verification

The final verification result must be recorded after running:

```bash
git diff --cached --check
```

The expected result is no whitespace errors. Line-ending warnings, if present, should be distinguished from actual whitespace errors.

**Commit:** [f972743](https://github.com/Srikala-kanduri/TrustVault/commit/f972743) — initial Maven project setup.

---

## 3. Troubleshooting the Missing Development Admin Password

**Date:** 20 October 2026
**Tools:** ChatGPT, Spring Boot, Maven Wrapper

### Task and AI assistance

I shared a Spring Boot test failure with ChatGPT and asked for help identifying why the application context could not load.

### Problem encountered

Spring could not resolve the `TRUSTVAULT_DEV_ADMIN_PASSWORD` environment variable used by the development admin configuration.

### Fix implemented

I reviewed the password property configuration and adjusted it so that tests would not require a development password merely to load the application context. The development account initializer retains password validation before creating an account.

### Verification

I ran the Maven Wrapper tests and obtained a successful build after correcting the configuration.

```bash
.\mvnw.cmd test
```

**Result:** `BUILD SUCCESS` was reported during the development session.

**Commit:** [f972743](https://github.com/Srikala-kanduri/TrustVault/commit/f972743) — initial test and development configuration.

---

## 4. Separating Test and Development Configuration

**Date:** 20 October 2026
**Tools:** ChatGPT, VS Code, Maven Wrapper

### Task and AI assistance

I asked ChatGPT how to prevent automated tests from depending on development-only account configuration.

### Problem encountered

The test application context was loading configuration that expected a development password, causing the context to fail before the tests could complete.

### Fix implemented

I configured the relevant tests to use test-specific configuration, including an in-memory H2 database, `create-drop` schema management, and disabled development admin initialization.

### Verification

I ran the Maven Wrapper tests after updating the configuration.

```bash
.\mvnw.cmd test
```

**Result:** `BUILD SUCCESS` was reported during the development session.

**Commit:** [f972743](https://github.com/Srikala-kanduri/TrustVault/commit/f972743) — initial application foundation and configuration.

---

## 5. Connecting the Project to GitHub

**Date:** 20 October 2026
**Tools:** ChatGPT, Git CLI, GitHub

### Task and AI assistance

I asked ChatGPT to guide me through connecting the local TrustVault repository to a private GitHub repository.

### Problem encountered

The local repository initially did not have the required remote configured.

### Fix implemented

I created the GitHub repository, configured the `origin` remote, and pushed the local `main` branch.

### Verification

Git confirmed that the `main` branch was pushed and configured to track `origin/main`.

**Repository:** https://github.com/Srikala-kanduri/TrustVault

**Commit:** `f972743` — Initial TrustVault authentication foundation.

---

## 6. Permission Changes and Audit-Failure Rollback Tests

**Date:** 22 October 2026
**Tools:** ChatGPT, Spring Boot, Maven Wrapper

### Task and AI assistance

I used ChatGPT to help review the relationship between document permission changes and audit logging, particularly what should happen if an audit write fails.

### Security requirement

A permission grant or revocation must not be committed when the required audit operation fails.

### Implementation and tests

The following integration tests were used to verify the behavior:

- `failedAuditWriteRollsBackPermissionGrant`
- `failedAuditWriteRollsBackPermissionRevocation`

### Verification

```bash
.\mvnw.cmd clean test
```

**Result:** `BUILD SUCCESS` was reported.

**Outcome:** The tests confirmed that simulated audit-write failures roll back the associated permission changes.

**Commit:** [4e0301c](https://github.com/Srikala-kanduri/TrustVault/commit/4e0301c) — permission operations and rollback test coverage.

---

## 7. Download Failure and Authorization Audit Tests

**Date:** 22 October 2026
**Tools:** ChatGPT, Spring Boot, Maven Wrapper

### Task and AI assistance

I used ChatGPT to review failed-download auditing and the handling of audit-recording errors during authorization failures.

### Security requirement

A failed or unauthorized download should be recorded where required. An audit-recording failure must not turn a denied request into an authorized one.

### Tests

- `failedDownloadIsAudited`
- `auditFailureDoesNotHideAccessDenial`

### Verification

```bash
.\mvnw.cmd clean test
```

**Result:** `BUILD SUCCESS` was reported.

**Outcome:** The tests confirmed that failed downloads are audited and that audit-recording errors do not grant unauthorized access.

**Limitation:** These tests do not independently establish that audit records are immutable against direct database modification.

**Commit:** [4e0301c](https://github.com/Srikala-kanduri/TrustVault/commit/4e0301c) — download auditing, authorization checks, and related tests.

---

## 8. Database-Backed Document Authorization Tests

**Date:** 22 October 2026
**Tools:** ChatGPT, Spring Boot, Maven Wrapper

### Task and AI assistance

I used ChatGPT to review document visibility rules and how repository-level queries should enforce access restrictions.

### Security requirement

Users should see only documents they own or have permission to access. Revoked permissions and unrelated users must not provide access through document listing or search.

### Test coverage

The `DocumentRepositoryTest.java` tests cover repository-level document visibility for owners, users with sharing permissions, unauthorized users, and revoked permissions.

### Verification

```bash
.\mvnw.cmd clean test
```

**Result:** `BUILD SUCCESS` was reported.

**Outcome:** The tests confirmed the intended visibility behavior in the test database.

**Limitation:** Repository-level tests complement, but do not replace, endpoint-level authorization and manual security testing.

**Commit:** [1d7b55a](https://github.com/Srikala-kanduri/TrustVault/commit/1d7b55a) — accessible-document search and filter implementation/tests.

---

## 9. Development-Only OWNER and VIEWER Accounts

**Date:** 22 October 2026
**Tools:** ChatGPT, VS Code, Spring Boot

### Task and AI assistance

I asked ChatGPT to help set up separate OWNER and VIEWER accounts so that I could test document sharing and permission revocation using different roles.

### Problem encountered

The development database initially contained only the admin account, so I needed separate accounts to verify the sharing workflow.

### Fix implemented

I added development-profile-only initializers for OWNER and VIEWER test accounts. The initializers use the existing `UserService` to validate passwords and hash them before storing account records.

I configured test-account provisioning to be disabled by default.

### Verification

I started the application with the appropriate development configuration and used separate accounts to test the sharing workflow.

**Security consideration:** Development test accounts must not be enabled or assigned predictable passwords in production. Credentials should be supplied securely through environment variables.

**Commit:** [3aba0e4](https://github.com/Srikala-kanduri/TrustVault/commit/3aba0e4) — development OWNER and VIEWER account initialization.

---

## 10. Manual Verification of Document Sharing and Revocation

**Date:** 22 October 2026
**Tools:** ChatGPT, VS Code, Spring Boot, React frontend, browser

### Task and AI assistance

I asked ChatGPT to help verify whether TrustVault enforced per-document permissions and removed a viewer's access after permission revocation.

### Test procedure

1. Signed in as the OWNER.
2. Uploaded a test document.
3. Granted READ permission to the VIEWER.
4. Signed in as the VIEWER and attempted to access the shared document.
5. Attempted an unauthorized deletion as the VIEWER.
6. Revoked the VIEWER's permission as the OWNER.
7. Retested document listing and direct download access.
8. Reviewed the admin audit dashboard.

### Verification results

- The VIEWER successfully downloaded the shared document while READ permission was active.
- The VIEWER's attempt to delete the document returned HTTP 403 Forbidden.
- After the OWNER revoked access, the document no longer appeared in the VIEWER's accessible document list.
- A direct download request after revocation returned HTTP 403 Forbidden.
- The admin audit dashboard displayed upload, permission grant, download, denied deletion, permission revocation, and denied download events.
- `.\mvnw.cmd clean test` completed with `BUILD SUCCESS`.
- `npm run build` completed successfully.

### Outcome

The manual tests demonstrated that the tested sharing and revocation workflow enforced the expected access restrictions.

### Limitations

These tests establish the observed authorization behavior, not the tamper-proof integrity of audit records against direct database modification. Revoking permission cannot recall a document that a viewer has already downloaded.

**Commit:** [4e0301c](https://github.com/Srikala-kanduri/TrustVault/commit/4e0301c) — sharing, revocation, authorization, and audit tests.

---

## 11. Development Account Configuration Troubleshooting

**Date:** 22 October 2026
**Tools:** ChatGPT, VS Code, Spring Boot, Maven Wrapper

### Task and AI assistance

I used ChatGPT to troubleshoot development account initialization and understand why account creation depended on environment variables and the active Spring profile.

### Problem encountered

The development OWNER and VIEWER initializers required their corresponding password environment variables. Account creation could fail when the variables were missing, when the development profile was not active, or when the initializers were disabled.

### Fix and configuration

I reviewed the development profile, account initializer properties, and password validation. Development account creation was configured to remain disabled by default, with credentials supplied through environment variables when explicitly enabling the development setup.

### Verification

The account workflow was checked through the manual document-sharing tests described in Section 10. The test suite was also run using the Maven Wrapper.

```bash
.\mvnw.cmd clean test
```

**Result:** `BUILD SUCCESS` was reported during the development session.

**Commit:** [3aba0e4](https://github.com/Srikala-kanduri/TrustVault/commit/3aba0e4) — development role account configuration.

---

## AI Assistance and Development Practices

ChatGPT was used to explain Spring Security, database-backed authentication, authorization rules, audit logging, repository queries, configuration, Git operations, and test failures.

I reviewed the suggested changes and used automated tests, build commands, and manual browser testing to evaluate the implementation. Test success was treated as evidence for the behaviors covered by those tests, not as proof that every security requirement had been satisfied.

The project still requires production deployment, persistent production storage, and completion of the external submission materials.

## 12. Review Corrections Found While Checking the Submission

**Date:** 25 October 2026
**Tools:** ChatGPT, Spring Boot, Maven Wrapper, React/Vite

These are concrete corrections made in the current working tree. They are not committed yet, so they do not have honest commit URLs. The historical links identify the closest earlier feature commits, not commits for these latest corrections; replace them with exact links after committing.

### Personal audit feed returned 404

- **Prompt:** Diagnose why the profile audit section shows no events for owner/viewer.
- **AI result:** The UI requested `/api/audit-logs`, but the route was missing from the running backend version.
- **What was wrong:** UI and backend were out of sync, and there was no integration test proving owner/viewer personal-feed behavior.
- **Correction:** Added a personal feed filtered by normalized actor email, plus owner/viewer isolation and unauthenticated-access integration tests.
- **Related history:** [3aba0e4](https://github.com/Srikala-kanduri/TrustVault/commit/3aba0e4); the route and tests are uncommitted.

### Denied document deletion was not consistently audited

- **Prompt:** Fix audit gaps from the requirements review.
- **AI result:** The owner-only deletion branch recorded a denial, but `requireReadAccess` could throw first when the caller lacked read access.
- **What was wrong:** That denied path produced no denial event.
- **Correction:** Safely record the denied deletion when read authorization throws, then rethrow; added a regression test checking the audit and unchanged document/file state.
- **Related history:** [4e0301c](https://github.com/Srikala-kanduri/TrustVault/commit/4e0301c); this edge-case fix is uncommitted.

### Upload role was enforced only by the interface

- **Prompt:** Check whether role behavior meets the requirement to define what each role can do.
- **AI result:** The UI hid upload for non-owners, but the upload service accepted any authenticated account.
- **What was wrong:** A direct API request could bypass the hidden button.
- **Correction:** The service now permits upload only for an `OWNER`, audits the denial, and tests that viewers cannot upload. Sidebar navigation uses the same role check.
- **Related history:** [901f1fe](https://github.com/Srikala-kanduri/TrustVault/commit/901f1fe); this role restriction is uncommitted.

### Download assertion had become stale

- **Prompt:** Run the backend suite after audit changes.
- **AI result:** The failing test expected the old generic download audit details.
- **What was wrong:** Current audit details include document title and original filename, but the test fixture did not set a title.
- **Correction:** Set the title and assert the actual event. That run had 81 tests; the latest clean suite, expanded with audit tamper cases, passed 86.
- **Related history:** [4e0301c](https://github.com/Srikala-kanduri/TrustVault/commit/4e0301c); fixture correction is uncommitted.

### Windows Maven wrapper failed before tests

- **Prompt:** Run backend tests after the earlier startup failures.
- **AI result:** The wrapper failed in PowerShell with “Cannot index into a null array” while reading a normal `.m2` directory.
- **What was wrong:** The wrapper assumed `Get-Item(...).Target[0]` exists for every directory.
- **Correction:** Check link type and target safely before resolving the Maven cache. Tests then ran successfully.
- **Related history:** [f972743](https://github.com/Srikala-kanduri/TrustVault/commit/f972743); wrapper fix is uncommitted.

## Remaining Work

The following items must be checked against the final submission:

- [x] Update README setup/role instructions; no public demo credentials are included.
- [x] Update `THREATS.md` with implemented controls and accepted risks.
- [x] Document API endpoints, authentication requirements, and role-based permissions in `trustvault/API.md`.
- [ ] Configure and test persistent production database and private file storage.
- [ ] Verify the deployed application and its environment configuration.
- [ ] Assess and document audit-log tamper resistance.
- [ ] Prepare the required five-minute demonstration video.
- [ ] Prepare the final project write-up, including completed features, unfinished work, commands, AI usage, and errors encountered.
- [ ] Commit the final uncommitted fixes and link those commits here before submission. The working-tree changes are not represented by the historical links above.
- [x] Latest clean backend suite: 86 tests passed on 25 October 2026; frontend production build passed on 25 October 2026.
