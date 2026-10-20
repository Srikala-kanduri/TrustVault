# TrustVault — Threat Model and Security Considerations

## 1. Introduction

TrustVault is designed to provide secure document sharing with controlled access and an auditable history of important activities. Since documents may contain confidential information, the application must prevent unauthorized access, modification, and deletion.

This document identifies the main security threats relevant to TrustVault, the protections planned to address them, and the implementation status of those protections.

## 2. Assets to Protect

The main assets in TrustVault are:

- **User accounts:** Email addresses, password hashes, roles, and authentication sessions.
- **Documents:** Uploaded files and their associated metadata.
- **Document permissions:** Rules defining which users can read or modify individual documents.
- **Audit records:** Records of document activity and permission changes.
- **Configuration and credentials:** Database connection details, passwords, and deployment secrets.

## 3. User Roles

TrustVault defines three application roles:

- **ADMIN:** Intended to support administrative operations and activity monitoring.
- **OWNER:** Intended to upload and manage owned documents and grant or revoke document permissions.
- **VIEWER:** Intended to access documents only when the applicable permission allows it.

Application roles alone must not grant access to every document. The backend must also verify the user's permission for the specific document and requested operation.

## 4. Threats and Mitigations

### 4.1 Unauthorized Authentication

**Threat:** An attacker may attempt to access TrustVault using incorrect or stolen credentials.

**Planned protections:**
- Store passwords using a secure password-hashing algorithm.
- Require authentication for protected endpoints.
- Return generic login failure messages.
- Avoid exposing passwords or sensitive authentication data in logs.

**Current status:** BCrypt password hashing, a Spring Security authentication provider, and authentication-required request rules have been implemented. Additional authentication abuse protections still require review and testing.

### 4.2 Unauthorized Document Access

**Threat:** A user may attempt to access another user's document by changing a document ID in a URL or API request.

**Planned protections:**
- Check authorization on the server for every protected document operation.
- Verify document ownership or an explicit permission before allowing access.
- Apply authorization checks to viewing, downloading, editing, and deleting documents.
- Return an appropriate denial response when access is not permitted.

**Current status:** Document-specific authorization is a required security feature and must be verified once document management is implemented.

### 4.3 Privilege Escalation

**Threat:** A viewer may attempt to perform owner or administrator operations by modifying requests or parameters.

**Planned protections:**
- Enforce role checks on the backend.
- Validate ownership and document permissions separately from general role checks.
- Prevent users from changing their own role through ordinary document operations.

**Current status:** The `ADMIN`, `OWNER`, and `VIEWER` roles and the initial authentication foundation exist. Complete role-based authorization tests remain necessary.

### 4.4 Permission Revocation Abuse or Delay

**Threat:** A user whose access has been revoked may continue accessing a document through an old link or an authorization decision based on outdated permissions.

**Planned protections:**
- Check current permissions for every new protected request.
- Apply revocation changes to subsequent access attempts.
- Avoid exposing permanent, unrestricted document URLs.
- Test that revoked users cannot subsequently view or download protected documents.

**Current status:** Pending implementation and verification of document permissions and protected file access.

### 4.5 Unauthorized Document Modification or Deletion

**Threat:** An unauthorized user may attempt to alter or delete a document.

**Planned protections:**
- Require appropriate write or delete permission on the backend.
- Verify document ownership and permissions before making changes.
- Record successful changes and relevant denied attempts in the audit history.

**Current status:** Pending document-management implementation and authorization testing.

### 4.6 Audit Log Tampering

**Threat:** A user or attacker may attempt to alter or delete audit records to conceal activity.

**Planned protections:**
- Restrict audit record creation and access to trusted backend components.
- Prevent ordinary users from editing or deleting audit events.
- Record important events with the actor, action, target, timestamp, and outcome where applicable.
- Evaluate stronger tamper-evidence mechanisms, such as hash chaining, for audit integrity.

**Current status:** Audit logging and tamper-evidence mechanisms must be implemented and tested before they can be considered effective.

### 4.7 Cross-Site Request Forgery (CSRF)

**Threat:** A malicious website may attempt to make a user's authenticated browser submit an unwanted request.

**Planned protections:**
- Keep CSRF protection enabled for session-based authentication.
- Require valid CSRF tokens for state-changing requests.
- Configure cookie and browser security settings appropriately for deployment.

**Current status:** Spring Security's default CSRF protection is retained. The frontend retrieves a CSRF token for login integration. All state-changing endpoints and deployment settings still require testing.

### 4.8 Cross-Origin Request Abuse

**Threat:** An untrusted website may attempt to interact with the backend from a browser.

**Planned protections:**
- Restrict allowed origins to the intended frontend.
- Avoid permissive credentialed CORS configurations.
- Review allowed headers and HTTP methods.

**Current status:** CORS is configured for the local frontend origin `http://localhost:5173`. Production origins must be configured before deployment.

### 4.9 Credential and Configuration Exposure

**Threat:** Passwords, database credentials, or other secrets may be exposed through source control or logs.

**Planned protections:**
- Use environment variables or a managed secret store for credentials.
- Keep real environment files out of Git.
- Commit only example configuration containing placeholder values.
- Avoid logging passwords, session identifiers, or sensitive document contents.

**Current status:** The root `.gitignore` excludes environment files and allows `.env.example`. Repository contents and deployment configuration must still be checked for accidentally committed secrets.

### 4.10 Unsafe File Uploads

**Threat:** An attacker may upload oversized, unexpected, or malicious files.

**Planned protections:**
- Enforce file size limits.
- Validate file types and content rather than trusting filenames or browser-supplied MIME types alone.
- Generate server-controlled storage names.
- Store uploaded files outside publicly accessible static directories.
- Prevent path traversal and unauthorized file retrieval.

**Current status:** Pending file-upload implementation and security testing.

## 5. Security Testing Plan

The following tests are required as the corresponding features are implemented:

1. Unauthenticated users cannot access protected endpoints.
2. A user cannot access another user's document without permission.
3. A viewer cannot edit or delete documents without the required permission.
4. Revoked access prevents subsequent protected requests.
5. Invalid permission changes are rejected.
6. Important successful and denied operations generate appropriate audit events.
7. Ordinary users cannot alter or delete audit records.
8. Invalid and oversized file uploads are rejected.
9. CSRF protection works for state-changing requests.
10. Credentials and sensitive information do not appear in source control or application logs.

Test results must be recorded only after the corresponding tests have actually been run.

## 6. Remaining Risks and Limitations

TrustVault is still under development. The authentication foundation does not by itself establish that document authorization, file storage, permission revocation, or audit integrity is secure.

Before deployment, the application requires implementation and verification of the remaining security controls, production database configuration, secure file storage, secret management, and end-to-end security tests.

## 7. Review and Maintenance

This threat model will be updated as new features are implemented, security tests reveal issues, and deployment decisions are finalized. Each mitigation should be considered complete only after its implementation has been reviewed and its relevant tests have passed.
