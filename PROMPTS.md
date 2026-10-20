# TrustVault — AI Usage Log

## Introduction

During the development of TrustVault, I used ChatGPT as a development assistant to understand technical issues, troubleshoot errors, improve configuration, and organize the project. I reviewed the suggestions, made changes in my code, and ran commands to verify the results.

This document records the main tasks where AI assistance was used, the issues encountered, and the steps taken to resolve them. I will continue updating this file as new features are implemented and tested.

## 1. Setting Up Git and `.gitignore`

**Date:** 20 October 2026

**Tool Used:** ChatGPT, VS Code, Git

**What I asked:**

I asked ChatGPT to check whether my `.gitignore` file was correctly configured for a project containing a Spring Boot backend and a React frontend.

**Problem encountered:**

While staging the project, Git displayed several LF-to-CRLF line-ending warnings. I also needed to ensure that generated files, dependency folders, and environment files were not accidentally committed.

**How I addressed it:**

I reviewed the root `.gitignore` file and checked the staged files using `git status`. The ignore rules cover Maven build output, Node.js dependencies, environment files, and common IDE-generated files.

**Verification:**

I inspected the staged file list and created the initial Git commit.

**Commit:** `f972743` — Initial TrustVault authentication foundation.

## 2. Fixing Whitespace in `pom.xml`

**Date:** 20 October 2026

**Tool Used:** ChatGPT, VS Code, Git

**What I asked:**

I shared my Maven `pom.xml` with ChatGPT and asked for help correcting its formatting and trailing whitespace.

**Problem encountered:**

Git reported trailing whitespace in the dependency section when I ran `git diff --cached --check`.

**How I addressed it:**

I reviewed the Maven configuration and corrected the formatting around the dependency declarations. I then staged the updated file again.

**Verification:**

The final result of `git diff --cached --check` should be recorded after confirming that the command completes without reporting whitespace errors.

**Commit:** To be added after the fix is verified and committed.

## 3. Troubleshooting the Missing Admin Password

**Date:** 20 October 2026

**Tool Used:** ChatGPT, Maven Wrapper

**What I asked:**

I shared the Maven test failure with ChatGPT and asked for help identifying why the Spring Boot application context was not loading.

**Problem encountered:**

The test failed because Spring could not resolve the `TRUSTVAULT_DEV_ADMIN_PASSWORD` environment variable used by the development admin configuration.

**How I addressed it:**

I examined the error message and updated the password property's configuration to allow an empty default when the property is not supplied. The development initializer still checks that a sufficiently long password is configured before creating the admin account.

**Verification:**

After updating the test configuration, I ran `mvnw.cmd test` and received `BUILD SUCCESS`.

**Commit:** To be added after the changes are committed.

## 4. Separating Test and Development Configuration

**Date:** 20 October 2026

**Tool Used:** ChatGPT, VS Code, Maven Wrapper

**What I asked:**

I asked ChatGPT how to configure Spring Boot tests so they would not depend on the development admin password.

**Problem encountered:**

The test application context was loading configuration that expected a development password, causing the test to fail before it could complete.

**How I addressed it:**

I configured the context test to use the `test` profile and added test-specific properties. The test configuration uses an in-memory H2 database, `create-drop` schema management, and disables development admin initialization.

**Verification:**

I ran `mvnw.cmd test` and confirmed that Maven reported `BUILD SUCCESS`.

**Commit:** To be added after the changes are committed.

## 5. Connecting the Project to GitHub

**Date:** 20 October 2026

**Tool Used:** ChatGPT, Git CLI, GitHub

**What I asked:**

I asked ChatGPT to guide me through creating a private GitHub repository and connecting it to my existing local project.

**Problem encountered:**

The local Git repository did not initially have a remote configured.

**How I addressed it:**

I created the GitHub repository, configured it as the `origin` remote, and pushed the local `main` branch.

**Verification:**

Git confirmed that the `main` branch was pushed successfully and set up to track `origin/main`.

**Commit:** `f972743` — Initial TrustVault authentication foundation.

## Future AI-Assisted Development Entries

As development continues, I will document AI assistance used for implementing and testing the core security features of TrustVault, including:

- Server-side document ownership and authorization checks.
- Granting and revoking document permissions.
- Tests for unauthorized document access.
- Audit logging and protection against unauthorized modification of audit records.

For each entry, I will record the actual prompt or a faithful summary, the tool used, the issue encountered, the changes I made, the verification results, and the relevant commit.

I will only mark a feature as implemented or tested after verifying it in the project. This log is intended to provide an honest account of how AI assistance was used during development and how I evaluated and applied its suggestions.
