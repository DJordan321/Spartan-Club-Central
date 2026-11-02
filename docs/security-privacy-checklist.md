# Spartan Club Central: Security and Privacy Checklist

## Security and Privacy Requirements

| ID | Requirement | Category | Verification Method |
|----|-------------|----------|---------------------|
| SPR-01 | The system shall require a club leader to log in with a registered email and password before allowing access to the dashboard or any action that creates, edits, or deactivates a listing. | Authentication | Open dashboard and edit URLs while logged out and confirm each redirects to the login page. |
| SPR-02 | The system shall store each password only as a salted hash (for example BCrypt) and shall never store, log, or display a plain-text password. | Password handling / Secure data storage | Inspect the leader table to confirm only hashes are saved; unit test AuthService hashing; code review. |
| SPR-03 | The system shall allow a club leader to edit or deactivate only the listing that belongs to that leader's own club. | Authorization | Log in as Leader A and try to edit or deactivate Club B by changing the ID in the request; expect it to be denied. |
| SPR-04 | The system shall show a student's name, email, and message only to the leader of the club the request was sent to, and to no other user. | Privacy / Role-based access control | Test with two leader accounts: each sees only their own club's requests; unit test the RequestService access check. |
| SPR-05 | The system shall validate all submitted input on the server, rejecting missing required fields, invalid email formats, and over-length text, and shall use parameterized queries for all database access. | Input validation | Unit tests for RequestValidator with invalid input; enter SQL and script text in search and form fields and confirm nothing is executed. |
| SPR-06 | The system shall show generic error messages (for example "Invalid email or password") and shall not display stack traces, SQL errors, or whether an email address is registered. | Safe error messages | Wrong password and unknown email return the same message; force a server error and confirm no technical details appear. |
| SPR-07 | The system shall end a leader's session on logout and after 30 minutes of inactivity, and shall issue a new session ID at login. | Session management | Stay idle past the timeout and confirm redirect to login; log out, press Back, and confirm the dashboard is not shown. |
| SPR-08 | The system shall log login attempts, password resets, and listing changes with a timestamp and account ID, and shall not write passwords or student contact information to the logs. | Logging | Perform each action in a test run, then inspect the log file for the entries and confirm no sensitive data appears. |

## Input Validation Checklist

| Input Field | Validation Rule | Error Message |
|-------------|-----------------|---------------|
| Email | Must follow a valid email format. | Enter a valid email address. |
| Password | Must meet minimum length and security requirements. | Password does not meet the security requirements. |
| Student Name | Cannot be blank; must not exceed the maximum character limit. | Enter a valid student name. |
| Club Name | Cannot be blank; must not exceed the maximum character limit. | Club name is required. |
| Club Description | Must not exceed the maximum length and must be safely processed. | Club description exceeds the allowed length. |
| Club Category | Must match an approved category in the system. | Select a valid club category. |
| Join/Contact Request Message | Cannot be blank; must not exceed the maximum character limit. | Enter a valid message within the allowed length. |
| Search Field | Must not exceed the maximum length; input must be safely processed. | Search text exceeds the allowed length. |
| Account Status | Must be an approved value, such as active or inactive. | Select a valid account status. |

## Threat and Vulnerability Review

| Risk | How It Could Affect the System | Mitigation |
|------|-------------------------------|------------|
| Unauthorized access | An attacker could access leader accounts, modify listings, or view private join requests. | Secure login, role-based access control, and a permission check on every protected action. |
| Invalid input | Malicious input could cause errors or attacks such as SQL injection. | Validate all input on the server and use parameterized queries. |
| Sensitive data exposure | Student names, emails, and messages could be exposed to unauthorized users. | Restrict access to personal information and use HTTPS. |
| Weak password handling | Attackers could guess weak passwords or obtain stored credentials. | Salted hashes (bcrypt), password requirements, and limits on repeated login attempts. |
| Unsafe file upload | Malicious files could harm the system (only if uploads are added). | Restrict file types and sizes, validate content, and store files securely. |

## Security Test Cases

| ID | Test | Action | Expected Result |
|----|------|--------|-----------------|
| ST-01 | Unauthenticated dashboard access (SPR-01) | While logged out, open the dashboard URL and a listing edit URL. | Redirect to the login page; no dashboard data shown. |
| ST-02 | Password stored as a salted hash (SPR-02) | Register a test leader with password "TestPass123", then inspect the leader table. | The password column holds a BCrypt hash, not the plain text; it is not in the logs. |
| ST-03 | Editing another club's listing (SPR-03) | Log in as Leader A, change the club ID in the edit request to Club B's, and submit. | Access denied; Club B's listing is unchanged. |
| ST-04 | Request privacy between clubs (SPR-04) | Submit a request to Club A; view requests as Club A's leader and as Club B's leader; view the public Club A profile. | Only Club A's leader sees the student's details. |
| ST-05 | SQL injection in the search field (SPR-05) | Enter `' OR '1'='1'; --` in the search box. | Treated as a normal search term; no extra records, no changes, no SQL error. |
| ST-06 | Script input in the request form (SPR-05) | Submit a request containing `<script>alert('test')</script>` in the message, then view it as the leader. | Rejected or shown as plain text; no script runs. |
| ST-07 | Invalid email format (SPR-05) | Submit the form with the email "student@". | Rejected with "Enter a valid email address."; nothing saved. |
| ST-08 | Over-length message (SPR-05) | Submit a message longer than the maximum length. | Rejected with "Enter a valid message within the allowed length."; nothing saved. |
