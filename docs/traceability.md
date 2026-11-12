# Requirements Traceability (as built)

Every requirement, the code that meets it, the automated tests that check it, and how to see it by hand. Automated tests live under `src/test/java/com/spartanclubcentral/Spartan/Club/` and run with `./mvnw test` (134 passing, plus 1 opt-in MySQL check). Manual steps refer to the tables in `docs/run-locally.md`.

## Functional requirements

| ID | Requirement | Code | Automated tests | Status |
|---|---|---|---|---|
| FR-01 | Keyword search (name and description) | `ClubRepository.search`, `ClubService.search`, `ClubController`, `directory.html` | `ClubRepositoryTest` (TC-01, TC-02, TC-03), `ClubControllerTest`, `MySqlSampleDataTest` | Met |
| FR-02 | Filter by category, combined with keyword | same + `index.html` category cards | `ClubRepositoryTest` (TC-04, TC-05), `ClubControllerTest` | Met |
| FR-03 | Join request without login | `RequestController`, `RequestService.submit`, `RequestValidator`, `club.html` | `RequestControllerTest` (TC-07, TC-21), `RequestServiceTest`, `RequestValidatorTest` | Met |
| FR-04 | Login for the dashboard | `SecurityConfig`, `AuthService`, `AuthController`, `DashboardController`, `AdminSeeder` | `AuthenticationFlowTest` (TC-17), `RegistrationControllerTest` (TC-08), `SessionFixationTest` | Met |
| FR-05 | Club profile page | `ClubController.profile`, `club.html` | `ClubControllerTest` (TC-06) | Met |
| FR-06 | Social links on the profile | `Club` URL fields, `club.html` | `ClubControllerTest` (TC-06), `MySqlSampleDataTest` | Met |
| FR-07 | Requests emailed to the club | `NotificationService.notifyClub`, `RequestService` | `NotificationServiceTest`, `RequestControllerTest` (TC-09, TC-10), `RequestServiceTest` | Met |
| FR-08 | Leaders create a club | `ClubService.createClub`, `DashboardController`, `club-form.html` | `DashboardControllerTest` (TC-11) | Met |
| FR-09 | Leaders edit their club | `ClubService.updateClub`, `ClubValidator` | `DashboardControllerTest` (TC-12), `ClubValidatorTest` | Met |
| FR-10 | Store all club data | `database/schema.sql`, `model/*`, `ddl-auto=validate` | `MySqlSampleDataTest` (schema matches entities), `ClubRepositoryTest` | Met |
| FR-11 | Reset a forgotten password | `AuthService.requestReset/resetPassword`, `AuthController`, `AdminService.sendResetLink` | `PasswordResetTest` (TC-15, TC-16), `AdminControllerTest` | Met |
| FR-12 | Deactivate or delete a listing | `ClubService.setActive/deleteClub`, `AdminService.setClubActive`, `confirm-delete.html` | `DashboardControllerTest` (TC-13, TC-14), `AdminControllerTest`, `ClubRepositoryTest` | Met |

## Non-functional requirements

| ID | Requirement | How it's met | Evidence | Status |
|---|---|---|---|---|
| NFR-01 | Hashed passwords | `PasswordHasher` (BCrypt 12) | `RegistrationControllerTest` (TC-18), `AdminSeederTest`, `DemoLeaderSeederTest`; Workbench `SELECT password_hash FROM club_leader` | Met |
| NFR-02 | Student info only for that club's leader | Inbox behind `requireOwnedClub`; public and admin pages never get request data | `ClubControllerTest`, `DashboardControllerTest` (TC-20), `AdminControllerTest` | Met |
| NFR-03 | Filter by category within 3 clicks | Homepage category cards (1 click) | `ClubControllerTest.homepage`; manual check | Met |
| NFR-04 | Results < 2 s for 100 users | Indexed single query; `scripts/LoadCheck.java` | 100 users × 5 searches on a 4-core machine: median 282 ms, 95th percentile 706 ms, 0 failures | Met (re-run on the demo laptop) |
| NFR-05 | 99% uptime | Deferred: the system runs on a demo laptop (conflict C1) | n/a | Deferred |
| NFR-06 | WCAG 2.1 AA, screen readers | Labels, error summaries linked with `aria-describedby`, skip link, contrast ≥ 4.5:1 | axe-core scan of every page: 0 violations | Met (add a manual screen-reader pass) |
| NFR-07 | Documented MVC structure | Layered packages, class comments, `docs/architecture.md` | Code review | Met |
| NFR-08 | Browsers and 320 to 1920 px | Mobile-first CSS, scrolling tables | Every page measured at 320 px: no sideways scrolling | Met in Chromium (check Firefox and Safari by hand) |

## Security and privacy requirements

| ID | Requirement | How it's met | Automated tests | Status |
|---|---|---|---|---|
| SPR-01 | Login before dashboard actions | `SecurityConfig` roles | `AuthenticationFlowTest` (TC-17), `DashboardControllerTest`, `AdminControllerTest` | Met |
| SPR-02 | BCrypt only, never plain text | `PasswordHasher`; passwords cleared before re-rendering | `RegistrationControllerTest`, `PasswordResetTest` | Met |
| SPR-03 | Edit only your own club | `ClubService.requireOwnedClub` | `DashboardControllerTest` (TC-19: 7 actions denied and logged), `ClubServiceTest` | Met |
| SPR-04 | See only your own requests | `RequestService.getRequestsForClub` | `DashboardControllerTest` (TC-20) | Met |
| SPR-05 | Server validation and safe queries | Validators + `FieldLimits`; Spring Data bound parameters; `th:text`; http(s)-only links | Validator tests, `ClubRepositoryTest` (SQL text), escaping tests (TC-21) | Met |
| SPR-06 | Generic errors | Error pages, `GlobalExceptionHandler`, one login message, one "forgot password" answer | `ClubControllerTest`, `AuthenticationFlowTest`, `PasswordResetTest` | Met |
| SPR-07 | Session timeout, new ID at login | 30-min timeout, `changeSessionId` | `SessionFixationTest` (real server and cookies) | Met |
| SPR-08 | Security logging without personal data | `AuditLogger`, `SecurityEventListener`; no email/password fields in `audit_log` | `AuthenticationFlowTest`, `DashboardControllerTest`, `PasswordResetTest`, `AdminControllerTest` | Met |

## Original failing test cases

| Test | Problem | Fixed in | Check |
|---|---|---|---|
| TC-02 | Search ignored descriptions | Phase 1 (`ClubRepository.search`) | Search "robotics" finds Engineering Builders Society |
| TC-05 | Category ignored with a keyword | Phase 1 (one combined query) | "club" + Academic shows only 5 Academic clubs |
| TC-06 | Blank contact info and links | Phase 1/2 (explicit column mapping, `ddl-auto=validate`) | Every profile shows contact and links |
| TC-07 | Join request gave HTTP 500, saved nothing | Phase 3 (save first, email can't throw) | Request confirmed even with Mailpit stopped (saved as FAILED) |
| TC-08 | Registration failed | Phase 4 (60-char hash column, nullable club, CSRF) | Register a leader, then log in |
